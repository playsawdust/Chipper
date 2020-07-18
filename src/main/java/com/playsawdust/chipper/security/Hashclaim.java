/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.security;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.math.BigIntegerMath;

import com.playsawdust.chipper.toolbox.lipstick.SharedRandom;

/**
 * Hashcash-inspired proof-of-work algorithm with binary proofs and a flexible
 * hash function.
 */
public class Hashclaim {

	public enum HashclaimFunction {
		@Deprecated
		MD5("MD5", Hashing.md5()),
		@Deprecated
		SHA1("SHA-1", Hashing.sha1()),
		SHA2("SHA-2", Hashing.sha512()),
		SHA3("SHA-3", MoreHashing.sha3_512()),
		BLAKE2B("BLAKE2b", MoreHashing.blake2b_512()),
		;
		private final String name;
		private final HashFunction underlying;
		private HashclaimFunction(String name, HashFunction underlying) {
			this.name = name;
			this.underlying = underlying;
		}
		@Override
		public String toString() {
			return name;
		}
	}

	public static final int DEFAULT_PREIMAGE = 22;
	public static final HashclaimFunction DEFAULT_HASH = HashclaimFunction.BLAKE2B;

	private static final byte[] MAGIC = "HASHCLAIMv2".getBytes(Charsets.US_ASCII);

	/**
	 * Generates a Hashclaim proof with the default parameters and hash
	 * function.
	 * @param claim the resource being claimed, such as
	 * 		USERNAME@playsawdust.com
	 * @return a proof that satisfies the requirements set by the default
	 * 		parameters
	 */
	public static byte[] generateProof(String claim) {
		return generateProof(DEFAULT_HASH, DEFAULT_PREIMAGE, new Date().getTime(), claim);
	}

	/**
	 * Generates a Hashclaim proof with the given parameters.
	 * @param func the hash function to be used
	 * @param neededPreimage the number of preimage (leading zero) bits needed
	 * 		for a valid hash (maximum 258, minimum 3)
	 * @param timestamp the time this hash begun being generated
	 * @param claim the resource being claimed, such as
	 * 		USERNAME@playsawdust.com
	 * @return a proof that satisfies these parameters with the given hash
	 * 		function
	 */
	public static byte[] generateProof(HashclaimFunction func, int neededPreimage, long timestamp, String claim) {
		if (neededPreimage < 3) throw new IllegalArgumentException("At least 3 preimage bits must be requested");
		if (neededPreimage > 258) throw new IllegalArgumentException("At most 258 preimage bits may be requested");
		if (func.underlying.bits() < neededPreimage) throw new IllegalArgumentException("Cannot generate a "+neededPreimage+" bit preimage of a "+func.underlying.bits()+" bit hash");

		byte[] rand = SharedRandom.bytes(16);
		ByteArrayDataOutput claimBado = ByteStreams.newDataOutput();
		claimBado.writeUTF(claim);
		byte[] claimBytes = claimBado.toByteArray();
		long i = 0;
		while (true) {
			ByteArrayDataOutput bado = ByteStreams.newDataOutput(MAGIC.length+1+8+claimBytes.length+rand.length+8);
			bado.write(MAGIC);
			bado.writeByte(neededPreimage-3);
			bado.writeUTF(func.name());
			bado.writeLong(timestamp);
			bado.write(claimBytes);
			bado.write(rand);
			bado.writeLong(i);
			byte[] proof = bado.toByteArray();
			if (verifyPreimage(func, neededPreimage, proof)) return proof;
			i++;
		}
	}

	/**
	 * Verifies a Hashclaim proof with the default parameters and hash function.
	 * @param proof the proof
	 * @return the resource this proof is attempting to claim if valid, or
	 * 		null if the proof is invalid
	 */
	public static String verifyProof(byte[] proof) {
		return verifyProof(DEFAULT_HASH, DEFAULT_PREIMAGE, System.currentTimeMillis(), proof);
	}

	/**
	 * Verifies a Hashclaim proof.
	 * @param neededPreimage the exact number of preimage (leading zero) bits
	 * 		needed for a valid hash (maximum 258, recommended 22)
	 * @param timestamp the time that this hash is being verified, must be
	 * 		within 4 hours of the proof's time to be valid
	 * @param proof the proof
	 * @return the resource this proof is attempting to claim if valid, or
	 * 		null if the proof is invalid
	 */
	public static String verifyProof(HashclaimFunction func, int neededPreimage, long timestamp, byte[] proof) {
		if (neededPreimage < 2) throw new IllegalArgumentException("At least 2 preimage bits must be requested");
		if (neededPreimage > 258) throw new IllegalArgumentException("At most 258 preimage bits may be requested");
		if (func.underlying.bits() < neededPreimage) throw new IllegalArgumentException("Cannot generate a "+neededPreimage+" bit preimage of a "+func.underlying.bits()+" bit hash");
		try {
			if (verifyPreimage(func, neededPreimage, proof)) {
				ByteArrayDataInput badi = ByteStreams.newDataInput(proof);
				byte[] magicBuf = new byte[MAGIC.length];
				badi.readFully(magicBuf);
				if (!Arrays.equals(MAGIC, magicBuf)) return null;
				if (badi.readUnsignedByte()+3 != neededPreimage) return null;
				long claimTimestamp = badi.readLong();
				if (Math.abs(timestamp-claimTimestamp) > TimeUnit.HOURS.toMillis(4)) return null;
				String claim = badi.readUTF();
				return claim;
			} else {
				return null;
			}
		} catch (IllegalStateException ise) {
			return null;
		}
	}

	private static boolean verifyPreimage(HashclaimFunction func, int neededPreimage, byte[] proof) {
		byte[] hash = func.underlying.hashBytes(proof).asBytes();
		int preimage = 0;
		for (int j = 0; j < hash.length; j++) {
			if (hash[j] == 0) {
				preimage += 8;
			} else {
				preimage += Integer.numberOfLeadingZeros(hash[j]&0xFF)-24;
				break;
			}
		}
		return preimage == neededPreimage;
	}

	public static void main(String[] args) {
		int trials = 80;
		int threads = 8;
		AtomicLongArray times = new AtomicLongArray(trials);
		AtomicInteger trial = new AtomicInteger(0);
		AtomicInteger completedTrials = new AtomicInteger(0);
		if ((trials % threads) != 0) {
			System.err.println("Trials is not divisible by threads");
			return;
		}
		System.out.println("Performing "+trials+" trials in "+threads+" threads... ("+(trials/threads)+" trials per thread)");
		Object lock = new Object();
		for (int t = 0; t < threads; t++) {
			new Thread(() -> {
				for (int i = 0; i < trials/threads; i++) {
					int id = trial.getAndIncrement();
					Stopwatch sw = Stopwatch.createStarted();
					generateProof("una@playsawdust.com");
					sw.stop();
					times.set(id, sw.elapsed(TimeUnit.NANOSECONDS));
					completedTrials.getAndIncrement();
					synchronized (lock) {
						lock.notify();
					}
				}
			}).start();
		}
		while (completedTrials.get() < trials) {
			try {
				synchronized (lock) {
					lock.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long comp = completedTrials.get();
			System.out.println(comp+"/"+trials+" ("+(int)(((double)comp/(double)trials)*100)+"%)");
		}
		long accum = 0;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for (int i = 0; i < trials; i++) {
			long time = times.get(i);
			accum += time;
			min = Math.min(min, time);
			max = Math.max(max, time);
		}
		long avg = accum/trials;
		BigInteger dev = BigInteger.ZERO;
		for (int i = 0; i < trials; i++) {
			long time = times.get(i);
			long d = Math.abs(time-avg);
			dev = dev.add(new BigInteger(Long.toString(d)).pow(2));
		}
		dev = BigIntegerMath.sqrt(dev.divide(new BigInteger(Long.toString(trials))), RoundingMode.HALF_EVEN);
		System.out.println("== "+DEFAULT_PREIMAGE+" bits of preimage, "+DEFAULT_HASH.toString()+" ==");
		System.out.println("min: "+durToStr(min)+", avg: "+durToStr(avg)+", max: "+durToStr(max)+", dev: "+durToStr(dev.longValue()));
	}

	private static String durToStr(long ns) {
		SettableTicker st = new SettableTicker();
		Stopwatch sw = Stopwatch.createStarted(st);
		st.setValue(ns);
		sw.stop();
		return sw.toString();
	}

	private static class SettableTicker extends Ticker {

		private long value;

		public void setValue(long value) {
			this.value = value;
		}

		@Override
		public long read() {
			return value;
		}

	}

}

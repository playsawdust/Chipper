/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network;

import static org.junit.Assert.*;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.math.LongMath;
import com.playsawdust.chipper.network.Marshaller;
import com.playsawdust.chipper.network.Unmarshaller;

public class MarshallerTest {

	private Marshaller marshaller() {
		return new Marshaller(ByteBuffer.allocate(8192));
	}

	@Test
	public void testBitPartial() {
		Marshaller m = marshaller();
		m.writeBit(true);
		m.writeBit(true);
		m.writeBit(false);
		ByteBuffer buf = m.finish();
		assertEquals(1, buf.remaining());
		Unmarshaller u = new Unmarshaller(buf);
		assertTrue(u.readBit());
		assertTrue(u.readBit());
		assertFalse(u.readBit());
	}

	@Test
	public void testBitTwoByte() {
		Marshaller m = marshaller();
		m.writeBit(true);
		m.writeBit(true);
		m.writeBit(false);
		m.writeBit(false);
		m.writeBit(true);
		m.writeBit(false);
		m.writeBit(true);
		m.writeBit(false);

		m.writeBit(true);
		m.writeBit(true);
		m.writeBit(true);
		ByteBuffer buf = m.finish();
		assertEquals(2, buf.remaining());
		Unmarshaller u = new Unmarshaller(buf);
		assertTrue(u.readBit());
		assertTrue(u.readBit());
		assertFalse(u.readBit());
		assertFalse(u.readBit());
		assertTrue(u.readBit());
		assertFalse(u.readBit());
		assertTrue(u.readBit());
		assertFalse(u.readBit());

		assertTrue(u.readBit());
		assertTrue(u.readBit());
		assertTrue(u.readBit());
	}

	@Test
	public void testBitSkip() {
		Marshaller m = marshaller();
		m.writeBit(true);
		m.writeBit(true);
		m.writeBit(false);

		m.writeI8(184);

		m.writeBit(true);
		m.writeBit(true);
		m.writeBit(true);
		ByteBuffer buf = m.finish();
		assertEquals(3, buf.remaining());
		Unmarshaller u = new Unmarshaller(buf);
		assertTrue(u.readBit());
		assertTrue(u.readBit());
		assertFalse(u.readBit());

		assertEquals(184, u.readUI8());

		assertTrue(u.readBit());
		assertTrue(u.readBit());
		assertTrue(u.readBit());
	}

	private long third(long min, long max) {
		return min+((max-min)/3);
	}

	private long half(long min, long max) {
		return min+((max-min)/2);
	}

	private long twoThirds(long min, long max) {
		return min+(((max-min)/3)*2);
	}

	private void test(BiConsumer<Marshaller, Number> write, Function<Unmarshaller, Number> signedRead,
			Function<Unmarshaller, Number> unsignedRead, int bitSize) {

		long unsignedMin = 0;
		long unsignedMax = LongMath.pow(2, bitSize)-1;

		long unsignedThird = third(unsignedMin, unsignedMax);
		long unsignedHalf = half(unsignedMin, unsignedMax);
		long unsignedTwoThirds = twoThirds(unsignedMin, unsignedMax);

		long signedMin = -LongMath.pow(2, bitSize-1);
		long signedMax = LongMath.pow(2, bitSize-1)-1;

		long signedThird = third(signedMin, signedMax);
		long signedHalf = half(signedMin, signedMax);
		long signedTwoThirds = twoThirds(signedMin, signedMax);

		if (unsignedRead != null) {
			test(write, unsignedRead, unsignedMin, unsignedThird, unsignedHalf, unsignedTwoThirds, unsignedMax, 0, 1);
		}
		test(write, signedRead, signedMin, signedThird, signedHalf, signedTwoThirds, signedMax, -1, 0, 1);
	}

	private void test(BiConsumer<Marshaller, Number> write, Function<Unmarshaller, Number> read, long... testValues) {
		Marshaller m = marshaller();
		for (long l : testValues) {
			write.accept(m, l);
		}

		Unmarshaller u = new Unmarshaller(m.finish());
		for (long l : testValues) {
			assertEquals(l, read.apply(u).longValue());
		}

		m = new Marshaller(ByteBuffer.allocate(16));
		int successfulWrites = 0;
		try {
			for (int i = 0; i < 18; i++) {
				write.accept(m, testValues[0]);
				successfulWrites++;
			}
			fail("BufferOverflowException not thrown");
		} catch (BufferOverflowException e) {}
		u = new Unmarshaller(m.finish());
		int successfulReads = 0;
		try {
			for (int i = 0; i < 18; i++) {
				assertEquals(testValues[0], read.apply(u).longValue());
				successfulReads++;
			}
			fail("BufferUnderflowException not thrown");
		} catch (BufferUnderflowException e) {}
		assertEquals(successfulWrites, successfulReads);
	}

	private BiConsumer<Marshaller, Number> squernch(BiConsumer<Marshaller, Integer> a) {
		return (m, n) -> a.accept(m, n.intValue());
	}

	private BiConsumer<Marshaller, Number> squencho(BiConsumer<Marshaller, Long> a) {
		return (m, n) -> a.accept(m, n.longValue());
	}

	@Test
	public void testI8() {
		test(squernch(Marshaller::writeI8), Unmarshaller::readI8, Unmarshaller::readUI8, 8);
	}

	@Test
	public void testI16() {
		test(squernch(Marshaller::writeI16), Unmarshaller::readI16, Unmarshaller::readUI16, 16);
	}

	@Test
	public void testI24() {
		test(squernch(Marshaller::writeI24), Unmarshaller::readI24, Unmarshaller::readUI24, 24);
	}

	@Test
	public void testI32() {
		test(squencho(Marshaller::writeI32), Unmarshaller::readI32, Unmarshaller::readUI32, 32);
	}

	@Test
	public void testI40() {
		test(squencho(Marshaller::writeI40), Unmarshaller::readI40, Unmarshaller::readUI40, 40);
	}

	@Test
	public void testI48() {
		test(squencho(Marshaller::writeI48), Unmarshaller::readI48, Unmarshaller::readUI48, 48);
	}

	@Test
	public void testI56() {
		test(squencho(Marshaller::writeI56), Unmarshaller::readI56, Unmarshaller::readUI56, 56);
	}

	@Test
	public void testI64() {
		test(squencho(Marshaller::writeI64), Unmarshaller::readI64, null, 64);
	}

	@Test
	public void testV32() {
		test(squernch(Marshaller::writeIVar32), Unmarshaller::readIVar32, null, 32);
	}

	@Test
	public void testV64() {
		test(squencho(Marshaller::writeIVar64), Unmarshaller::readIVar64, null, 64);
	}

	@Test
	public void testV64MinValue() {
		test(squencho(Marshaller::writeIVar64), Unmarshaller::readIVar64, Long.MIN_VALUE);
	}

	@Test
	public void testF16() {
		test(squencho(Marshaller::writeF16), Unmarshaller::readF16, -1, 0, 1, -4096, 4096, 32768, -32768);
	}
}

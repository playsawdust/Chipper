/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.security;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.common.hash.HashFunction;
import com.google.common.hash._$ChipperToolboxHashingHack;

public final class MoreHashing {

	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			try {
				System.err.println("MoreHashing warning: Last-minute installing BouncyCastle provider. You should do this yourself in application init!");
				Security.addProvider(new BouncyCastleProvider());
			} catch (Throwable t) {
				System.err.println("MoreHashing warning: Unable to last-minute install BouncyCastle provider. HashFunctions returned by MoreHashing probably won't work; install the BouncyCastle provider in application init!");
			}
		}
	}

	private static class Sha3224Holder {
		private static final HashFunction SHA3_224 = _$ChipperToolboxHashingHack.newMessageDigest("SHA3-224", "MoreHashing.sha3_224()");
	}
	private static class Sha3256Holder {
		private static final HashFunction SHA3_256 = _$ChipperToolboxHashingHack.newMessageDigest("SHA3-256", "MoreHashing.sha3_256()");
	}
	private static class Sha3384Holder {
		private static final HashFunction SHA3_384 = _$ChipperToolboxHashingHack.newMessageDigest("SHA3-384", "MoreHashing.sha3_384()");
	}
	private static class Sha3512Holder {
		private static final HashFunction SHA3_512 = _$ChipperToolboxHashingHack.newMessageDigest("SHA3-512", "MoreHashing.sha3_512()");
	}
	private static class Blake2B160Holder {
		private static final HashFunction BLAKE2B_160 = _$ChipperToolboxHashingHack.newMessageDigest("BLAKE2B-160", "MoreHashing.blake2b_160()");
	}
	private static class Blake2B256Holder {
		private static final HashFunction BLAKE2B_256 = _$ChipperToolboxHashingHack.newMessageDigest("BLAKE2B-256", "MoreHashing.blake2b_256()");
	}
	private static class Blake2B384Holder {
		private static final HashFunction BLAKE2B_384 = _$ChipperToolboxHashingHack.newMessageDigest("BLAKE2B-384", "MoreHashing.blake2b_384()");
	}
	private static class Blake2B512Holder {
		private static final HashFunction BLAKE2B_512 = _$ChipperToolboxHashingHack.newMessageDigest("BLAKE2B-512", "MoreHashing.blake2b_512()");
	}

	public static HashFunction sha3_224() {
		return Sha3224Holder.SHA3_224;
	}
	public static HashFunction sha3_256() {
		return Sha3256Holder.SHA3_256;
	}
	public static HashFunction sha3_384() {
		return Sha3384Holder.SHA3_384;
	}
	public static HashFunction sha3_512() {
		return Sha3512Holder.SHA3_512;
	}

	public static HashFunction blake2b_160() {
		return Blake2B160Holder.BLAKE2B_160;
	}
	public static HashFunction blake2b_256() {
		return Blake2B256Holder.BLAKE2B_256;
	}
	public static HashFunction blake2b_384() {
		return Blake2B384Holder.BLAKE2B_384;
	}
	public static HashFunction blake2b_512() {
		return Blake2B512Holder.BLAKE2B_512;
	}

	private MoreHashing() {}

}

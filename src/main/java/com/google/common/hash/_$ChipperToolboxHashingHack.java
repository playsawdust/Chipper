/*
 * Chipper Toolbox - a collection of assorted utilities for Java, building upon Google Guava
 * Copyright (c) 2019 the Chipper developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.google.common.hash;

import java.util.zip.Checksum;

import com.google.common.base.Supplier;

/**
 * Accesses package-private HashFunction implementations.
 */
public class _$ChipperToolboxHashingHack {

	public static HashFunction newMessageDigest(String algorithmName, String toString) {
		return new MessageDigestHashFunction(algorithmName, toString);
	}

	public static HashFunction newChecksum(Supplier<Checksum> checksum, int bits, String toString) {
		return new ChecksumHashFunction(() -> checksum.get(), bits, toString);
	}

}

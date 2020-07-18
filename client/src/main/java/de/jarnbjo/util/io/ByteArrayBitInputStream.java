/*
 * This software is based on or using the J-Ogg library available from
 * http://www.j-ogg.de and copyrighted by Tor-Einar Jarnbjo.
 *
 * You are free to use, modify, redistribute or include this software in your own
 * free or commercial software. The only restriction is, that you make it obvious
 * that your software is based on J-Ogg by including this notice in the
 * documentation, about box or wherever you feel appropriate.
 */

package de.jarnbjo.util.io;

import java.io.IOException;

/**
 * Implementation of the <code>BitInputStream</code> interface, using a byte
 * array as data source.
 */

public class ByteArrayBitInputStream implements BitInputStream {

	private byte[] source;
	private byte currentByte;

	private int endian;

	private int byteIndex = 0;
	private int bitIndex = 0;

	public ByteArrayBitInputStream(byte[] source) {
		this(source, LITTLE_ENDIAN);
	}

	public ByteArrayBitInputStream(byte[] source, int endian) {
		this.endian = endian;
		this.source = source;
		currentByte = source[0];
		bitIndex = (endian == LITTLE_ENDIAN) ? 0 : 7;
	}

	@Override
	public boolean getBit() throws IOException {
		if (endian == LITTLE_ENDIAN) {
			if (bitIndex > 7) {
				bitIndex = 0;
				currentByte = source[++byteIndex];
			}
			return (currentByte & (1 << (bitIndex++))) != 0;
		} else {
			if (bitIndex < 0) {
				bitIndex = 7;
				currentByte = source[++byteIndex];
			}
			return (currentByte & (1 << (bitIndex--))) != 0;
		}
	}

	@Override
	public int getInt(int bits) throws IOException {
		if (bits > 32) {
			throw new IllegalArgumentException(
					"Argument \"bits\" must be <= 32");
		}
		int res = 0;
		if (endian == LITTLE_ENDIAN) {
			for (int i = 0; i < bits; i++) {
				if (getBit()) {
					res |= (1 << i);
				}
			}
		} else {
			if (bitIndex < 0) {
				bitIndex = 7;
				currentByte = source[++byteIndex];
			}
			if (bits <= bitIndex + 1) {
				int ci = (currentByte) & 0xff;
				int offset = 1 + bitIndex - bits;
				int mask = ((1 << bits) - 1) << offset;
				res = (ci & mask) >> offset;
				bitIndex -= bits;
			} else {
				res = ((currentByte) & 0xff
						& ((1 << (bitIndex + 1)) - 1)) << (bits - bitIndex - 1);
				bits -= bitIndex + 1;
				currentByte = source[++byteIndex];
				while (bits >= 8) {
					bits -= 8;
					res |= ((source[byteIndex]) & 0xff) << bits;
					currentByte = source[++byteIndex];
				}
				if (bits > 0) {
					int ci = (source[byteIndex]) & 0xff;
					res |= (ci >> (8 - bits)) & ((1 << bits) - 1);
					bitIndex = 7 - bits;
				} else {
					currentByte = source[--byteIndex];
					bitIndex = -1;
				}
			}
		}

		return res;
	}

	@Override
	public int getSignedInt(int bits) throws IOException {
		int raw = getInt(bits);
		if (raw >= 1 << (bits - 1)) {
			raw -= 1 << bits;
		}
		return raw;
	}

	@Override
	public long getLong(int bits) throws IOException {
		if (bits > 64) {
			throw new IllegalArgumentException(
					"Argument \"bits\" must be <= 64");
		}
		long res = 0;
		if (endian == LITTLE_ENDIAN) {
			for (int i = 0; i < bits; i++) {
				if (getBit()) {
					res |= (1L << i);
				}
			}
		} else {
			for (int i = bits - 1; i >= 0; i--) {
				if (getBit()) {
					res |= (1L << i);
				}
			}
		}
		return res;
	}

	@Override
	public void align() {
		if (endian == BIG_ENDIAN && bitIndex >= 0) {
			bitIndex = 7;
			byteIndex++;
		} else if (endian == LITTLE_ENDIAN && bitIndex <= 7) {
			bitIndex = 0;
			byteIndex++;
		}
	}

	@Override
	public void setEndian(int endian) {
		if (this.endian == BIG_ENDIAN && endian == LITTLE_ENDIAN) {
			bitIndex = 0;
			byteIndex++;
		} else if (this.endian == LITTLE_ENDIAN && endian == BIG_ENDIAN) {
			bitIndex = 7;
			byteIndex++;
		}
		this.endian = endian;
	}

	/**
	 * @return the byte array used as a source for this instance
	 */

	public byte[] getSource() {
		return source;
	}
}
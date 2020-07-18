/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.google.common.base.Charsets;
import com.playsawdust.chipper.Identifier;

import android.util.Half;

/**
 * Wrapper for NIO's ByteBuffer that allows treating it similarly to a data
 * stream, and adds support for more data types, including  "in-between"
 * (24-bit, 40-bit, 48-bit, and 56-bit), and unsigned integers, as well as
 * single-bit booleans.
 * @see Unmarshaller
 */
public class Marshaller {

	private final ByteBuffer buf;

	private int bitBuffer = 0;
	private int bitWriteIndex = 0;

	public Marshaller(ByteBuffer buf) {
		if (buf.order() != ByteOrder.BIG_ENDIAN)
			throw new IllegalArgumentException("Network order is big endian, so message buffers must be BIG_ENDIAN");
		this.buf = buf;
	}

	private void commitBits() {
		if (bitWriteIndex != 0) {
			bitWriteIndex = 0;
			writeI8(bitBuffer);
			bitBuffer = 0;
		}
	}

	/**
	 * Write a 1-bit boolean value to this marshaller's current position.
	 * <p>
	 * As the underlying system used by Marshaller is aligned to octets
	 * (8 bits), booleans will be written in groups of 8. A series of 8 adjacent
	 * writeBit calls will write 8 bits, but if the calls are broken up, the
	 * remaining bits will be set to 0. As such, writing a 1-bit value, then a
	 * 8-bit value, then a 1-bit value will actually use 24 bits.
	 * <p>
	 * This is major improvement over more common APIs, which will always waste
	 * a full 8-bit value on a boolean to avoid dealing with alignment issues.
	 * @param b the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeBit(boolean b) {
		if (bitWriteIndex >= 8) {
			commitBits();
		}
		if (b) {
			bitBuffer |= 1 << (7-bitWriteIndex);
		}
		bitWriteIndex++;
	}

	/**
	 * Write an 8-bit integer value to this marshaller's current position, and
	 * increment the position by 1.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -128 &mdash; 127<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 255
	 * @param i the value to write; only the last 8 bits will be used
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI8(byte i) {
		commitBits();
		buf.put(i);
	}

	/**
	 * Write an 8-bit integer value to this marshaller's current position, and
	 * increment the position by 1.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -128 &mdash; 127<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 255
	 * @param i the value to write; only the last 8 bits will be used
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI8(int i) {
		writeI8((byte)(i&0xFF));
	}

	/**
	 * Write a 16-bit integer value to this marshaller's current position, and
	 * increment the position by 2.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -32,768 &mdash; 32,767<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 65,535
	 * @param i the value to write; only the last 16 bits will be used
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI16(short i) {
		commitBits();
		buf.putShort(i);
	}

	/**
	 * Write a 16-bit integer value to this marshaller's current position, and
	 * increment the position by 2.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -32,768 &mdash; 32,767<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 65,535
	 * @param i the value to write; only the last 16 bits will be used
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI16(int i) {
		writeI16((short)(i&0xFFFF));
	}

	/**
	 * Write a 24-bit integer value to this marshaller's current position, and
	 * increment the position by 3.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -8,388,608 &mdash; 8,388,607<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 16,777,215
	 * @param i the value to write; only the last 24 bits will be used
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI24(int i) {
		writeI8(i>>>16);
		writeI16(i);
	}

	/**
	 * Write a 32-bit integer value to this marshaller's current position, and
	 * increment the position by 4.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -2,147,483,648 &mdash; 2,147,483,647<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 4,294,967,295
	 * @param i the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI32(int i) {
		commitBits();
		buf.putInt(i);
	}

	/**
	 * Write a 32-bit integer value to this marshaller's current position, and
	 * increment the position by 4.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -2,147,483,648 &mdash; 2,147,483,647<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 4,294,967,295
	 * @param i the value to write; only the last 32 bits will be used
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI32(long i) {
		commitBits();
		buf.putInt((int)(i&0xFFFFFFFFL));
	}

	/**
	 * Write a 40-bit integer value to this marshaller's current position, and
	 * increment the position by 5.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -549,755,813,888 &mdash; 549,755,813,887<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 1,099,511,627,775
	 * @param i the value to write; only the last 40 bits will be used
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI40(long i) {
		writeI8((int)(i>>>32L));
		writeI32((int)(i&0xFFFFFFFF));
	}

	/**
	 * Write a 48-bit integer value to this marshaller's current position, and
	 * increment the position by 6.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -140,737,488,355,328 &mdash; 140,737,488,355,327<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 281,474,976,710,655
	 * @param i the value to write; only the last 48 bits will be used
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI48(long i) {
		writeI16((int)(i>>>32L));
		writeI32((int)(i&0xFFFFFFFF));
	}

	/**
	 * Write a 56-bit integer value to this marshaller's current position, and
	 * increment the position by 7.
	 * <p>
	 * Signed or unsigned values will work; signedness only matters when
	 * reading due to the way twos-complement works.
	 * <p>
	 * <b>Signed Range</b>: -36,028,797,018,963,968 &mdash; 36,028,797,018,963,967<br/>
	 * <b>Unsigned Range</b>: 0 &mdash; 72,057,594,037,927,935
	 * @param i the value to write; only the last 56 bits will be used
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI56(long i) {
		writeI24((int)(i>>>32L));
		writeI32((int)(i&0xFFFFFFFF));
	}

	/**
	 * Write a 64-bit signed integer value to this marshaller's current position,
	 * and increment the position by 8. As 64-bit signed values are the largest
	 * primitive type offered by Java, this value will always be interpreted as
	 * signed.
	 * <p>
	 * <b>Signed Range</b>: −9,223,372,036,854,775,808 &mdash; 9,223,372,036,854,775,807<br/>
	 * @param i the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeI64(long i) {
		commitBits();
		buf.putLong(i);
	}

	/**
	 * Write a 16-bit "half-precision" floating point value to this marshaller's
	 * current position, and increment the position by 2.
	 * <p>
	 * The range of floating point values is hard to concisely define usefully.
	 * Their absolute limits aren't helpful and explaining the intricaces of
	 * floating point is out of the scope of a method javadoc.
	 * @param f the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeF16(float f) {
		commitBits();
		buf.putShort(Half.toHalf(f));
	}

	/**
	 * Write a 32-bit "single-precision" floating point value to this marshaller's
	 * current position, and increment the position by 4.
	 * <p>
	 * The range of floating point values is hard to concisely define usefully.
	 * Their absolute limits aren't helpful and explaining the intricaces of
	 * floating point is out of the scope of a method javadoc.
	 * @param f the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeF32(float f) {
		commitBits();
		buf.putFloat(f);
	}

	/**
	 * Write a 64-bit "double-precision" floating point value to this marshaller's
	 * current position, and increment the position by 8.
	 * <p>
	 * The range of floating point values is hard to concisely define usefully.
	 * Their absolute limits aren't helpful and explaining the intricaces of
	 * floating point is out of the scope of a method javadoc.
	 * @param d the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeF64(double d) {
		commitBits();
		buf.putDouble(d);
	}

	/**
	 * Write a signed <i>unit</i> floating-point value to this marshaller's current position, and
	 * increment the position by 1.
	 * <p>
	 * A <i>unit</i> value is one from -1 to 1. This method encodes it as a signed byte from -128 to
	 * 127, giving <sup>1</sup>/<sub>127</sub> of accuracy for positive values, which is more than
	 * enough for most use cases. Due to two's complement, negative values are afforded a slightly
	 * higher accuracy of <sup>1</sup>/<sub>128</sub>.
	 * <p>
	 * Range: -1 &mdash; 1
	 * @param d the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 * @see #writeFUnit8(double)
	 */
	public void writeFSUnit8(double d) {
		if (d < 0) {
			writeI8((int)(d*128));
		} else {
			writeI8((int)(d*127));
		}
	}

	/**
	 * Write an unsigned <i>unit</i> floating-point value to this marshaller's current position, and
	 * increment the position by 1.
	 * <p>
	 * An unsigned <i>unit</i> value is one from 0 to 1. This method encodes it as an unsigned byte
	 * from 0 to 255, giving <sup>1</sup>/<sub>255</sub> of accuracy, which is more than enough for
	 * most use cases.
	 * <p>
	 * Range: 0 &mdash; 1
	 * @param d the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 * @see #writeFSUnit8(double)
	 */
	public void writeFUnit8(double d) {
		writeI8((int)(d*255));
	}

	/**
	 * Write a fixed-point value as a varint, with a 57-bit integral part and a 6-bit fractional
	 * part, plus a sign bit (Q6.57), to this marshaller's current position, and increment the
	 * position by anywhere from 1 to 10.
	 * <p>
	 * This gives fractional values a precision of <sup>1</sup>/<sub>64</sub>, and limits the
	 * range of values considerably compared to floating point, but precision is <i>fixed</i>
	 * throughout the entire value range; always <sup>1</sup>/<sub>64</sub>, no matter how large
	 * the number gets.
	 * <p>
	 * This encoding saves space over a standard float or double for small numbers, and wastes
	 * space for larger numbers. For a lot of use cases, this is an acceptable tradeoff, and
	 * <sup>1</sup>/<sub>64</sub> is often a reasonable level of accuracy.
	 * <p>
	 * A 6-bit fractional part is chosen due to the varint encoding using the MSB of each 8-bit
	 * value as an indicator of if the integer will continue, leaving us 7 bits. ZigZag encoding
	 * additionally moves the sign bit to the LSB of the first byte (varints are little-endian),
	 * using another bit, leaving us 6 bits in the first byte. As such, any values less than
	 * -0.984375 or greater than 0.984375 use two bytes. More values could be encoded in a single
	 * byte by using a 5-bit fractional part, but <sup>1</sup>/<sub>32</sub> accuracy is considered
	 * unacceptable for many cases by the author, and unit values, where this is most likely to be
	 * considered a problematic use of space, are better encoded as a byte. See {@link #writeFUnit8}
	 * and {@link #writeFSUnit8} if this is your use case.
	 * <p>
	 * This method is implemented as a simple multiplication, and is mainly offered as a way to
	 * explain fixed-point and its importance in the protocol, and introduce the concept to those
	 * unfamiliar. For custom Q values, just multiply your number by an arbitrary power-of-two and
	 * pass it to {@link #writeIVar64}. The value for Q6 is 64; 2<sup>6</sup>.
	 * <p>
	 * <b>Range</b>: -144,115,188,075,855,872.000000 &mdash; 144,115,188,075,855,871.984375<br/>
	 * @param d the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeFVarFixedQ6(double d) {
		writeIVar64((long)(d*64));
	}

	/**
	 * Write a variable sized integer value to this marshaller's current position,
	 * in Protobuf "ZigZag varint" format.
	 * <p>
	 * Range is identical to {@link #writeI32 a 32-bit integer}. Anywhere from
	 * 1 to 5 bytes will be used, and the position will be incremented by that
	 * amount.
	 * <p>
	 * This method uses ZigZag encoding to make negative numbers more efficient,
	 * at the cost of being signed. If you expect incredibly large unsigned
	 * values that won't fit in a signed integer, then a varint probably won't
	 * save any space for your use case anyway.
	 * @param i the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeIVar32(int i) {
		writeIVar64(i);
	}

	/**
	 * Write a variable sized integer value to this marshaller's current position,
	 * in Protobuf "ZigZag varint" format.
	 * <p>
	 * Range is identical to {@link #writeI64 a 64-bit integer}. Anywhere from
	 * 1 to 10 bytes will be used, and the position will be incremented by that
	 * amount.
	 * <p>
	 * This method uses ZigZag encoding to make negative numbers more efficient,
	 * at the cost of being signed. If you expect incredibly large unsigned
	 * values that won't fit in a signed integer, then a varint probably won't
	 * save any space for your use case anyway.
	 * @param i the value to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeIVar64(long i) {
		long zig = (i << 1) ^ (i >> 63);
		while ((zig & ~0x7F) != 0) {
			writeI8((int)((zig & 0x7F) | 0x80));
			zig >>>= 7L;
		}
		writeI8((int)(zig&0x7F));
	}

	/**
	 * Write a UTF-8 varint-length-prefixed string value to this marshaller's
	 * current position, and increment the position by the number of octets
	 * written.
	 * @param str the string to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeString(@NonNull String str) {
		ByteBuffer encoded = Charsets.UTF_8.encode(str);
		writeIVar32(encoded.remaining());
		write(encoded);
	}

	/**
	 * Write the given Identifier to this marshaller's current position, as two
	 * {@link #writeString strings}, and increment the position by the number of octets
	 * written.
	 * @param id the identifier to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data.
	 */
	public void writeIdentifier(@NonNull Identifier id) {
		writeString(id.namespace);
		writeString(id.path);
	}

	/**
	 * Copy the contents of the given byte buffer into this writer, at its
	 * current position. The position will be incremented by the number of
	 * remaining bytes in the given buffer.
	 * @param in the buffer to read from
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void write(@NonNull ByteBuffer in) {
		commitBits();
		buf.put(in);
	}

	/**
	 * Copy the contents of the given byte array into this writer, at its
	 * current position. The position will be incremented by the length of the
	 * array.
	 * @param bys the bytes to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void write(byte[] bys) {
		write(bys, 0, bys.length);
	}

	/**
	 * Copy the contents of the given byte array from {@code ofs} into this
	 * writer, at its current position. The position will be incremented by
	 * {@code len}.
	 * @param bys the source of the bytes to write
	 * @param ofs the offset into {@code bys} to start at
	 * @param len the number of bytes to write
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void write(byte[] bys, int ofs, int len) {
		commitBits();
		buf.put(bys, ofs, len);
	}

	/**
	 * Write a 128-bit UUID value to this marshaller's buffer. The position will
	 * be incremented by 16.
	 * @throws BufferOverflowException if there isn't enough space in the buffer for the data
	 */
	public void writeUUID(UUID id) {
		writeI64(id.getMostSignificantBits());
		writeI64(id.getLeastSignificantBits());
	}

	/**
	 * Write any pending values to the underlying byte buffer and return it.
	 */
	public ByteBuffer finish() {
		commitBits();
		ByteBuffer rtrn = buf.duplicate();
		rtrn.flip();
		return rtrn.asReadOnlyBuffer();
	}

}

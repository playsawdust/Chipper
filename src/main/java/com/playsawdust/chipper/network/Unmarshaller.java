/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.NonNull;
import com.google.common.base.Charsets;
import com.playsawdust.chipper.Identifier;

import android.util.Half;

/**
 * Wrapper for NIO's ByteBuffer that allows treating it similarly to a data
 * stream, and adds support for more data types, including "in-between"
 * (24-bit, 40-bit, 48-bit, and 56-bit), and unsigned integers, as well as
 * single-bit booleans.
 * @see Marshaller
 */
public class Unmarshaller {

	private final ByteBuffer buf;

	private int bitIndex = 0;

	public Unmarshaller(ByteBuffer buf) {
		if (buf.order() != ByteOrder.BIG_ENDIAN)
			throw new IllegalArgumentException("Network order is big endian, so message buffers must be BIG_ENDIAN");
		this.buf = buf;
	}

	private void skipBits() {
		if (bitIndex > 0) {
			buf.get();
		}
		bitIndex = 0;
	}

	/**
	 * Read a 1-bit boolean value from this unmarshaller's current position.
	 * <p>
	 * As the underlying system used by Unmarshaller is aligned to octets
	 * (8 bits), booleans will be read in groups of 8. A series of 8 adjacent
	 * readBit calls will read 8 bits, but if the calls are broken up, the
	 * remaining bits will be skipped. As such, reading a 1-bit value, then a
	 * 8-bit value, then a 1-bit value will actually read 24 bits.
	 * <p>
	 * This is major improvement over more common APIs, which will always waste
	 * a full 8-bit value on a boolean to avoid dealing with alignment issues.
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public boolean readBit() {
		if (bitIndex >= 8) {
			buf.get();
			bitIndex = 0;
		}
		int i = buf.get(buf.position())&0xFF;
		boolean b = (i & (1<<(7-bitIndex))) != 0;
		bitIndex++;
		return b;
	}

	/**
	 * Read a signed 8-bit integer value from this unmarshaller's current position,
	 * and increment the position by 1.
	 * <p>
	 * <b>Range</b>: -128 &mdash; 127
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public byte readI8() {
		skipBits();
		return buf.get();
	}

	/**
	 * Read an unsigned 8-bit integer value from this unmarshaller's current position,
	 * and increment the position by 1.
	 * <p>
	 * <b>Range</b>: 0 &mdash; 255
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public int readUI8() {
		return readI8()&0xFF;
	}

	/**
	 * Read a signed 16-bit integer value from this unmarshaller's current position,
	 * and increment the position by 2.
	 * <p>
	 * <b>Range</b>: -32,768 &mdash; 32,767
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public short readI16() {
		skipBits();
		return buf.getShort();
	}

	/**
	 * Read an unsigned 16-bit integer value from this unmarshaller's current
	 * position, and increment the position by 2.
	 * <p>
	 * <b>Range</b>: 0 &mdash; 65,535
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public int readUI16() {
		return readI16()&0xFFFF;
	}

	/**
	 * Read a signed 24-bit integer value from this unmarshaller's current position,
	 * and increment the position by 3.
	 * <p>
	 * <b>Range</b>: -8,388,608 &mdash; 8,388,607
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public int readI24() {
		return (readI8() << 16) | readUI16();
	}

	/**
	 * Read an unsigned 24-bit integer value from this unmarshaller's current
	 * position, and increment the position by 3.
	 * <p>
	 * <b>Range</b>: 0 &mdash; 16,777,215
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public int readUI24() {
		return (readUI8() << 16) | readUI16();
	}

	/**
	 * Read a signed 32-bit integer value from this unmarshaller's current position,
	 * and increment the position by 4.
	 * <p>
	 * <b>Range</b>: -2,147,483,648 &mdash; 2,147,483,647
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public int readI32() {
		skipBits();
		return buf.getInt();
	}

	/**
	 * Read an unsigned 32-bit integer value from this unmarshaller's current
	 * position, and increment the position by 4.
	 * <p>
	 * <b>Range</b>: 0 &mdash; 4,294,967,295
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public long readUI32() {
		return readI32()&0xFFFFFFFFL;
	}

	/**
	 * Read a signed 40-bit integer value from this unmarshaller's current position,
	 * and increment the position by 5.
	 * <p>
	 * <b>Range</b>: -549,755,813,888 &mdash; 549,755,813,887
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public long readI40() {
		return (((long)readI8()) << 32L) | readUI32();
	}

	/**
	 * Read an unsigned 40-bit integer value from this unmarshaller's current
	 * position, and increment the position by 5.
	 * <p>
	 * <b>Range</b>: 0 &mdash; 1,099,511,627,775
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public long readUI40() {
		return (((long)readUI8()) << 32L) | readUI32();
	}

	/**
	 * Read a signed 48-bit integer value from this unmarshaller's current position,
	 * and increment the position by 6.
	 * <p>
	 * <b>Range</b>: -140,737,488,355,328 &mdash; 140,737,488,355,327
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public long readI48() {
		return (((long)readI16()) << 32L) | readUI32();
	}

	/**
	 * Read an unsigned 48-bit integer value from this unmarshaller's current
	 * position, and increment the position by 6.
	 * <p>
	 * <b>Range</b>: 0 &mdash; 281,474,976,710,655
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public long readUI48() {
		return (((long)readUI16()) << 32L) | readUI32();
	}

	/**
	 * Read a signed 56-bit integer value from this unmarshaller's current position,
	 * and increment the position by 7.
	 * <p>
	 * <b>Range</b>: -36,028,797,018,963,968 &mdash; 36,028,797,018,963,967
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public long readI56() {
		return (((long)readI24()) << 32L) | readUI32();
	}

	/**
	 * Read an unsigned 56-bit integer value from this unmarshaller's current
	 * position, and increment the position by 7.
	 * <p>
	 * <b>Range</b>: 0 &mdash; 72,057,594,037,927,935
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public long readUI56() {
		return (((long)readUI24()) << 32L) | readUI32();
	}

	/**
	 * Read a signed 64-bit integer value from this unmarshaller's current position,
	 * and increment the position by 8.
	 * <p>
	 * <b>Range</b>: −9,223,372,036,854,775,808 &mdash; 9,223,372,036,854,775,807
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public long readI64() {
		skipBits();
		return buf.getLong();
	}

	/**
	 * Read a 16-bit "half-precision" floating point value from this unmarshaller's
	 * current position, and increment the position by 2.
	 * <p>
	 * The range of floating point values is hard to concisely define usefully.
	 * Their absolute limits aren't helpful and explaining the intricaces of
	 * floating point is out of the scope of a method javadoc.
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public float readF16() {
		return Half.toFloat(readI16());
	}

	/**
	 * Read a 32-bit "single-precision" floating point value from this unmarshaller's
	 * current position, and increment the position by 4.
	 * <p>
	 * The range of floating point values is hard to concisely define usefully.
	 * Their absolute limits aren't helpful and explaining the intricaces of
	 * floating point is out of the scope of a method javadoc.
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public float readF32() {
		skipBits();
		return buf.getFloat();
	}

	/**
	 * Read a 64-bit "double-precision" floating point value from this unmarshaller's
	 * current position, and increment the position by 8.
	 * <p>
	 * The range of floating point values is hard to concisely define usefully.
	 * Their absolute limits aren't helpful and explaining the intricaces of
	 * floating point is out of the scope of a method javadoc.
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public double readF64() {
		skipBits();
		return buf.getDouble();
	}

	/**
	 * Read a signed <i>unit</i> floating-point value from this unmarshaller's current position, and
	 * increment the position by 1.
	 * <p>
	 * A <i>unit</i> value is one from -1 to 1. This method decodes it from a signed byte from -128
	 * to 127, giving <sup>1</sup>/<sub>127</sub> of accuracy for positive values, which is more than
	 * enough for most use cases. Due to two's complement, negative values are afforded a slightly
	 * higher accuracy of <sup>1</sup>/<sub>128</sub>.
	 * <p>
	 * Range: -1 &mdash; 1
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 * @see #readFUnit8()
	 */
	public double readFSUnit8() {
		int val = readI8();
		if (val < 0) {
			return val/128D;
		} else {
			return val/127D;
		}
	}

	/**
	 * Read an unsigned <i>unit</i> floating-point value from this unmarshaller's current position,
	 * and increment the position by 1.
	 * <p>
	 * An unsigned <i>unit</i> value is one from 0 to 1. This method decodes it from an unsigned
	 * byte from 0 to 255, giving <sup>1</sup>/<sub>255</sub> of accuracy, which is more than enough
	 * for most use cases.
	 * <p>
	 * Range: 0 &mdash; 1
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 * @see #readFSUnit8()
	 */
	public double readFUnit8() {
		return readUI8()/255D;
	}

	/**
	 * Read a fixed-point value as a varint, with a 57-bit integral part and a 6-bit fractional
	 * part, plus a sign bit (Q6.57), from this unmarshaller's current position, and increment the
	 * position by anywhere from 1 to 10.
	 * <p>
	 * This gives fractional values a precision of <sup>1</sup>/<sub>64</sub>, and limits the
	 * range of values considerably compared to floating point, but precision is <i>fixed</i>
	 * throughout the entire value range; always <sup>1</sup>/<sub>64</sub>, no matter how large
	 * the number gets.
	 * <p>
	 * This encoding saves space over a standard float for small numbers, and wastes space for
	 * larger numbers. For a lot of use cases, this is an acceptable tradeoff, and
	 * <sup>1</sup>/<sub>64</sub> is often a reasonable level of accuracy.
	 * <p>
	 * A 6-bit fractional part is chosen due to the varint encoding using the MSB of each 8-bit
	 * value as an indicator of if the integer will continue, leaving us 7 bits. ZigZag encoding
	 * additionally moves the sign bit to the LSB of the first byte (varints are little-endian),
	 * using another bit, leaving us 6 bits in the first byte. As such, any values less than
	 * -0.984375 or greater than 0.984375 use two bytes. More values could be encoded in a single
	 * byte by using a 5-bit fractional part, but <sup>1</sup>/<sub>32</sub> accuracy is considered
	 * unacceptable for many cases by the author, and unit values, where this is most likely to be
	 * considered a problematic use of space, are better encoded as a byte. See {@link #readFUnit8}
	 * and {@link #readFSUnit8} if this is your use case.
	 * <p>
	 * This method is implemented as a simple division, and is mainly offered as a way to
	 * explain fixed-point and its importance in the protocol, and introduce the concept to those
	 * unfamiliar. For custom Q values, just divide a number read from {@link #readIVar64} by an
	 * arbitrary power-of-two. The value for Q6 is 64; 2<sup>6</sup>.
	 * <p>
	 * <b>Range</b>: -144,115,188,075,855,872.000000 &mdash; 144,115,188,075,855,871.984375<br/>
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public double readFVarFixedQ6() {
		return readIVar64()/64D;
	}

	/**
	 * Read a variable sized integer value from this unmarshaller's current position,
	 * in Protobuf "ZigZag varint" format.
	 * <p>
	 * Range is identical to {@link #readI32 a 32-bit integer}. Anywhere from
	 * 1 to 5 bytes will be read, and the position will be incremented by that
	 * amount.
	 * <p>
	 * This method uses ZigZag encoding to make negative numbers more efficient,
	 * at the cost of being signed. If you expect incredibly large unsigned
	 * values that won't fit in a signed integer, then a varint probably won't
	 * save any space for your use case anyway.
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public int readIVar32() {
		return (int)_readIVar(5);
	}

	/**
	 * Read a variable sized integer value from this unmarshaller's current position,
	 * in Protobuf "ZigZag varint" format.
	 * <p>
	 * Range is identical to {@link #readI64 a 64-bit integer}. Anywhere from
	 * 1 to 10 bytes will be read, and the position will be incremented by that
	 * amount.
	 * <p>
	 * This method uses ZigZag encoding to make negative numbers more efficient,
	 * at the cost of being signed. If you expect incredibly large unsigned
	 * values that won't fit in a signed integer, then a varint probably won't
	 * save any space for your use case anyway.
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public long readIVar64() {
		return _readIVar(10);
	}

	private long _readIVar(int max) {
		long value = 0;
		int size = 0;
		while (true) {
			int b = readUI8();
			value |= (b & 0x7FL) << (size * 7L);
			if (size > max) {
				throw new IllegalArgumentException("IVar too long (maximum of "+max+" bytes exceeded)");
			}
			if ((b & 0x80) == 0) break;
			size++;
		}
		long zag = (value >>> 1) ^ (-(value & 1));
		return zag;
	}

	/**
	 * Read a UTF-8 varint-length-prefixed string value from this unmarshaller's
	 * current position, and increment the position by the number of octets
	 * read.
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public String readString() {
		int len = readIVar32();
		System.out.println(len);
		if (len > buf.remaining()) throw new BufferUnderflowException();
		ByteBuffer slice = buf.slice();
		slice.limit(len);
		String s = Charsets.UTF_8.decode(slice).toString();
		buf.position(buf.position()+len);
		return s;
	}

	/**
	 * Read an Identifier from this unmarshaller's current position, as two
	 * {@link #readString strings}, and increment the position by the number of octets
	 * read.
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public Identifier readIdentifier() {
		String ns = readString();
		String p = readString();
		return new Identifier(ns, p);
	}

	/**
	 * Copy from this unmarshaller's buffer into the given buffer. The position will
	 * be incremented by the amount of bytes read, which will be the amount of
	 * bytes that were remaining in the passed-in buffer.
	 * @param out the buffer to write to
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public void read(@NonNull ByteBuffer out) {
		if (out.remaining() > buf.remaining()) throw new BufferUnderflowException();
		ByteBuffer slice = buf.slice();
		int len = out.remaining();
		slice.limit(len);
		out.put(slice);
		buf.position(buf.position()+len);
	}

	/**
	 * Copy from this unmarshaller's buffer into the given byte array. The position
	 * will be incremented by the amount of bytes read, which will be the length
	 * of the given byte array.
	 * @param bys the array to write to
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public void read(byte[] bys) {
		read(bys, 0, bys.length);
	}

	/**
	 * Copy from this unmarshaller's buffer into the given byte array. The position
	 * will be incremented by the amount of bytes read, which will be {@code len}.
	 * @param bys the array to write to
	 * @param ofs the offset into {@code bys} to start at
	 * @param len the number of bytes to write
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public void read(byte[] bys, int ofs, int len) {
		buf.get(bys, ofs, len);
	}

	/**
	 * Read a 128-bit UUID value from this unmarshaller's buffer. The position will
	 * be incremented by 16.
	 * @throws BufferUnderflowException if there isn't enough data to satisfy the request
	 */
	public UUID readUUID() {
		return new UUID(readI64(), readI64());
	}

}

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
 *	An interface with methods allowing bit-wise reading from
 * an input stream. All methods in this interface are optional
 * and an implementation not support a method or a specific state
 * (e.g. endian) will throw an UnspportedOperationException if
 * such a method is being called. This should be speicified in
 * the implementation documentation.
 */

public interface BitInputStream {

	/**
	 *  constant for setting this stream's mode to little endian
	 *
	 *  @see #setEndian(int)
	 */

   int LITTLE_ENDIAN = 0;

	/**
	 *  constant for setting this stream's mode to big endian
	 *
	 *  @see #setEndian(int)
	 */

   int BIG_ENDIAN = 1;

	/**
	 *  reads one bit (as a boolean) from the input stream
	 *
	 *  @return <code>true</code> if the next bit is 1,
	 *          <code>false</code> otherwise
	 *
	 *  @throws IOException if an I/O error occurs
	 *  @throws UnsupportedOperationException if the method is not supported by the implementation
	 */
   boolean getBit() throws IOException;

	/**
	 *  reads <code>bits</code> number of bits from the input
	 *  stream
	 *
	 *  @return the unsigned integer value read from the stream
	 *
	 *  @throws IOException if an I/O error occurs
	 *  @throws UnsupportedOperationException if the method is not supported by the implementation
	 */
   int getInt(int bits) throws IOException;

	/**
	 *  reads <code>bits</code> number of bits from the input
	 *  stream
	 *
	 *  @return the signed integer value read from the stream
	 *
	 *  @throws IOException if an I/O error occurs
	 *  @throws UnsupportedOperationException if the method is not supported by the implementation
	 */
   int getSignedInt(int bits) throws IOException;

	/**
	 *  reads <code>bits</code> number of bits from the input
	 *  stream
	 *
	 *  @return the unsigned long value read from the stream
	 *
	 *  @throws IOException if an I/O error occurs
	 *  @throws UnsupportedOperationException if the method is not supported by the implementation
	 */
   long getLong(int bits) throws IOException;

	/**
	 *  causes the read pointer to be moved to the beginning
	 *  of the next byte, remaining bits in the current byte
	 *  are discarded
	 *
	 *  @throws UnsupportedOperationException if the method is not supported by the implementation
	 */
   void align();

	/**
	 *  changes the endian mode used when reading bit-wise from
	 *  the stream, changing the mode mid-stream will cause the
	 *  read cursor to move to the beginning of the next byte
	 *  (as if calling the <code>allign</code> method
	 *
	 *  @see #align()
	 *
	 *  @throws UnsupportedOperationException if the method is not supported by the implementation
	 */
   void setEndian(int endian);
}
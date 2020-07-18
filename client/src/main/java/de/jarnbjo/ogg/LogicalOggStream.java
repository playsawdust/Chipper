/*
 * This software is based on or using the J-Ogg library available from
 * http://www.j-ogg.de and copyrighted by Tor-Einar Jarnbjo.
 *
 * You are free to use, modify, redistribute or include this software in your own
 * free or commercial software. The only restriction is, that you make it obvious
 * that your software is based on J-Ogg by including this notice in the
 * documentation, about box or wherever you feel appropriate.
 */

package de.jarnbjo.ogg;

import java.io.IOException;

/**
 * Interface providing access to a logical Ogg stream as part of a physical Ogg
 * stream.
 */
public interface LogicalOggStream extends Iterable<OggPage> {

	/**
	 * <i>Note:</i> To read from the stream, you must use either this method or
	 * the method <code>getNextOggPacket</code>. Mixing calls to the two methods
	 * will cause data corruption.
	 *
	 * @return the next Ogg page
	 *
	 * @see #getNextOggPacket()
	 *
	 * @throws OggFormatException
	 *             if the ogg stream is corrupted
	 * @throws IOException
	 *             if some other IO error occurs
	 */
	OggPage getNextOggPage() throws OggFormatException, IOException;
	
	/**
	 * Get the next Ogg packet in this logical stream, accounting for
	 * partial packets split over pages.
	 * <p>
	 * <i>Note:</i> To read from the stream, you must use either
	 * this method or the method <code>getNextOggPage</code>.
	 * Mixing calls to the two methods will cause data corruption.
	 *
	 * @return the next packet as a byte array
	 *
	 * @see #getNextOggPage()
	 *
	 * @throws OggFormatException if the ogg stream is corrupted
	 * @throws IOException if some other IO error occurs
	 */
	byte[] getNextOggPacket() throws OggFormatException, IOException;

	/**
	 * Checks if this stream is open for reading.
	 *
	 * @return <code>true</code> if this stream is open for reading,
	 *         <code>false</code> otherwise
	 */
	boolean isOpen();

	/**
	 * Closes this stream. After invoking this method, no further access to the
	 * streams data is possible.
	 *
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void close() throws IOException;

	/**
	 * This method is invoked on all logical streams when calling the same
	 * method on the physical stream. The same restrictions as mentioned there
	 * apply. This method does not work if the physical Ogg stream is not
	 * seekable.
	 *
	 * @param granulePosition
	 *
	 * @see PhysicalOggStream#setTime(long)
	 *
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void setTime(long granulePosition) throws IOException;

	/**
	 * @return the last parsed granule position of this stream
	 */
	long getTime();
}
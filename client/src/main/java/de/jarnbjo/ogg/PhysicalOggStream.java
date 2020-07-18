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
import java.util.Collection;

/**
 * Interface providing access to a physical Ogg stream.
 */
public interface PhysicalOggStream {

	/**
	 * Returns a collection of objects implementing
	 * <code>LogicalOggStream</code> for accessing the separate logical streams
	 * within this physical Ogg stream.
	 *
	 * @return a collection of objects implementing
	 *         <code>LogicalOggStream</code> which are representing the logical
	 *         streams contained within this physical stream
	 *
	 * @see LogicalOggStream
	 */
	Collection<LogicalOggStream> getLogicalStreams();

	/**
	 * Return the next Ogg page in the stream. This method should only
	 * be used by implementations of <code>LogicalOggStream</code> to access the
	 * raw pages.
	 *
	 * @return the next Ogg page encountered
	 *
	 * @throws OggFormatException
	 *             if the ogg stream is corrupted
	 * @throws IOException
	 *             if some other IO error occurs
	 */
	OggPage getNextRawOggPage() throws OggFormatException, IOException;

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
	 */
	void close() throws IOException;
}
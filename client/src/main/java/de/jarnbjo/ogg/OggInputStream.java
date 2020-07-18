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

import java.io.*;
import java.util.*;

/**
 * Implementation of the <code>PhysicalOggStream</code> interface for reading an
 * Ogg stream from a URL. This class performs no internal caching, and will not
 * read data from the network before requested to do so. It is intended to be
 * used in non-realtime applications like file download managers or similar.
 */
public class OggInputStream implements PhysicalOggStream {

	private boolean closed = false;
	private InputStream sourceStream;

	private HashMap<Integer, LogicalOggStream> logicalStreams = new HashMap<>();
	private OggPage firstPage;

	public OggInputStream(InputStream sourceStream) throws OggFormatException, IOException {
		this.sourceStream = sourceStream;
		firstPage = OggPage.create(sourceStream);
		LogicalOggStreamImpl los = new LogicalOggStreamImpl(this, firstPage.getStreamSerialNumber());
		logicalStreams.put(new Integer(firstPage.getStreamSerialNumber()), los);
	}

	@Override
	public Collection<LogicalOggStream> getLogicalStreams() {
		return logicalStreams.values();
	}

	@Override
	public boolean isOpen() {
		return !closed;
	}

	@Override
	public void close() throws IOException {
		closed = true;
		sourceStream.close();
	}

	@Override
	public OggPage getNextRawOggPage() throws IOException {
		if (firstPage != null) {
			OggPage tmp = firstPage;
			firstPage = null;
			return tmp;
		} else {
			OggPage page = OggPage.create(sourceStream);
			return page;
		}
	}

}
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

public class LogicalOggStreamImpl implements LogicalOggStream {

	private PhysicalOggStream source;
	private int serialNumber;

	private ArrayList<Integer> pageNumberMapping = new ArrayList<>();
	private ArrayList<Long> granulePositions = new ArrayList<>();

	private int pageIndex = 0;
	private OggPage currentPage;
	private int currentSegmentIndex;

	private boolean open = true;

	public LogicalOggStreamImpl(PhysicalOggStream source, int serialNumber) {
		this.source = source;
		this.serialNumber = serialNumber;
	}

	public void addPageNumberMapping(int physicalPageNumber) {
		pageNumberMapping.add(new Integer(physicalPageNumber));
	}

	public void addGranulePosition(long granulePosition) {
		granulePositions.add(new Long(granulePosition));
	}

	public int getSerialNumber() {
		return serialNumber;
	}

	@Override
	public synchronized OggPage getNextOggPage()
			throws EndOfOggStreamException, OggFormatException, IOException {
		currentPage = source.getNextRawOggPage();
		return currentPage;
	}
	
	public byte[] getNextOggPacket() throws EndOfOggStreamException, OggFormatException, IOException {
		ByteArrayOutputStream res = new ByteArrayOutputStream();
		int segmentLength = 0;

		if (currentPage == null) {
			currentPage = getNextOggPage();
			if (currentPage == null) {
				return null;
			}
		}

		do {
			if (currentSegmentIndex >= currentPage.getSegmentOffsets().length) {
				currentSegmentIndex = 0;

				if (!currentPage.isEos()) {
					currentPage = getNextOggPage();
					if (currentPage == null) {
						return null;
					}

					if (res.size() == 0 && currentPage.isContinued()) {
						while (true) {
							if (currentSegmentIndex > currentPage.getSegmentTable().length) {
								currentPage = source.getNextRawOggPage();
							}
							if (currentPage.getSegmentLengths()[currentSegmentIndex++] != 255) {
								break;
							}
						}
					}
				} else {
					return null;
				}
			}
			segmentLength = currentPage.getSegmentLengths()[currentSegmentIndex];
			res.write(currentPage.getData(), currentPage.getSegmentOffsets()[currentSegmentIndex], segmentLength);
			currentSegmentIndex++;
		} while (segmentLength == 255);

		return res.toByteArray();
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void close() throws IOException {
		open = false;
	}

	@Override
	public synchronized long getTime() {
		return currentPage != null ? currentPage.getAbsoluteGranulePosition()
				: -1;
	}

	@Override
	public synchronized void setTime(long granulePosition) throws IOException {

		int page = 0;
		for (page = 0; page < granulePositions.size(); page++) {
			Long gp = granulePositions.get(page);
			if (gp.longValue() > granulePosition) {
				break;
			}
		}

		pageIndex = page;
		currentPage = source.getNextRawOggPage();
		currentSegmentIndex = 0;
		int segmentLength = 0;
		do {
			if (currentSegmentIndex >= currentPage.getSegmentOffsets().length) {
				currentSegmentIndex = 0;
				if (pageIndex >= pageNumberMapping.size()) {
					throw new EndOfOggStreamException();
				}
				currentPage = source.getNextRawOggPage();
			}
			segmentLength = currentPage
					.getSegmentLengths()[currentSegmentIndex];
			currentSegmentIndex++;
		} while (segmentLength == 255);
	}

	@Override
	public Iterator<OggPage> iterator() {
		return new Iterator<OggPage>() {
			
			@Override
			public OggPage next() {
				try {
					return getNextOggPage();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			
			@Override
			public boolean hasNext() {
				return false;
			}
		};
	}

}
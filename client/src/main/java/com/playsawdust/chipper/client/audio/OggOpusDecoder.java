/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.audio;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.opus.Opus.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.lwjgl.system.MemoryUtil;

import com.google.common.base.Ascii;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.playsawdust.chipper.AbstractNativeResource;
import com.playsawdust.chipper.collect.unmodifiable.Unmodifiable;
import com.playsawdust.chipper.collect.unmodifiable.UnmodifiableList;
import com.playsawdust.chipper.toolbox.io.Slice;

import de.jarnbjo.ogg.LogicalOggStream;
import de.jarnbjo.ogg.OggFormatException;
import de.jarnbjo.ogg.OggInputStream;
import de.jarnbjo.util.io.BitInputStream;
import de.jarnbjo.util.io.ByteArrayBitInputStream;

/**
 * Decodes Ogg Opus (.opus) files into signed 16-bit 48kHz audio.
 * <p>
 * Uses a modified version of J-Ogg and LWJGL's libopus bindings.
 */
public class OggOpusDecoder extends AbstractNativeResource {
	private final OggInputStream ogg;
	private final LogicalOggStream los;

	private long decoder;
	private ByteBuffer pktBuf;
	private ShortBuffer outBuf;

	private boolean initialized = false;
	private boolean needsPreSkip = true;

	// OpusHead
	private final int version;
	private final int channels;
	private final int preSkip;
	private final int originalSampleRate;
	private final int outputGain;
	private final int channelMappingFamily;

	// OpusTags
	private final String vendor;
	private final ListMultimap<String, String> comments = MultimapBuilder.hashKeys().arrayListValues().build();


	/**
	 * Construct a new OggOpusDecoder for the given input stream.
	 * Immediately reads in the first Ogg page and looks for the Opus
	 * header to provide metadata.
	 * @param in the input stream to read from
	 * @throws OggFormatException if the input stream does not contain valid
	 * 		Ogg Opus data
	 * @throws IOException if an IO error occurs
	 */
	public OggOpusDecoder(InputStream in) throws OggFormatException, IOException {
		this.ogg = new OggInputStream(in);
		// ATTN this isn't possible due to OggInputStream being derived from
		// BasicStream in J-Ogg, which is incredibly limited and only detects
		// the first stream in the file - this is okay for now but needs to get
		// cleaned up at some point, possibly by redesigning J-Ogg's API to be
		// similar to VorbisJava's API, where it just gives you every packet in
		// encounter order and tells you what stream it belongs to
		if (ogg.getLogicalStreams().size() != 1)
			throw new OggFormatException("Ogg file does not contain exactly one stream");
		los = Iterables.getOnlyElement(ogg.getLogicalStreams());
		byte[] packet = los.getNextOggPacket();
		if (OPUS_HEAD.equals(packet, 0, 8)) {
			BitInputStream bis = new ByteArrayBitInputStream(packet, BitInputStream.LITTLE_ENDIAN);
			bis.getLong(64);
			version = bis.getInt(8);
			channels = bis.getInt(8);
			preSkip = bis.getInt(16);
			originalSampleRate = bis.getInt(32);
			outputGain = bis.getInt(16);
			channelMappingFamily = bis.getInt(8);
		} else {
			throw new OggFormatException("Not a valid Ogg Opus file (first packet is not OpusHead)");
		}
		packet = los.getNextOggPacket();
		if (OPUS_TAGS.equals(packet, 0, 8)) {
			BitInputStream bis = new ByteArrayBitInputStream(packet, BitInputStream.LITTLE_ENDIAN);
			bis.getLong(64);
			vendor = readUTF8Vector(bis);
			int amt = bis.getInt(32);
			if (amt < 0)
				throw new IOException("Cannot decode more than 2.14 billion comments");
			for (int i = 0; i < amt; i++) {
				String str = readUTF8Vector(bis);
				int eq = str.indexOf('=');
				if (eq == -1)
					throw new OggFormatException("Comment contains no equals sign: "+str);
				String key = str.substring(0, eq);
				String val = str.substring(eq+1);
				comments.put(Ascii.toUpperCase(key), val);
			}
		} else {
			throw new OggFormatException("Not a valid Ogg Opus file (second packet is not OpusTags)");
		}
	}

	private static String readUTF8Vector(BitInputStream bis) throws IOException {
		int len = bis.getInt(32);
		if (len < 0)
			throw new IOException("Cannot decode a comment string larger than 2GiB");
		byte[] arr = new byte[len];
		for (int i = 0; i < len; i++) {
			arr[i] = (byte)bis.getInt(8);
		}
		return new String(arr, Charsets.UTF_8);
	}

	/**
	 * @return the major version of of this file
	 */
	public int getMajorVersion() {
		return version >> 4;
	}

	/**
	 * @return the minor version of this file
	 */
	public int getMinorVersion() {
		return version & 0xF;
	}

	/**
	 * @return the number of channels encoded in this file; only 1 (mono) and
	 * 		2 (stereo) are supported
	 */
	public int getChannels() {
		return channels;
	}

	/**
	 * <b>Note</b>: Preskip is handled by OggOpusDecoder for you. You
	 * don't need to worry about skipping the right amount of samples.
	 * @return the number of junk samples at the beginning of the file
	 */
	public int getPreSkip() {
		return preSkip;
	}

	/**
	 * @return the sample rate of the <b>original input file</b> that was
	 * 		encoded to Opus. Opus files are always 48kHz
	 */
	public int getOriginalSampleRate() {
		return originalSampleRate;
	}

	/**
	 * @return the desired output gain in dB
	 */
	public float getOutputGain() {
		return outputGain/256f;
	}

	/**
	 * @return the channel mapping family; only 0 (RTP) and 1 (Vorbis) are
	 * 		supported
	 */
	public int getChannelMappingFamily() {
		return channelMappingFamily;
	}

	/**
	 * <b>Note<b>: The "vendor" is the lowest level piece of whatever
	 * software encoded the file, such as libopus for opusenc. The ENCODER
	 * comment defines the high-level software, such as opusenc or FFmpeg.
	 * @return the identification of the software that encoded this file
	 */
	public String getVendor() {
		return vendor;
	}

	/**
	 * @return the last comment with the given key, or null
	 * @see StandardComments
	 */
	public String getComment(String key) {
		if (!comments.containsKey(key)) return null;
		return Iterables.getLast(comments.get(key));
	}

	/**
	 * @return a list of the values of each comment with the given
	 * 		key (usually, it is of size one, and {@link #getComment}
	 * 		is sufficient and easier to use)
	 */
	public UnmodifiableList<String> getComments(String key) {
		return Unmodifiable.list(comments.get(key));
	}

	private void initDecoder() throws IOException {
		checkFreed();
		if (initialized)
			throw new IllegalStateException("This decoder has already been used");
		if (getMajorVersion() > 0)
			throw new OggFormatException("Don't know how to decode Opus v"+getMajorVersion()+"."+getMinorVersion());
		if (channelMappingFamily == 2)
			throw new OggFormatException("Ambisonic Opus files are not supported");
		if (channelMappingFamily != 0 && channelMappingFamily != 1)
			throw new OggFormatException("Unknown channel mapping family "+channelMappingFamily);
		if (channels > 2)
			throw new OggFormatException("Surround sound Opus files are not supported");
		if (channels == 0)
			throw new OggFormatException("0 channels is illegal");
		initialized = true;
		IntBuffer errBuf = memAllocInt(1);
		decoder = opus_decoder_create(48000, channels, errBuf);
		int err = errBuf.get();
		memFree(errBuf);
		if (err != OPUS_OK) {
			throw new IOException("Failed to initialize Opus decoder ["+getOpusErrorString(err)+"]");
		}
		pktBuf = memAlloc(2048);
		outBuf = memAllocShort(5760*channels);
	}

	/**
	 * Decode the next Ogg Opus packet and return the samples it contained,
	 * or null if there are no more packets. The returned buffer will become
	 * invalid when decodeStep is called again.
	 * @throws OggFormatException if the input stream does not contain valid
	 * 		Ogg Opus data
	 * @throws IOException if an IO error occurs
	 */
	public ShortBuffer decodeStep() throws IOException, OggFormatException {
		checkFreed();
		if (!initialized) initDecoder();
		byte[] packet = los.getNextOggPacket();
		if (packet == null) {
			free();
			return null;
		}
		pktBuf.rewind().limit(pktBuf.capacity());
		pktBuf.put(packet).flip();
		int framesize = opus_decoder_get_nb_samples(decoder, pktBuf);
		outBuf.rewind().limit(outBuf.capacity());
		outBuf = ensureCapacity(outBuf, framesize*channels);
		int decoded = opus_decode(decoder, pktBuf, outBuf, framesize, 0);
		if (decoded < 0) {
			throw new OggFormatException(getOpusErrorString(decoded));
		}
		if (needsPreSkip) {
			needsPreSkip = false;
			// TODO if the pre-skip is larger than one packet, this won't work properly
			outBuf.position(preSkip*channels);
		}
		outBuf.limit(decoded*channels);
		return outBuf.slice().asReadOnlyBuffer();
	}

	private static ShortBuffer ensureCapacity(ShortBuffer buf, int capacity) {
		if (buf.remaining() < capacity) {
			int newCap = ((buf.capacity()*3)/2)+capacity;
			System.out.println(buf.remaining()+" too small, need at least "+capacity+" - reallocating to "+newCap);
			buf = memRealloc(buf, newCap);
		}
		return buf;
	}

	/**
	 * Decode Ogg Opus packets until at least the given number of samples have
	 * been decoded, or the stream is exhausted, whichever comes first. If no
	 * samples are decoded, null is returned. The returned buffer is your
	 * responsibility and must be {@link MemoryUtil#memFree(java.nio.Buffer) freed}.
	 * @param sampleTarget the number of samples per channel to decode
	 * @throws OggFormatException if the input stream does not contain valid
	 * 		Ogg Opus data
	 * @throws IOException if an IO error occurs
	 */
	public ShortBuffer decodeAbout(int sampleTarget) throws OggFormatException, IOException {
		ShortBuffer out = memAllocShort(sampleTarget == Integer.MAX_VALUE ? 65536 : (sampleTarget*channels)+5760);
		try {
			while (out.position() < (sampleTarget*channels)) {
				ShortBuffer samples = decodeStep();
				if (samples == null) break;
				out = ensureCapacity(out, samples.remaining());
				out.put(samples);
			}
		} catch (IOException | RuntimeException | Error e) {
			memFree(out);
			throw e;
		}
		out.flip();
		return out;
	}

	/**
	 * Decode this entire Ogg Opus file all at once and return all of
	 * its samples as one array.
	 * @return all samples in this file
	 * @throws OggFormatException if the input stream does not contain valid
	 * 		Ogg Opus data
	 * @throws IOException if an IO error occurs
	 */
	public ShortBuffer decodeAll() throws OggFormatException, IOException {
		return decodeAbout(Integer.MAX_VALUE);
	}

	private static final Slice OPUS_HEAD = Slice.fromString("OpusHead", Charsets.UTF_8);
	private static final Slice OPUS_TAGS = Slice.fromString("OpusTags", Charsets.UTF_8);

	private static String getOpusErrorString(int error) {
		switch (error) {
			case OPUS_OK: return "No error";
			case OPUS_BAD_ARG: return "Bad argument to Opus decoder";
			case OPUS_BUFFER_TOO_SMALL: return "Buffer too small to decode Opus file";
			case OPUS_INTERNAL_ERROR: return "Internal Opus decoder error";
			case OPUS_INVALID_PACKET: return "Corrupt Opus file";
			case OPUS_UNIMPLEMENTED: return "Unimplemented method in Opus decoder";
			case OPUS_INVALID_STATE: return "Invalid state for Opus decoder";
			case OPUS_ALLOC_FAIL: throw new OutOfMemoryError();
		}
		return "Unknown error";
	}

	@Override
	protected void _free() {
		if (initialized) {
			opus_decoder_destroy(decoder);
			memFree(outBuf);
			memFree(pktBuf);
			try {
				ogg.close();
			} catch (IOException e) {
			}
		}
	}

	public static final class StandardComments {
		/**
		 * Track/Work name
		 */
		public static final String TITLE = "TITLE";
		/**
		 * The version field may be used to differentiate multiple versions of the same
		 * track title in a single collection. (e.g. remix info)
		 */
		public static final String VERSION = "VERSION";
		/**
		 * The collection name to which this track belongs
		 */
		public static final String ALBUM = "ALBUM";
		/**
		 * The track number of this piece if part of a specific larger collection or
		 * album
		 */
		public static final String TRACKNUMBER = "TRACKNUMBER";
		/**
		 * The artist generally considered responsible for the work. In popular music
		 * this is usually the performing band or singer. For classical music it would
		 * be the composer. For an audio book it would be the author of the original
		 * text.
		 */
		public static final String ARTIST = "ARTIST";
		/**
		 * The artist(s) who performed the work. In classical music this would be the
		 * conductor, orchestra, soloists. In an audio book it would be the actor who
		 * did the reading. In popular music this is typically the same as the ARTIST
		 * and is omitted.
		 */
		public static final String PERFORMER = "PERFORMER";
		/**
		 * Copyright attribution, e.g., '2001 Nobody's Band' or '1999 Jack Moffitt'
		 */
		public static final String COPYRIGHT = "COPYRIGHT";
		/**
		 * License information, eg, 'All Rights Reserved', 'Any Use Permitted', a URL to
		 * a license such as a Creative Commons license
		 * ("www.creativecommons.org/blahblah/license.html") or the EFF Open Audio
		 * License ('distributed under the terms of the Open Audio License. see
		 * http://www.eff.org/IP/Open_licenses/eff_oal.html for details'), etc.
		 */
		public static final String LICENSE = "LICENSE";
		/**
		 * Name of the organization producing the track (i.e. the 'record label')
		 */
		public static final String ORGANIZATION = "ORGANIZATION";
		/**
		 * A short text description of the contents
		 */
		public static final String DESCRIPTION = "DESCRIPTION";
		/**
		 * A short text indication of music genre
		 */
		public static final String GENRE = "GENRE";
		/**
		 * Date the track was recorded
		 */
		public static final String DATE = "DATE";
		/**
		 * Location where track was recorded
		 */
		public static final String LOCATION = "LOCATION";
		/**
		 * Contact information for the creators or distributors of the track. This could
		 * be a URL, an email address, the physical address of the producing label.
		 */
		public static final String CONTACT = "CONTACT";
		/**
		 * ISRC number for the track
		 */
		public static final String ISRC = "ISRC";

		private StandardComments() {}
	}

}

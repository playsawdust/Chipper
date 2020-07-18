/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.audio;

import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.SOFTDirectChannels.*;

import java.io.IOException;
import java.nio.ShortBuffer;
import org.lwjgl.openal.AL;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.common.primitives.Longs;
import com.playsawdust.chipper.client.audio.OggOpusDecoder.StandardComments;

import com.playsawdust.chipper.AbstractNativeResource;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Handles streaming an Ogg Opus file from an input stream
 * and performing OpenAL buffer swaps to avoid buffering the
 * entire file in memory.
 */
public class MusicOrchestrator extends AbstractNativeResource {
	private static final Logger log = LoggerFactory.getLogger(MusicOrchestrator.class);

	/**
	 * Marker class to throw an exception in setLooping.
	 */
	public static abstract class UnloopableByteSource extends ByteSource {}

	// how many 48kHz samples to buffer
	private final int BUFFER_SIZE = 48000;

	private final ByteSource in;
	private final int alSource;

	private OggOpusDecoder decoder;

	private boolean started = false;
	private boolean looping = false;

	private IntList free = new IntArrayList();

	private Runnable onEnd;

	private final Int2ObjectMap<ShortBuffer> currentSamples = new Int2ObjectOpenHashMap<>();
	private final Int2IntMap indexToBuffer = new Int2IntOpenHashMap();

	private int index = 0;
	private int processedBuffers = 0;

	private long totalSamples = 0;

	private String artist;
	private String title;

	/**
	 * Creates a new MusicOrchestrator and immediately creates the required
	 * OpenAL source, OpenAL buffers, and Ogg Opus decoder. The Ogg Opus
	 * headers will immediately be read from the stream.
	 */
	public MusicOrchestrator(ByteSource in) throws IOException {
		this.in = in;
		this.decoder = new OggOpusDecoder(in.openStream());
		artist = decoder.getComment(StandardComments.ARTIST);
		title = decoder.getComment(StandardComments.TITLE);
		Long total = Longs.tryParse(Strings.nullToEmpty(decoder.getComment("CHIPPER_DURATION_IN_SAMPLES")));
		if (total != null) {
			totalSamples = total;
		}
		this.alSource = alGenSources();
	}

	/**
	 * Must be called regularly on the main thread.
	 */
	public void update() {
		checkFreed();
		int buf = alSourceUnqueueBuffers(alSource);
		if (buf != 0) {
			processedBuffers++;
			free.add(buf);
			try {
				advance(0);
			} catch (IOException e) {
				log.error("IO exception while attempting to play music", e);
				decoder.free();
				looping = false;
			}
		}
	}

	private int advance(int depth) throws IOException {
		if (decoder.isFreed()) {
			if (looping) {
				decoder = new OggOpusDecoder(in.openStream());
			} else {
				if (onEnd != null) {
					onEnd.run();
					onEnd = null;
				}
				return -1;
			}
		}
		ShortBuffer samples = null;
		try {
			samples = decoder.decodeAbout(BUFFER_SIZE/2);
		} catch (IOException e) {
			log.error("Error during music decode", e);
			return -1;
		}
		if (samples == null) {
			if (looping) {
				if (depth > 5) {
					log.warn("Attempted to restart decoder 5 times, but still failed; killing music");
					looping = false;
					decoder.free();
					if (onEnd != null) {
						onEnd.run();
						onEnd = null;
					}
					return -1;
				}
				decoder.free();
				decoder = new OggOpusDecoder(in.openStream());
				return advance(depth++);
			} else {
				return -1;
			}
		}
		int buf = free.isEmpty() ? alGenBuffers() : free.removeInt(0);
		indexToBuffer.remove(index-4);
		indexToBuffer.put(index++, buf);
		int len = samples.remaining();
		alBufferData(buf, decoder.getChannels() == 2 ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16, samples, 48000);
		alSourceQueueBuffers(alSource, buf);
		ShortBuffer slice = samples.slice();
		if (currentSamples.containsKey(buf)) {
			MemoryUtil.memFree(currentSamples.get(buf));
		}
		currentSamples.put(buf, slice);
		return len;
	}

	/**
	 * Begin playing music. The decoder will be initialized if it hasn't
	 * been already.
	 */
	public void play() {
		checkFreed();
		if (!started) {
			started = true;
			alSourcei(alSource, AL_SOURCE_RELATIVE, AL_TRUE);
			alSourcei(alSource, AL_SOURCE_TYPE, AL_STREAMING);
			alSource3f(alSource, AL_POSITION, 0, 0, 0);
			if (AL.getCapabilities().AL_SOFT_direct_channels) {
				alSourcei(alSource, AL_DIRECT_CHANNELS_SOFT, AL_TRUE);
			}
			int total = -BUFFER_SIZE;
			while (total < BUFFER_SIZE) {
				try {
					int amt = advance(0);
					if (amt == -1) break;
					total += amt;
				} catch (IOException e) {
					log.error("Error while attempting to start music", e);
					decoder.free();
					return;
				}
			}
		}
		alSourcePlay(alSource);
	}

	/**
	 * Temporarily pause the music.
	 */
	public void pause() {
		checkFreed();
		alSourcePause(alSource);
	}

	public double getVU(int spread) {
		checkFreed();
		if (isPlaying()) {
			int offset = alGetSourcei(alSource, AL_SAMPLE_OFFSET);
			int bufferPrev = indexToBuffer.get(processedBuffers-2);
			ShortBuffer samplesPrev = currentSamples.get(bufferPrev);
			int buffer = indexToBuffer.get(processedBuffers-1);
			ShortBuffer samples = currentSamples.get(buffer);
			if (samples != null) {
				int count = 0;
				double accum = 0;
				for (int i = -spread; i <= 0; i++) {
					int idx = offset+i;
					short sample;
					if (idx < 0) {
						if (samplesPrev != null) {
							if (idx < -samplesPrev.limit()) continue;
							sample = samplesPrev.get(samplesPrev.limit()+idx);
						} else {
						continue;
						}
					} else {
						sample = samples.get(idx);
					}
					if (idx >= samples.limit()) continue;
					accum += Math.abs(sample/32768D);
					count++;
				}
				return accum/count;
			}
		}
		return -1;
	}

	public void setLooping(boolean looping) {
		if (looping && in instanceof UnloopableByteSource) {
			throw new IllegalStateException("Cannot enable looping on an unloopable source");
		}
		this.looping = looping;
	}

	public int getAlSource() {
		return alSource;
	}

	public boolean isPlaying() {
		return alGetSourcei(alSource, AL_SOURCE_STATE) == AL_PLAYING;
	}

	public String getArtist() {
		return artist;
	}

	public String getTitle() {
		return title;
	}

	public double getProgress() {
		if (totalSamples == 0) return 0;
		return ((processedBuffers*BUFFER_SIZE)+(alGetSourcei(alSource, AL_SAMPLE_OFFSET))*2)/(double)(totalSamples*decoder.getChannels());
	}

	public MusicOrchestrator onEnd(Runnable r) {
		this.onEnd = r;
		return this;
	}

	// TODO fade-in / fade-out

	@Override
	protected void _free() {
		decoder.free();
		for (int buf : free) {
			alDeleteBuffers(buf);
		}
		alDeleteSources(alSource);
	}

}

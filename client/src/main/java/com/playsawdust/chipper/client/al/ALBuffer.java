/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.al;

import static org.lwjgl.openal.AL11.*;

import java.nio.ShortBuffer;

import com.playsawdust.chipper.AbstractNativeResource;

public class ALBuffer extends AbstractNativeResource {
	private final int name;

	private ALBuffer(int name) {
		this.name = name;
	}

	/**
	 * <b>Unsafe</b>. ALBuffer assumes it has complete ownership over the
	 * buffer name it is holding, and this method can break that assumption.
	 */
	public int unsafeGetBufferName() {
		return name;
	}

	/**
	 * Delete this buffer. This object becomes invalid and the audio data is
	 * deallocated.
	 */
	@Override
	protected void _free() {
		alDeleteBuffers(name);
	}

	/**
	 * Upload the given 16-bit signed audio data to this ALBuffer.
	 * @param sampleRate the sample rate of the audio - usually 48000
	 * @param channels the number of channels - 1 for mono, 2 for stereo
	 * @param data the 16-bit signed audio data
	 */
	public void upload(int sampleRate, int channels, short[] data) {
		checkFreed();
		int type = channelsToType(channels);
		alBufferData(name, type, data, sampleRate);
	}

	/**
	 * Upload the given 16-bit signed audio data to this ALBuffer.
	 * @param sampleRate the sample rate of the audio - usually 48000
	 * @param channels the number of channels - 1 for mono, 2 for stereo
	 * @param data the 16-bit signed audio data
	 */
	public void upload(int sampleRate, int channels, ShortBuffer data) {
		checkFreed();
		int type = channelsToType(channels);
		alBufferData(name, type, data, sampleRate);
	}

	private static int channelsToType(int channels) {
		switch (channels) {
			case 1:
				return AL_FORMAT_MONO16;
			case 2:
				return AL_FORMAT_STEREO16;
			default: throw new IllegalArgumentException("Invalid number of channels - only mono and stereo are supported");
		}
	}

	/**
	 * Allocate a new ALBuffer.
	 * @return a newly allocated ALBuffer
	 */
	public static ALBuffer allocate() {
		return new ALBuffer(alGenBuffers());
	}

	/**
	 * <b>Unsafe</b>. ALBuffer assumes it has complete ownership over the
	 * buffer name it is holding, and this breaks that assumption.
	 * @param name the texture name to wrap
	 * @return a newly constructed ALBuffer representing the given buffer name
	 */
	public static ALBuffer unsafeFromBufferName(int name) {
		return new ALBuffer(name);
	}

}

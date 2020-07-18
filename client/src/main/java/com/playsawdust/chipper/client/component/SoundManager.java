/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.component;

import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.SoundManagerInternalAccess;
import com.playsawdust.chipper.client.al.ALBuffer;
import com.playsawdust.chipper.client.audio.MusicOrchestrator;
import com.playsawdust.chipper.client.audio.MusicOrchestrator.UnloopableByteSource;
import com.playsawdust.chipper.component.Component;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.component.Context.WhiteLotus;
import com.playsawdust.chipper.toolbox.lipstick.MonotonicTime;

public class SoundManager extends SoundManagerInternalAccess implements Component {
	private static final Logger log = LoggerFactory.getLogger(SoundManager.class);

	private List<Integer> scratchSources = Lists.newArrayList();
	private MusicOrchestrator music;
	private boolean ready = false;

	private FloatBuffer orientationScratch = MemoryUtil.memAllocFloat(6);

	private SoundManager(WhiteLotus lotus) {
		WhiteLotus.verify(lotus);
	}

	@Override
	protected void init() {
		ALCCapabilities alcCapsNull = ALC.createCapabilities(NULL);
		if (alcCapsNull.OpenALC11) {
			// TODO let the user pick a device
			String def = alcGetString(NULL, ALC_DEFAULT_ALL_DEVICES_SPECIFIER);
			if (def != null) {
				long dev = alcOpenDevice(def);
				if (dev != NULL) {
					ALCCapabilities alcCaps = ALC.createCapabilities(dev);
					long ctx = alcCreateContext(dev, new int[] {
							ALC_MONO_SOURCES, 65535-256,
							ALC_STEREO_SOURCES, 256,
							0
					});
					if (ctx != NULL) {
						alcMakeContextCurrent(ctx);
						AL.createCapabilities(alcCaps);
						log.debug("OpenALC {}.{} OpenAL {}",
								alcGetInteger(dev, ALC_MAJOR_VERSION), alcGetInteger(dev, ALC_MINOR_VERSION), alGetString(AL_VERSION));
						log.debug("freq({}) refresh({}) sync({}) sources({}m, {}s)",
								alcGetInteger(dev, ALC_FREQUENCY), alcGetInteger(dev, ALC_REFRESH),
								alcGetInteger(dev, ALC_SYNC) == ALC_TRUE, alcGetInteger(dev, ALC_MONO_SOURCES), alcGetInteger(dev, ALC_STEREO_SOURCES));
						log.debug("{}", alGetString(AL_RENDERER));
					} else {
						log.warn("Failed to create audio context. Sound will not play.");
					}
				} else {
					log.warn("Failed to open default audio device. Sound will not play.");
				}
			}
		} else {
			log.warn("OpenAL 1.1 is not supported. Sound will not play.\n(This shouldn't be possible - did you replace the OpenAL Soft natives with a hardware driver?)");
		}
		ready = true;
		alDopplerFactor(1.0f);
		alSpeedOfSound(340.27f);
	}

	/**
	 * Set the current listener position to the given position.
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @param z the Z coordinate
	 */
	public void setListenerPosition(double x, double y, double z) {
		if (!ready) return;
		alListener3f(AL_POSITION, (float)x, (float)y, (float)z);
	}

	/**
	 * Set the current listener velocity to the given velocity.
	 * @param x the X velocity
	 * @param y the Y velocity
	 * @param z the Z velocity
	 */
	public void setListenerVelocity(double x, double y, double z) {
		if (!ready) return;
		alListener3f(AL_VELOCITY, (float)x, (float)y, (float)z);
	}

	/**
	 * Set the current listener orientation to the given vectors. Each triplet
	 * must represent a unit vector.
	 * @param lookX the X coordinate the listener is looking toward
	 * @param lookY the Y coordinate the listener is looking toward
	 * @param lookZ the Z coordinate the listener is looking toward
	 * @param upX the X coordinate the top of the listener's head is pointing towards
	 * @param upY the Y coordinate the top of the listener's head is pointing towards
	 * @param upZ the Z coordinate the top of the listener's head is pointing towards
	 */
	public void setListenerOrientation(double lookX, double lookY, double lookZ,
			double upX, double upY, double upZ) {
		if (!ready) return;
		orientationScratch.put((float)lookX).put((float)lookY).put((float)lookZ);
		orientationScratch.put((float)upX).put((float)upY).put((float)upZ);
		orientationScratch.flip();
		alListenerfv(AL_ORIENTATION, orientationScratch);
	}

	/**
	 * Play a sound clip with the given pitch and volume multiplier at the
	 * given position.
	 * @param clip the clip to play
	 * @param pitch the pitch multiplier - 0.5 is half pitch, 2.0 is double
	 * 		(values outside this range are valid but quickly become distorted)
	 * @param volume the volume multiplier - 0.0 is silent, 1.0 is full volume
	 */
	public void playClipAt(ALBuffer clip, double pitch, double volume, double x, double y, double z) {
		if (!ready) return;
		playClip(clip, pitch, volume, x, y, z, AL_SOURCE_ABSOLUTE);
	}

	/**
	 * Play a sound clip with the given pitch and volume multiplier with no
	 * volume attenuation. Good for menu sounds.
	 * @param clip the clip to play
	 * @param pitch the pitch multiplier - 0.5 is half pitch, 2.0 is double
	 * 		(values outside this range are valid but quickly become distorted)
	 * @param volume the volume multiplier - 0.0 is silent, 1.0 is full volume
	 */
	public void playClip(ALBuffer clip, double pitch, double volume) {
		if (!ready) return;
		playClip(clip, pitch, volume, 0, 0, 0, AL_SOURCE_RELATIVE);
	}

	private void playClip(ALBuffer clip, double pitch, double volume, double x, double y, double z, int sourceType) {
		int source = findUnusedSource();
		alSourcei(source, AL_BUFFER, clip.unsafeGetBufferName());
		alSourcei(source, AL_SOURCE_TYPE, sourceType);
		alSource3f(source, AL_POSITION, (float)x, (float)y, (float)z);
		alSourcef(source, AL_GAIN, (float)volume);
		alSourcef(source, AL_PITCH, (float)pitch);
		alSourcePlay(source);
	}

	/**
	 * Start playing Ogg Opus music from the given input stream. As input streams cannot be rewound,
	 * it cannot be looped.
	 * @param source the input stream to read from
	 * @throws IOException if an IO error occurs or the data is invalid
	 */
	public void playMusic(InputStream source) throws IOException {
		playMusic(new UnloopableByteSource() {
			/**
			 * This breaks the contract of openStream, but is okay for use with MusicOrchestrator
			 * as long as looping isn't set to true.
			 */
			@Override
			public InputStream openStream() throws IOException {
				return source;
			}
		}, false);
	}

	/**
	 * Start playing Ogg Opus music from the given input stream. As input streams cannot be rewound,
	 * it cannot be looped. The given Runnable will be executed when the music ends.
	 * @param source the input stream to read from
	 * @throws IOException if an IO error occurs or the data is invalid
	 */
	public void playMusic(InputStream source, Runnable onEnd) throws IOException {
		playMusic(source);
		if (music != null) {
			music.onEnd(onEnd);
		}
	}

	/**
	 * Start playing Ogg Opus music from the given byte source, and optionally loop it when it
	 * ends.
	 * @param source the byte source to read from
	 * @param loop {@code true} to endlessly loop the music
	 * @throws IOException if an IO error occurs or the data is invalid
	 */
	public void playMusic(ByteSource source, boolean loop) throws IOException {
		if (!ready) return;
		music = new MusicOrchestrator(source);
		music.setLooping(loop);
		music.play();
	}

	public void setMusicLooping(boolean looping) {
		if (!ready) return;
		if (music != null) {
			music.setLooping(looping);
		}
	}

	public void setMusicOnEndListener(Runnable onEnd) {
		if (music != null) {
			music.onEnd(onEnd);
		}
	}

	public boolean isMusicPlaying() {
		return ready && music != null && music.isPlaying();
	}

	public void stopMusic() {
		if (!ready) return;
		if (music != null) {
			music.free();
			music = null;
		}
	}

	public MusicOrchestrator getMusic() {
		return music != null && !music.isFreed() ? music : null;
	}

	@Override
	protected void update() {
		if (!ready) return;

		if (music != null) {
			if (music.isFreed()) {
				music = null;
			} else {
				music.update();
			}
		}
	}

	@Override
	protected void stop() {
		if (!ready) return;
		boolean allStopped = allStopped();
		if (allStopped && music == null) {
			log.info("Nothing is playing. Not delaying shutdown.");
			return;
		}
		double lastTime = MonotonicTime.seconds();
		double time = 0;
		if (allStopped) {
			log.info("Fading out music...");
		} else if (music != null) {
			log.info("Fading out music and waiting for sounds to stop...");
		} else {
			log.info("Waiting for sounds to stop...");
		}
		while (true) {
			double delta = MonotonicTime.seconds()-lastTime;
			lastTime = MonotonicTime.seconds();
			time += delta;
			if (time >= 3) {
				log.info("More than 3 seconds have passed. Quitting now.");
				break;
			}
			if (music == null) {
				if (allStopped()) break;
			}
			if (music != null) {
				if (time >= 0.4) {
					music.free();
					music = null;
				} else if (time < 0.4) {
					float vol = (float)(1-(time/0.4));
					alSourcef(music.getAlSource(), AL_GAIN, vol);
				}
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// wait an additional 100ms for buffers to drain
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void destroy() {
		long ctx = alcGetCurrentContext();
		if (ctx != NULL) {
			long dev = alcGetContextsDevice(ctx);
			alcMakeContextCurrent(NULL);
			alcDestroyContext(ctx);
			alcCloseDevice(dev);
		}
	}

	private boolean allStopped() {
		boolean allStopped = true;
		for (Integer i : scratchSources) {
			if (alGetSourcei(i, AL_SOURCE_STATE) != AL_STOPPED) {
				allStopped = false;
				break;
			}
		}
		return allStopped;
	}

	private int findUnusedSource() {
		for (Integer i : scratchSources) {
			if (alGetSourcei(i, AL_SOURCE_STATE) == AL_STOPPED) {
				return i;
			}
		}
		int i = alGenSources();
		scratchSources.add(i);
		return i;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		MemoryUtil.memFree(orientationScratch);
	}

	public static SoundManager obtain(Context<? extends ClientEngine> ctx) {
		return ctx.getComponent(SoundManager.class);
	}

	@Override
	public boolean compatibleWith(Engine engine) {
		return engine instanceof ClientEngine;
	}


}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import java.util.Locale;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Actual boolean properties of the GPU. These are few and far between; most booleans are toggles available in {@link GLFeatureToggle}.
 */
public enum GLBoolean {
	DEPTH_WRITEMASK       (11, GL11.GL_DEPTH_WRITEMASK, GL11::glDepthMask),
	//DOUBLEBUFFER          (11, GL11.GL_DOUBLEBUFFER), //immutable; whether doublebuffering is supported
	PACK_LSB_FIRST        (11, GL11.GL_PACK_LSB_FIRST,    (b)->GL30.glPixelStorei(GL11.GL_PACK_LSB_FIRST,  (b) ? GL11.GL_TRUE : GL11.GL_FALSE)),
	PACK_SWAP_BYTES       (11, GL11.GL_PACK_SWAP_BYTES,   (b)->GL30.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, (b) ? GL11.GL_TRUE : GL11.GL_FALSE)),
	//SAMPLE_COVERAGE_INVERT(13, GL13.GL_SAMPLE_COVERAGE_INVERT, (b)->GL13.glSampleCoverage(GL13.glGetInteger(GL13.GL_SAMPLE_COVERAGE_VALUE), b)), //Slow but non-destructive
	//SHADER_COMPILER       (41, GL41.GL_SHADER_COMPILER), // Immutable, Always true for desktops.
	//STEREO                (11, GL11.GL_STEREO), // Constant attrib
	UNPACK_LSB_FIRST      (11, GL11.GL_UNPACK_LSB_FIRST,  (b)->GL30.glPixelStorei(GL11.GL_UNPACK_LSB_FIRST,  (b) ? GL11.GL_TRUE : GL11.GL_FALSE)),
	UNPACK_SWAP_BYTES     (11, GL11.GL_UNPACK_SWAP_BYTES, (b)->GL30.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, (b) ? GL11.GL_TRUE : GL11.GL_FALSE))
	;
	
	private static Int2ObjectMap<GLBoolean> REVERSE_LOOKUP = new Int2ObjectOpenHashMap<>();
	static {
		for(GLBoolean i : values()) {
			REVERSE_LOOKUP.put(i.value, i);
		}
	}
	
	private final int value;
	private final int minVersion;
	private final @Nullable BooleanConsumer setFunction;
	
	GLBoolean(int value) {
		this.value = value;
		this.minVersion = 11;
		this.setFunction = null;
	}
	
	GLBoolean(int version, int value) {
		this.value = value;
		this.minVersion = version;
		this.setFunction = null;
	}
	
	GLBoolean(int version, int value, BooleanConsumer setFunction) {
		this.value = value;
		this.minVersion = version;
		this.setFunction = setFunction;
	}
	
	public int glConstant() {
		return value;
	}
	
	public int minimumVersion() {
		return minVersion;
	}
	
	//public boolean query() {
	//	return GL11.glGetBoolean(value);
	//}
	
	protected void set(boolean value) {
		if (setFunction==null) throw new UnsupportedOperationException("GL boolean "+this.name().toLowerCase(Locale.ROOT)+" is immutable.");
		setFunction.accept(value);
	}
	
	@Nullable
	public static final GLBoolean forConstant(int value) {
		return REVERSE_LOOKUP.get(value);
	}
}

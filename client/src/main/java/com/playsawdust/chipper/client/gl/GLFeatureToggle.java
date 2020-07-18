/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.EnumSet;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL43;

/**
 * As of OpenGL 4.3, this is an exhaustive list of every constant that is appropriate for glEnable, glDisable, and glIsEnabled.
 */
public enum GLFeatureToggle {
	BLEND                    (11, GL11.GL_BLEND),
	CLIP_DISTANCE0           (30, GL30.GL_CLIP_DISTANCE0),
	CLIP_DISTANCE1           (30, GL30.GL_CLIP_DISTANCE1),
	CLIP_DISTANCE2           (30, GL30.GL_CLIP_DISTANCE2),
	CLIP_DISTANCE3           (30, GL30.GL_CLIP_DISTANCE3),
	CLIP_DISTANCE4           (30, GL30.GL_CLIP_DISTANCE4),
	CLIP_DISTANCE5           (30, GL30.GL_CLIP_DISTANCE5),
	CLIP_DISTANCE6           (30, GL30.GL_CLIP_DISTANCE6),
	CLIP_DISTANCE7           (30, GL30.GL_CLIP_DISTANCE7),
	COLOR_LOGIC_OP           (11, GL11.GL_COLOR_LOGIC_OP),
	CULL_FACE                (11, GL11.GL_CULL_FACE),
	DEPTH_CLAMP              (32, GL32.GL_DEPTH_CLAMP),
	DEBUG_OUTPUT             (43, GL43.GL_DEBUG_OUTPUT),
	DEBUG_OUTPUT_SYNCHRONOUS (43, GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS),
	DEPTH_TEST               (11, GL11.GL_DEPTH_TEST),
	DITHER                   (11, GL11.GL_DITHER),
	FRAMEBUFFER_SRGB         (30, GL30.GL_FRAMEBUFFER_SRGB),
	POINT_SMOOTH             (11, GL11.GL_POINT_SMOOTH),
	LINE_SMOOTH              (11, GL11.GL_LINE_SMOOTH),
	MULTISAMPLE              (13, GL13.GL_MULTISAMPLE),
	POLYGON_SMOOTH           (11, GL11.GL_POLYGON_SMOOTH),
	POLYGON_OFFSET_FILL      (11, GL11.GL_POLYGON_OFFSET_FILL),
	POLYGON_OFFSET_LINE      (11, GL11.GL_POLYGON_OFFSET_LINE),
	POLYGON_OFFSET_POINT     (11, GL11.GL_POLYGON_OFFSET_POINT),
	PROGRAM_POINT_SIZE       (32, GL32.GL_PROGRAM_POINT_SIZE),
	PRIMITIVE_RESTART        (31, GL31.GL_PRIMITIVE_RESTART),
	SAMPLE_ALPHA_TO_COVERAGE (13, GL13.GL_SAMPLE_ALPHA_TO_COVERAGE),
	SAMPLE_ALPHA_TO_ONE      (13, GL13.GL_SAMPLE_ALPHA_TO_ONE),
	SAMPLE_COVERAGE          (13, GL13.GL_SAMPLE_COVERAGE),
	SAMPLE_MASK              (32, GL32.GL_SAMPLE_MASK),
	SCISSOR_TEST             (11, GL11.GL_SCISSOR_TEST),
	STENCIL_TEST             (11, GL11.GL_STENCIL_TEST),
	TEXTURE_CUBE_MAP_SEAMLESS(32, GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS),

	//The following are deprecated in Core 3.0 and removed in Core 3.1
	@Deprecated
	COLOR_MATERIAL           (11, GL11.GL_COLOR_MATERIAL),
	@Deprecated
	LIGHTING                 (11, GL11.GL_LIGHTING),
	@Deprecated
	LIGHT0                   (11, GL11.GL_LIGHT0),
	@Deprecated
	LIGHT1                   (11, GL11.GL_LIGHT1),
	@Deprecated
	LIGHT2                   (11, GL11.GL_LIGHT2),
	@Deprecated
	LIGHT3                   (11, GL11.GL_LIGHT3),
	@Deprecated
	LIGHT4                   (11, GL11.GL_LIGHT4),
	@Deprecated
	LIGHT5                   (11, GL11.GL_LIGHT5),
	@Deprecated
	LIGHT6                   (11, GL11.GL_LIGHT6),
	@Deprecated
	LIGHT7                   (11, GL11.GL_LIGHT7),
	@Deprecated
	TEXTURE_1D               (11, GL11.GL_TEXTURE_1D),
	@Deprecated
	TEXTURE_2D               (11, GL11.GL_TEXTURE_2D),
	@Deprecated
	TEXTURE_3D               (12, GL12.GL_TEXTURE_3D),
	@Deprecated
	RESCALE_NORMAL           (12, GL12.GL_RESCALE_NORMAL)
	;

	private static Int2ObjectMap<GLFeatureToggle> REVERSE_LOOKUP = new Int2ObjectOpenHashMap<>();
	static {
		for(GLFeatureToggle i : values()) {
			REVERSE_LOOKUP.put(i.value, i);
		}
	}

	private final int value;
	private final int minVersion;

	GLFeatureToggle(int value) {
		this.value = value;
		this.minVersion = 11;
	}

	GLFeatureToggle(int version, int value) {
		this.value = value;
		this.minVersion = version;
	}

	public int minimumVersion() {
		return minVersion;
	}

	//public void setEnabled(boolean enable) {
	//	if (enable) {
	//		GL11.glEnable(value);
	//	} else {
	//		GL11.glDisable(value);
	//	}
	//}

	//public boolean isEnabled() {
	//	return GL11.glIsEnabled(value);
	//}
	
	public int glConstant() {
		return value;
	}

	@Nullable
	public static GLFeatureToggle forConstant(int value) {
		return REVERSE_LOOKUP.get(value);
	}
}

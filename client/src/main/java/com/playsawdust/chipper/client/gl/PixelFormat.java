/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import static org.lwjgl.opengl.GL11.GL_ALPHA8;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE8;
import static org.lwjgl.opengl.GL11.GL_RGB8;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL30.GL_BGRA_INTEGER;

public enum PixelFormat {
	RGBA(GL_RGBA8),
	RGB(GL_RGB8),
	ALPHA(GL_ALPHA8),
	LUMINANCE(GL_LUMINANCE8),
	/** Java's ordinary sRGB colors, when viewed as little-endian. */
	BGRA(GL_BGRA_INTEGER),
	;
	final int glConstant;
	private PixelFormat(int glConstant) {
		this.glConstant = glConstant;
	}
}
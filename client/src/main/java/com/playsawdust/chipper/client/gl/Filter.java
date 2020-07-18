/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;

public enum Filter {
	/**
	 * "Pixelated" nearest-neighbor scaling.
	 */
	NEAREST(GL_NEAREST),
	/**
	 * "Smooth" linear scaling.
	 */
	LINEAR(GL_LINEAR),
	;
	final int glConstant;
	private Filter(int glConstant) {
		this.glConstant = glConstant;
	}
}
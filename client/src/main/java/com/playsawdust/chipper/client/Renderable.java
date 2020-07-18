/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.component.Context;

/**
 * Represents something that can be rendered with OpenGL.
 */
public interface Renderable {
	/**
	 * Render this Renderable.
	 */
	void render(Context<ClientEngine> ctx);
	/**
	 * Returns true if this Renderable is "plain" - i.e., it is completely featureless,
	 * other than a simple color or gradient. Used as a hint to disable the frosted glass
	 * effect for speed.
	 *
	 * @return {@code true} if this Renderable is "plain"
	 */
	default boolean isPlain() {
		return false;
	}
}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl.builder;

import static com.playsawdust.chipper.client.BaseGL.*;

public class DisplayListVertexBuilder extends ImmediateModeVertexBuilder {

	private final int list;

	public DisplayListVertexBuilder(int list) {
		this.list = list;
	}

	@Override
	public void end() {
		glNewList(list, GL_COMPILE);
		super.end();
		glEndList();
	}

}

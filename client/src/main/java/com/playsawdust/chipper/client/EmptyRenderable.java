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
 * An implementation of Renderable that draws nothing.
 */
public final class EmptyRenderable implements Renderable {

	@Override
	public void render(Context<ClientEngine> ctx) {
		// no-op
	}

	@Override
	public boolean isPlain() {
		return true;
	}

}

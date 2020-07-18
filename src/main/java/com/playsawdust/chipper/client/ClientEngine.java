/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import com.playsawdust.chipper.Addon;
import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.component.EngineType;

/**
 * Dummy class. Replaced by the
 * <a href="../../../../../../../client/src/main/java/com/playsawdust/chipper/client/ClientEngine.java">actual ClientEngine</a>
 * in the client codebase for client builds.
 */
public class ClientEngine implements Engine {

	public ClientEngine() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Addon getDefaultAddon() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int run(String... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EngineType getType() {
		throw new UnsupportedOperationException();
	}

}

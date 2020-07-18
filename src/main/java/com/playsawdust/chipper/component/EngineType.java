/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.component;

import java.util.Locale;

public enum EngineType {
	/**
	 * A headless client, such as one used for performing automated tests. Graphics, audio, etc
	 * will all be disconnected no-ops.
	 */
	HEADLESS_CLIENT,
	/**
	 * A full client with graphics and audio, not sharing with other clients.
	 */
	SOLE_CLIENT,
	/**
	 * One of many clients running in a single process, possibly in split screen.
	 */
	ZAPHOD_CLIENT,

	/**
	 * A server running in the same process as a client, only processing messages for that
	 * client over a fast memory channel.
	 */
	SOLDERED_SERVER,
	/**
	 * A server running in the same process as a client, but accepting connections from other
	 * clients.
	 */
	DESOLDERED_SERVER,
	/**
	 * A server running in a dedicated process.
	 */
	DEDICATED_SERVER,
	;

	private final boolean isServer;
	private final String toString;
	
	private EngineType() {
		if (name().endsWith("_SERVER")) {
			isServer = true;
		} else if (name().endsWith("_CLIENT")) {
			isServer = false;
		} else {
			throw new AssertionError("Invalid EngineType constant name "+name()+" - must end in _CLIENT or _SERVER");
		}
		this.toString = name().replace('_', ' ').toLowerCase(Locale.ROOT);
	}

	/**
	 * @return {@code true} if this EngineType represents a client, {@code false} if it does not
	 */
	public boolean isClient() {
		return !isServer;
	}
	/**
	 * @return {@code true} if this EngineType represents a server, {@code false} if it does not
	 */
	public boolean isServer() {
		return isServer;
	}

	@Override
	public String toString() {
		return toString;
	}
}
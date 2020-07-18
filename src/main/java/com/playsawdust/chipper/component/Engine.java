/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.component;

import com.playsawdust.chipper.Addon;

public interface Engine {

	// TODO replace with addon loader system
	/**
	 * @deprecated Not API. Will be removed soon and replaced with a proper addon loader.
	 */
	@Deprecated
	Addon getDefaultAddon();

	/**
	 * Run this Engine. Does not return until the game is finished running.
	 * @param args the command-line arguments to the Engine
	 * @return the exit code; 0 is success
	 * @throws IllegalStateException if the Engine is already running
	 */
	int run(String... args);

	/**
	 * Dynamically determine what kind of Engine this is. Useful for guarding {@code obtain} calls.
	 */
	EngineType getType();

}

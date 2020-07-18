/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.qual.unification.ClientOnly;
import com.playsawdust.chipper.qual.unification.Unified;

public interface Addon {

	/**
	 * <i><b>Note:</b> This is a {@link Unified unified method}. Depending on if this is a server or
	 * client, certain components may be unavailable in the given Context. Be sure to
	 * {@link Context#getEngineType check}.</i>
	 * <p>
	 * Called while a loader is visible and the game has not finished initializing.
	 * <p>
	 * On the server, this means the socket has not been opened. On the client, this means the
	 * OpenGL context is currently owned by another thread.
	 * <p>
	 * This is where you should load any needed resources and spin off threads to do network
	 * operations, or any other tasks that can be run early or would block the UI if run later.
	 */
	@Unified
	void load(Context<?> ctx, Loader loader);

	/**
	 * <i><b>Note:</b> This is a {@link Unified unified method}. Depending on if this is a server or
	 * client, certain components may be unavailable in the given Context. Be sure to
	 * {@link Context#getEngineType check}.</i>
	 * <p>
	 * Called after the loader has been dismissed and the game is ready to run.
	 * <p>
	 * Implementations of this method must spend as little time as possible; if they spend too
	 * much time, the game will freeze. Long-running operations must be performed in {@link #load}
	 * so progress updates can be shown to the user. Only do things here that depend on having the
	 * socket open on the server or owning contexts on the client.
	 */
	@Unified
	void init(Context<?> ctx);

	/**
	 * <i><b>Note:</b> This is a {@link Unified unified method}. Depending on if this is a server or
	 * client, certain components may be unavailable in the given Context. Be sure to
	 * {@link Context#getEngineType check}.</i>
	 * <p>
	 * Called when the game is exiting.
	 */
	@Unified
	void exit(Context<?> ctx);

	/**
	 * Called every frame. TODO replace with some kind of frame listener
	 */
	@ClientOnly
	void preFrame(Context<ClientEngine> ctx);

	/**
	 * Called every frame. TODO replace with some kind of frame listener
	 */
	@ClientOnly
	void postFrame(Context<ClientEngine> ctx);

}

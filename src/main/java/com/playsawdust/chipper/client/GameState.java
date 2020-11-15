/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.playsawdust.chipper.component.Context;

/**
 * Lifecycle class representing a "state" the game can be in, such as "main menu" or "in-game".
 * Generally responsible for setting up root Renderables and widget layers.
 * <p>
 * Invariants:
 * <ul>
 * <li>setUp will only be called if the object is newly constructed or followed by a previous tearDown.</li>
 * <li>preFrame and postFrame will only be called after setUp and before tearDown.</li>
 * <li>tearDown will always be called after setUp.</li>
 * <li>setUp will never be called more than once in a row; it will always be separated by calls to tearDown.</li>
 * </ul>
 * These invariants are upheld by a state engine.
 * @see LayerController
 */
public abstract class GameState {

	private static final Logger log = LoggerFactory.getLogger(GameState.class);

	public enum LifecycleStage {
		// strings are to avoid "Cannot reference a field before it is defined" errors
		/**
		 * The GameState has been constructed and is ready to be set up.
		 */
		CONSTRUCTED("SETTING_UP"),
		/**
		 * The GameState is in the process of setting up.
		 */
		SETTING_UP("READY"),
		/**
		 * The GameState has been set up and is ready to receive events or be torn down.
		 */
		READY("TEARING_DOWN"),
		/**
		 * The GameState is in the process of tearing down.
		 */
		TEARING_DOWN("TORN_DOWN"),
		/**
		 * The GameState has been torn down, and is ready to be set up again.
		 */
		TORN_DOWN("SETTING_UP"),
		/**
		 * An unexpected exception has been thrown by the GameState, and it must be reconstructed
		 * from scratch.
		 */
		ERRORED(null)
		;
		private final String nextStr;
		private LifecycleStage next;
		LifecycleStage(String next) {
			this.nextStr = next;
		}
		static {
			for (LifecycleStage stage : values()) {
				stage.next = stage.nextStr == null ? null : valueOf(stage.nextStr);
			}
		}
	}

	private LifecycleStage stage = LifecycleStage.CONSTRUCTED;

	private void advance(LifecycleStage nextStage) {
		if (nextStage != stage.next) {
			if (stage.next == null) {
				throw new IllegalStateException("Attempted to move GameState from stage "+stage+" to "+nextStage+"; the "+stage+" stage cannot be left, the object must be reconstructed");
			} else {
				throw new IllegalStateException("Attempted to move GameState from stage "+stage+" to "+nextStage+"; the only valid next stage is "+stage.next);
			}
		}
		this.stage = nextStage;
	}

	private void error(String method, Throwable t) {
		log.warn("GameState subclass {} threw an exception in {}, forcing to stage ERRORED", getClass().getSimpleName(), method, t);
		this.stage = LifecycleStage.ERRORED;
	}

	public boolean isReady() {
		return stage == LifecycleStage.READY;
	}

	public boolean isErrored() {
		return stage == LifecycleStage.ERRORED;
	}

	public LifecycleStage getStage() {
		return stage;
	}

	/**
	 * Register or allocate any resources needed by this GameState, such as setting the
	 * {@link LayerController} root Renderable, or adding Widget layers, uploading textures to the
	 * GPU, etc.
	 */
	public final void setUp(Context<ClientEngine> ctx) {
		advance(LifecycleStage.SETTING_UP);
		try {
			_setUp(ctx);
		} catch (RuntimeException | Error e) {
			error("setUp", e);
			throw e;
		}
		advance(LifecycleStage.READY);
	}

	/**
	 * Called at the beginning of each frame, before any rendering has occurred. Useful for update
	 * code that needs called every frame, as a Renderable's render method may be skipped due to
	 * various optimizations.
	 * @see #postFrame
	 */
	public final void preFrame(Context<ClientEngine> ctx) {
		if (stage != LifecycleStage.READY) throw new IllegalStateException("Cannot call preFrame in stage "+stage);
		try {
			_preFrame(ctx);
		} catch (RuntimeException | Error e) {
			error("preFrame", e);
			throw e;
		}
	}

	/**
	 * Called at the end of each frame, after any rendering has occurred, but before buffers are
	 * swapped.
	 * @see #preFrame
	 */
	public final void postFrame(Context<ClientEngine> ctx) {
		if (stage != LifecycleStage.READY) throw new IllegalStateException("Cannot call postFrame in stage "+stage);
		try {
			_postFrame(ctx);
		} catch (RuntimeException | Error e) {
			error("postFrame", e);
			throw e;
		}
	}

	/**
	 * Unregister or deallocate any resources used by this GameState, such as clearing the
	 * {@link LayerController} root Renderable, removing Widget layers, deleting GPU objects, etc.
	 */
	public final void tearDown(Context<ClientEngine> ctx) {
		advance(LifecycleStage.TEARING_DOWN);
		try {
			_tearDown(ctx);
		} catch (RuntimeException | Error e) {
			error("tearDown", e);
			throw e;
		}
		advance(LifecycleStage.TORN_DOWN);
	}

	/**
	 * Register or allocate any resources needed by this GameState, such as setting the
	 * {@link LayerController} root Renderable, or adding Widget layers, uploading textures to the
	 * GPU, etc.
	 */
	protected abstract void _setUp(Context<ClientEngine> ctx);

	/**
	 * Called at the beginning of each frame, before any rendering has occurred. Useful for update
	 * code that needs called every frame, as a Renderable's render method may be skipped due to
	 * various optimizations.
	 * @see #_postFrame
	 */
	protected abstract void _preFrame(Context<ClientEngine> ctx);

	/**
	 * Called at the end of each frame, after any rendering has occurred, but before buffers are
	 * swapped.
	 * @see #_preFrame
	 */
	protected abstract void _postFrame(Context<ClientEngine> ctx);

	/**
	 * Unregister or deallocate any resources used by this GameState, such as clearing the
	 * {@link LayerController} root Renderable, removing Widget layers, deleting GPU objects, etc.
	 */
	protected abstract void _tearDown(Context<ClientEngine> ctx);

	/**
	 * Switch to the given GameState, performing proper cleanup of the current GameState, if any.
	 * @param ctx the context
	 * @param state the state to switch to
	 */
	public static void switchTo(Context<ClientEngine> ctx, GameState state) {
		ctx.getEngine().switchToState(state);
	}

}

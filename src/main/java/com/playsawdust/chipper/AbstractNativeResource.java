/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import org.lwjgl.system.NativeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.playsawdust.chipper.exception.UseAfterFreeException;

public abstract class AbstractNativeResource implements NativeResource {
	private static final Logger log = LoggerFactory.getLogger("NativeResources");

	private boolean freed = false;

	protected abstract void _free();

	/**
	 * @return {@code true} if {@link #free} has been called and this resource
	 * 		is no longer valid
	 */
	public boolean isFreed() {
		return freed;
	}

	@Override
	public final void free() {
		if (freed) return;
		try {
			_free();
		} finally {
			freed = true;
		}
	}

	/**
	 * @throws UseAfterFreeException if this resource has been {@link #free freed}
	 */
	protected void checkFreed() throws UseAfterFreeException {
		if (freed) throw new UseAfterFreeException();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (!freed) {
			log.warn("A native resource, {}, is being garbage collected, but it was never explicitly freed!\n"
					+ "Before releasing references to a native resource, you should call free to ensure it is immediately deallocated.", getClass().getSimpleName());
			free();
		}
	}

}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

public class GLCompileException extends Exception {

	public GLCompileException() {}

	public GLCompileException(String message) {
		super(message);
	}

	public GLCompileException(Throwable cause) {
		super(cause);
	}

	public GLCompileException(String message, Throwable cause) {
		super(message, cause);
	}

}
/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.exception;

/**
 * Like {@link NumberFormatException} for things that aren't numbers.
 */
public class FormatException extends IllegalArgumentException {
	private static final long serialVersionUID = -8947672559764890756L;

	public FormatException() {
	}

	public FormatException(String message) {
		super(message);
	}

	public FormatException(Throwable cause) {
		super(cause);
	}

	public FormatException(String message, Throwable cause) {
		super(message, cause);
	}

}

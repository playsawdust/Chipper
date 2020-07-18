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
 * Thrown when the other side of a connection violates the protocol.
 */
public class ProtocolViolationException extends RuntimeException {
	private static final long serialVersionUID = -2263930377659706318L;

	public ProtocolViolationException() {
		super();
	}

	public ProtocolViolationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProtocolViolationException(String message) {
		super(message);
	}

	public ProtocolViolationException(Throwable cause) {
		super(cause);
	}



}

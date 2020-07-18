/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.exception;

import java.io.IOException;

import com.playsawdust.chipper.Identifier;

public class ResourceNotFoundException extends IOException {
	private static final long serialVersionUID = -2931254256592747688L;

	public ResourceNotFoundException() {}

	public ResourceNotFoundException(String message, Identifier id) {
		this(message, id, null);
	}

	public ResourceNotFoundException(String message, Identifier id, Throwable cause) {
		super(message+" with path "+id.path+" in namespace "+id.namespace, cause);
	}

}
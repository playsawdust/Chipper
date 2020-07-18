/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

import com.playsawdust.chipper.rcl.AccessRules;
import com.playsawdust.chipper.rcl.RuledClassLoader;

/**
 * Thrown when a Forbidden access rule is violated.
 * @see AccessRules
 * @see RuledClassLoader
 */
public class ForbiddenClassError extends Error {
	private static final long serialVersionUID = 8165138290664789357L;

	public ForbiddenClassError() {}

	// sufficient information is printed by RuledClassLoader when it detects a
	// violation and throws this error in the first place
	@Override public void printStackTrace() {}
	@Override public void printStackTrace(PrintStream s) {}
	@Override public void printStackTrace(PrintWriter s) {}

}
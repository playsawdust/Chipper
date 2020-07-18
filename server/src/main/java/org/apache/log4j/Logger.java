/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.apache.log4j;

// just enough to get JediTerm to work
public class Logger {

	private static final Logger inst = new Logger();

	public static Logger getLogger(String str) {
		return inst;
	}

	public static Logger getLogger(Class<?> clazz) {
		return inst;
	}

	public boolean isDebugEnabled() { return false; }
	public void error(String s) {}
	public void error(Object o) {}

	public void info(String s) {}
	public void info(Object o) {}

	public void debug(String s) {}
	public void debug(Object o) {}


}

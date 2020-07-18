/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface MoreLibC extends Library {
	MoreLibC INSTANCE = Native.load("c", MoreLibC.class);
	int getpid();
}
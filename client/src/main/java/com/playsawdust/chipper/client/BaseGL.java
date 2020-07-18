/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import org.lwjgl.opengl.GL12;

/**
 * Extends the global OpenGL base used by Chipper. Currently, this is OpenGL 1.2.
 * This class makes it very easy to change the base OpenGL target version.
 */
public class BaseGL extends GL12 {}

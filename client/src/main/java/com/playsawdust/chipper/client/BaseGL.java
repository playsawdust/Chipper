/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.NativeType;
import org.slf4j.LoggerFactory;

import com.playsawdust.chipper.toolbox.Ternary;

/**
 * Extends the global OpenGL base used by Chipper. Currently, this is OpenGL 1.2.
 * This class makes it very easy to change the base OpenGL target version, and add workarounds.
 */
public class BaseGL extends GL12 {
	
	private static Ternary driverMissingClientState = Ternary.MAYBE;
	
	private static void checkClientStateMissing() {
		if (driverMissingClientState == Ternary.MAYBE) {
			driverMissingClientState = GL.getCapabilities().glEnableClientState == 0 ? Ternary.YES : Ternary.NO;
			LoggerFactory.getLogger("GL").warn("glEnableClientState is missing in this driver. Enabling workaround.");
		}
	}
	
	/**
	 * @see GL11#glEnableClientState(int)
	 */
	public static void glEnableClientState(@NativeType("GLenum") int cap) {
		checkClientStateMissing();
		if (driverMissingClientState == Ternary.YES) {
			glEnable(cap);
		} else {
			GL12.glEnableClientState(cap);
		}
	}
	
	/**
	 * @see GL11#glDisableClientState(int)
	 */
	public static void glDisableClientState(@NativeType("GLenum") int cap) {
		checkClientStateMissing();
		if (driverMissingClientState == Ternary.YES) {
			glDisable(cap);
		} else {
			GL12.glDisableClientState(cap);
		}
	}

}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class GLPipeline {
	private static final Logger log = LoggerFactory.getLogger(GLPipeline.class);
	
	
	
	/**
	 * Print off a bunch of commonly-edited elements of the GL state machine.
	 */
	public static void inspect() {
		StringBuilder out = new StringBuilder();
		out.append("pipeline_state: {\n");
		out.append("    COLOR_MATERIAL: "+enabled(GL11.GL_COLOR_MATERIAL)+",\n");
		out.append("    TEXTURE_2D:     "+enabled(GL11.GL_TEXTURE_2D)+",\n");
		if (GL11.glIsEnabled(GL11.GL_LIGHTING)) {
			out.append("    LIGHTING: {\n");
			out.append("        LIGHT0: "+enabled(GL11.GL_LIGHT0)+",\n");
			out.append("        LIGHT1: "+enabled(GL11.GL_LIGHT1)+",\n");
			out.append("        LIGHT2: "+enabled(GL11.GL_LIGHT2)+",\n");
			out.append("        LIGHT3: "+enabled(GL11.GL_LIGHT3)+"\n");
			out.append("    },\n");
		} else {
			out.append("    LIGHTING:       disabled,\n");
		}
		out.append("}");
		log.debug(out.toString());
	}
	
	private static String enabled(int cap) {
		return (GL11.glIsEnabled(cap)) ? "enabled" : "disabled";
	}
}

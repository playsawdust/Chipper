/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.KHRDebug.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLDebugMessageCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenGLDebug {
	private static final Logger log = LoggerFactory.getLogger("OpenGL");

	@SuppressWarnings("unused")
	private static GLDebugMessageCallbackI callback;

	private static Map<Integer, String> strings = new HashMap<>();
	static {
		strings.put(GL_DEBUG_SOURCE_API, "API");
		strings.put(GL_DEBUG_SOURCE_WINDOW_SYSTEM, "Window System");
		strings.put(GL_DEBUG_SOURCE_SHADER_COMPILER, "Shader Compiler");
		strings.put(GL_DEBUG_SOURCE_THIRD_PARTY, "Third Party");
		strings.put(GL_DEBUG_SOURCE_APPLICATION, "Application");
		strings.put(GL_DEBUG_SOURCE_OTHER, "Other");
		strings.put(GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR, "Deprecated Behavior");
		strings.put(GL_DEBUG_TYPE_ERROR, "Error");
		strings.put(GL_DEBUG_TYPE_MARKER, "Marker");
		strings.put(GL_DEBUG_TYPE_OTHER, "Other");
		strings.put(GL_DEBUG_TYPE_PERFORMANCE, "Performance");
		strings.put(GL_DEBUG_TYPE_POP_GROUP, "Pop Group");
		strings.put(GL_DEBUG_TYPE_PORTABILITY, "Portability");
		strings.put(GL_DEBUG_TYPE_PUSH_GROUP, "Push Group");
		strings.put(GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR, "Undefined Behavior");
	}

	public static void install() {
		if (GL.getCapabilities().GL_KHR_debug) {
			glEnable(GL_DEBUG_OUTPUT);
			glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
			glDebugMessageCallback(callback = (sourceId, typeId, id, severity, length, messagePtr, userParam) -> {
				String message = memASCII(messagePtr);
				String source = strings.containsKey(sourceId) ? strings.get(sourceId) : "Unknown";
				String type = strings.containsKey(typeId) ? strings.get(typeId) : "Unknown";
				if (severity == GL_DEBUG_SEVERITY_NOTIFICATION) {
					if (sourceId == GL_DEBUG_SOURCE_SHADER_COMPILER && typeId == GL_DEBUG_TYPE_OTHER) {
						// Mesa's shader compiler emits a *lot* of garbage
						log.trace("{} {}: {}", source, type, message);
					} else {
						log.debug("{} {}: {}", source, type, message);
					}
				} else if (severity == GL_DEBUG_SEVERITY_LOW) {
					log.info("{} {}: {}", source, type, message);
				} else if (severity == GL_DEBUG_SEVERITY_MEDIUM) {
					log.warn("{} {}: {}", source, type, message);
				} else if (severity == GL_DEBUG_SEVERITY_HIGH) {
					log.error("{} {}: {}", source, type, message, new Throwable().fillInStackTrace());
				}
			}, NULL);
		}
	}

}

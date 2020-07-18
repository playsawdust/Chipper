/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import static org.lwjgl.opengl.ARBShaderObjects.*;
import static com.playsawdust.chipper.client.BaseGL.*;
import static org.lwjgl.opengl.ARBFragmentShader.*;
import static org.lwjgl.opengl.ARBVertexShader.*;
import static org.lwjgl.opengl.ARBComputeShader.*;

import java.util.Locale;

import org.lwjgl.opengl.GL;

import com.playsawdust.chipper.AbstractNativeResource;

public class GLShader extends AbstractNativeResource {
	public enum ShaderType {
		VERTEX(GL_VERTEX_SHADER_ARB),
		FRAGMENT(GL_FRAGMENT_SHADER_ARB),
		COMPUTE(GL_COMPUTE_SHADER)
		;
		private final int glConst;
		private ShaderType(int glConst) {
			this.glConst = glConst;
		}
	}

	private final ShaderType type;
	private final int name;

	private boolean compiled = false;

	private GLShader(ShaderType type, int name) {
		this.type = type;
		this.name = name;
	}

	public void compile(String source) throws GLCompileException {
		checkFreed();
		glShaderSourceARB(name, source);
		glCompileShaderARB(name);
		int status = glGetObjectParameteriARB(name, GL_OBJECT_COMPILE_STATUS_ARB);
		if (status == GL_FALSE) {
			throw new GLCompileException(glGetInfoLogARB(name));
		}
		compiled = true;
	}

	public boolean isCompiled() {
		return compiled;
	}

	public ShaderType getType() {
		return type;
	}

	@Override
	protected void _free() {
		glDeleteObjectARB(name);
	}

	/**
	 * <b>Unsafe</b>. GLShader assumes it has complete ownership over the
	 * shader name it is holding, and this method can break that assumption.
	 */
	public int unsafeGetShaderName() {
		return name;
	}

	/**
	 * Allocate a new GLShader.
	 * @return a newly allocated GLShader
	 */
	public static GLShader allocate(ShaderType type) {
		if (!GL.getCapabilities().GL_ARB_shader_objects)
			throw new IllegalStateException("ARB_shader_objects is not supported");
		switch (type) {
			case VERTEX:
				if (!GL.getCapabilities().GL_ARB_vertex_shader)
					throw new IllegalStateException("ARB_vertex_shader is not supported");
				break;
			case FRAGMENT:
				if (!GL.getCapabilities().GL_ARB_fragment_shader)
					throw new IllegalStateException("ARB_fragment_shader is not supported");
				break;
			case COMPUTE:
				if (!GL.getCapabilities().GL_ARB_compute_shader)
					throw new IllegalStateException("ARB_compute_shader is not supported");
				break;
			default:
				throw new AssertionError("missing case for "+type);
		}
		int name = glCreateShaderObjectARB(type.glConst);
		return new GLShader(type, name);
	}

	/**
	 * <b>Unsafe</b>. GLShader assumes it has complete ownership over the
	 * shader name it is holding, and this breaks that assumption.
	 * @param name the program name to wrap
	 * @return a newly constructed GLShader representing the given texture name
	 */
	public static GLShader unsafeFromProgramName(int name) {
		if (!GL.getCapabilities().GL_ARB_shader_objects)
			throw new IllegalStateException("ARB_shader_objects is not supported");
		int type = glGetObjectParameteriARB(name, GL_OBJECT_TYPE_ARB);
		ShaderType st = null;
		for (ShaderType t : ShaderType.values()) {
			if (type == t.glConst) {
				st = t;
				break;
			}
		}
		if (st == null) throw new IllegalArgumentException("Unrecognized shader type 0x"+Integer.toHexString(type).toUpperCase(Locale.ROOT));
		return new GLShader(st, name);
	}

}

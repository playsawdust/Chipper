/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import static com.playsawdust.chipper.client.BaseGL.*;
import static org.lwjgl.opengl.ARBShaderObjects.*;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joml.Matrix4d;
import org.lwjgl.opengl.ARBProgramInterfaceQuery;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.ARBUniformBufferObject;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.system.MemoryUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.playsawdust.chipper.AbstractNativeResource;
import com.playsawdust.chipper.client.gl.GLShader.ShaderType;
import com.playsawdust.chipper.collect.unmodifiable.Unmodifiable;
import com.playsawdust.chipper.collect.unmodifiable.UnmodifiableList;
import com.playsawdust.chipper.exception.UseAfterFreeException;

public class GLProgram extends AbstractNativeResource {
	public static final GLProgram NONE = new GLProgram(0) {
		@Override protected void _free() { throw new IllegalArgumentException("Cannot free GLProgram.NONE"); }
		@Override protected void checkFreed() throws UseAfterFreeException {}
		@Override public void attachShader(GLShader shader) { throw new IllegalArgumentException("Cannot attach shaders to GLProgram.NONE"); }
		@Override public void detachShader(GLShader shader) { throw new IllegalArgumentException("Cannot detach shaders from GLProgram.NONE"); }
		@Override public boolean isFreed() { return false; }
		@Override public boolean isLinked() { return true; }
		@Override public Uniform getUniform(String uniform) throws NoSuchElementException { throw new NoSuchElementException(uniform); }
	};

	private final int name;
	private boolean linked;

	private final List<GLShader> shaders = Lists.newArrayList();
	private final UnmodifiableList<GLShader> shadersUnmodifiable = Unmodifiable.list(shaders);

	private final Map<String, Uniform> uniforms = Maps.newHashMap();
	/** Holds bindings for uniform buffers and shader storage buffers */
	private final Map<String, Integer> uniformBlocks = Maps.newHashMap();
	private final Map<String, Integer> shaderStorageBlocks = Maps.newHashMap();

	protected GLProgram(int name) {
		this.name = name;
	}

	@Override
	protected void _free() {
		glDeleteObjectARB(name);
	}

	public void attachShader(GLShader shader) {
		checkFreed();
		if (shader.isFreed()) throw new IllegalArgumentException("shader is freed");
		if (linked) throw new IllegalStateException("program is already linked");
		checkShaderArgument(shader);
		
		shaders.add(shader);
		glAttachObjectARB(name, shader.unsafeGetShaderName());
	}

	public void detachShader(GLShader shader) {
		checkFreed();
		if (shader.isFreed()) throw new IllegalArgumentException("shader is freed");
		if (linked) throw new IllegalStateException("program is already linked");
		shaders.remove(shader);
		glDetachObjectARB(name, shader.unsafeGetShaderName());
	}
	
	protected void checkShaderArgument(GLShader shader) {
		Preconditions.checkArgument(shader.getType()!=ShaderType.COMPUTE, "cannot use compute shaders in a graphics pipeline program");
	}

	public void link() throws GLLinkException {
		if (linked) return;
		glLinkProgramARB(name);
		if (glGetObjectParameteriARB(name, GL_OBJECT_LINK_STATUS_ARB) == GL_FALSE) {
			throw new GLLinkException(glGetInfoLogARB(name));
		}
		linked = true;
	}

	public void use() {
		glUseProgramObjectARB(name);
	}

	public boolean isLinked() {
		return linked;
	}

	public Uniform getUniform(String uniform) throws NoSuchElementException {
		if (!linked) throw new IllegalStateException("program not linked");
		if (!uniforms.containsKey(uniform)) {
			int loc = glGetUniformLocationARB(name, uniform);
			if (loc == -1) throw new NoSuchElementException(uniform);
			uniforms.put(uniform, new Uniform(loc));
		}
		return uniforms.get(uniform);
	}
	
	/**
	 * Binds the given buffer to a named interface block of this shader program.
	 * @param buf         the ShaderBuffer to bind
	 * @param bufferName  the name of the interface block to bind it to
	 */
	public void bindShaderBuffer(ShaderBuffer buf, String bufferName) {
		if (!linked) throw new IllegalStateException("program not linked");
		checkFreed();
		//Additionally, this program must be *the currently bound shader program*
		
		ARBVertexBufferObject.glBindBufferARB(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, buf.name);
		int binding = getBinding(bufferName, buf.type);
		ARBUniformBufferObject.glBindBufferBase(buf.type.value(), binding, buf.name);
	}
	
	/**
	 * Gets the "binding point" for a shader interface block, sometimes also known as a "target". This may be specified in the shader, e.g. {@code layout(std140, binding = 3)} would yield 3.
	 * But may be automatically be assigned by OpenGL if no binding is specified. You probably won't need to use this method; just call {@link #bindShaderBuffer(ShaderBuffer, ShaderBuffer.Type, String)}
	 * 
	 * <p>Bindings are an indirection step between the shader program and the OpenGL object, presumably to allow new kinds of objects to be bound,
	 * or to allow existing objects to be bound to new targets, without changing the interface on either side. So any binding needs to happen twice:
	 * a shader interface block to the binding point (we either let GL auto-assign this or allow the shader to self-specify); and a buffer object to the
	 * binding point, which is what bindShaderBuffer does.
	 * 
	 * 
	 * @param bufferName the name of the shader interface block to find a binding point for
	 * @param type       the type of buffer - uniform buffers and shader storage buffers each have different interfaces to find their bindings
	 * @return           the binding for the named shader interface block
	 */
	private int getBinding(String bufferName, ShaderBuffer.Type type) throws NoSuchElementException {
		if (!linked) throw new IllegalStateException("program not linked");
		checkFreed();
		
		switch(type) {
			case SHADER_STORAGE_BUFFER:
				if (!shaderStorageBlocks.containsKey(bufferName)) {
					int loc = ARBProgramInterfaceQuery.glGetProgramResourceIndex(name, ARBProgramInterfaceQuery.GL_SHADER_STORAGE_BLOCK, bufferName);
					if (loc == -1) throw new NoSuchElementException(bufferName);
					shaderStorageBlocks.put(bufferName, loc);
				}
				return shaderStorageBlocks.get(bufferName);
				
			case UNIFORM_BUFFER:
				if (!uniformBlocks.containsKey(bufferName)) {
					int loc = ARBUniformBufferObject.glGetUniformBlockIndex(name, bufferName);
					if (loc == -1) throw new NoSuchElementException(bufferName);
					uniformBlocks.put(bufferName, loc);
				}
				return uniformBlocks.get(bufferName);
				
			default:
				throw new IllegalArgumentException("Unknown buffer type");
		}
		
	}

	public UnmodifiableList<GLShader> getShaders() {
		return shadersUnmodifiable;
	}
	
	public int getShaderCount() {
		return shaders.size();
	}

	/**
	 * <b>Unsafe</b>. GLProgram assumes it has complete ownership over the
	 * program name it is holding, and this method can break that assumption.
	 */
	public int unsafeGetProgramName() {
		return name;
	}

	/**
	 * Allocate a new GLProgram.
	 * @return a newly allocated GLProgram
	 */
	public static GLProgram allocate() {
		int name = glCreateProgramObjectARB();
		return new GLProgram(name);
	}

	/**
	 * <b>Unsafe</b>. GLProgram assumes it has complete ownership over the
	 * program name it is holding, and this breaks that assumption.
	 * @param name the program name to wrap
	 * @return a newly constructed GLProgram representing the given texture name
	 */
	public static GLProgram unsafeFromProgramName(int name) {
		return new GLProgram(name);
	}

	public static final class Uniform {
		private int location;

		private Uniform(int location) {
			this.location = location;
		}

		public void setFloat(float val) {
			glUniform1fARB(location, val);
		}
		public void setInt(int val) {
			glUniform1iARB(location, val);
		}

		public void setFloat(float val1, float val2) {
			glUniform2fARB(location, val1, val2);

		}
		public void setInt(int val1, int val2) {
			glUniform2iARB(location, val1, val2);
		}

		public void setFloat(float val1, float val2, float val3) {
			glUniform3fARB(location, val1, val2, val3);
		}
		public void setInt(int val1, int val2, int val3) {
			glUniform3iARB(location, val1, val2, val3);
		}

		public void setFloat(float val1, float val2, float val3, float val4) {
			glUniform4fARB(location, val1, val2, val3, val4);
		}
		public void setInt(int val1, int val2, int val3, int val4) {
			glUniform4iARB(location, val1, val2, val3, val4);
		}

		public void setFloats(float... vals) {
			glUniform1fvARB(location, vals);
		}
		public void setInts(int... vals) {
			glUniform1ivARB(location, vals);
		}
		
		public void setMatrix(FloatBuffer matrix) {
			glUniformMatrix4fvARB(location, false, matrix);
		}
	}

}

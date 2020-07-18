/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;



import java.nio.ByteBuffer;

import static org.lwjgl.opengl.ARBShaderStorageBufferObject.*;
import static org.lwjgl.opengl.ARBUniformBufferObject.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;

import com.playsawdust.chipper.AbstractNativeResource;

/**
 * Represents a UniformBufferObject or a ShaderStorageBufferObject
 * 
 * <p> NOTE: Uniform buffers are stuck with layout(std140), which is OWORD-aligned (effectively a bunch of vec4's). Data written must be spaced
 * out according to rules at https://www.khronos.org/registry/OpenGL/specs/gl/glspec45.core.pdf#page=159
 * 
 * <p> A summary of those layout(std140) rules:
 * <ul>
 *   <li> Scalars get packed sensibly. A short takes two bytes, a long takes 8 bytes, a float takes 4 bytes.
 *   <li> Vectors get packed as vec2's or vec4's. Vec3's get packed as if they were vec4's
 *   <li> Scalar arrays get packed sensibly, but the next element after gets vec4-aligned: a 2-element float array has one float at ofs=0 and one at ofs=4, but has two floats' worth of padding at the end.
 *   <li> Arrays of vectors use that vector's padding: a 32-element vec3f array takes 32*4*4 bytes, with one vec3 at ofs=0 and the next one at ofs=4*4=16 bytes, with the last 4 bytes in each stride being padding.
 *   <li> A CxR matrix is stored as if it was an array of C R-component vectors like vecR[C] Row-major matrices are flipped: vecC[R]
 *   <li> An array of matrices is treated as if it was an array of the vectors that they decompose into
 *   <li> Structs are aligned to the *largest* element containing them: a struct containing floats and vec2's would align its members to 8 bytes, and pad its length to a multiple of 16 bytes (vec4 length)
 * </ul>
 */
public class ShaderBuffer extends AbstractNativeResource {
	
	protected final int name;
	protected final Type type;
	
	protected ShaderBuffer(int name, Type type) {
		this.name = name;
		this.type = type;
	}

	@Override
	protected void _free() {
		glDeleteBuffersARB(name);
	}
	
	/**
	 * Uploads data to this buffer
	 * @param buf        the data to upload for this buffer
	 * @param frequency  How frequently data in this buffer will be changed or reuploaded; typically STATIC or DYNAMIC.
	 * @param nature     The usage for this buffer; almost always DRAW unless you're reading data back from it.
	 */
	public void upload(ByteBuffer buf, GLVertexBuffer.HintFrequency frequency, GLVertexBuffer.HintNature nature) {
		checkFreed();
		
		glBindBufferARB(type.value(), name);
		glBufferDataARB(type.value(), buf, GLVertexBuffer.hintsToGlConstant(frequency, nature));
		glBindBufferARB(type.value(), 0);
	}
	
	public static ShaderBuffer allocate(Type type) {
		return new ShaderBuffer(glGenBuffersARB(), type);
	}
	
	public static enum Type {
		/** Uniform Buffer Object */
		UNIFORM_BUFFER(GL_UNIFORM_BUFFER),
		SHADER_STORAGE_BUFFER(GL_SHADER_STORAGE_BUFFER);
		
		private int type;
		
		Type(int glValue) {
			this.type = glValue;
		}
		
		public int value() {
			return type;
		}
	}
}

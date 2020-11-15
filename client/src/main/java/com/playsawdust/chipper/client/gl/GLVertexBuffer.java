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
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.opengl.ARBHalfFloatVertex.*;

import android.util.Half;

import org.checkerframework.checker.guieffect.qual.UIType;
import org.lwjgl.opengl.GL;
import com.playsawdust.chipper.AbstractNativeResource;
import com.playsawdust.chipper.math.FastMath;
import com.playsawdust.chipper.math.ProtoColor;

import com.playsawdust.chipper.client.gl.builder.DisplayListVertexBuilder;
import com.playsawdust.chipper.client.gl.builder.VertexBuilder;
import java.nio.ByteBuffer;

/**
 * Represents an OpenGL Vertex Buffer Object or display list, depending on driver support, and
 * provides a convenient API for manipulating them.
 */
@UIType
public abstract class GLVertexBuffer extends AbstractNativeResource {
	public enum HintFrequency {
		/**
		 * The buffer contents will be modified once and used at most a few times.
		 */
		STREAM,
		/**
		 * The buffer contents will be modified once and used many times.
		 */
		STATIC,
		/**
		 * The buffer contents will be modified repeatedly and used many times.
		 */
		DYNAMIC,
		;
	}
	public enum HintNature {
		/**
		 * The buffer contents are modified by the appication, and used as the source for GL
		 * drawing and image specification commands.
		 * <p>
		 * This is the most common usage of a vertex buffer.
		 */
		DRAW,
		/**
		 * The buffer contents are modified by reading data from the GL, and used to return that
		 * data when queried by the application.
		 * <p>
		 * Please note that GLVertexBuffer does not yet offer an API to read buffers back from the
		 * GPU, and this nature hint is not useful.
		 */
		READ,
		/**
		 * The buffer contents are modified by reading data from the GL, and used as the source
		 * for GL drawing and image specification commands.
		 */
		COPY,
		;
	}

	public static int hintsToGlConstant(HintFrequency freq, HintNature nature) {
		if (freq == null) freq = HintFrequency.DYNAMIC;
		if (nature == null) nature = HintNature.DRAW;

		if (freq == HintFrequency.STREAM && nature == HintNature.DRAW) return GL_STREAM_DRAW_ARB;
		if (freq == HintFrequency.STREAM && nature == HintNature.READ) return GL_STREAM_READ_ARB;
		if (freq == HintFrequency.STREAM && nature == HintNature.COPY) return GL_STREAM_COPY_ARB;

		if (freq == HintFrequency.STATIC && nature == HintNature.DRAW) return GL_STATIC_DRAW_ARB;
		if (freq == HintFrequency.STATIC && nature == HintNature.READ) return GL_STATIC_READ_ARB;
		if (freq == HintFrequency.STATIC && nature == HintNature.COPY) return GL_STATIC_COPY_ARB;

		if (freq == HintFrequency.DYNAMIC && nature == HintNature.DRAW) return GL_DYNAMIC_DRAW_ARB;
		if (freq == HintFrequency.DYNAMIC && nature == HintNature.READ) return GL_DYNAMIC_READ_ARB;
		if (freq == HintFrequency.DYNAMIC && nature == HintNature.COPY) return GL_DYNAMIC_COPY_ARB;

		throw new AssertionError("Unknown buffer usage "+freq+"_"+nature);
	}

	private static boolean isHalfSupported() {
		// NV_half_float and ARB_half_float_vertex use the same constant for GL_HALF_FLOAT, so they're effectively equivalent
		return GL.getCapabilities().GL_ARB_half_float_vertex || GL.getCapabilities().GL_NV_half_float;
	}

	protected final int name;

	protected GLVertexBuffer(int name) {
		this.name = name;
	}

	/**
	 * Erase this vertex buffer and rebuild its shape data from scratch.
	 * <p>
	 * No changes are actually made to the buffer until {@code end} is called.
	 */
	public abstract VertexBuilder.Format builder();

	/**
	 * Set the usage hints for this vertex buffer, which may allow the graphics driver to better
	 * optimize the buffer.
	 * <p>
	 * If this GLVertexBuffer actually represents a display list, this method will have no effect.
	 * This method must be called before {@link #builder} for it to take effect.
	 */
	public abstract void setUsageHints(HintFrequency freq, HintNature nature);

	/**
	 * Draw the contents of this GLVertexBuffer.
	 */
	public abstract void draw();

	/**
	 * Gets the number of vertices in this GLVertexBuffer
	 */
	public abstract int size();
	
	/**
	 * Allocate a new GLVertexBuffer.
	 * @return a newly allocated GLVertexBuffer
	 */
	public static GLVertexBuffer allocate() {
		// ATTN should we always use display lists on nVidia?
		// there's inklings around of nV drivers being faster with display lists than VBOs
		// needs testing.
		if (GL.getCapabilities().GL_ARB_vertex_buffer_object && GL.getCapabilities().glEnableClientState > 0) {
			return new ImplVBO();
		} else {
			return new ImplDL();
		}
	}

	private static class ImplVBO extends GLVertexBuffer {

		private HintFrequency hintFreq = null;
		private HintNature hintNature = null;

		private int vertices = 0;

		private int shadeModel = GL_SMOOTH;
		private int mode = 0;

		private boolean fixedColor = false;
		private float fixedR;
		private float fixedG;
		private float fixedB;
		private float fixedA;

		private boolean hasTex;
		private boolean hasColor;
		private boolean hasNormal;

		private boolean vertex3d;
		private boolean tex3d;

		protected ImplVBO() {
			super(glGenBuffersARB());
		}

		protected ImplVBO(int name) {
			super(name);
		}

		@Override
		public VertexBuilder.Format builder() {
			checkFreed();
			return new VBOBuilder(this);
		}

		@Override
		public void setUsageHints(HintFrequency freq, HintNature nature) {
			checkFreed();
			this.hintFreq = freq;
			this.hintNature = nature;
		}

		@Override
		public void draw() {
			checkFreed();
			if (vertices == 0) return;
			int vertexSize = (Float.BYTES*(vertex3d?3:2));
			int texSize = 0;
			if (hasTex) texSize = (isHalfSupported() ? Half.BYTES : Float.BYTES)*(tex3d?3:2);
			int colorSize = hasColor ? (Byte.BYTES*4) : 0;
			int normalSize = hasNormal ? (Byte.BYTES*3) : 0;
			
			int stride = vertexSize + texSize + colorSize + normalSize;
			int ofs = 0;
			
			glBindBufferARB(GL_ARRAY_BUFFER_ARB, name);
			glEnableClientState(GL_VERTEX_ARRAY);
			glVertexPointer(vertex3d ? 3 : 2, GL_FLOAT, stride, 0);
			ofs += vertexSize;
			
			if (hasTex) {
				glEnableClientState(GL_TEXTURE_COORD_ARRAY);
				glTexCoordPointer(tex3d ? 3 : 2, isHalfSupported() ? GL_HALF_FLOAT : GL_FLOAT, stride, ofs);
				ofs += texSize;
			}
			if (hasColor) {
				glEnableClientState(GL_COLOR_ARRAY);
				glColorPointer(4, GL_UNSIGNED_BYTE, stride, ofs);
				ofs += colorSize;
			}
			if (hasNormal) {
				glEnableClientState(GL_NORMAL_ARRAY);
				glNormalPointer(GL_BYTE, stride, ofs);
				ofs += normalSize;
			}


			if (fixedColor) {
				glColor4f(fixedR, fixedG, fixedB, fixedA);
			}
			glShadeModel(shadeModel);
			glDrawArrays(mode, 0, vertices);

			glDisableClientState(GL_VERTEX_ARRAY);
			glDisableClientState(GL_TEXTURE_COORD_ARRAY);
			glDisableClientState(GL_COLOR_ARRAY);
			glDisableClientState(GL_NORMAL_ARRAY);

		}

		@Override
		protected void _free() {
			glDeleteBuffersARB(name);
		}

		@Override
		public int size() {
			return vertices;
		}

	}

	private static class ImplDL extends GLVertexBuffer {

		protected ImplDL() {
			super(glGenLists(1));
		}

		protected ImplDL(int name) {
			super(name);
		}

		@Override
		public VertexBuilder.Format builder() {
			checkFreed();
			return new DisplayListVertexBuilder(name);
		}

		@Override
		public void setUsageHints(HintFrequency freq, HintNature nature) {
			checkFreed();
		}

		@Override
		public void draw() {
			checkFreed();
			glCallList(name);
		}

		@Override
		protected void _free() {
			glDeleteLists(name, 1);
		}

		@Override
		public int size() {
			return 1;
		}

	}

	private static class VBOBuilder implements VertexBuilder.All {
		private final ImplVBO vbo;

		private ByteBuffer buffer;

		private boolean zerothVertex = true;
		private boolean firstVertex = true;

		private int textureDimensionality = 0;
		private int textureDimensionalityThisVertex = 0;

		private int vertexDimensionality = 0;

		private boolean hasEverUsedColor = false;
		private boolean hasUsedColorThisVertex = false;

		private boolean hasEverUsedNormal = false;
		private boolean hasUsedNormalThisVertex = false;

		private boolean settingInitialColor = false;

		private boolean fixedColor = false;
		private float fixedR;
		private float fixedG;
		private float fixedB;
		private float fixedA;

		private int mode = 0;
		private int shadeModel = GL_SMOOTH;

		private int vertices;

		public VBOBuilder(ImplVBO vbo) {
			this.vbo = vbo;
			buffer = memAlloc(1024);
		}

		private void ensureCapacity(int count) {
			if (buffer.remaining() < count) {
				buffer = memRealloc(buffer, (buffer.capacity()*3)/2);
			}
		}

		@Override
		public VertexBuilder.NextOrBlend polygon() {
			mode = GL_POLYGON;
			return this;
		}

		@Override
		public VertexBuilder.NextOrBlend quads() {
			mode = GL_QUADS;
			return this;
		}

		@Override
		public VertexBuilder.NextOrBlend quadStrip() {
			mode = GL_QUAD_STRIP;
			return this;
		}

		@Override
		public VertexBuilder.NextOrBlend triangles() {
			mode = GL_TRIANGLES;
			return this;
		}

		@Override
		public VertexBuilder.NextOrBlend triangleStrip() {
			mode = GL_TRIANGLE_STRIP;
			return this;
		}

		@Override
		public VertexBuilder.NextOrBlend triangleFan() {
			mode = GL_TRIANGLE_FAN;
			return this;
		}

		@Override
		public VertexBuilder.NextOrEnd smooth() {
			shadeModel = GL_SMOOTH;
			return this;
		}

		@Override
		public VertexBuilder.ColorThenNext flat() {
			settingInitialColor = true;
			shadeModel = GL_FLAT;
			return this;
		}

		@Override
		public VertexBuilder.Vertex next() {
			if (!zerothVertex) {
				if (hasEverUsedColor && !hasUsedColorThisVertex)
					throw new IllegalStateException("Cannot end a vertex before defining all its attributes (color was forgotten)");
				if (textureDimensionality != 0 && textureDimensionalityThisVertex == 0)
					throw new IllegalStateException("Cannot end a vertex before defining all its attributes (tex was forgotten)");
				if (hasEverUsedNormal && !hasUsedNormalThisVertex)
					throw new IllegalStateException("Cannot end a vertex before defining all its attributes (normal was forgotten)");
				firstVertex = false;
				hasUsedColorThisVertex = false;
				textureDimensionalityThisVertex = 0;
				hasUsedNormalThisVertex = false;
			}
			zerothVertex = false;
			vertices++;
			return this;
		}

		@Override
		public VertexBuilder.NormalOrNextOrEnd color(double r, double g, double b, double a) {
			if (!settingInitialColor) {
				if (!hasEverUsedColor && !firstVertex)
					throw new IllegalStateException("Cannot add color when the first vertex did not have it");
				if (hasUsedColorThisVertex)
					throw new IllegalStateException("Color has already been defined for this vertex");
				hasEverUsedColor = true;
				hasUsedColorThisVertex = true;
			}
			if (settingInitialColor) {
				fixedColor = true;
				fixedR = (float)r;
				fixedG = (float)g;
				fixedB = (float)b;
				fixedA = (float)a;
			} else {
				fixedColor = false;
				ensureCapacity(4);
				buffer.put((byte)((int)(r*255)&0xFF));
				buffer.put((byte)((int)(g*255)&0xFF));
				buffer.put((byte)((int)(b*255)&0xFF));
				buffer.put((byte)((int)(a*255)&0xFF));
			}
			settingInitialColor = false;
			return this;
		}

		@Override
		public VertexBuilder.NormalOrNextOrEnd color(double r, double g, double b) {
			return color(r, g, b, 1);
		}

		@Override
		public VertexBuilder.NormalOrNextOrEnd color(ProtoColor color) {
			return color(color, 1);
		}

		@Override
		public VertexBuilder.NormalOrNextOrEnd color(ProtoColor color, double a) {
			return color(color.getRed(), color.getGreen(), color.getBlue(), a);
		}

		@Override
		public VertexBuilder.NormalOrNextOrEnd color(int rgb) {
			return color(((rgb>>16)&0xFF)/255D, ((rgb>>8 )&0xFF)/255D, ((rgb>>0 )&0xFF)/255D, ((rgb>>24)&0xFF)/255D);
		}

		@Override
		public VertexBuilder.ColorOrNormalOrNextOrEnd tex(double u, double v) {
			tex(u, v, 0, 2);
			return this;
		}

		@Override
		public VertexBuilder.ColorOrNormalOrNextOrEnd tex(double u, double v, double w) {
			tex(u, v, w, 3);
			return this;
		}

		private void tex(double u, double v, double w, int dim) {
			if (textureDimensionality == 0 && !firstVertex)
				throw new IllegalStateException("Cannot add texture coordinates when the first vertex did not have it");
			if (textureDimensionalityThisVertex != 0)
				throw new IllegalStateException("Texture coordinates have already been defined for this vertex");
			if (!firstVertex && textureDimensionality != dim)
				throw new IllegalStateException("Cannot add "+dim+"D texture coordinates when "+textureDimensionality+"D coordinates have already been used");
			textureDimensionality = dim;
			textureDimensionalityThisVertex = dim;
			if (isHalfSupported()) {
				ensureCapacity(dim * 2);
				buffer.putShort(Half.toHalf((float) u));
				buffer.putShort(Half.toHalf((float) v));
				if (dim == 3) {
					buffer.putShort(Half.toHalf((float) w));
				}
			} else {
				ensureCapacity(dim * Float.BYTES);
				buffer.putFloat((float) u);
				buffer.putFloat((float) v);
				if (dim == 3) {
					buffer.putFloat((float) w);
				}
			}
		}

		@Override
		public VertexBuilder.TexOrColorOrNormalOrNextOrEnd vertex(double x, double y) {
			vertex(x, y, 0, 2);
			return this;
		}

		@Override
		public VertexBuilder.TexOrColorOrNormalOrNextOrEnd vertex(double x, double y, double z) {
			vertex(x, y, z, 3);
			return this;
		}

		private void vertex(double x, double y, double z, int dim) {
			if (!firstVertex && vertexDimensionality != dim)
				throw new IllegalStateException("Cannot add "+dim+"D vertex coordinates when "+vertexDimensionality+"D coordinates have already been used");
			vertexDimensionality = dim;
			if (dim == 2) {
				ensureCapacity(2*4);
				buffer.putFloat((float)x);
				buffer.putFloat((float)y);
			} else {
				ensureCapacity(3*4);
				buffer.putFloat((float)x);
				buffer.putFloat((float)y);
				buffer.putFloat((float)z);
			}
		}

		@Override
		public VertexBuilder.NextOrEnd normal(double x, double y, double z) {
			if (!hasEverUsedNormal && !firstVertex)
				throw new IllegalStateException("Cannot add normal when the first vertex did not have it");
			if (hasUsedNormalThisVertex)
				throw new IllegalStateException("Normal has already been defined for this vertex");
			hasEverUsedNormal = true;
			hasUsedNormalThisVertex = true;
			x = FastMath.clamp(x, -1, 1);
			y = FastMath.clamp(y, -1, 1);
			z = FastMath.clamp(z, -1, 1);
			ensureCapacity(3);
			buffer.put((byte)(x*127));
			buffer.put((byte)(y*127));
			buffer.put((byte)(z*127));
			return this;
		}

		@Override
		public void end() {
			next();
			buffer.flip();
			glBindBufferARB(GL_ARRAY_BUFFER_ARB, vbo.name);
			glBufferDataARB(GL_ARRAY_BUFFER_ARB, buffer, hintsToGlConstant(vbo.hintFreq, vbo.hintNature));
			memFree(buffer);
			buffer = null;
			vbo.mode = mode;
			vbo.shadeModel = shadeModel;
			vbo.fixedColor = fixedColor;
			vbo.fixedR = fixedR;
			vbo.fixedG = fixedG;
			vbo.fixedB = fixedB;
			vbo.fixedA = fixedA;
			vbo.hasColor = hasEverUsedColor && !fixedColor;
			vbo.hasTex = textureDimensionality != 0;
			vbo.hasNormal = hasEverUsedNormal;
			vbo.tex3d = textureDimensionality == 3;
			vbo.vertex3d = vertexDimensionality == 3;
			vbo.vertices = vertices;
		}
	}

}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl.builder;

import static com.playsawdust.chipper.client.BaseGL.*;

import com.google.common.collect.Lists;

import com.playsawdust.chipper.math.FastMath;
import com.playsawdust.chipper.math.ProtoColor;

import java.util.List;

/**
 * An implementation of VertexBuilder that uses GL immediate mode functions. Useful for light
 * on-the-fly 2D drawing or building display lists.
 */
public class ImmediateModeVertexBuilder implements VertexBuilder.All {
	// we buffer actions to prevent state leakage if end never gets called
	// (such as due to an exception being thrown)
	private final List<Runnable> actions = Lists.newArrayListWithCapacity(32);

	private double nextVertexX;
	private double nextVertexY;
	private double nextVertexZ;

	private boolean zerothVertex = true;
	private boolean firstVertex = true;

	private int textureDimensionality = 0;
	private int textureDimensionalityThisVertex = 0;

	private int vertexDimensionality = 0;

	private boolean hasEverUsedColor = false;
	private boolean hasUsedColorThisVertex = false;

	private boolean hasSetShadeModel = false;
	private boolean settingInitialColor = false;

	private boolean hasEverUsedNormal = false;
	private boolean hasUsedNormalThisVertex = false;

	private int mode;

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
		hasSetShadeModel = true;
		actions.add(() -> glShadeModel(GL_SMOOTH));
		actions.add(() -> glBegin(mode));
		return this;
	}

	@Override
	public VertexBuilder.ColorThenNext flat() {
		hasSetShadeModel = true;
		settingInitialColor = true;
		actions.add(() -> glShadeModel(GL_FLAT));
		actions.add(() -> glBegin(mode));
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
			if (vertexDimensionality == 2) {
				double x = nextVertexX;
				double y = nextVertexY;
				actions.add(() -> glVertex2d(x, y));
			} else if (vertexDimensionality == 3) {
				double x = nextVertexX;
				double y = nextVertexY;
				double z = nextVertexZ;
				actions.add(() -> glVertex3d(x, y, z));
			} else {
				throw new AssertionError("Unknown vertex dimensionality "+vertexDimensionality);
			}
		}
		zerothVertex = false;
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
		settingInitialColor = false;
		byte rb = (byte)((int)(r*255)&0xFF);
		byte gb = (byte)((int)(g*255)&0xFF);
		byte bb = (byte)((int)(b*255)&0xFF);
		byte ab = (byte)((int)(a*255)&0xFF);
		actions.add(() -> glColor4ub(rb, gb, bb, ab));
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
		if (dim == 2) {
			actions.add(() -> glTexCoord2f((float)u, (float)v));
		} else {
			actions.add(() -> glTexCoord3f((float)u, (float)v, (float)w));
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
		if (!hasSetShadeModel) {
			actions.add(() -> glShadeModel(GL_SMOOTH));
			actions.add(() -> glBegin(mode));
		}
		nextVertexX = x;
		nextVertexY = y;
		nextVertexZ = z;
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
		byte xb = (byte)(x*127);
		byte yb = (byte)(y*127);
		byte zb = (byte)(z*127);
		actions.add(() -> glNormal3b(xb, yb, zb));
		return this;
	}

	@Override
	public void end() {
		next();
		for (Runnable r : actions) {
			r.run();
		}
		glEnd();
	}
}
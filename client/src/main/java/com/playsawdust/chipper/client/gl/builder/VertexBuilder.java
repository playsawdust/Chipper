/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl.builder;

import org.checkerframework.checker.guieffect.qual.UIType;

import com.playsawdust.chipper.math.ProtoColor;

import com.playsawdust.chipper.client.component.Canvas;
import com.playsawdust.chipper.client.gl.GLVertexBuffer;

import org.joml.Vector2dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * A fluent type-safe interface for efficiently constructing 2D or 3D collections of vertices in
 * arbitrary formats, and implying what data needs to be stored.
 * <p>
 * The various interfaces under the VertexBuilder umbrella provide a concrete order to the method
 * calls, and make IDE autocomplete helpful in showing what options are available.
 * <dl>
 * <dt>For people interested in using this interface, see:</dt>
 * <dd>{@link GLVertexBuffer#builder()}</dd>
 * <dd>{@link Canvas#startShape()}</dd>
 * <dt>For people interested in providing this interface, see:</dt>
 * <dd>{@link ImmediateModeVertexBuilder}</dd>
 * </dl>
 */
@UIType
public final class VertexBuilder {

	/**
	 * Convenience interface that extends all the others for implementors of VertexBuilder.
	 */
	@UIType
	public interface All extends Format, Vertex, NextOrBlend, TexOrColorOrNormalOrNextOrEnd, ColorThenNext {}

	/**
	 * The entry point to VertexBuilder. Any methods constructing a VertexBuilder implementation
	 * must return this interface to enforce the correct ordering.
	 */
	@UIType
	public interface Format {
		/**
		 * Build a single polygon, where each vertex is a vertex of the
		 * polygon.
		 */
		VertexBuilder.NextOrBlend polygon();

		/**
		 * Build a collection of disconnected quads, where every group of
		 * four vertexes represents a quadrilateral.
		 */
		VertexBuilder.NextOrBlend quads();
		/**
		 * Build a strip of connected quads, where every pair of vertexes
		 * represents the next quadrilateral.
		 */
		VertexBuilder.NextOrBlend quadStrip();
		/**
		 * Build a collection of disconnected triangles, where every group
		 * of three vertexes represents a triangle.
		 * <p>
		 * This is the native representation of shapes on modern GPUs. Other
		 * modes convert to triangles on-the-fly.
		 */
		VertexBuilder.NextOrBlend triangles();
		/**
		 * Build a strip of connected triangles, where every vertex
		 * represents the far corner of the next triangle.
		 */
		VertexBuilder.NextOrBlend triangleStrip();
		/**
		 * Build a fan of connected triangles, where the first vertex
		 * represents the center point and every further vertex represents
		 * a corner of a triangle with its tip at the center.
		 */
		VertexBuilder.NextOrBlend triangleFan();
	}

	@UIType
	public interface Next {
		/**
		 * Start building the next vertex.
		 */
		VertexBuilder.Vertex next();
	}

	@UIType
	public interface NextOrEnd extends VertexBuilder.Next {
		/**
		 * End this shape. Don't add another vertex.
		 * <p>
		 * Depending on how this VertexBuilder was constructed, this may immediately draw the
		 * shape, or it may simply save the shape in a buffer on the GPU.
		 */
		void end();
	}

	@UIType
	public interface NextOrBlend extends VertexBuilder.Next {
		/**
		 * Enable smooth blending, where each vertex's color will be
		 * smoothly interpolated between. Allows gradients. This is the
		 * default.
		 */
		VertexBuilder.Next smooth();
		/**
		 * Disable smooth blending. With flat blending, only the color of
		 * the <i>provoking vertex</i> is considered, making setting the color
		 * of each vertex pointless.
		 */
		VertexBuilder.ColorThenNext flat();
	}

	@UIType
	public interface Vertex {
		/**
		 * Start a new vertex, with the given coordinates. The Z coordinate is defaulted to zero.
		 */
		VertexBuilder.TexOrColorOrNormalOrNextOrEnd vertex(double x, double y);
		/**
		 * Start a new vertex, with the given coordinates.
		 */
		VertexBuilder.TexOrColorOrNormalOrNextOrEnd vertex(double x, double y, double z);
		/**
		 * Start a new vertex, with the given coordinates.
		 */
		default VertexBuilder.TexOrColorOrNormalOrNextOrEnd vertex(Vector2dc vec) {
			return vertex(vec.x(), vec.y());
		}
		/**
		 * Start a new vertex, with the given coordinates.
		 */
		default VertexBuilder.TexOrColorOrNormalOrNextOrEnd vertex(Vector3dc vec) {
			return vertex(vec.x(), vec.y(), vec.z());
		}
	}

	@UIType
	public interface TexOrColorOrNormalOrNextOrEnd extends VertexBuilder.ColorOrNormalOrNextOrEnd {
		/**
		 * Set this vertex's texture coordinates to the given U and V. For
		 * 2D textures (which is most of them).
		 * @param u the horizontal coordinate within the texture, from 0 to 1
		 * @param v the vertical coordinate within the texture, from 0 to 1
		 */
		VertexBuilder.ColorOrNormalOrNextOrEnd tex(double u, double v);
		/**
		 * Set this vertex's texture coordinates to the given U, V, and W.
		 * For 3D textures.
		 * @param u the horizontal coordinate within the texture, from 0 to 1
		 * @param v the vertical coordinate within the texture, from 0 to 1
		 * @param w the layer of the texture, from 0 to 1
		 */
		VertexBuilder.ColorOrNormalOrNextOrEnd tex(double u, double v, double w);
	}

	@UIType
	public interface ColorOrNormalOrNextOrEnd extends VertexBuilder.NormalOrNextOrEnd {
		/**
		 * Set this vertex's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.NormalOrNextOrEnd color(double r, double g, double b, double a);
		/**
		 * Set this vertex's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.NormalOrNextOrEnd color(double r, double g, double b);
		/**
		 * Set this vertex's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.NormalOrNextOrEnd color(ProtoColor color);
		/**
		 * Set this vertex's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.NormalOrNextOrEnd color(ProtoColor color, double a);
		/**
		 * Set this vertex's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.NormalOrNextOrEnd color(int rgb);
	}

	@UIType
	public interface NormalOrNextOrEnd extends VertexBuilder.NextOrEnd {
		/**
		 * Set this vertex's normal (facing direction) to the given coordinates.
		 */
		VertexBuilder.NextOrEnd normal(double x, double y, double z);
		/**
		 * Set this vertex's normal (facing direction) to the given coordinates.
		 */
		default VertexBuilder.NextOrEnd normal(Vector3dc vec) {
			return normal(vec.x(), vec.y(), vec.z());
		}
	}

	@UIType
	public interface ColorThenNext {
		/**
		 * Set this shape's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.Next color(double r, double g, double b, double a);
		/**
		 * Set this shape's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.Next color(double r, double g, double b);
		/**
		 * Set this shape's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.Next color(ProtoColor color);
		/**
		 * Set this shape's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.Next color(ProtoColor color, double a);
		/**
		 * Set this shape's color to the given color. If it is also
		 * textured, this color is multiplied with the color of the texture.
		 */
		VertexBuilder.Next color(int rgb);
	}

	private VertexBuilder() {}
}
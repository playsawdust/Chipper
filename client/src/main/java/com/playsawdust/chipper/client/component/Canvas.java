/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.component;

import static org.lwjgl.opengl.GL20.*;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.units.qual.degrees;
import org.checkerframework.checker.units.qual.radians;

import com.playsawdust.chipper.client.CanvasInternalAccess;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.gl.GLTexture2D;
import com.playsawdust.chipper.client.gl.builder.ImmediateModeVertexBuilder;
import com.playsawdust.chipper.client.gl.builder.VertexBuilder;
import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.component.Component;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.component.Context.WhiteLotus;
import com.playsawdust.chipper.math.FastMath;
import com.playsawdust.chipper.math.ProtoColor;
import com.playsawdust.chipper.qual.PackedARGB;


/**
 * Utility methods for drawing 2D primitives with OpenGL.
 */
@UIType
public final class Canvas extends CanvasInternalAccess implements Component, Cloneable {

	public static final class State implements AutoCloseable {
		private boolean popped = false;

		private State() {}

		public void pop() {
			if (popped) return;
			popped = true;
			glPopMatrix();
		}

		/**
		 * @deprecated If popping States manually, call the more semantic
		 * 		{@link #pop} method instead; this method is for AutoCloseable conformance
		 */
		@Override
		@Deprecated
		public void close() {
			pop();
		}

	}

	/**
	 * Represents human-readable blend modes similar to those found in image
	 * manipulation programs.
	 */
	public enum BlendMode {
		NONE,
		NORMAL(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA),
		NORMAL_PREMULTIPLIED(GL_ONE, GL_ONE_MINUS_SRC_ALPHA),
		ADDITION(GL_SRC_ALPHA, GL_ONE),
		MULTIPLY(GL_ZERO, GL_SRC_COLOR),
		INVERT(GL_ONE_MINUS_DST_COLOR, GL_ZERO),
		;
		private final Runnable applicator;
		private BlendMode(int src, int dst) {
			applicator = () -> {
				glDisable(GL_ALPHA_TEST);
				glEnable(GL_BLEND);
				glBlendFunc(src, dst);
			};
		}
		private BlendMode() {
			applicator = () -> {
				glEnable(GL_ALPHA_TEST);
				glDisable(GL_BLEND);
			};
		}
		public void apply() {
			applicator.run();
		}
	}

	/**
	 * Represents raw OpenGL blend factors, for advanced use cases.
	 */
	public enum BlendFactor {
		ZERO(GL_ZERO),
		ONE(GL_ONE),
		SRC_COLOR(GL_SRC_COLOR),
		ONE_MINUS_SRC_COLOR(GL_ONE_MINUS_SRC_COLOR),
		SRC_ALPHA(GL_SRC_ALPHA),
		ONE_MINUS_SRC_ALPHA(GL_ONE_MINUS_SRC_ALPHA),
		DST_ALPHA(GL_DST_ALPHA),
		ONE_MINUS_DST_ALPHA(GL_ONE_MINUS_DST_ALPHA),
		SRC_ALPHA_SATURATE(GL_SRC_ALPHA_SATURATE),
		;
		public int constant;
		private BlendFactor(int constant) {
			this.constant = constant;
		}
	}

	/**
	 * Marks a Canvas method as not being {@link Canvas#startRecording recordable};
	 * it will throw an exception if it is called while a recording
	 * is in progress;
	 */
	@Documented
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.METHOD)
	public @interface NotRecordable {}

	/**
	 * Represents a recording captured by a pair of calls to
	 * {@link Canvas#startRecording() startRecording} and
	 * {@link Canvas#stopRecording() stopRecording}.
	 */
	@UIType
	public static final class Recording {
		private final int list;
		private boolean deleted = false;
		private Recording(int list) {
			this.list = list;
		}
		/**
		 * Delete any data related to this Recording, freeing up any memory
		 * used by it.
		 */
		public void delete() {
			if (deleted) return;
			glDeleteLists(list, 1);
			deleted = true;
		}
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			delete();
		}
	}

	private int list = 0;

	private int width;
	private int height;

	private Canvas(WhiteLotus lotus) {
		WhiteLotus.verify(lotus);
	}

	@Override
	protected void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * @return the width of the window in cpx
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return the height of the window in cpx
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Push the current render state onto the stack, for later restoration.
	 * <p>
	 * The returned State object is {@link AutoCloseable}, meaning it can
	 * be used with a try-with-resources block, like so:
	 * <pre>
	 * try (Canvas.State state = canvas.pushState()) {
	 *     // do stuff...
	 * }
	 * // the state is now restored
	 * </pre>
	 * The use of a try-with-resources also ensures that the render state will
	 * be restored even if an exception is thrown.
	 */
	public State pushState() {
		glPushMatrix();
		return new State();
	}

	/**
	 * Translate (move) the current coordinate space of this canvas by the given amount.
	 * @param x the number of {@link CanvasPixels cpx} to translate horizontally
	 * @param y the number of {@link CanvasPixels cpx} to translate vertically
	 */
	public void translate(double x, double y) {
		glTranslated(x, y, 0);
	}

	/**
	 * Scale the current coordinate space of this canvas by the given amount.
	 * @param x the horizontal multiplier to apply - for example, {@code 2} doubles sizes
	 * @param y the vertical multiplier to apply
	 */
	public void scale(double x, double y) {
		glScaled(x, y, 0);
	}

	/**
	 * Rotate the current coordinate space of this canvas by the given amount.
	 * @param deg the number of degrees to rotate by
	 */
	public void rotateDeg(@degrees double deg) {
		glRotated(deg, 0, 0, 1);
	}

	/**
	 * Rotate the current coordinate space of this canvas by the given amount.
	 * @param deg the number of degrees to rotate by
	 */
	public void rotate(@radians double rad) {
		rotateDeg(FastMath.toDegrees(rad));
	}

	/**
	 * Start recording render commands. Only some commands can be recorded;
	 * those that cannot will be annotated with {@link NotRecordable}. This
	 * can be used to cache complex shapes.
	 */
	@NotRecordable
	public void startRecording() {
		startRecording(GL_COMPILE_AND_EXECUTE);
	}

	/**
	 * Start recording render commands. Only some commands can be recorded;
	 * those that cannot will be annotated with {@link NotRecordable}. This
	 * can be used to cache complex shapes.
	 * <p>
	 * This variant of the method will not execute render commands run during
	 * the recording; the recording must be played back separately for anything
	 * drawn during the recording to appear on screen.
	 */
	@NotRecordable
	public void startRecordingSilent() {
		startRecording(GL_COMPILE);
	}

	private void startRecording(int mode) {
		if (list != 0) throw new IllegalStateException("Already recording");
		list = glGenLists(1);
		glNewList(list, mode);
	}

	/**
	 * Finish recording render commands, and return a handle to the recorded
	 * commands.
	 */
	public Recording stopRecording() {
		if (list == 0) throw new IllegalStateException("Not recording");
		glEndList();
		Recording r = new Recording(list);
		list = 0;
		return r;
	}

	/**
	 * Play back the render commands held by the given recording.
	 * @param recording
	 */
	public void playback(Recording recording) {
		if (recording.deleted) throw new IllegalStateException("Recording has been deleted");
		glCallList(recording.list);
	}

	/**
	 * Set the current blend mode to the passed blend mode.
	 */
	public void setBlendMode(BlendMode mode) {
		mode.apply();
	}

	/**
	 * Set the current blend function to the given factors.
	 */
	public void setBlendFunction(BlendFactor src, BlendFactor dst) {
		glEnable(GL_BLEND);
		glBlendFunc(src.constant, dst.constant);
	}

	/**
	 * Start building a shape. The return types of the given vertex builder and
	 * how they progress is designed to make it easy to use IDE autocomplete to
	 * see what you can do next. Upon calling {@link VertexBuilder.NextOrEnd#end end()},
	 * the shape will be immediately drawn.
	 * <p>
	 * Recommended code style when using this method (this example draws a
	 * square with an entire texture, colorizing the corners in a rainbow):
	 * <pre>
	 * canvas.startShape().quads().smooth()
	 *     .next().vertex(0, 0).tex(0, 0).color(1, 0, 0)
	 *     .next().vertex(1, 0).tex(1, 0).color(1, 1, 0)
	 *     .next().vertex(1, 1).tex(1, 1).color(0, 0, 1)
	 *     .next().vertex(0, 1).tex(0, 1).color(0, 1, 1)
	 * .end();
	 * </pre>
	 */
	public VertexBuilder.Format startShape() {
		return new ImmediateModeVertexBuilder();
	}

	/**
	 * Draw a rectangle of the given size and color at the given position.
	 */
	public void drawRect(double x, double y, double width, double height, @PackedARGB int color) {
		drawRect(x, y, width, height, ((color>>16)&0xFF)/255D, ((color>>8 )&0xFF)/255D, ((color>>0 )&0xFF)/255D, ((color>>24)&0xFF)/255D);
	}

	/**
	 * Draw a rectangle of the given size and color at the given position.
	 */
	public void drawRect(double x, double y, double width, double height, ProtoColor color) {
		drawRect(x, y, width, height, color, 1);
	}

	/**
	 * Draw a rectangle of the given size and color at the given position.
	 */
	public void drawRect(double x, double y, double width, double height, ProtoColor color, double alpha) {
		drawRect(x, y, width, height, color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	/**
	 * Draw a rectangle of the given size and color at the given position.
	 */
	public void drawRect(double x, double y, double width, double height, double r, double g, double b, double a) {
		double lef = x;
		double rih = x+width;
		double top = y;
		double bot = y+height;
		glDisable(GL_TEXTURE_2D);
		startShape().quads().flat().color(r, g, b, a)
			.next().vertex(lef, top)
			.next().vertex(rih, top)
			.next().vertex(rih, bot)
			.next().vertex(lef, bot)
		.end();
	}

	/**
	 * Draw a rectangle of the given size at the given position with the given
	 * texture and a default color of white.
	 */
	public void drawImage(GLTexture2D tex, double x, double y, double width, double height) {
		drawImage(tex, x, y, width, height, 1, 1, 1, 1);
	}

	/**
	 * Draw a rectangle of the given size and color at the given position with
	 * the given texture.
	 */
	public void drawImage(GLTexture2D tex, double x, double y, double width, double height, @PackedARGB int color) {
		drawImage(tex, x, y, width, height, ((color>>16)&0xFF)/255D, ((color>>8 )&0xFF)/255D, ((color>>0 )&0xFF)/255D, ((color>>24)&0xFF)/255D);
	}

	/**
	 * Draw a rectangle of the given size and color at the given position with
	 * the given texture.
	 */
	public void drawImage(GLTexture2D tex, double x, double y, double width, double height, ProtoColor color) {
		drawImage(tex, x, y, width, height, color, 1);
	}

	/**
	 * Draw a rectangle of the given size and color at the given position with
	 * the given texture.
	 */
	public void drawImage(GLTexture2D tex, double x, double y, double width, double height, ProtoColor color, double alpha) {
		drawImage(tex, x, y, width, height, color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	/**
	 * Draw a rectangle of the given size and color at the given position with
	 * the given texture.
	 */
	public void drawImage(GLTexture2D tex, double x, double y, double width, double height, double r, double g, double b, double a) {
		drawImage(tex, x, y, width, height, 0, 0, tex.getWidth(), tex.getHeight(), r, g, b, a);
	}

	/**
	 * Draw a rectangle of the given size and color at the given position with
	 * the given texture.
	 */
	public void drawImage(GLTexture2D tex,
			double x, double y, double width, double height,
			double srcX, double srcY, double srcWidth, double srcHeight,
			double r, double g, double b, double a) {
		double lef = x;
		double rih = x+width;
		double top = y;
		double bot = y+height;
		glEnable(GL_TEXTURE_2D);
		tex.bind();
		double minU = srcX/tex.getWidth();
		double minV = srcY/tex.getHeight();
		double maxU = (srcX+srcWidth)/tex.getWidth();
		double maxV = (srcY+srcHeight)/tex.getHeight();
		startShape().quads().flat().color(r, g, b, a)
			.next().vertex(lef, top).tex(minU, minV)
			.next().vertex(rih, top).tex(maxU, minV)
			.next().vertex(rih, bot).tex(maxU, maxV)
			.next().vertex(lef, bot).tex(minU, maxV)
		.end();
		glDisable(GL_TEXTURE_2D);
	}

	public static Canvas obtain(Context<? extends ClientEngine> ctx) {
		return ctx.getComponent(Canvas.class);
	}

	@Override
	public boolean compatibleWith(Engine engine) {
		return engine instanceof ClientEngine;
	}

}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.input;

import static com.playsawdust.chipper.client.BaseGL.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;

import org.lwjgl.glfw.GLFWImage;

import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.img.BufferedImage;


/**
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/cursor">https://developer.mozilla.org/en-US/docs/Web/CSS/cursor</a>
 */
public enum CursorType {
	/**
	 * No cursor is rendered.
	 */
	NONE(0),
	/**
	 * The platform-dependent default cursor. Typically an arrow.
	 */
	DEFAULT(GLFW_ARROW_CURSOR),
	/**
	 * The cursor is a pointer that indicates a link. Typically an image of a
	 * pointing hand.
	 */
	POINTER(GLFW_HAND_CURSOR),
	/**
	 * The text can be selected. Typically the shape of an I-beam.
	 */
	TEXT(GLFW_IBEAM_CURSOR),
	/**
	 * Cross cursor, often used to indicate selection in a bitmap.
	 */
	CROSSHAIR(GLFW_CROSSHAIR_CURSOR),
	/**
	 * Something is to be moved.
	 * <p>
	 * Falls back to {@link #CROSSHAIR} when system cursors are enabled due to
	 * <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	MOVE(GLFW_CROSSHAIR_CURSOR),
	/**
	 * Something can be grabbed (dragged to be moved).
	 * <p>
	 * Falls back to {@link #CROSSHAIR} when system cursors are enabled due to
	 * <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	GRAB(GLFW_CROSSHAIR_CURSOR),
	/**
	 * Something is being grabbed (dragged to be moved).
	 * <p>
	 * Falls back to {@link #CROSSHAIR} when system cursors are enabled due to
	 * <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	GRABBING(GLFW_CROSSHAIR_CURSOR),
	/**
	 * A top (north) edge is to be moved.
	 * <p>
	 * Falls back to {@link #RESIZE_NORTH_SOUTH} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_NORTH(GLFW_VRESIZE_CURSOR),
	/**
	 * A right (east) edge is to be moved.
	 * <p>
	 * Falls back to {@link #RESIZE_EAST_WEST} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_EAST(GLFW_HRESIZE_CURSOR),
	/**
	 * A bottom (south) edge is to be moved.
	 * <p>
	 * Falls back to {@link #RESIZE_NORTH_SOUTH} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_SOUTH(GLFW_VRESIZE_CURSOR),
	/**
	 * A left (west) edge is to be moved.
	 * <p>
	 * Falls back to {@link #RESIZE_EAST_WEST} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_WEST(GLFW_HRESIZE_CURSOR),
	/**
	 * A top-left (northwest) corner is to be moved.
	 * <p>
	 * Falls back to {@link #CROSSHAIR} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_NORTHWEST(GLFW_CROSSHAIR_CURSOR),
	/**
	 * A top-right (northeast) corner is to be moved.
	 * <p>
	 * Falls back to {@link #CROSSHAIR} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_NORTHEAST(GLFW_CROSSHAIR_CURSOR),
	/**
	 * A bottom-right (southeast) corner is to be moved.
	 * <p>
	 * Falls back to {@link #CROSSHAIR} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_SOUTHEAST(GLFW_CROSSHAIR_CURSOR),
	/**
	 * A bottom-left (southwest) corner is to be moved.
	 * <p>
	 * Falls back to {@link #CROSSHAIR} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_SOUTHWEST(GLFW_CROSSHAIR_CURSOR),
	/**
	 * A vertical (north or south / top or bottom) edge is to be moved.
	 */
	RESIZE_NORTH_SOUTH(GLFW_VRESIZE_CURSOR),
	/**
	 * A horizontal (east or west / right or left) edge is to be moved.
	 */
	RESIZE_EAST_WEST(GLFW_HRESIZE_CURSOR),
	/**
	 * A diagonal (top-right or bottom-left / northeast or southwest) corner is
	 * to be moved.
	 * <p>
	 * Falls back to {@link #CROSSHAIR} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_NORTHEAST_SOUTHWEST(GLFW_CROSSHAIR_CURSOR),
	/**
	 * A diagonal (top-left or bottom-right / northwest or southeast) corner is
	 * to be moved.
	 * <p>
	 * Falls back to {@link #CROSSHAIR} when system cursors are enabled
	 * due to <a href="https://github.com/glfw/glfw/issues/427">a GLFW limitation</a>.
	 */
	RESIZE_NORTHWEST_SOUTHEAST(GLFW_CROSSHAIR_CURSOR),
	;
	public final int glfwConst;
	protected long cursorHandle = NULL;
	protected int texture = -1;
	protected int hotspotX;
	protected int hotspotY;
	protected int width;
	protected int height;

	private ByteBuffer pixels;
	private boolean changed = true;

	private CursorType(int glfwConst) {
		this.glfwConst = glfwConst;
	}

	public void replaceWithSystemCursor() {
		if (cursorHandle != NULL) {
			glfwDestroyCursor(cursorHandle);
		}
		cursorHandle = NULL;
		if (pixels != null) {
			memFree(pixels);
			pixels = null;
		}
		changed = true;
	}
	
	public void replaceWithStandardCursor() {
		if (cursorHandle != NULL) {
			glfwDestroyCursor(cursorHandle);
		}
		cursorHandle = NULL;
		if (pixels != null) {
			memFree(pixels);
			pixels = null;
		}
		cursorHandle = glfwCreateStandardCursor(glfwConst);
		changed = false;
	}

	public void replace(Context<ClientEngine> ctx, BufferedImage img, int hotspotX, int hotspotY) {
		if (cursorHandle != NULL) {
			glfwDestroyCursor(cursorHandle);
		}
		int w = img.getWidth();
		int h = img.getHeight();
		replace(img.getRawABGRData(), w, h, hotspotX, hotspotY);
	}

	public void replace(ByteBuffer pixels, int width, int height, int hotspotX, int hotspotY) {
		changed = true;
		if (this.pixels != null) {
			memFree(this.pixels);
		}
		this.pixels = memAlloc(pixels.remaining());
		pixels.mark();
		this.pixels.put(pixels);
		this.pixels.flip();
		pixels.reset();
		this.width = width;
		this.height = height;
		this.hotspotX = hotspotX;
		this.hotspotY = hotspotY;
		try (GLFWImage gi = GLFWImage.malloc()) {
			gi.set(width, height, pixels);
			cursorHandle = glfwCreateCursor(gi, hotspotX, hotspotY);
		}
	}

	public boolean hasBeenCustomized() {
		return pixels != null;
	}
	
	public long unsafeGetCursorHandle() {
		return cursorHandle;
	}
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getHotspotX() { return hotspotX; }
	public int getHotspotY() { return hotspotY; }
	public int getTexture() { return texture; }
	
	public void createTexture() {
		if (texture == -1) {
			texture = glGenTextures();
		}
		if (pixels == null) return;
		if (changed) {
			glBindTexture(GL_TEXTURE_2D, texture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
		}
		changed = false;
	}
}

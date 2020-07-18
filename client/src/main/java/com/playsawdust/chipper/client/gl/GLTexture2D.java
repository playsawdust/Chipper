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

import java.nio.ByteBuffer;

import org.checkerframework.checker.guieffect.qual.UIType;

import com.playsawdust.chipper.AbstractNativeResource;
import com.playsawdust.chipper.img.BufferedImage;

/**
 * Represents a 2D OpenGL texture.
 */
@UIType
public final class GLTexture2D extends AbstractNativeResource {
	private final int name;

	private int width;
	private int height;

	private GLTexture2D(int name) {
		this.name = name;
	}

	/**
	 * <b>Unsafe</b>. GLTexture2D assumes it has complete ownership over the
	 * texture name it is holding, and this method can break that assumption.
	 */
	public int unsafeGetTextureName() {
		return name;
	}

	/**
	 * Bind this texture. Alters visible state.
	 */
	public void bind() {
		checkFreed();
		glBindTexture(GL_TEXTURE_2D, name);
	}

	@Override
	protected void _free() {
		glDeleteTextures(name);
	}

	public int getWidth() {
		checkFreed();
		return width;
	}

	public int getHeight() {
		checkFreed();
		return height;
	}

	/**
	 * Overwrite any existing pixel data in this texture with the pixels from
	 * the given image.
	 * @param format the pixel format to use on the GPU
	 * @param img the image whose pixels are to be uploaded
	 */
	public void upload(PixelFormat format, BufferedImage img) {
		checkFreed();
		bind();
		ByteBuffer rawData = img.getRawABGRData();
		this.width = img.getWidth();
		this.height = img.getHeight();
		glTexImage2D(GL_TEXTURE_2D, 0, format.glConstant, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, rawData);
	}

	/**
	 * Set the minification and magnification filters at the same time.
	 * @param filter the filter to use when shrinking or stretching this texture
	 */
	public void setFilter(Filter filter) {
		setMinFilter(filter);
		setMagFilter(filter);
	}

	/**
	 * Set the <i>min</i>ification filter for this texture.
	 * @param filter the filter to use when shrinking this texture
	 */
	public void setMinFilter(Filter filter) {
		checkFreed();
		bind();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter.glConstant);
	}

	/**
	 * Set the <i>mag</i>nification filter for this texture.
	 * @param filter the filter to use when stretching this texture
	 */
	public void setMagFilter(Filter filter) {
		checkFreed();
		bind();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter.glConstant);
	}

	/**
	 * Allocate a new GLTexture2D.
	 * @return a newly allocated GLTexture2D
	 */
	public static GLTexture2D allocate() {
		int name = glGenTextures();
		glPushAttrib(GL_TEXTURE_BIT);
		glBindTexture(GL_TEXTURE_2D, name);
		// the default min filter wants mipmaps, which makes textures incomplete
		// unless mipmaps are provided (not supported by this class yet)
		// rather than requiring everyone remember to call setMinFilter to switch
		// away from a default value they can't even set themselves, just set our
		// own defaults
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glPopAttrib();
		return new GLTexture2D(name);
	}

	/**
	 * <b>Unsafe</b>. GLTexture2D assumes it has complete ownership over the
	 * texture name it is holding, and this breaks that assumption.
	 * @param name the texture name to wrap
	 * @return a newly constructed GLTexture2D representing the given texture name
	 */
	public static GLTexture2D unsafeFromTextureName(int name) {
		return new GLTexture2D(name);
	}
}

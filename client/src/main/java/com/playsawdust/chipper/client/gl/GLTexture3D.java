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
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.util.Collection;

import org.checkerframework.checker.guieffect.qual.UIType;

import com.playsawdust.chipper.AbstractNativeResource;
import com.playsawdust.chipper.img.BufferedImage;

/**
 * Represents a 3D OpenGL texture - basically an array of textures of equal size.
 */
@UIType
public final class GLTexture3D extends AbstractNativeResource {
	private final int name;

	private int width;
	private int height;
	private int depth;

	private GLTexture3D(int name) {
		this.name = name;
	}

	/**
	 * <b>Unsafe</b>. GLTexture3D assumes it has complete ownership over the
	 * texture name it is holding, and this method can break that assumption.
	 * @return
	 */
	public int unsafeGetTextureName() {
		return name;
	}

	/**
	 * Bind this texture array. Alters visible state.
	 */
	public void bind() {
		checkFreed();
		glBindTexture(GL_TEXTURE_3D, name);
	}

	/**
	 * Delete this texture array. This object becomes invalid and the texture
	 * data on the GPU is deallocated.
	 */
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

	public int getDepth() {
		checkFreed();
		return depth;
	}

	/**
	 * Overwrite any existing pixel data in this texture with the pixels from
	 * the given images.
	 * @param format the pixel format to use on the GPU
	 * @param images the images whose pixels are to be uploaded
	 */
	public void upload(PixelFormat format, Collection<BufferedImage> images) {
		checkFreed();
		bind();
		int width = -1;
		int height = -1;
		if (images.isEmpty()) {
			width = 0;
			height = 0;
		} else {
			for (BufferedImage img : images) {
				if (img == null)
					throw new IllegalArgumentException("images may not contain nulls");
				if (width == -1 || height == -1) {
					width = img.getWidth();
					height = img.getHeight();
				}
				if (width != img.getWidth() || height != img.getHeight())
					throw new IllegalArgumentException("all images uploaded to a 3D texture must have the same width and height");
			}
		}
		ByteBuffer buf = memAlloc(width*height*images.size()*4);
		try {
			for (BufferedImage img : images) {
				buf.put(img.getRawABGRData());
			}
			buf.flip();
			glTexImage3D(GL_TEXTURE_3D, 0, format.glConstant, width, height, images.size(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
		} finally {
			memFree(buf);
		}
		this.width = width;
		this.height = height;
		this.depth = images.size();
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
		bind();
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, filter.glConstant);
	}

	/**
	 * Set the <i>mag</i>nification filter for this texture.
	 * @param filter the filter to use when stretching this texture
	 */
	public void setMagFilter(Filter filter) {
		bind();
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, filter.glConstant);
	}

	/**
	 * Allocate a new GLTexture2D.
	 * @return a newly allocated GLTexture2D
	 */
	public static GLTexture3D allocate() {
		int name = glGenTextures();
		glPushAttrib(GL_TEXTURE_BIT);
		glBindTexture(GL_TEXTURE_3D, name);
		// the default min filter wants mipmaps, which makes textures incomplete
		// unless mipmaps are provided (not supported by this class yet)
		// rather than requiring everyone remember to call setMinFilter to switch
		// away from a default value they can't even set themselves, just set our
		// own defaults
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glPopAttrib();
		return new GLTexture3D(name);
	}

	/**
	 * <b>Unsafe</b>. GLTexture3D assumes it has complete ownership over the
	 * texture name it is holding, and this breaks that assumption.
	 * @param name the texture name to wrap
	 * @return a newly constructed GLTexture2D representing the given texture name
	 */
	public static GLTexture3D unsafeFromTextureName(int name) {
		return new GLTexture3D(name);
	}
}

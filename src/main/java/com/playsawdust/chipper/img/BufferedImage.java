/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.img;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import com.playsawdust.chipper.AbstractNativeResource;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.*;

/**
 * BufferedImage is a replacement for {@link java.awt.image.BufferedImage}
 * for the purposes of using an image loader not tied to the entire AWT toolkit.
 *
 * LWJGL plays a lot nicer with its inbuilt stb_image, so let's wrap over it!
 */
public class BufferedImage extends AbstractNativeResource implements Image {
	private int height;
	private int width;
	private ByteBuffer image;

	public BufferedImage(int width, int height) {
		this.width = width;
		this.height = height;
		this.image = MemoryUtil.memAlloc(width*height*Integer.BYTES);
		this.image.order(ByteOrder.nativeOrder());
	}

	public BufferedImage(ByteBuffer buffer) throws IOException {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);
			IntBuffer comp = stack.mallocInt(1);

			image = stbi_load_from_memory(buffer, w, h, comp, 4);
			if (image == null) {
				throw new IOException("Failed to load a texture!"
						+ System.lineSeparator() + stbi_failure_reason());
			}

			width = w.get();
			height = h.get();
		}
	}

	@Override
	public int getWidth() {
		checkFreed();
		return width;
	}

	@Override
	public int getHeight() {
		checkFreed();
		return height;
	}

	@Override
	public int getPixel(int x, int y) {
		checkFreed();
		int index = ((y*this.width)+x)*4;
		int abgr = image.getInt(index);
		int rgba = abgr&0xFF00FF00;
		rgba |= (abgr&0x00FF0000)>>16;
		rgba |= (abgr&0x000000FF)<<16;
		return rgba;
	}

	@Override
	public void setPixel(int x, int y, int argb) {
		checkFreed();
		int index = ((y*this.width)+x)*4;
		int abgr = argb&0xFF00FF00;
		abgr |= (argb&0x00FF0000)>>16;
		abgr |= (argb&0x000000FF)<<16;
		image.putInt(index, abgr);
	}

	/**
	 * Note: Java is big-endian, so what is "ABGR" in Java is actually RGBA in little-endian native
	 * code, like OpenGL.
	 */
	public ByteBuffer getRawABGRData() {
		checkFreed();
		return image.slice().asReadOnlyBuffer().order(image.order());
	}

	public void getARGB(int startX, int startY, int width, int height, int[] argbBuf, int offset, int scansize) {
		checkFreed();
		for (int y = startY; y < height; y++) {
			for (int x = startX; x < width; x++) {
				int i = offset + (y - startY) * scansize + (x - startX);
				int abgr = image.getInt(((y*this.width)+x)*4);
				int argb = abgr&0xFF00FF00;
				argb |= (abgr&0x00FF0000)>>16;
				argb |= (abgr&0x000000FF)<<16;
				argbBuf[i] = argb;
			}
		}
	}

	public void getABGR(int startX, int startY, int width, int height, int[] abgrBuf, int offset, int scansize) {
		checkFreed();
		for (int y = startY; y < height; y++) {
			for (int x = startX; x < width; x++) {
				int i = offset + (y - startY) * scansize + (x - startX);
				int abgr = image.getInt(((y*this.width)+x)*4);
				abgrBuf[i] = abgr;
			}
		}
	}

	@Override
	protected void _free() {
		MemoryUtil.memFree(image);
	}
}

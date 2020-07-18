/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.img;

import org.bouncycastle.util.Arrays;

import com.google.common.base.Preconditions;

/**
 * Image which isn't backed by a native resource. Offers image transformation facilities.
 * LWImages may be slices of other LWImages, and are neither immutable nor threadsafe.
 */
public class LWImage implements Image {
	private int width;
	private int height;
	private int[] argb;
	
	private int windowX = 0;
	private int windowY = 0;
	private int windowWidth  = 0;
	private int windowHeight = 0;
	
	protected LWImage() {}
	
	public LWImage(Image im) {
		this(im.getWidth(), im.getHeight());
		
		//TODO: Optimize
		
		for(int y=0; y<im.getHeight(); y++) {
			for(int x=0; x<im.getWidth(); x++) {
				this.setPixel(x, y, im.getPixel(x, y));
			}
		}
	}
	
	public LWImage(int width, int height) {
		this.argb = new int[width*height];
		this.width = width;
		this.height = height;
		
		this.windowX = 0;
		this.windowY = 0;
		this.windowWidth = width;
		this.windowHeight= height;
	}
	
	@Override
	public int getWidth() {
		return windowWidth;
	}
	
	@Override
	public int getHeight() {
		return windowHeight;
	}
	
	/**
	 * Returns true only if this image's window size is not equal to the parent image size; a full-sized slice is not counted as a slice.
	 * 
	 * <p>This method is used to optimize certain block transfer operations.
	 */
	public boolean isSlice() {
		return width!=windowWidth || height!=windowHeight;
	}
	
	@Override
	public int getPixel(int x, int y) {
		Preconditions.checkElementIndex(x, windowWidth);
		Preconditions.checkElementIndex(y, windowHeight);
		
		int ix = x+windowX;
		int iy = y+windowY;
		
		int arrayIndex = iy*width+ix;
		return argb[arrayIndex];
	}
	
	@Override
	public void setPixel(int x, int y, int argb) {
		Preconditions.checkElementIndex(x, windowWidth);
		Preconditions.checkElementIndex(y, windowHeight);
		
		int ix = x+windowX;
		int iy = y+windowY;
		
		int arrayIndex = iy*width+ix;
		this.argb[arrayIndex] = argb;
	}
	
	/**
	 * Fills this image so that all pixels are the specified color
	 * @param argb the color, in sRGB space and ARGB order.
	 */
	public void fill(int argb) {
		if (windowX==0 && windowY==0 && windowWidth==width && windowHeight==height) {
			Arrays.fill(this.argb, argb);
		} else {
			for(int y=0; y<windowHeight; y++) {
				for(int x=0; x<windowWidth; x++) {
					setPixel(x, y, argb);
				}
			}
		}
	}
	
	/**
	 * Paints an image into this image.
	 * @param im
	 * @param x
	 * @param y
	 */
	public void paintImage(Image im, int x, int y) {
		for(int iy = 0; iy<im.getHeight(); iy++) {
			for(int ix = 0; ix<im.getWidth(); ix++) {
				setPixel(x+ix, y+iy, im.getPixel(ix, iy));
			}
		}
	}
	
	/**
	 * Creates a subimage of this image. Subimages use the same backing data, so performing an operation
	 * on the subimage will be reflected in the main image. Drawing operations called on the subimage which
	 * attempt to read or write areas outside the subimage will generate exceptions.
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
	public LWImage slice(int x, int y, int width, int height) {
		//The described window must fit inside the *existing* window and have nonzero dimensions
		Preconditions.checkArgument(x+width<=windowWidth);
		Preconditions.checkArgument(y+height<=windowHeight);
		Preconditions.checkArgument(x>=0);
		Preconditions.checkArgument(y>=0);
		Preconditions.checkArgument(width>0);
		Preconditions.checkArgument(height>0);
		
		LWImage result = new LWImage();
		result.argb = this.argb;
		result.width = this.width;
		result.height = this.height;
		result.windowX = this.windowX+x;
		result.windowY = this.windowY+y;
		result.windowWidth = width;
		result.windowHeight = height;
		
		return result;
	}
	
	/** Creates a copy of this image that is scaled to the specified dimensions. */
	public LWImage getScaled(int width, int height) {
		LWImage result = new LWImage(width, height);
		double xd = this.width  / (double) width;
		double yd = this.height / (double) height;
		double xi = 0;
		double yi = 0;
		for(int y=0; y<height; y++) {
			for(int x=0; x<width; x++) {
				result.setPixel(x, y, this.getPixel((int)xi, (int)yi));
				
				xi += xd;
			}
			xi = 0;
			yi += yd;
		}
		
		return result;
	}
	
	public BufferedImage asBufferedImage() {
		BufferedImage result = new BufferedImage(this.windowWidth, this.windowHeight);
		
		//TODO: Optimize?
		for(int y=0; y<windowHeight; y++) {
			for(int x=0; x<windowWidth; x++) {
				result.setPixel(x, y, this.getPixel(x, y));
			}
		}
		
		return result;
	}
}

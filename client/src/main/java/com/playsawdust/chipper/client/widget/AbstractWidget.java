/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.widget;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.client.widget.container.Container;

/**
 * Abstract implementation of most methods in {@link Widget}. See that class for
 * further documentation.
 */
public abstract class AbstractWidget implements Widget {

	private Container parent = null;

	private boolean needsLayout = true;

	private int minWidth = 0;
	private int minHeight = 0;

	private int maxWidth = UNLIMITED;
	private int maxHeight = UNLIMITED;



	@Override
	public boolean needsLayout() {
		return needsLayout;
	}

	@Override
	public void clearNeedsLayout() {
		needsLayout = false;
	}

	@Override
	public void setNeedsLayout() {
		needsLayout = true;
		Container p = getParent();
		if (p != null) p.setNeedsLayout();
	}

	@Override
	public void preLayout() {}

	@Override
	public void layout(int width, int height) {
		clearNeedsLayout();
	}



	@Override
	public @Nullable Container getParent() {
		return this.parent;
	}

	@Override
	public void setParent(@Nullable Container parent) {
		removeFromParent();
		this.parent = parent;
	}



	@Override
	public void setMinWidth(@CanvasPixels int minWidth) {
		this.minWidth = minWidth;
		setNeedsLayout();
	}

	@Override
	public void setMinHeight(@CanvasPixels int minHeight) {
		this.minHeight = minHeight;
		setNeedsLayout();
	}

	@Override
	public @CanvasPixels int getMinWidth() {
		return minWidth;
	}

	@Override
	public @CanvasPixels int getMinHeight() {
		return minHeight;
	}



	@Override
	public void setMaxWidth(@CanvasPixels int maxWidth) {
		this.maxWidth = maxWidth;
		setNeedsLayout();
	}

	@Override
	public void setMaxHeight(@CanvasPixels int maxHeight) {
		this.maxHeight = maxHeight;
		setNeedsLayout();
	}

	@Override
	public @CanvasPixels int getMaxWidth() {
		return maxWidth;
	}

	@Override
	public @CanvasPixels int getMaxHeight() {
		return maxHeight;
	}


	@Override
	public boolean isOpaque() {
		// false is the safe assumption
		return false;
	}

	@Override
	public boolean isFocusable() {
		return false;
	}


}

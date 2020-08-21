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

import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.Font;
import com.playsawdust.chipper.client.component.Canvas;
import com.playsawdust.chipper.client.input.KeyModifiers;
import com.playsawdust.chipper.client.input.CursorType;
import com.playsawdust.chipper.client.input.Key;
import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.component.Context;

/**
 * Abstract implementation of a button, leaving background drawing undefined,
 * as that's an art choice left up to the game built on Chipper.
 */
public abstract class AbstractButtonWidget extends TextWidget {

	private Runnable onClick;
	private Runnable onAlternateClick;

	private boolean hovered = false;

	public AbstractButtonWidget(Font font, String text) {
		super(font, text);
	}

	public AbstractButtonWidget(Font font) {
		super(font);
	}

	/**
	 * @param onClick the runnable to be invoked when this button is
	 * 		{@link Widget#onClick clicked}
	 */
	public AbstractButtonWidget onClick(Runnable onClick) {
		this.onClick = onClick;
		return this;
	}

	/**
	 * @param onAlternateClick the runnable to be invoked when this button is
	 * 		{@link Widget#onAlternateClick alternate-clicked}
	 */
	public AbstractButtonWidget onAlternateClick(Runnable onAlternateClick) {
		this.onAlternateClick = onAlternateClick;
		return this;
	}

	public boolean isHovered() {
		return hovered;
	}

	@Override
	public EventResponse onClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		if (onClick != null) {
			onClick.run();
			return EventResponse.ACCEPT;
		}
		return EventResponse.PASS;
	}

	@Override
	public EventResponse onAlternateClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		if (onAlternateClick != null) {
			onAlternateClick.run();
			return EventResponse.ACCEPT;
		}
		return EventResponse.PASS;
	}

	@Override
	public EventResponse onEnter(@CanvasPixels double x, @CanvasPixels double y) {
		hovered = true;
		return EventResponse.ACCEPT;
	}

	@Override
	public EventResponse onLeave(@CanvasPixels double x, @CanvasPixels double y) {
		hovered = false;
		return EventResponse.ACCEPT;
	}

	@Override
	public EventResponse onKeyUp(Key key, int scancode, KeyModifiers mod) {
		if (key == Key.ENTER || key == Key.SPACE) {
			if (mod.isShiftHeld() || mod.isControlHeld()) {
				onAlternateClick(0, 0, KeyModifiers.NONE);
			} else {
				onClick(0, 0, KeyModifiers.NONE);
			}
			return EventResponse.ACCEPT;
		}
		return EventResponse.PASS;
	}

	@Override
	public @Nullable CursorType getCursorType(double x, double y) {
		return CursorType.POINTER;
	}

	@Override
	public boolean isFocusable() {
		return true;
	}


	@Override
	public void draw(Context<ClientEngine> ctx, @CanvasPixels int width, @CanvasPixels int height, Canvas canvas) {
		drawBackground(ctx, width, height, canvas);
		super.draw(ctx, width, height, canvas);
	}

	/**
	 * Draw this button's background, based on its current state. May also
	 * transform the canvas for various animations, such as a hover effect.
	 * When doing so, {@link #getNaturalWidth()} and/or {@link #getNaturalHeight()}
	 * should be overridden to accomodate for the extra space taken by these
	 * transformations. (For example, if the hover animation translates the
	 * canvas 2cpx left, then {@code getNaturalWidth} should return at least
	 * {@code super.getNaturalWidth()+2}.)
	 * @param ctx the engine context
	 * @param width the width of this widget
	 * @param height the height of this widget
	 * @param canvas the canvas to draw to
	 */
	protected abstract void drawBackground(Context<ClientEngine> ctx, @CanvasPixels int width, @CanvasPixels int height, Canvas canvas);

}

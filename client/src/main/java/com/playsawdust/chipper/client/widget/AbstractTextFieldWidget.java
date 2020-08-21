/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.widget;

import static com.playsawdust.chipper.client.STBTexteditJ.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.Font;
import com.playsawdust.chipper.client.STBTexteditJ.STBTexteditString;
import com.playsawdust.chipper.client.STBTexteditJ.STB_TexteditState;
import com.playsawdust.chipper.client.STBTexteditJ.STBTexteditRow;
import com.playsawdust.chipper.client.component.Canvas;
import com.playsawdust.chipper.client.input.KeyModifiers;
import com.playsawdust.chipper.client.input.CursorType;
import com.playsawdust.chipper.client.input.Key;
import com.playsawdust.chipper.client.qual.CanvasPixels;

import com.google.common.base.Strings;

import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.math.ProtoColor;

import com.playsawdust.chipper.toolbox.lipstick.MonotonicTime;


/**
 * Abstract implementation of a text field, leaving background drawing undefined,
 * as that's an art choice left up to the game built on Chipper.
 */
public abstract class AbstractTextFieldWidget extends TextWidget {

	private STB_TexteditState state = new STB_TexteditState();
	private STBTexteditString str;

	private boolean mouseDown = false;

	private String placeholder = "";

	private ProtoColor selectionColor = ProtoColor.BLUE;
	private boolean ourSelectionColor = false;
	private double selectionAlpha = 0.2;
	private boolean selectionBehind = false;

	private String lastSentToCallback = null;

	private Consumer<String> inputCallback = null;

	private String obscuration;


	private final STBTexteditJDelegate delegate = new STBTexteditJDelegate() {

		@Override
		public float getWidth(STBTexteditString str, int from, int to) {
			return getFont().measure(str.toString(from, to));
		}

		@Override
		public void layoutRow(STBTexteditRow r, STBTexteditString str, int start) {
			String pre = str.toString(0, start);
			String post = str.toString(start, str.length());
			r.num_chars = (int)post.codePoints().count();
			r.x0 = getFont().measure(pre);
			r.x1 = getFont().measure(post)+r.x0;
			r.baseline_y_delta = getFont().getGlyphHeight();
			r.ymin = -getFont().getGlyphHeight();
			r.ymax = 0;
		}
	};

	public AbstractTextFieldWidget(Font font) {
		this(font, "");
	}

	public AbstractTextFieldWidget(Font font, String text) {
		super(font, text);
		setWordWrap(false);
		str = new STBTexteditString(text);
		stb_textedit_initialize_state(state, true);
	}

	@Override
	public String getText() {
		return str.toString();
	}

	@Override
	public void setText(String text) {
		str.delete(0, str.length());
		for (int i : text.codePoints().toArray()) {
			str.insert(str.length(), i);
		}
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	public void setSelectionColor(int color) {
		if (ourSelectionColor) this.selectionColor.recycle();
		this.selectionColor = ProtoColor.fromRGB(color);
		ourSelectionColor = true;
		selectionAlpha = (color >> 24)/255D;
	}

	public void setSelectionColor(ProtoColor color, double a) {
		if (ourSelectionColor) this.selectionColor.recycle();
		this.selectionColor = color;
		ourSelectionColor = false;
		this.selectionAlpha = a;
	}

	public void setSelectionColor(double r, double g, double b, double a) {
		if (ourSelectionColor) this.selectionColor.recycle();
		this.selectionColor = ProtoColor.fromRGB(r, g, b);
		ourSelectionColor = true;
		this.selectionAlpha = a;
	}

	public void setSelectionAlpha(double selectionAlpha) {
		this.selectionAlpha = selectionAlpha;
	}

	public void setSelectionBehind(boolean selectionBehind) {
		this.selectionBehind = selectionBehind;
	}

	public void setInputCallback(Consumer<String> inputCallback) {
		this.inputCallback = inputCallback;
		String s = str.toString();
		inputCallback.accept(s);
		lastSentToCallback = s;
	}

	public void setObscuration(String obscuration) {
		this.obscuration = obscuration;
	}

	public void setStandardObscuration() {
		setObscuration("\u2022");
	}

	public String getObscuration() {
		return obscuration;
	}

	@Override
	public EventResponse onClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		return EventResponse.ACCEPT;
	}

	@Override
	public EventResponse onMouseDown(int button, @CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		if (button == 0) {
			if (getParent() != null) {
				getParent().requestFocus(this);
			}
			stb_textedit_click(delegate, str, state, (float)x, (float)y);
			mouseDown = true;
		}
		return EventResponse.ACCEPT;
	}

	@Override
	public EventResponse onMove(@CanvasPixels double x, @CanvasPixels double y) {
		if (mouseDown) {
			stb_textedit_drag(delegate, str, state, (float)x, (float)y);
		}
		return EventResponse.ACCEPT;
	}

	@Override
	public EventResponse onMouseUp(int button, @CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		if (button == 0) {
			mouseDown = false;
		}
		return EventResponse.ACCEPT;
	}

	@Override
	public EventResponse onLeave(@CanvasPixels double x, @CanvasPixels double y) {
		mouseDown = false;
		return EventResponse.ACCEPT;
	}

	@Override
	public EventResponse onKeyDown(Key key, int scancode, KeyModifiers mod) {
		return onKeyRepeat(key, scancode, mod);
	}

	@Override
	public EventResponse onKeyRepeat(Key key, int scancode, KeyModifiers mod) {
		if (key == Key.UNKNOWN) return EventResponse.PASS;
		int i = key.getGlfwKeyCode() | (mod.toGlfwModifiers() << 16);
		switch (i) {
			case GLFW_KEY_ESCAPE:
				if (getParent() != null) {
					getParent().requestFocus(null);
					return EventResponse.ACCEPT;
				}
				return EventResponse.PASS;
			case STB_TEXTEDIT_K_CUT_WIN:
			case STB_TEXTEDIT_K_CUT_MAC:
			case STB_TEXTEDIT_K_CUT_CUA:
				if (STB_TEXT_HAS_SELECTION(state)) {
					String selection = str.toString(state.select_start, state.select_end);
					stb_textedit_cut(str, state);
					glfwSetClipboardString(NULL, selection);
					return EventResponse.ACCEPT;
				}
				return EventResponse.PASS;
			case STB_TEXTEDIT_K_COPY_WIN:
			case STB_TEXTEDIT_K_COPY_MAC:
			case STB_TEXTEDIT_K_COPY_CUA:
				if (STB_TEXT_HAS_SELECTION(state)) {
					String selection = str.toString(state.select_start, state.select_end);
					glfwSetClipboardString(NULL, selection);
					return EventResponse.ACCEPT;
				}
				return EventResponse.PASS;
			case STB_TEXTEDIT_K_PASTE_WIN:
			case STB_TEXTEDIT_K_PASTE_MAC:
			case STB_TEXTEDIT_K_PASTE_CUA:
				String clipboard = glfwGetClipboardString(NULL);
				if (clipboard != null) {
					int[] utf32 = clipboard.codePoints().toArray();
					return stb_textedit_paste(str, state, utf32, utf32.length) ? EventResponse.ACCEPT : EventResponse.PASS;
				}
				return EventResponse.PASS;
			case STB_TEXTEDIT_K_SELECTALL:
				state.select_start = 0;
				state.select_end = str.length();
				return EventResponse.ACCEPT;
			default:
				return stb_textedit_key(delegate, str, state, i) ? EventResponse.ACCEPT : EventResponse.PASS;
		}
	}

	@Override
	public EventResponse onTextEntered(int codepoint) {
		return stb_textedit_text(delegate, str, state, codepoint) ? EventResponse.ACCEPT : EventResponse.PASS;
	}

	@Override
	public @Nullable CursorType getCursorType(double x, double y) {
		return CursorType.TEXT;
	}

	@Override
	public boolean isFocusable() {
		return true;
	}


	@Override
	public void draw(Context<ClientEngine> ctx, @CanvasPixels int width, @CanvasPixels int height, Canvas canvas) {
		String pre = str.toString(0, state.cursor);
		String post = str.toString(state.cursor, str.length());
		String visiblePre = pre;
		String cat = pre.concat(post);
		double oldA = getTextAlpha();
		if (pre.isEmpty() && post.isEmpty()) {
			super.setText(placeholder);
			setTextAlpha(oldA/2);
		} else {
			if (obscuration != null) {
				String obscured = Strings.repeat(obscuration, cat.length()/obscuration.length())+obscuration.substring(0, cat.length()%obscuration.length());
				visiblePre = obscured.substring(0, state.cursor);
				super.setText(obscured);
			} else {
				super.setText(cat);
			}
		}
		if (!cat.equals(lastSentToCallback)) {
			lastSentToCallback = cat;
			if (inputCallback != null) {
				inputCallback.accept(cat);
			}
		}
		if (selectionBehind) {
			drawSelection(ctx, width, height, canvas);
		}
		drawBackground(ctx, width, height, canvas);
		super.draw(ctx, width, height, canvas);
		setTextAlpha(oldA);
		if (isFocused() && (int)(MonotonicTime.seconds()*2)%2 == 0) {
			double xofs = getFont().measure(visiblePre);
			canvas.drawRect(xofs, 0, 1, getFont().getGlyphHeight(), getTextColor(), getTextAlpha());
		}
		if (!selectionBehind) {
			drawSelection(ctx, width, height, canvas);
		}
	}

	private void drawSelection(Context<ClientEngine> ctx, @CanvasPixels int width, @CanvasPixels int height, Canvas canvas) {
		if (STB_TEXT_HAS_SELECTION(state)) {
			int start = Math.min(state.select_start, state.select_end);
			int end = Math.max(state.select_start, state.select_end);
			String preSel = str.toString(0, start);
			String sel = str.toString(start, end);
			double selxofs = getFont().measure(preSel);
			double selw = getFont().measure(sel);
			canvas.drawRect(selxofs, 0, selw, getFont().getGlyphHeight(), selectionColor, selectionAlpha);
		}
	}

	/**
	 * Draw this text field's background, based on its current state. May also
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

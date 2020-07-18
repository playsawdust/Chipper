/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.widget;

import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.Font;
import com.playsawdust.chipper.client.Font.PreparedString;
import com.playsawdust.chipper.client.component.Canvas;
import com.playsawdust.chipper.client.qual.AffectsLayout;
import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.math.ProtoColor;

public class TextWidget extends AbstractWidget {

	public enum Justify {
		LEFT,
		RIGHT,
		// TODO - CENTER and JUSTIFY
	}

	public enum Align {
		TOP,
		BOTTOM,
		CENTER,
	}

	private Font font;
	private String text;
	private PreparedString prepared;
	private ProtoColor textColor = ProtoColor.WHITE;
	private boolean ourTextColor = false;
	private double textAlpha = 1;
	private boolean shadowed = false;
	private Justify justify = Justify.LEFT;
	private Align align = Align.TOP;
	private boolean wordWrap = true;


	public TextWidget(Font font) {
		this.font = font;
	}

	public TextWidget(Font font, String text) {
		this.font = font;
		this.text = text;
	}

	@AffectsLayout
	public void setFont(Font font) {
		this.font = font;
		prepared = null;
		setNeedsLayout();
	}

	@AffectsLayout
	public void setText(String text) {
		this.text = text;
		prepared = null;
		setNeedsLayout();
	}

	public void setTextColor(int color) {
		if (ourTextColor) this.textColor.recycle();
		this.textColor = ProtoColor.fromRGB(color);
		ourTextColor = true;
		textAlpha = (color >> 24)/255D;
	}

	public void setTextColor(ProtoColor color, double a) {
		if (ourTextColor) this.textColor.recycle();
		this.textColor = color;
		ourTextColor = false;
		this.textAlpha = a;
	}

	public void setTextColor(double r, double g, double b, double a) {
		if (ourTextColor) this.textColor.recycle();
		this.textColor = ProtoColor.fromRGB(r, g, b);
		ourTextColor = true;
		this.textAlpha = a;
	}

	public void setTextAlpha(double a) {
		this.textAlpha = a;
	}

	public void setJustify(Justify justify) {
		this.justify = justify;
	}

	public void setAlign(Align align) {
		this.align = align;
	}

	public void setWordWrap(boolean wordWrap) {
		this.wordWrap = wordWrap;
	}

	public ProtoColor getTextColor() {
		return ourTextColor ? textColor.clone() : textColor;
	}

	public double getTextAlpha() {
		return textAlpha;
	}

	public boolean isShadowed() {
		return shadowed;
	}

	public Font getFont() {
		return font;
	}

	public String getText() {
		return text;
	}

	public Justify getJustify() {
		return justify;
	}

	public Align getAlign() {
		return align;
	}

	@Override
	public void layout(int width, int height) {
		prepared = prepare(text, width);
		clearNeedsLayout();
	}

	@Override
	public void draw(Context<ClientEngine> ctx, @CanvasPixels int width, @CanvasPixels int height, Canvas canvas) {
		if (prepared == null || !prepared.isValid()) {
			prepared = prepare(text, width);
		}
		if (justify == Justify.RIGHT) {
			canvas.translate(width-prepared.getWidth(), 0);
		}
		if (align == Align.BOTTOM) {
			canvas.translate(0, height-prepared.getHeight());
		} else if (align == Align.CENTER) {
			canvas.translate(0, (height-prepared.getHeight())/2);
		}
		if (shadowed) {
			font.drawShadowedString(0, 0, prepared, textColor, textAlpha);
		} else {
			font.drawString(0, 0, prepared, textColor, textAlpha);
		}
	}

	private PreparedString prepare(String text, @CanvasPixels int width) {
		if (wordWrap) {
			return font.wordWrap(text, width);
		}
		return font.prepare(text);
	}

	@Override
	public @CanvasPixels int getNaturalWidth() {
		if (font == null || text == null) return 0;
		return font.prepare(text).getWidth();
	}

	@Override
	public @CanvasPixels int getNaturalHeight() {
		if (font == null || text == null) return 0;
		return font.prepare(text).getHeight();
	}
}

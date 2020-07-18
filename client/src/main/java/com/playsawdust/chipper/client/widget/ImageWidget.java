/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.widget;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.component.Canvas;
import com.playsawdust.chipper.client.component.ResourceCache;
import com.playsawdust.chipper.client.component.Canvas.BlendMode;
import com.playsawdust.chipper.client.gl.GLTexture2D;
import com.playsawdust.chipper.client.qual.AffectsLayout;
import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.exception.ResourceNotFoundException;
import com.playsawdust.chipper.math.ProtoColor;
import com.playsawdust.chipper.qual.Namespace;

public class ImageWidget extends AbstractWidget {

	private Context<ClientEngine> ctx;
	private Identifier id;

	private BlendMode blendMode = BlendMode.NORMAL;
	private boolean preserveAspectRatio = true;

	private int padding = 0;

	public ImageWidget(Context<ClientEngine> ctx, @Namespace String namespace, String path) {
		this(ctx, new Identifier(namespace, path));
	}

	public ImageWidget(Context<ClientEngine> ctx, Identifier id) {
		this.ctx = ctx;
		this.id = id;
	}

	public void setPreserveAspectRatio(boolean preserveAspectRatio) {
		this.preserveAspectRatio = preserveAspectRatio;
	}
	public void setBlendMode(BlendMode blendMode) {
		this.blendMode = blendMode;
	}
	@AffectsLayout
	public void setImage(Identifier id) {
		this.id = id;
		setNeedsLayout();
	}
	public void setPadding(int padding) {
		this.padding = padding;
	}
	public boolean getPreserveAspectRatio() {
		return preserveAspectRatio;
	}
	public BlendMode getBlendMode() {
		return blendMode;
	}
	public Identifier getImage() {
		return id;
	}
	public int getPadding() {
		return padding;
	}

	@Override
	public void draw(Context<ClientEngine> ctx, @CanvasPixels int width, @CanvasPixels int height, Canvas canvas) {
		GLTexture2D tex;
		if (id == null) {
			tex = null;
		} else {
			try {
				tex = ResourceCache.obtain(ctx).getTexture(id);
			} catch (ResourceNotFoundException e) {
				id = null;
				tex = null;
				e.printStackTrace();
			}
		}
		if (tex == null) {
			double wh = width/2;
			double hh = height/2;
			canvas.drawRect(0, 0, wh, hh, ProtoColor.BLACK);
			canvas.drawRect(wh, 0, wh, hh, ProtoColor.MAGENTA);
			canvas.drawRect(0, hh, wh, hh, ProtoColor.MAGENTA);
			canvas.drawRect(wh, hh, wh, hh, ProtoColor.BLACK);
		} else {
			if (tex != null) {
				canvas.setBlendMode(blendMode);
				double x, y, w, h;
				if (preserveAspectRatio) {
					double r = Math.min(width/(double)tex.getWidth(), height/(double)tex.getHeight());
					w = tex.getWidth()*r;
					h = tex.getHeight()*r;
					x = (width-w)/2;
					y = (height-h)/2;
				} else {
					x = 0;
					y = 0;
					w = width;
					h = height;
				}
				x += padding;
				y += padding;
				w -= padding;
				h -= padding;
				canvas.drawImage(tex, x, y, w, h);
			}
		}
	}

	@Override
	public @CanvasPixels int getNaturalWidth() {
		if (id == null) return padding*2;
		try {
			return ResourceCache.obtain(ctx).getTexture(id).getWidth()+(padding*2);
		} catch (ResourceNotFoundException e) {
			return 0;
		}
	}

	@Override
	public @CanvasPixels int getNaturalHeight() {
		if (id == null) return padding*2;
		try {
			return ResourceCache.obtain(ctx).getTexture(id).getHeight()+(padding*2);
		} catch (ResourceNotFoundException e) {
			return 0;
		}
	}

}

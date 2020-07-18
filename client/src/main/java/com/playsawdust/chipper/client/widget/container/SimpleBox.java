/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.widget.container;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.component.Canvas;
import com.playsawdust.chipper.client.qual.AffectsLayout;
import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.client.widget.AbstractWidget;
import com.playsawdust.chipper.client.widget.DefaultEvent;
import com.playsawdust.chipper.client.widget.Widget;
import com.playsawdust.chipper.collect.unmodifiable.Unmodifiable;
import com.playsawdust.chipper.collect.unmodifiable.UnmodifiableList;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.math.FastMath;
import com.playsawdust.chipper.math.Point2D;
import com.playsawdust.chipper.math.Point2I;
import com.playsawdust.chipper.math.RectI;

/**
 * Implementation of a {@link Container} with a simple layout algorithm; every
 * child gets an equal slice of the box area, oriented in this box's direction.
 * <p>
 * Comparable to Box in AWT/Swing, UIStackView on iOS, and LinearLayout on
 * Android. Intended as an easy and quick introduction to Chipper's Widget
 * system, and a simple reference implementation of Container.
 */
public class SimpleBox extends AbstractWidget implements Container {

	public enum Orientation {
		/**
		 * Children will have a fixed height equal to this box's height, and a
		 * width consisting of an equal slice of this box's width.
		 */
		HORIZONTAL,
		/**
		 * Children will have a fixed width equal to this box's width, and a
		 * height consisting of an equal slice of this box's height.
		 */
		VERTICAL,
		;
	}

	public enum AlignX {
		/**
		 * Place widgets with a maximum size smaller than their allocated slice
		 * toward the left (-X) side of their slice.
		 */
		LEFT,
		/**
		 * Place widgets with a maximum size smaller than their allocated slice
		 * in the center of their slice.
		 */
		CENTER,
		/**
		 * Place widgets with a maximum size smaller than their allocated slice
		 * toward the right (+X) side of their slice.
		 */
		RIGHT,
		/**
		 * Force widgets to be the size of their slice, even if that would make
		 * their width greater than their maximum width.
		 */
		STRETCH,
	}
	public enum AlignY {
		/**
		 * Place widgets with a maximum size smaller than their allocated slice
		 * toward the top (-Y) side of their slice.
		 */
		TOP,
		/**
		 * Place widgets with a maximum size smaller than their allocated slice
		 * in the center of their slice.
		 */
		CENTER,
		/**
		 * Place widgets with a maximum size smaller than their allocated slice
		 * toward the bottom (+Y) side of their slice.
		 */
		BOTTOM,
		/**
		 * Force widgets to be the size of their slice, even if that would make
		 * their height greater than their maximum height.
		 */
		STRETCH,
	}

	private Orientation orientation;
	private AlignX alignX;
	private AlignY alignY;

	private List<RectI> widgetRects = Lists.newArrayList();

	private boolean opaque = false;
	private final List<Widget> childrenModifiable = Lists.newArrayList();
	protected final UnmodifiableList<Widget> children = Unmodifiable.list(childrenModifiable);
	private final List<Widget> focusableChildren = Lists.newArrayList();

	private Widget focused = null;

	public SimpleBox(Orientation orientation) {
		Preconditions.checkArgument(orientation != null, "orientation cannot be null");
		this.orientation = orientation;
		if (orientation == Orientation.HORIZONTAL) {
			alignX = AlignX.STRETCH;
			alignY = AlignY.TOP;
		} else {
			alignX = AlignX.LEFT;
			alignY = AlignY.STRETCH;
		}
	}

	public SimpleBox( Orientation orientation, AlignX alignX, AlignY alignY) {
		Preconditions.checkArgument(orientation != null, "orientation cannot be null");
		Preconditions.checkArgument(alignX != null, "alignX cannot be null");
		Preconditions.checkArgument(alignY != null, "alignY cannot be null");
		this.orientation = orientation;
		this.alignX = alignX;
		this.alignY = alignY;
	}

	@AffectsLayout
	public void setOrientation(Orientation orientation) {
		Preconditions.checkArgument(orientation != null, "orientation cannot be null");
		this.orientation = orientation;
		setNeedsLayout();
	}

	@AffectsLayout
	public void setAlignX(AlignX alignX) {
		Preconditions.checkArgument(alignX != null, "alignX cannot be null");
		this.alignX = alignX;
		setNeedsLayout();
	}

	@AffectsLayout
	public void setAlignY(AlignY alignY) {
		Preconditions.checkArgument(alignY != null, "alignY cannot be null");
		this.alignY = alignY;
		setNeedsLayout();
	}

	public Orientation getOrientation() {
		return orientation;
	}

	public AlignX getAlignX() {
		return alignX;
	}

	public AlignY getAlignY() {
		return alignY;
	}

	@Override
	public void layout(int width, int height) {
		widgetRects.forEach(RectI::recycle);
		widgetRects.clear();
		focusableChildren.clear();
		for (Widget w : children) {
			if (w.needsLayout()) w.preLayout();
			if (w.isFocusable()) focusableChildren.add(w);
		}
		if (!focusableChildren.contains(focused)) {
			requestFocus(null);
		}
		int sliceWidth;
		int sliceHeight;
		if (orientation == Orientation.HORIZONTAL) {
			sliceWidth = width/children.size();
			sliceHeight = height;
		} else {
			sliceWidth = width;
			sliceHeight = height/children.size();
		}
		int x = 0;
		int y = 0;
		opaque = true;
		for (int i = 0; i < children.size(); i++) {
			Widget w = children.get(i);
			int widgetWidth = sliceWidth;
			int widgetHeight = sliceHeight;
			int ofsX = 0;
			int ofsY = 0;
			if (alignX != AlignX.STRETCH && widgetWidth > w.getMaxWidth()) {
				widgetWidth = w.getMaxWidth();
				if (alignX == AlignX.RIGHT) {
					ofsX = sliceWidth-widgetWidth;
				} else if (alignX == AlignX.CENTER) {
					ofsX = (sliceWidth-widgetWidth)/2;
				}
			}
			if (alignY != AlignY.STRETCH && widgetHeight > w.getMaxHeight()) {
				widgetHeight = w.getMaxHeight();
				if (alignY == AlignY.BOTTOM) {
					ofsY = sliceHeight-widgetHeight;
				} else if (alignY == AlignY.CENTER) {
					ofsY = (sliceHeight-widgetHeight)/2;
				}
			}
			// are there any gaps?
			if (ofsX != 0 || ofsY != 0 || widgetWidth < sliceWidth || widgetHeight < sliceHeight) {
				opaque = false;
			}
			widgetRects.add(RectI.fromSize(x+ofsX, y+ofsY, widgetWidth, widgetHeight));
			w.layout(widgetWidth, widgetHeight);
			// if any widget isn't opaque, then the container is not opaque
			if (!w.isOpaque()) {
				opaque = false;
			}
			if (orientation == Orientation.HORIZONTAL) {
				x += sliceWidth;
			} else {
				y += sliceHeight;
			}
		}
		clearNeedsLayout();
	}

	@Override
	public void draw(Context<ClientEngine> ctx, @CanvasPixels int width, @CanvasPixels int height, Canvas canvas) {
		if (widgetRects.size() != children.size()) {
			throw new IllegalStateException("Layout not in sync!");
		}
		for (int i = 0; i < children.size(); i++) {
			Widget w = children.get(i);
			RectI r = widgetRects.get(i);
			try (Canvas.State s = canvas.pushState()) {
				canvas.translate(r.getX(), r.getY());
				w.draw(ctx, r.getWidth(), r.getHeight(), canvas);
			}
		}
	}

	@Override
	public UnmodifiableList<Widget> hit(double x, double y) {
		if (widgetRects.size() != children.size()) {
			throw new IllegalStateException("Layout not in sync!");
		}
		List<Widget> li = Lists.newArrayList();
		// in Z order
		for (int i = children.size()-1; i >= 0; i--) {
			if (widgetRects.get(i).intersects(FastMath.floor(x), FastMath.floor(y))) {
				li.add(children.get(i));
			}
		}
		return Unmodifiable.list(li);
	}

	private RectI getRect(Widget w) {
		if (widgetRects.size() != children.size()) {
			throw new IllegalStateException("Layout not in sync!");
		}
		for (int i = 0; i < children.size(); i++) {
			Widget that = children.get(i);
			if (that == w) {
				return widgetRects.get(i);
			}
		}
		throw new IllegalArgumentException("widget is not a child of this container");
	}

	@Override
	public boolean wouldHit(double x, double y, Widget w) {
		return getRect(w).intersects(x, y);
	}

	@Override
	public void adjust(Widget w, Point2D point) {
		try (Point2I topLeft = getRect(w).getTopLeft()) {
			point.subtract(topLeft);
		}
	}

	@Override
	public void unadjust(Widget w, Point2D point) {
		try (Point2I topLeft = getRect(w).getTopLeft()) {
			point.add(topLeft);
		}
	}

	@Override
	public void getSize(Widget w, RectI rect) throws IllegalArgumentException {
		rect.set(getRect(w));
		rect.setX(0);
		rect.setY(0);
	}

	@Override
	public @CanvasPixels int getNaturalWidth() {
		int accum = 0;
		for (Widget w : children) {
			accum += Math.max(w.getMinWidth(), w.getNaturalWidth());
		}
		return accum;
	}

	@Override
	public @CanvasPixels int getNaturalHeight() {
		int accum = 0;
		for (Widget w : children) {
			accum += Math.max(w.getMinHeight(), w.getNaturalHeight());
		}
		return accum;
	}

	@Override
	public boolean isOpaque() {
		if (widgetRects.size() != children.size()) {
			throw new IllegalStateException("Layout not in sync!");
		}
		return opaque;
	}

	public void add(Widget widget) {
		Preconditions.checkArgument(widget != null, "widget cannot be null");
		widget.removeFromParent();
		childrenModifiable.add(widget);
		widget.setParent(this);
		setNeedsLayout();
	}

	@Override
	public void remove(Widget widget) {
		Preconditions.checkArgument(widget != null, "widget cannot be null");
		if (childrenModifiable.remove(widget)) {
			widget.setParent(null);
		}
		setNeedsLayout();
	}

	@Override
	public void clear() {
		for (Widget w : childrenModifiable) {
			w.setParent(null);
		}
		childrenModifiable.clear();
		setNeedsLayout();
	}

	@Override
	public UnmodifiableIterator<Widget> iterator() {
		return children.iterator();
	}

	@Override
	public int size() {
		return childrenModifiable.size();
	}

	@Override
	public @Nullable Widget getFocusedWidget() {
		return focused;
	}

	@Override
	public void requestFocus(Widget w) {
		if (w != null )
		if (focused != null) {
			try (DefaultEvent e = DefaultEvent.focusLost()) {
				w.processEvent(e);
			}
		}
		focused = w;
		if (w != null) {
			try (DefaultEvent e = DefaultEvent.focusGained()) {
				w.processEvent(e);
			}
		}
	}

	@Override
	public void focusNext() {
		if (focusableChildren.isEmpty()) return;
		if (Container.defaultFocusNextBehavior(this)) return;
		int idx = focused == null ? -1 : focusableChildren.indexOf(focused);
		idx++;
		if (idx >= focusableChildren.size()) {
			idx = 0;
		}
		Widget newFocused = focusableChildren.get(idx);
		if (newFocused == focused) {
			if (focused instanceof Container) {
				((Container)focused).focusNext();
			}
		}
		requestFocus(newFocused);
	}

	@Override
	public void focusPrevious() {
		if (focusableChildren.isEmpty()) return;
		if (Container.defaultFocusPreviousBehavior(this)) return;
		int idx = focused == null ? 0 : focusableChildren.indexOf(focused);
		idx--;
		if (idx < 0) {
			idx = focusableChildren.size()-1;
		}
		Widget newFocused = focusableChildren.get(idx);
		if (newFocused == focused) {
			if (focused instanceof Container) {
				((Container)focused).focusPrevious();
			}
		}
		requestFocus(newFocused);
	}

	@Override
	public boolean focusAtStart() {
		if (focused instanceof Container && !((Container) focused).focusAtStart()) return false;
		return focusableChildren.indexOf(focused) == 0;
	}

	@Override
	public boolean focusAtEnd() {
		if (focused instanceof Container && !((Container) focused).focusAtEnd()) return false;
		return focusableChildren.indexOf(focused) == focusableChildren.size()-1;
	}

}

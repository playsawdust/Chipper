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
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.component.Canvas;
import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.client.widget.AbstractWidget;
import com.playsawdust.chipper.client.widget.DefaultEvent;
import com.playsawdust.chipper.client.widget.Widget;
import com.playsawdust.chipper.collect.unmodifiable.Unmodifiable;
import com.playsawdust.chipper.collect.unmodifiable.UnmodifiableList;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.math.Point2D;
import com.playsawdust.chipper.math.RectI;

import blue.endless.splinter.Alignment;
import blue.endless.splinter.GrowType;
import blue.endless.splinter.Layout;
import blue.endless.splinter.LayoutContainer;
import blue.endless.splinter.LayoutContainerMetrics;
import blue.endless.splinter.LayoutElement;
import blue.endless.splinter.LayoutElementMetrics;

/**
 * An implementation of Container, using Falkreon's Splinter layout system.
 * <p>
 * Documentation TODO - Splinter isn't finished.
 */
public class SplinterBox extends AbstractWidget implements Container {

	private class SplinterWidget implements LayoutElement {
		protected final Widget widget;
		private final LayoutElementMetrics metrics;

		private int x;
		private int y;
		private int width;
		private int height;

		private SplinterWidget(Widget widget, LayoutElementMetrics metrics) {
			this.widget = widget;
			this.metrics = metrics;
		}

		@Override
		public int getNaturalWidth() {
			return widget.getNaturalWidth();
		}
		@Override
		public int getNaturalHeight() {
			return widget.getNaturalHeight();
		}
	}

	private class SplinterBoxChild extends SplinterWidget implements LayoutContainer {
		private SplinterBoxChild(SplinterBox widget, LayoutElementMetrics metrics) {
			super(widget, metrics);
		}

		@Override
		public Iterable<? extends LayoutElement> getLayoutChildren() {
			return ((SplinterBox)widget).container.getLayoutChildren();
		}

		@Override
		public LayoutElementMetrics getLayoutElementMetrics(LayoutElement elem) {
			return ((SplinterBox)widget).container.getLayoutElementMetrics(elem);
		}

		@Override
		public LayoutContainerMetrics getLayoutContainerMetrics() {
			return ((SplinterBox)widget).container.getLayoutContainerMetrics();
		}

		@Override
		public void setLayoutValues(LayoutElement elem, int x, int y, int width, int height) {
			((SplinterBox)widget).container.setLayoutValues(elem, x, y, width, height);
		}
	}

	private class SplinterContainer implements LayoutContainer {
		private final LayoutContainerMetrics metrics = new LayoutContainerMetrics();

		@Override
		public Iterable<? extends LayoutElement> getLayoutChildren() {
			return children;
		}

		@Override
		public LayoutElementMetrics getLayoutElementMetrics(LayoutElement elem) {
			if (elem instanceof SplinterWidget) {
				return ((SplinterWidget)elem).metrics;
			} else {
				throw new AssertionError();
			}
		}

		@Override
		public LayoutContainerMetrics getLayoutContainerMetrics() {
			return metrics;
		}

		@Override
		public void setLayoutValues(LayoutElement elem, int x, int y, int width, int height) {
			if (elem instanceof SplinterWidget) {
				SplinterWidget sw = (SplinterWidget)elem;
				sw.x = x;
				sw.y = y;
				sw.width = width;
				sw.height = height;
			} else {
				throw new AssertionError();
			}
		}
	}

	private final SplinterContainer container = new SplinterContainer();
	private final List<SplinterWidget> children = Lists.newArrayList();
	private final Map<Widget, SplinterWidget> childrenLookup = Maps.newIdentityHashMap();
	private final List<Widget> focusableChildren = Lists.newArrayList();

	private Widget focused = null;

	private boolean debug = false;

	@Override
	public @CanvasPixels int getNaturalWidth() {
		int sum = 0;
		for (SplinterWidget sw : children) {
			sum += sw.getNaturalWidth();
		}
		return sum;
	}

	@Override
	public @CanvasPixels int getNaturalHeight() {
		int sum = 0;
		for (SplinterWidget sw : children) {
			sum += sw.getNaturalHeight();
		}
		return sum;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void setCellPadding(int padding) {
		container.metrics.setCellPadding(padding);
	}

	@Override
	public UnmodifiableList<Widget> hit(double x, double y) {
		List<Widget> li = Lists.newArrayList();
		try (RectI rect = RectI.get()) {
			for (SplinterWidget sw : children) {
				rect.setSize(sw.x, sw.y, sw.width, sw.height);
				if (rect.intersects(x, y)) li.add(sw.widget);
			}
		}
		return Unmodifiable.list(li);
	}

	@Override
	public boolean wouldHit(double x, double y, Widget w) throws IllegalArgumentException {
		SplinterWidget sw = lookup(w);
		try (RectI rect = RectI.fromSize(sw.x, sw.y, sw.width, sw.height)) {
			return rect.intersects(x, y);
		}
	}

	@Override
	public void adjust(Widget w, Point2D point) throws IllegalArgumentException {
		adjust(w, point, -1);
	}

	@Override
	public void unadjust(Widget w, Point2D point) throws IllegalArgumentException {
		adjust(w, point, 1);
	}

	private void adjust(Widget w, Point2D point, int mul) {
		SplinterWidget sw = lookup(w);
		point.add(sw.x*mul, sw.y*mul);
	}

	private SplinterWidget lookup(Widget w) {
		SplinterWidget sw = childrenLookup.get(w);
		if (sw != null) {
			return sw;
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void getSize(Widget w, RectI rect) throws IllegalArgumentException {
		SplinterWidget sw = lookup(w);
		rect.setEdges(0, 0, sw.width, sw.height);
	}

	@Override
	public void draw(Context<ClientEngine> ctx, @CanvasPixels int width, @CanvasPixels int height, Canvas canvas) {
		for (SplinterWidget sw : children) {
			try (Canvas.State s = canvas.pushState()) {
				canvas.translate(sw.x, sw.y);
				if (debug) {
					canvas.drawRect(0, 0, sw.width, sw.height, sw.hashCode()|0xFF000000);
				}
				sw.widget.draw(ctx, sw.width, sw.height, canvas);
			}
		}
	}

	@Override
	public void layout(int width, int height) {
		focusableChildren.clear();
		for (SplinterWidget sw : children) {
			sw.widget.preLayout();
			sw.metrics.fixedMinX = sw.widget.getMinWidth();
			sw.metrics.fixedMinY = sw.widget.getMinHeight();
		}
		Layout.layout(container, 0, 0, width, height, false);
		for (SplinterWidget sw : children) {
			sw.widget.layout(sw.width, sw.height);
			if (sw.widget.isFocusable()) {
				focusableChildren.add(sw.widget);
			}
		}
		focusableChildren.sort((a, b) -> Integer.compare(lookup(a).y, lookup(b).y));
		clearNeedsLayout();
	}

	/**
	 * Add the given Widget to this SplinterBox at the given cell coordinates.
	 * @param widget the widget to add
	 * @param cellX the cell X (horizontal)
	 * @param cellY the cell Y (vertical)
	 */
	public void add(Widget widget, int cellX, int cellY) {
		add(widget, new LayoutElementMetrics(cellX, cellY));
	}

	/**
	 * Add the given Widget to this SplinterBox at the given cell coordinates,
	 * spanning the given number of cells.
	 * @param widget the widget to add
	 * @param cellX the cell X (horizontal)
	 * @param cellY the cell Y (vertical)
	 * @param cellWidth the horizontal cell span
	 * @param cellHeight the vertical cell span
	 */
	public void add(Widget widget, int cellX, int cellY, int cellWidth, int cellHeight) {
		add(widget, new LayoutElementMetrics(cellX, cellY, cellWidth, cellHeight));
	}

	private void add(Widget widget, LayoutElementMetrics metrics) {
		Preconditions.checkArgument(widget != null, "widget cannot be null");
		widget.removeFromParent();
		SplinterWidget sw = widget instanceof SplinterBox
				? new SplinterBoxChild((SplinterBox)widget, metrics)
				: new SplinterWidget(widget, metrics);
		children.add(sw);
		childrenLookup.put(widget, sw);
		widget.setParent(this);
		setNeedsLayout();
	}

	/**
	 * Set the padding for all sides of the given widget. That is, the amount of
	 * space there must be between the child element and the borders of its
	 * logical cell.
	 * @param widget the widget to set padding for
	 * @param all the number of canvas pixels of padding to have on each side
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	public void setPadding(Widget widget, @CanvasPixels int all) throws IllegalArgumentException {
		setPadding(widget, all, all, all, all);
	}

	/**
	 * Set the padding for all sides of the given widget, individually. That is,
	 * the amount of space there must be between the child element and the
	 * borders of its logical cell.
	 * @param widget the widget to set padding for
	 * @param left the number of canvas pixels of padding to have on the left
	 * @param top the number of canvas pixels of padding to have on the top
	 * @param right the number of canvas pixels of padding to have on the right
	 * @param bottom the number of canvas pixels of padding to have on the bottom
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	public void setPadding(Widget widget,
			@CanvasPixels int left, @CanvasPixels int top,
			@CanvasPixels int right, @CanvasPixels int bottom)
					throws IllegalArgumentException {
		SplinterWidget sw = lookup(widget);
		sw.metrics.paddingLeft = left;
		sw.metrics.paddingTop = top;
		sw.metrics.paddingRight = right;
		sw.metrics.paddingBottom = bottom;
		setNeedsLayout();
	}

	/**
	 * Set the padding for the left side of the given widget. That is, the
	 * amount of space there must be between the child element and the borders
	 * of its logical cell.
	 * @param widget the widget to set padding for
	 * @param padding the number of canvas pixels of padding to have on the left
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	public void setPaddingLeft(Widget widget, @CanvasPixels int padding) throws IllegalArgumentException {
		lookup(widget).metrics.paddingLeft = padding;
		setNeedsLayout();
	}

	/**
	 * Set the padding for the top side of the given widget. That is, the
	 * amount of space there must be between the child element and the borders
	 * of its logical cell.
	 * @param widget the widget to set padding for
	 * @param padding the number of canvas pixels of padding to have on the top
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	public void setPaddingTop(Widget widget, @CanvasPixels int padding) throws IllegalArgumentException {
		lookup(widget).metrics.paddingTop = padding;
		setNeedsLayout();
	}

	/**
	 * Set the padding for the right side of the given widget. That is, the
	 * amount of space there must be between the child element and the borders
	 * of its logical cell.
	 * @param widget the widget to set padding for
	 * @param padding the number of canvas pixels of padding to have on the right
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	public void setPaddingRight(Widget widget, @CanvasPixels int padding) throws IllegalArgumentException {
		lookup(widget).metrics.paddingRight = padding;
		setNeedsLayout();
	}

	/**
	 * Set the padding for the bottom side of the given widget. That is, the
	 * amount of space there must be between the child element and the borders
	 * of its logical cell.
	 * @param widget the widget to set padding for
	 * @param padding the number of canvas pixels of padding to have on the bottom
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	public void setPaddingBottom(Widget widget, @CanvasPixels int padding) throws IllegalArgumentException {
		lookup(widget).metrics.paddingBottom = padding;
		setNeedsLayout();
	}

	public void setHorizontalGrowType(Widget widget, GrowType type) throws IllegalArgumentException {
		lookup(widget).metrics.horizontalGrowType = type;
		setNeedsLayout();
	}

	public void setVerticalGrowType(Widget widget, GrowType type) throws IllegalArgumentException {
		lookup(widget).metrics.verticalGrowType = type;
		setNeedsLayout();
	}

	public void setGrowType(Widget widget, GrowType type) throws IllegalArgumentException {
		lookup(widget).metrics.horizontalGrowType = type;
		lookup(widget).metrics.verticalGrowType = type;
		setNeedsLayout();
	}

	public void setHorizontalAlignment(Widget widget, Alignment alignment) throws IllegalArgumentException {
		lookup(widget).metrics.horizontalAlignment = alignment;
		setNeedsLayout();
	}

	public void setVerticalAlignment(Widget widget, Alignment alignment) throws IllegalArgumentException {
		lookup(widget).metrics.verticalAlignment = alignment;
		setNeedsLayout();
	}

	public void setAlignment(Widget widget, Alignment alignment) throws IllegalArgumentException {
		lookup(widget).metrics.horizontalAlignment = alignment;
		lookup(widget).metrics.verticalAlignment = alignment;
		setNeedsLayout();
	}

	@Override
	public void remove(Widget widget) {
		Preconditions.checkArgument(widget != null, "widget cannot be null");
		SplinterWidget sw = childrenLookup.remove(widget);
		if (sw != null) {
			widget.setParent(null);
			children.remove(sw);
			setNeedsLayout();
		}
	}

	@Override
	public void clear() {
		for (SplinterWidget sw : children) {
			sw.widget.setParent(null);
		}
		children.clear();
		setNeedsLayout();
	}

	@Override
	public UnmodifiableIterator<Widget> iterator() {
		return Unmodifiable.iterator(Iterators.transform(children.iterator(), sw -> sw.widget));
	}

	@Override
	public int size() {
		return children.size();
	}

	@Override
	public @Nullable Widget getFocusedWidget() {
		return focused;
	}

	@Override
	public boolean isFocusable() {
		return !focusableChildren.isEmpty();
	}

	@Override
	public void requestFocus(Widget w) {
		Widget wasFocused = focused;
		focused = w;
		if (wasFocused != null) {
			try (DefaultEvent e = DefaultEvent.focusLost()) {
				wasFocused.processEvent(e);
			}
		}
		if (isFocused() && getParent() != null && w == null) getParent().requestFocus(null);
		if (!isFocused() && w != null) getParent().requestFocus(this);
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
		requestFocus(newFocused);
		if (newFocused instanceof Container) {
			while (!((Container)newFocused).focusAtEnd()) {
				((Container)newFocused).focusPrevious();
			}
		}
	}

	@Override
	public boolean focusAtStart() {
		if (!isFocusable()) return true;
		if (focused instanceof Container && !((Container) focused).focusAtStart()) return false;
		return focusableChildren.indexOf(focused) == 0;
	}

	@Override
	public boolean focusAtEnd() {
		if (!isFocusable()) return true;
		if (focused instanceof Container && !((Container) focused).focusAtEnd()) return false;
		return focusableChildren.indexOf(focused) == focusableChildren.size()-1;
	}

}

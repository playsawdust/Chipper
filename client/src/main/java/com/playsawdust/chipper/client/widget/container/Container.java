/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.widget.container;

import java.util.Collections;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.UnmodifiableIterator;
import com.google.common.graph.Traverser;
import com.playsawdust.chipper.client.EventProcessor;
import com.playsawdust.chipper.client.input.KeyModifiers;
import com.playsawdust.chipper.client.input.Key;
import com.playsawdust.chipper.client.qual.AffectsLayout;
import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.client.qual.RequiresLayout;
import com.playsawdust.chipper.client.widget.EventResponse;
import com.playsawdust.chipper.client.widget.Widget;
import com.playsawdust.chipper.collect.unmodifiable.Unmodifiable;
import com.playsawdust.chipper.collect.unmodifiable.UnmodifiableIterable;
import com.playsawdust.chipper.collect.unmodifiable.UnmodifiableList;
import com.playsawdust.chipper.math.Point2D;
import com.playsawdust.chipper.math.RectI;

/**
 * A {@link Widget} that can contain other {@code Widget}s.
 * <p>
 * Add methods are not defined by this class, as concrete container
 * implementations almost always have specific needs and would prefer to have
 * add methods with more arguments than just a widget. Implementations of
 * add methods <i>must</i> call {@link Widget#setParent}.
 * <p>
 * Currently, the only concrete implementation offered by Chipper itself is
 * {@link SimpleBox}.
 */
public interface Container extends Widget, Iterable<Widget> {

	/**
	 * Remove the given widget from this container.
	 * <p>
	 * Note: Implementors of this method must call {@link Widget#setParent} on
	 * any added widgets with a null parent to relinquish ownership, after
	 * verifying the widget is in fact a child of this container.
	 * @param widget the widget to remove
	 */
	@AffectsLayout
	void remove(Widget widget);

	/**
	 * Remove the given widgets from this container.
	 * @param widget the widgets to remove
	 */
	@AffectsLayout
	default void remove(Widget... widgets) {
		for (Widget w : widgets) remove(w);
	}

	/**
	 * Remove the given widgets from this container.
	 * @param widget the widgets to remove
	 */
	@AffectsLayout
	default void remove(Iterable<Widget> widgets) {
		for (Widget w : widgets) remove(w);
	}

	/**
	 * Remove every widget from this container.
	 */
	void clear();



	/**
	 * Returns an iterator over all this container's children, in no particular
	 * order.
	 */
	@Override
	UnmodifiableIterator<Widget> iterator();

	/**
	 * @return the number of children in this container
	 */
	int size();


	/**
	 * Returns an iterator over all this container's children, and its children's
	 * children, etc, in no particular order.
	 */
	default UnmodifiableIterable<Widget> recurse() {
		return Unmodifiable.iterable(Traverser.forTree((Widget w) -> {
			if (w instanceof Container) return ((Container)w);
			return Collections.emptySet();
		}).depthFirstPreOrder((Iterable<Widget>)this));
	}

	/**
	 * @return the child that currently has the focus, or null
	 */
	@Nullable Widget getFocusedWidget();

	/**
	 * Grant focus to the given widget. Called when a focusable widget, such as a text field, is
	 * clicked, or with null when this container loses focus.
	 * @param w the widget to focus, or null to remove the focus
	 * @throws IllegalArgumentException if the given widget is not a child of this container
	 */
	void requestFocus(@Nullable Widget w);

	/**
	 * Move the focus one element forward in the focus order.
	 */
	void focusNext();

	/**
	 * Move the focus one element backward in the focus order.
	 */
	void focusPrevious();

	/**
	 * Used as a hint to escape from one container and move on to the next when moving backward.
	 * @return {@code true} if this container's focused widget is the first in its focus order
	 */
	boolean focusAtStart();

	/**
	 * Used as a hint to escape from one container and move on to the next when moving forward.
	 * @return {@code true} if this container's focused widget is the last in its focus order
	 */
	boolean focusAtEnd();

	@Override
	default boolean isFocusable() {
		for (Widget w : this) {
			if (w.isFocusable()) return true;
		}
		return false;
	}

	@Override
	default EventResponse onFocusGained() {
		if (getFocusedWidget() == null) {
			// when we gain the focus, immediately pass it to our first focusable child, so we don't
			// force the user to tab into containers and then their children
			focusNext();
			return EventResponse.ACCEPT;
		}
		return EventResponse.PASS;
	}

	@Override
	default EventResponse onFocusLost() {
		requestFocus(null);
		return EventResponse.ACCEPT;
	}

	// note: the Chipper layout system is designed to be completely opaque, to
	// allow flexibility in Container implementations. as such, methods that
	// perform the exact desired action rather than expose internal layout data
	// like as coordinates and sizes are provided. this can be counter-intuitive
	// if you're coming from other widget systems.
	// (this opacity is also why there is no AbstractContainer)

	/**
	 * Returns a list of all <i>immediate</i> children intersecting the given
	 * point, relative to this container's top-left, in Z order, with the topmost
	 * widget first.
	 * <p>
	 * Generally, this method should return a list of size one, as overlapping
	 * widgets are usually indicative of a bug in the container's layout
	 * implementation. However, there is no good reason to prevent this behavior
	 * for containers, so we don't.
	 * @param x the x coordinate to check, relative to this container's left
	 * @param y the y coordinate to check, relative to this container's top
	 */
	@RequiresLayout
	UnmodifiableList<Widget> hit(double x, double y);

	/**
	 * Returns {@code true} if the given coordinates intersect with the given
	 * widget. Should be identical to {@code hit(x, y).contains(w)} - this
	 * method avoids allocating a return list.
	 * @param x the x coordinate to check, relative to this container's left
	 * @param y the y coordinate to check, relative to this container's top
	 * @param w the widget to check for
	 * @return {@code true} if the given coordinates intersect with the given
	 * 		widget
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	@RequiresLayout
	boolean wouldHit(double x, double y, Widget w) throws IllegalArgumentException;

	/**
	 * Adjust the given point in this container's coordinate space to be within
	 * the given child's coordinate space.
	 * @param w the widget whose coordinate space is to be adjusted to
	 * @param point the point to adjust (will be mutated)
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	@RequiresLayout
	void adjust(Widget w, Point2D point) throws IllegalArgumentException;

	/**
	 * Adjust the given point from the given child's coordinate space to be
	 * within this container's coordinate space.
	 * @param w the widget whose coordinate space is to be adjusted from
	 * @param point the point to adjust (will be mutated)
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	@RequiresLayout
	void unadjust(Widget w, Point2D point) throws IllegalArgumentException;

	/**
	 * Get the size of the given child into the given rectangle.
	 * @param w the widget whose size is to be retrieved
	 * @param rect the rect to put the size into
	 * @throws IllegalArgumentException if the given widget is not a child of
	 * 		this container
	 */
	@RequiresLayout
	void getSize(Widget w, RectI rect) throws IllegalArgumentException;

	@Override
	default EventResponse processEvent(EventProcessor.Event event) {
		try {
			event.enterContainer(this);
			for (Widget w : this) {
				if (event.isRelevant(w)) {
					if (w.processEvent(event) == EventResponse.ACCEPT) {
						return EventResponse.ACCEPT;
					}
				}
			}
		} finally {
			event.exitContainer(this);
		}
		return event.visit(this);
	}

	@Override
	default EventResponse onKeyRepeat(Key key, int scancode, KeyModifiers mod) {
		if (key == Key.TAB && getParent() == null) {
			if (mod.isShiftHeld()) {
				focusPrevious();
			} else {
				focusNext();
			}
			return EventResponse.ACCEPT;
		}
		return EventResponse.PASS;
	}

	@Override
	default EventResponse onKeyUp(Key key, int scancode, KeyModifiers mod) {
		if (key == Key.TAB && getParent() == null) {
			if (mod.isShiftHeld()) {
				focusPrevious();
			} else {
				focusNext();
			}
			return EventResponse.ACCEPT;
		}
		return EventResponse.PASS;
	}

	@Override
	default EventResponse onClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		requestFocus(null);
		return EventResponse.PASS;
	}

	static boolean defaultFocusNextBehavior(Container container) {
		Widget focused = container.getFocusedWidget();
		if (focused instanceof Container) {
			Container c = (Container)focused;
			if (!c.focusAtEnd()) {
				c.focusNext();
				return true;
			}
		}
		return false;
	}

	static boolean defaultFocusPreviousBehavior(Container container) {
		Widget focused = container.getFocusedWidget();
		if (focused instanceof Container) {
			Container c = (Container)focused;
			if (!c.focusAtStart()) {
				c.focusPrevious();
				return true;
			}
		}
		return false;
	}

}

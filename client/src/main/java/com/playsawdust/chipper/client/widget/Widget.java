/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.widget;

import java.util.List;

import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.component.Canvas;
import com.playsawdust.chipper.client.component.LayerController;
import com.playsawdust.chipper.client.input.KeyModifiers;
import com.playsawdust.chipper.client.input.CursorType;
import com.playsawdust.chipper.client.input.InputEventProcessor;
import com.playsawdust.chipper.client.input.Key;
import com.playsawdust.chipper.client.qual.AffectsLayout;
import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.client.qual.RequiresLayout;
import com.playsawdust.chipper.client.widget.container.Container;
import com.playsawdust.chipper.client.widget.container.SplinterBox;

import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.math.RectD;

/**
 * The base interface for any sort of 2D object that wants to be displayed in
 * the GUI. Generally, these are buttons in menus, indicators on the HUD,
 * etc.
 * <p>
 * Widgets cannot themselves contain children; {@link Container} is a
 * subinterface of Widget that offers this functionality, though the class you
 * probably want to use is {@link SplinterBox}, which implements layout for its
 * children.
 * <p>
 * If you're making a widget, you will want to extend {@link AbstractWidget}
 * instead of implementing this interface directly. It offers convenient
 * pre-rolled implementations of most methods in this interface.
 * <p>
 * Splitting classes into interfaces and abstract classes allows keeping lengthy
 * documentation out of implementation definitions, and allows skimming method
 * definitions and documentation without wading through implementation details.
 */
@UIType
public interface Widget extends InputEventProcessor {

	/**
	 * Semantic alias for Integer.MAX_VALUE in {@link #getMaxWidth} and
	 * {@link #getMaxHeight}. Indicates this widget has no desired maximum size
	 * and can be made as large as needed.
	 */
	@CanvasPixels int UNLIMITED = Integer.MAX_VALUE;
	/**
	 * Semantic alias for Integer.MAX_VALUE in {@link #getMinWidth} and
	 * {@link #getMinHeight}. Indicates this widget should use as much space as
	 * possible, <i>fill</i>ing its parent.
	 */
	@CanvasPixels int FILL = Integer.MAX_VALUE;


	/**
	 * Draw this widget's content, with the given width and height as calculated
	 * by a parent's layout engine. Attempts to draw outside of this area may be
	 * clipped.
	 * @param ctx the engine context
	 * @param width the current width of this widget
	 * @param height the current height of this widget
	 * @param canvas the canvas to draw to
	 */
	@RequiresLayout
	void draw(Context<ClientEngine> ctx, @CanvasPixels int width, @CanvasPixels int height, Canvas canvas);



	/**
	 * @return {@code true} if a property of this widget has changed that
	 * 		affects layout
	 */
	boolean needsLayout();
	/**
	 * Mark this widget as needing to be re-laid-out by its parent.
	 * <p>
	 * Note: Implementations must call {@code getParent().setNeedsLayout()} to
	 * propagate layout changes up the hierarchy. Remember to check if the
	 * parent is null.
	 */
	void setNeedsLayout();
	/**
	 * Mark this widget as not needing to be re-laid-out by its parent.
	 */
	void clearNeedsLayout();
	/**
	 * Perform pre-layout on this widget. Called before {@link #layout}
	 * before the parent has decided where its children are going. This is your
	 * chance to update your minimum/maximum width/height, or anything else you
	 * can do without knowing your assigned size.
	 * <p>
	 * You must not call {@link #clearNeedsLayout} in this method.
	 */
	void preLayout();
	/**
	 * Perform layout on this widget. Called before {@link #draw} if
	 * {@link #needsLayout} is true, or when the parent is performing layout, to
	 * give this widget a chance to lay out its contents.
	 * <p>
	 * If this widget is a container, you must call {@link #preLayout} on all
	 * children that need layout <i>before</i> doing any layout computation,
	 * followed by {@code layout} during your layout computation to inform
	 * children of their assigned size.
	 * <p>
	 * You must call {@link #clearNeedsLayout} when finished to prevent this
	 * method being called every frame.
	 */
	void layout(int width, int height);



	/**
	 * @return the parent of this widget, if any
	 */
	@Nullable Container getParent();
	/**
	 * Set the parent of this widget. If null, this widget becomes orphaned.
	 * <p>
	 * Should only be called by Container implementations.
	 */
	@AffectsLayout
	void setParent(@Nullable Container parent);
	/**
	 * Removes this widget from its parent, if it has one.
	 */
	default void removeFromParent() {
		Container p = getParent();
		if (p != null) p.remove(this);
	}

	/**
	 * Retrieves the <i>natural</i> width of this widget; i.e. the size of its
	 * content. If this widget contains no content, this method should return 0.
	 * For example, a text label would return the width of its text in the
	 * chosen font. A container should return the sum of each of its childrens'
	 * minimum or natural width, whichever is greater.
	 * <p>
	 * The natural size must be independent from the minimum or maximum width
	 * and height of <i>this</i> widget; it is a separate third parameter used
	 * to affect layout. (so, do not override this method to just return
	 * {@code getMinimumWidth})
	 * <p>
	 * Generally, this method calculates the natural width on-the-fly. If
	 * computing the natural size of this component is expensive, or the
	 * natural width and height are inherently tied together, you are
	 * encouraged to cache the natural size, and only update it when the
	 * widget's contents change.
	 * @return the natural width of this widget
	 */
	@CanvasPixels int getNaturalWidth();
	/**
	 * Retrieves the <i>natural</i> height of this widget; i.e. the size of its
	 * content. If this widget contains no content, this method should return 0.
	 * For example, a text label would return the height of its text in the
	 * chosen font. A container should return the sum of each of its childrens'
	 * minimum or natural height, whichever is greater.
	 * <p>
	 * The natural size must be independent from the minimum or maximum width
	 * and height of <i>this</i> widget; it is a separate third parameter used
	 * to affect layout. (so, do not override this method to just return
	 * {@code getMinimumHeight})
	 * <p>
	 * Generally, this method calculates the natural height on-the-fly. If
	 * computing the natural size of this component is expensive, or the
	 * natural width and height are inherently tied together, you are
	 * encouraged to cache the natural size, and only update it when the
	 * widget's contents change.
	 * @return the natural height of this widget
	 */
	@CanvasPixels int getNaturalHeight();



	/**
	 * Convenience method. Calls {@link #setWidth} and {@link #setHeight}.
	 */
	@AffectsLayout
	default void setSize(@CanvasPixels int width, @CanvasPixels int height) {
		setWidth(width);
		setHeight(height);
	}

	/**
	 * Convenience method. Calls {@link #setMinWidth} and {@link #setMaxWidth}.
	 * <p>
	 * Note: There is no {@code getWidth} because the actual size of a widget
	 * is determined by its parent dynamically, taking into account its minimum
	 * and maximum sizes, and it is only guaranteed to be known at the time of
	 * rendering.
	 */
	@AffectsLayout
	default void setWidth(@CanvasPixels int width) {
		setMinWidth(width);
		setMaxWidth(width);
	}

	/**
	 * Convenience method. Calls {@link #setMinHeight} and {@link #setMaxHeight}.
	 * <p>
	 * Note: There is no {@code getHeight} because the actual size of a widget
	 * is determined by its parent dynamically, taking into account its minimum
	 * and maximum sizes, and it is only guaranteed to be known at the time of
	 * rendering.
	 */
	@AffectsLayout
	default void setHeight(@CanvasPixels int height) {
		setMinHeight(height);
		setMaxHeight(height);
	}



	/**
	 * Convenience method. Calls {@link #setMinWidth} and {@link #setMinHeight}.
	 */
	@AffectsLayout
	default void setMinSize(@CanvasPixels int minWidth, @CanvasPixels int minHeight) {
		setMinWidth(minWidth);
		setMinHeight(minHeight);
	}

	/**
	 * Set the <em>desired</em> minimum width of this widget.
	 * @see #getMinWidth
	 * @param minWidth the minimum width, or {@link #FILL} to use as much space
	 * 		as possible
	 */
	@AffectsLayout
	void setMinWidth(@CanvasPixels int minWidth);

	/**
	 * Set the <em>desired</em> minimum height of this widget.
	 * @see #getMinHeight
	 * @param minHeight the minimum height, or {@link #FILL} to use as much space
	 * 		as possible
	 */
	@AffectsLayout
	void setMinHeight(@CanvasPixels int minHeight);

	/**
	 * Get the <em>desired</em> minimum width of this widget. Attempts will
	 * be made to ensure this widget is at least this wide, but under some
	 * circumstances, that may not be possible (such as if the minimum width
	 * is wider than this widget's parent, or the entire window)
	 * <p>
	 * If this widget should use as much space as possible, return {@link #FILL}.
	 * @return the <em>desired</em> minimum width of this widget
	 */
	@CanvasPixels int getMinWidth();

	/**
	 * Get the <em>desired</em> minimum height of this widget. Attempts will
	 * be made to ensure this widget is at least this tall, but under some
	 * circumstances, that may not be possible (such as if the minimum height
	 * is taller than this widget's parent, or the entire window)
	 * <p>
	 * If this widget should use as much space as possible, return {@link #FILL}.
	 * @return the <em>desired</em> minimum height of this widget
	 */
	@CanvasPixels int getMinHeight();



	/**
	 * Convenience method. Calls {@link #setMaxWidth} and {@link #setMaxHeight}.
	 */
	@AffectsLayout
	default void setMaxSize(@CanvasPixels int maxWidth, @CanvasPixels int maxHeight) {
		setMaxWidth(maxWidth);
		setMaxHeight(maxHeight);
	}

	/**
	 * Set the maximum width of this widget.
	 * @see #getMaxWidth
	 * @param maxWidth the maximum width, or {@link #UNLIMITED} to impose no
	 * 		limit
	 */
	@AffectsLayout
	void setMaxWidth(@CanvasPixels int maxWidth);

	/**
	 * Set the maximum height of this widget.
	 * @see #getMaxWidth
	 * @param maxWidth the maximum width, or {@link #UNLIMITED} to impose no
	 * 		limit
	 */
	@AffectsLayout
	void setMaxHeight(@CanvasPixels int maxHeight);

	/**
	 * Get the maximum width of this widget. Under normal circumstances, this
	 * widget will be no wider.
	 * <p>
	 * If this widget does not care how wide it is, return {@link #UNLIMITED}.
	 * @return the maximum width of this widget
	 */
	@CanvasPixels int getMaxWidth();

	/**
	 * Get the maximum height of this widget. Under normal circumstances, this
	 * widget will be no taller.
	 * <p>
	 * If this widget does not care how tall it is, return {@link #UNLIMITED}.
	 * @return the maximum height of this widget
	 */
	@CanvasPixels int getMaxHeight();

	/**
	 * Check if this Widget is opaque, meaning it covers every pixel of its
	 * given area in some opaque color. This is used to skip rendering layers
	 * that are occluded, or the root Renderable.
	 * @return {@code true} if this Widget is opaque
	 */
	@RequiresLayout
	boolean isOpaque();

	/**
	 * Check if this Widget can accept the input focus.
	 * @return {@code true} if this widget can be focused
	 */
	boolean isFocusable();

	/**
	 * Check if this Widget has the input focus. Convenience method.
	 */
	default boolean isFocused() {
		return getParent() == null || getParent().getFocusedWidget() == this;
	}


	/**
	 * Return the type of cursor to be used while this widget is hovered,
	 * or null to not affect the cursor.
	 * <p>
	 * If this Widget is the root of a {@link LayerController layer}, then
	 * this method behaves slightly differently. It is called every frame,
	 * and gives this Widget a chance to override the decision of any of its
	 * children regarding the cursor. Returning null respects the decision of
	 * the currently hovered widget (if any) - returning a non-null value
	 * forces that cursor type to be used.
	 *
	 * @param x the x coordinate of the mouse over this widget, relative to
	 * 		this widget's top-left corner
	 * @param y the y coordinate of the mouse over this widget, relative to
	 * 		this widget's top-left corner
	 * @return the type of cursor to be used when hovering this widget, or null
	 * 		to not affect the cursor
	 */
	default @Nullable CursorType getCursorType(double x, double y) {
		return null;
	}

	/**
	 * Get the list of <i>frost regions</i> for this widget - that is, the
	 * areas of the screen that should be blurred before this widget is rendered.
	 * The frost regions of all visible layers will be merged together and used
	 * to decide what parts of the screen to blur. (i.e. if two layers report
	 * overlapping blur regions, the overlapping part will still only be blurred
	 * once, not twice)
	 * <p>
	 * <b>Only called if this widget is the root of a {@link LayerController layer}.</b>
	 * The default implementation of this method adds one region to the list
	 * covering the entire screen, <i>if bottommost is true</i>, as this is
	 * generally the desired behavior. Layers that are not bottommost are
	 * usually separate components of a main UI on the bottommost layer, so it
	 * is generally wise in this case to delegate to the bottommost layer's
	 * frost regions. If you do not desire this behavior, override this method.
	 * <p>
	 * <i>The RectDs added to the list will be recycled for you when they are
	 * finished being used.</i> Do not re-use RectD instances or recycle them
	 * yourself - always call the RectD factory methods.
	 * @param width the width of the window, and therefore this widget
	 * @param height the height of the window, and therefore this widget
	 * @param regions the list to add all regions to
	 * @param bottommost {@code true} if this widget is the bottommost layer
	 */
	default void getFrostRegions(double width, double height, List<RectD> regions, boolean bottommost) {
		if (bottommost) regions.add(RectD.fromSize(0, 0, width, height));
	}


	/**
	 * Single point of dispatch for all the on* methods, to allow widgets to
	 * easily delegate all events to children without having to override all
	 * event methods and keep up with any new events that may be added. You
	 * generally shouldn't override this method.
	 * <p>
	 * <b>Note</b>: A few kinds of events do not go through this method, usually ones
	 * that are synthesized in response to other events. For example, enter
	 * events are synthesized as a response to a move event within the event
	 * processor, and will call {@code onEnter} without going through
	 * {@code processEvent}.
	 * @param event the event to process
	 * @return this widget's response to this event
	 */
	@Override
	default EventResponse processEvent(Event event) {
		return event.visit(this);
	}


	/**
	 * Called when a mouse button is pressed over this widget. X and Y are relative
	 * to your widget's top left corner. Generally, it is preferable to use one of
	 * the more specific {@code click} events than MouseDown/MouseUp.
	 * <p>
	 * The common mouse buttons are:
	 * <ol start="0">
	 * <li>Left</li>
	 * <li>Right</li>
	 * <li>Middle</li>
	 * <li>Back</li>
	 * <li>Forward</li>
	 * </ol>
	 * Higher buttons are possible, but uncommon. It's generally only reasonable to
	 * expect the left and right buttons to exist.
	 * @param button the pressed mouse button
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param bucky a Bucky object representing the held modifier keys (aka "bucky bits")
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 * @see #onClick
	 * @see #onAlternateClick
	 * @see #onBack
	 * @see #onForward
	 */
	@Override
	@RequiresLayout
	default EventResponse onMouseDown(int button, @CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky) {
		return EventResponse.PASS;
	}

	/**
	 * Called when a mouse button is released over this widget. X and Y are relative
	 * to your widget's top left corner. Generally, it is preferable to use one of
	 * the more specific {@code click} events than MouseDown/MouseUp.
	 * <p>
	 * <b>Note</b>: This event <i>will not be fired</i> if the mouse is released outside
	 * of this widget's area. You will likely want to move your mouse-up logic into a
	 * separate method, and call it from {@link #onLeave} as well.
	 * <p>
	 * The common mouse buttons are:
	 * <ol start="0">
	 * <li>Left</li>
	 * <li>Right</li>
	 * <li>Middle</li>
	 * <li>Back</li>
	 * <li>Forward</li>
	 * </ol>
	 * Higher buttons are possible, but uncommon. It's generally only reasonable to
	 * expect the left and right buttons to exist.
	 * @param button the pressed mouse button
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param bucky a Bucky object representing the held modifier keys (aka "bucky bits")
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 * @see #onClick
	 * @see #onAlternateClick
	 * @see #onBack
	 * @see #onForward
	 */
	@Override
	@RequiresLayout
	default EventResponse onMouseUp(int button, @CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky) {
		return EventResponse.PASS;
	}

	/**
	 * Called when this widget is tapped or clicked. X and Y are relative to your
	 * widget's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param bucky a Bucky object representing the held modifier keys (aka "bucky bits")
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky) {
		return EventResponse.PASS;
	}

	/**
	 * Called when this widget is right-clicked, ctrl-clicked, or long-pressed.
	 * X and Y are relative to your widget's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param bucky a Bucky object representing the held modifier keys (aka "bucky bits")
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onAlternateClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the scroll wheel is moved while the mouse is over this widget.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param xscroll how many notches horizontally the scroll wheel was moved - negative is left,
	 * 		probably? I don't have a mouse that supports horizontal scroll
	 * @param yscroll how many notches vertically the scroll wheel was moved - negative is down
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onScroll(@CanvasPixels double x, @CanvasPixels double y, double xscroll, double yscroll) {
		return EventResponse.PASS;
	}

	/**
	 * Called when this widget is clicked with the back mouse thumb button, or
	 * an unspecified "back" gesture is performed.
	 * X and Y are relative to your widget's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param bucky a Bucky object representing the held modifier keys (aka "bucky bits")
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onBack(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky) {
		return EventResponse.PASS;
	}

	/**
	 * Called when this widget is clicked with the front mouse thumb button, or
	 * an unspecified "forward" gesture is performed.
	 * X and Y are relative to your widget's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onForward(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the mouse enters this widget's area. X and Y are relative to
	 * your widget's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onEnter(@CanvasPixels double x, @CanvasPixels double y) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the mouse moves within this widget's area. X and Y are
	 * relative to your widget's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onMove(@CanvasPixels double x, @CanvasPixels double y) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the mouse leaves this widget's area. X and Y are relative to
	 * your widget's top left corner. <b>Note</b>: to ensure this method is
	 * always called when the mouse leaves this widget's area, even if the mouse
	 * is moving extremely quickly or the widget has left the hierarchy, this
	 * method <i>does not</i> go through the usual event system. Also note that
	 * if this widget has been removed from the hierarchy, the X and Y arguments
	 * will be relative to the top left of the window, as your widget no longer
	 * has a coordinate space.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onLeave(@CanvasPixels double x, @CanvasPixels double y) {
		return EventResponse.PASS;
	}

	/**
	 * Called when this widget has the focus and a key is pressed.
	 * @param key the abstract representation of the key
	 * @param scancode the raw platform-specific ephemeral scancode, for if key is {@code UNKNOWN}
	 * @param bucky a Bucky object representing the held modifier keys (aka "bucky bits")
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onKeyDown(Key key, int scancode, KeyModifiers bucky) {
		return EventResponse.PASS;
	}

	/**
	 * Called when this widget has the focus and a key is released.
	 * @param key the abstract representation of the key
	 * @param scancode the raw platform-specific ephemeral scancode, for if key is {@code UNKNOWN}
	 * @param bucky a Bucky object representing the held modifier keys (aka "bucky bits")
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onKeyUp(Key key, int scancode, KeyModifiers bucky) {
		return EventResponse.PASS;
	}

	/**
	 * Called when this widget has the focus and a key has been held down long enough for it to
	 * repeat. How long this takes, and how frequently the repeat event is fired, is platform
	 * dependent and often configurable by the user.
	 * @param key the abstract representation of the key
	 * @param scancode the raw platform-specific ephemeral scancode, for if key is {@code UNKNOWN}
	 * @param bucky a Bucky object representing the held modifier keys (aka "bucky bits")
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onKeyRepeat(Key key, int scancode, KeyModifiers bucky) {
		return EventResponse.PASS;
	}

	/**
	 * Called when a key is pressed, after that key has been converted into a Unicode codepoint.
	 * Will be called repeatedly after a platform-dependent delay at a platform-dependent frequency
	 * if the key is held down.
	 * @param codepoint the Unicode codepoint of the key that was pressed
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 * @see Character#toChars(int)
	 * @see StringBuilder#appendCodePoint(int)
	 */
	@Override
	default EventResponse onTextEntered(int codepoint) {
		return EventResponse.PASS;
	}

	/**
	 * Called when this Widget gains the focus.
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onFocusGained() {
		return EventResponse.PASS;
	}

	/**
	 * Called when this Widget loses the input focus.
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onFocusLost() {
		return EventResponse.PASS;
	}

}

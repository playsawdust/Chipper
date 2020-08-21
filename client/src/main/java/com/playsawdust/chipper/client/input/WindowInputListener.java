/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.input;

import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.client.qual.RequiresLayout;
import com.playsawdust.chipper.client.widget.EventResponse;

/**
 * An input listener outside of the widget system, listening to the window itself.
 */
public interface WindowInputListener extends InputEventProcessor {

	/**
	 * Single point of dispatch for all the on* methods, to allow easy event delegation without
	 * having to override all event methods and keep up with any new events that may be added. You
	 * generally shouldn't override this method.
	 * <p>
	 * <b>Note</b>: A few kinds of events do not go through this method, usually ones that are
	 * synthesized in response to other events.
	 * @param event the event to process
	 * @return this processor's response to this event
	 */
	@Override
	default EventResponse processEvent(Event event) {
		return event.visit(this);
	}


	/**
	 * Called when a mouse button is pressed on the window. X and Y are relative
	 * to the window's top left corner. Generally, it is preferable to use one of
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
	 * @param mod a KeyModifiers object representing the held modifier keys
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 * @see #onClick
	 * @see #onAlternateClick
	 * @see #onBack
	 * @see #onForward
	 */
	@Override
	@RequiresLayout
	default EventResponse onMouseDown(int button, @CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		return EventResponse.PASS;
	}

	/**
	 * Called when a mouse button is released over the window. X and Y are relative
	 * to the window's top left corner. Generally, it is preferable to use one of
	 * the more specific {@code click} events than MouseDown/MouseUp.
	 * <p>
	 * <b>Note</b>: This event <i>will not be fired</i> if the mouse is released outside
	 * of the window's area. You will likely want to move your mouse-up logic into a
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
	 * @param mod a KeyModifiers object representing the held modifier keys
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 * @see #onClick
	 * @see #onAlternateClick
	 * @see #onBack
	 * @see #onForward
	 */
	@Override
	@RequiresLayout
	default EventResponse onMouseUp(int button, @CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the window is tapped or clicked. X and Y are relative to the
	 * window's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param mod a KeyModifiers object representing the held modifier keys
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the window is right-clicked, ctrl-clicked, or long-pressed.
	 * X and Y are relative to the window's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param mod a KeyModifiers object representing the held modifier keys
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onAlternateClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the scroll wheel is moved while the mouse is over the window.
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
	 * Called when the window is clicked with the back mouse thumb button.
	 * X and Y are relative to the window's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param mod a KeyModifiers object representing the held modifier keys
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onBack(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the window is clicked with the front mouse thumb button.
	 * X and Y are relative to the window's top left corner.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	@RequiresLayout
	default EventResponse onForward(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers mod) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the mouse enters the window's area. X and Y are relative to
	 * the window's top left corner.
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
	 * Called when the mouse moves within the window's area. X and Y are
	 * relative to the window's top left corner.
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
	 * Called when the mouse leaves the window's area. X and Y are relative to
	 * the window's top left corner.
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
	 * Called when the window has the focus and a key is pressed.
	 * @param key the abstract representation of the key
	 * @param scancode the raw platform-specific ephemeral scancode, for if key is {@code UNKNOWN}
	 * @param mod a KeyModifiers object representing the held modifier keys
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onKeyDown(Key key, int scancode, KeyModifiers mod) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the window has the focus and a key is released.
	 * @param key the abstract representation of the key
	 * @param scancode the raw platform-specific ephemeral scancode, for if key is {@code UNKNOWN}
	 * @param mod a KeyModifiers object representing the held modifier keys
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onKeyUp(Key key, int scancode, KeyModifiers mod) {
		return EventResponse.PASS;
	}

	/**
	 * Called when the window has the focus and a key has been held down long enough for it to
	 * repeat. How long this takes, and how frequently the repeat event is fired, is platform
	 * dependent and often configurable by the user.
	 * @param key the abstract representation of the key
	 * @param scancode the raw platform-specific ephemeral scancode, for if key is {@code UNKNOWN}
	 * @param mod a KeyModifiers object representing the held modifier keys
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onKeyRepeat(Key key, int scancode, KeyModifiers mod) {
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
	 * Called when the window gains the focus.
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onFocusGained() {
		return EventResponse.PASS;
	}

	/**
	 * Called when the window loses focus.
	 * @return your response to this event, either {@link EventResponse#PASS PASS} or {@link EventResponse#ACCEPT ACCEPT}
	 */
	@Override
	default EventResponse onFocusLost() {
		return EventResponse.PASS;
	}

}

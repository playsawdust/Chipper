/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.input;

import com.playsawdust.chipper.client.EventProcessor;
import com.playsawdust.chipper.client.qual.CanvasPixels;
import com.playsawdust.chipper.client.widget.EventResponse;
import com.playsawdust.chipper.client.widget.Widget;

/**
 * Common interface between {@link Widget} and {@link WindowInputListener}. Exists solely for convenience
 * when performing event dispatch; no documentation is supplied and as such no guarantees are made.
 */
public interface InputEventProcessor extends EventProcessor {

	EventResponse onMouseDown(int button, @CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky);
	EventResponse onMouseUp(int button, @CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky);
	EventResponse onClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky);
	EventResponse onAlternateClick(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky);
	EventResponse onScroll(@CanvasPixels double x, @CanvasPixels double y, double xscroll, double yscroll);
	EventResponse onBack(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky);
	EventResponse onForward(@CanvasPixels double x, @CanvasPixels double y, KeyModifiers bucky);
	EventResponse onEnter(@CanvasPixels double x, @CanvasPixels double y);
	EventResponse onMove(@CanvasPixels double x, @CanvasPixels double y);
	EventResponse onLeave(@CanvasPixels double x, @CanvasPixels double y);
	EventResponse onKeyDown(Key key, int scancode, KeyModifiers bucky);
	EventResponse onKeyUp(Key key, int scancode, KeyModifiers bucky);
	EventResponse onKeyRepeat(Key key, int scancode, KeyModifiers bucky);
	EventResponse onTextEntered(int codepoint);
	EventResponse onFocusGained();
	EventResponse onFocusLost();

}

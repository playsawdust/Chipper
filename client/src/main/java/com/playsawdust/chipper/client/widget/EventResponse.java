/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.widget;

public enum EventResponse {
	/**
	 * Consume the event. This is usually what you want if your widget
	 * processes events such as clicks. It will not be passed to any
	 * more widgets, including this widget's parent.
	 */
	ACCEPT,
	/**
	 * Pass this event to this widget's children, if it has any, and
	 * then any overlapping widgets behind this one, if any, and then its parent.
	 */
	PASS,
	;

	public boolean isPass() {
		return this == PASS;
	}
}

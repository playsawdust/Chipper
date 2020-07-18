/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.img;

public interface Image {
	/** Gets the width in pixels that this Image can be displayed at without rescaling. */
	int getWidth();
	
	/** Gets the height in pixels that this Image can be displayed at without rescaling. */
	int getHeight();
	
	/** Gets this pixel as sRGB color in ARGB order. The color will be converted from this Image's underlying pixel format. */
	int getPixel(int x, int y);
	
	/** Sets this pixel to an sRGB value specified in ARGB order. The color will be converted into this Image's underlying pixel format. */
	void setPixel(int x, int y, int argb);
}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import com.google.common.base.CharMatcher;

/**
 * Private-use-area Unicode characters with special meaning in Chipper.
 * <p>
 * We use the final supplementary PUA plane, to attempt to avoid conflict with others. As such,
 * <i>all characters defined here are "astral plane" characters</i>, and require 2 chars to
 * represent. They are offered as UTF-32 codepoints and Strings. <b>Do not attempt to use any
 * of these characters as {@code char}s</b> - they will not fit in a char.
 * <p>
 * Chipper's text processing is designed to gracefully handle any Unicode characters, including
 * "astral plane" characters, but custom text processing will need to be designed with this in mind.
 */
public class PUAChars {

	// chosen by fair dice roll.
	// guaranteed to be random.
	private static final int BEGINNING = 0x106E00;


	private static final String str(int codepoint) {
		return new String(Character.toChars(codepoint));
	}

	/**
	 * A slim zero for use with the Binary number base.
	 */
	public static final int SMALL_ZERO = BEGINNING;
	/**
	 * A slim zero for use with the Binary number base, pre-converted to a String.
	 */
	public static final String SMALL_ZERO_STR = str(SMALL_ZERO);


	/**
	 * A slim one for use with the Binary number base.
	 */
	public static final int SMALL_ONE = BEGINNING+1;
	/**
	 * A slim one for use with the Binary number base, pre-converted to a String.
	 */
	public static final String SMALL_ONE_STR = str(SMALL_ONE);


	/**
	 * The Dwiggins "dek" digit for Dozenal. Pitman digits got added to Unicode, but Dwiggins digits
	 * never did.
	 */
	public static final int DWIGGINS_DEK = BEGINNING+2;
	/**
	 * The Dwiggins "dek" digit for Dozenal, pre-converted to a String. Pitman digits got added to
	 * Unicode, but Dwiggins digits never did.
	 */
	public static final String DWIGGINS_DEK_STR = str(DWIGGINS_DEK);


	/**
	 * The Dwiggins "el" digit for Dozenal. Pitman digits got added to Unicode, but Dwiggins digits
	 * never did.
	 */
	public static final int DWIGGINS_EL = BEGINNING+3;
	/**
	 * The Dwiggins "el" digit for Dozenal, pre-converted to a String. Pitman digits got added to
	 * Unicode, but Dwiggins digits never did.
	 */
	public static final String DWIGGINS_EL_STR = str(DWIGGINS_EL);


	/**
	 * A smushed "-1" ligature for use with balanced ternary.
	 */
	public static final int NEGATIVE_ONE = BEGINNING+4;
	/**
	 * A smushed "-1" ligature for use with balanced ternary, pre-converted to a String.
	 */
	public static final String NEGATIVE_ONE_STR = str(NEGATIVE_ONE);


	/**
	 * A smushed "+1" ligature for use with balanced ternary.
	 */
	public static final int POSITIVE_ONE = BEGINNING+5;
	/**
	 * A smushed "+1" ligature for use with balanced ternary, pre-converted to a String.
	 */
	public static final String POSITIVE_ONE_STR = str(POSITIVE_ONE);


	/**
	 * A box with an arrow pointing out of it. Used to indicate that an action will open an external
	 * web browser.
	 */
	public static final int EXTERNAL_LINK = BEGINNING+6;
	/**
	 * A box with an arrow pointing out of it, pre-converted to a String. Used to indicate that an
	 * action will open an external web browser.
	 */
	public static final String EXTERNAL_LINK_STR = str(EXTERNAL_LINK);


	/**
	 * A figure holding its hand out. Used to indicate that an action may result in information
	 * being shared with a third-party server.
	 */
	public static final int SHARING = BEGINNING+7;
	/**
	 * A figure holding its hand out, pre-converted to a String. Used to indicate that an action may
	 * result in information being shared with a third-party server.
	 */
	public static final String SHARING_STR = str(SHARING);


	/**
	 * A "networked computer" symbol, consisting of a rectangle with a tee below it indicating a
	 * network connection. Used to indicate that an action may result in information being sent to
	 * a first-party server.
	 */
	public static final int NETWORK = BEGINNING+8;
	/**
	 * A "networked computer" symbol, consisting of a rectangle with a tee below it indicating a
	 * network connection, pre-converted to a String. Used to indicate that an action may result in
	 * information being sent to a first-party server.
	 */
	public static final String NETWORK_STR = str(NETWORK);


	/**
	 * An invisible character to escape font renderer commands. Font renderer commands are indicated
	 * with invisible Unicode Tag characters following this character; to prevent rendering commands
	 * being interpreted, just this character can be stripped. The command itself will be preserved,
	 * but it will not render and will not affect the render state.
	 */
	public static final int FONT_RENDERER_CONTROL = BEGINNING+0xFF;
	/**
	 * An invisible character to escape font renderer commands, pre-converted to a String. Font
	 * renderer commands are indicated with invisible Unicode Tag characters following this
	 * character; to prevent rendering commands being interpreted, just this character can be
	 * stripped. The command itself will be preserved, but it will not render and will not affect
	 * the render state.
	 */
	public static final String FONT_RENDERER_CONTROL_STR = str(FONT_RENDERER_CONTROL);

	/**
	 * Generates a well-formed Chipper Font Renderer Control command.
	 * @param command the command, must be all-ASCII and not contain control codes
	 */
	public static String generateFontRendererCommand(String command) {
		if (!CharMatcher.ascii().matchesAllOf(command)) throw new IllegalArgumentException("Font renderer commands must be all-ASCII - \""+command+"\" is not");
		StringBuilder sb = new StringBuilder((command.length()*2)+4);
		sb.appendCodePoint(FONT_RENDERER_CONTROL);
		for (char c : command.toCharArray()) {
			if (c < 0x20 || c >= 0x7F) throw new IllegalArgumentException("Font renderer commands cannot contain control codes");
			sb.appendCodePoint(0xE0000+c);
		}
		sb.appendCodePoint(0xE007F);
		return sb.toString();
	}

}

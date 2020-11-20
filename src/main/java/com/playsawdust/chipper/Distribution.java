/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;

import com.playsawdust.chipper.math.ProtoColor;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;

public class Distribution {
	private static final Logger log = LoggerFactory.getLogger(Distribution.class);

	public static final String CHIPPER_VERSION = "0.1.7";

	public static final String ID;
	public static final String NAME;
	public static final String VERSION;
	public static final String AUTHOR;
	public static final ProtoColor LOADING_BACKGROUND_COLOR;
	public static final ProtoColor LOADING_ACCESSORY_COLOR;
	public static final String LOADING_FONT;
	public static final boolean PORTABLE;
	public static final int DEFAULT_PORT;
	public static final String SERVER_TERMINAL_ICON;
	public static final String SERVER_TERMINAL_FONT_TTF;
	public static final String DEFAULT_ADDON;

	// https://stackoverflow.com/a/31976060
	private static final ImmutableSet<String> ILLEGAL_WINDOWS_FILENAMES = ImmutableSet.of(
			"con", "prn", "aux", "nul",
			"com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
			"lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");
	private static final CharMatcher ILLEGAL_FILENAME_CHARACTERS = CharMatcher.inRange('\0', '\u001F').and(CharMatcher.anyOf("/<>:\"\\|?*"));

	static {
		try (InputStream is = ClassLoader.getSystemResourceAsStream("dist.jkson")) {
			if (is == null) {
				log.error("No dist.jkson found. A dist.jkson file must be provided in the root of the jar that Chipper is invoked from.");
				System.exit(255);
				throw new Error();
			} else {
				String s = new String(ByteStreams.toByteArray(is), Charsets.UTF_8);
				JsonObject obj = Jankson.builder()
						.allowBareRootObject()
						.build().load(s);
				ID = getRequired(obj, "id");
				if (ID.matches("[^a-z0-9_]")) {
					throw new IllegalArgumentException("id field must contain only lowercase alphanumerics and underscores");
				}
				NAME = getRequired(obj, "name");
				if (ILLEGAL_WINDOWS_FILENAMES.contains(Ascii.toLowerCase(NAME)) ||
						ILLEGAL_FILENAME_CHARACTERS.matchesAnyOf(NAME) ||
						NAME.endsWith(" ") || NAME.endsWith(".")) {
					throw new IllegalArgumentException("name field must be a valid filename on Windows and Linux");
				}
				VERSION = ID.equals("chipper") ? CHIPPER_VERSION : getRequired(obj, "version");
				AUTHOR = getRequired(obj, "author");
				LOADING_BACKGROUND_COLOR = ProtoColor.parse(getRequired(obj, "loading_background_color"));
				LOADING_ACCESSORY_COLOR = ProtoColor.parse(getRequired(obj, "loading_accessory_color"));
				LOADING_FONT = getRequired(obj, "loading_font");
				if (obj.containsKey("portable")) {
					PORTABLE = (Boolean)((JsonPrimitive)obj.get("portable")).getValue();
				} else {
					PORTABLE = false;
				}
				DEFAULT_PORT = Integer.parseInt(getRequired(obj, "default_port"));
				if (DEFAULT_PORT < 1024 || DEFAULT_PORT > 65535) {
					throw new IllegalArgumentException(DEFAULT_PORT+" is not a valid port number");
				}
				if (obj.containsKey("server_terminal_icon")) {
					SERVER_TERMINAL_ICON = ((JsonPrimitive)obj.get("server_terminal_icon")).asString();
				} else {
					SERVER_TERMINAL_ICON = null;
				}
				if (obj.containsKey("server_terminal_font_ttf")) {
					SERVER_TERMINAL_FONT_TTF = ((JsonPrimitive)obj.get("server_terminal_font_ttf")).asString();
				} else {
					SERVER_TERMINAL_FONT_TTF = null;
				}
				if (obj.containsKey("default_addon")) {
					DEFAULT_ADDON = ((JsonPrimitive)obj.get("default_addon")).asString();
				} else {
					DEFAULT_ADDON = null;
				}
			}
		} catch (IOException e) {
			log.error("Failed to load dist.jkson", e);
			System.exit(254);
			throw new Error();
		} catch (SyntaxError e) {
			log.error("Failed to load dist.jkson: {}", e.getCompleteMessage());
			System.exit(253);
			throw new Error();
		} catch (IllegalArgumentException e) {
			log.error("Failed to load dist.jkson: {}", e.getMessage());
			System.exit(252);
			throw new Error();
		}
	}

	private static String getRequired(JsonObject obj, String key) {
		if (!obj.containsKey(key)) throw new IllegalArgumentException(key+" field is missing");
		return ((JsonPrimitive)obj.get(key)).asString();
	}

}

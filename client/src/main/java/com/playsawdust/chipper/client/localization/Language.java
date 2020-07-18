/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.localization;

import org.checkerframework.checker.i18n.qual.Localized;

import com.google.common.collect.ImmutableList;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.client.component.I18n;

/**
 * Translation data for an individual language. Generally, you should not use this directly.
 * <p>
 * To properly handle fallbacks and honor the user's locale settings, use {@link I18n}.
 * <p>
 * Thread safe.
 * @see StaticLanguage
 */
public interface Language {

	/**
	 * Returns {@code true} if there is a localized string corresponding to the given identifier.
	 */
	boolean contains(Identifier id);

	/**
	 * Returns this Language's language tag; a well-formed IETF BCP 47 language tag. For example,
	 * US English is "en-US". Pirate English could be "x-en-PIRATE", Pig Latin could be
	 * "x-piglatin".
	 */
	String getLanguageTag();

	/**
	 * Return the localized string corresponding to the given identifier as a literal, collapsing
	 * newlines into spaces.
	 * <p>
	 * As there are no variable arguments, the resulting string will be cached. Only non-variable
	 * parameters will be replaced, such as {0} and references to other localized strings.
	 * @param id the identifier of the string to return
	 * @return the localized string with that identifier
	 */
	@Localized String format(Identifier id);

	/**
	 * Return the localized string corresponding to the given identifier, collapsing newlines into
	 * spaces.
	 * <p>
	 * Parameter replacement will be performed. {1} and {-1} both refer to the sole argument to this
	 * method.
	 * @param id the identifier of the string to return
	 * @param arg the first parameter
	 * @return the localized string with that identifier
	 */
	@Localized String format(Identifier id, Object arg);

	/**
	 * Return the localized string corresponding to the given identifier, collapsing newlines into
	 * spaces.
	 * <p>
	 * Parameter replacement will be performed. {1} and {-2} refer to the first argument to this
	 * method, {2} and {-1} to the second.
	 * @param id the identifier of the string to return
	 * @param arg1 the first parameter
	 * @param arg2 the second parameter
	 * @return the localized string with that identifier
	 */
	@Localized String format(Identifier id, Object arg1, Object arg2);

	/**
	 * Return the localized string corresponding to the given identifier, collapsing newlines into
	 * spaces.
	 * <p>
	 * Parameter replacement will be performed. {1} refers to the first argument to this method, {2}
	 * to the second, {3} to the third, {4} to the fourth if there is one, etc. {-1} refers to the
	 * last argument, {-2} to the second-to-last, etc.
	 * @param id the identifier of the string to return
	 * @param args the parameters
	 * @return the localized string with that identifier
	 */
	@Localized String format(Identifier id, Object... args);


	/**
	 * Return the localized string corresponding to the given identifier as a literal, preserving
	 * newlines.
	 * <p>
	 * As there are no variable arguments, the resulting string will be cached.
	 * @param id the identifier of the string to return
	 * @return the localized string with that identifier
	 */
	@Localized String formatMultiline(Identifier id);

	/**
	 * Return the localized string corresponding to the given identifier, preserving newlines.
	 * <p>
	 * Parameter replacement will be performed. {1} and {-1} both refer to the sole argument to this
	 * method.
	 * @param id the identifier of the string to return
	 * @param arg the first parameter
	 * @return the localized string with that identifier
	 */
	@Localized String formatMultiline(Identifier id, Object arg);

	/**
	 * Return the localized string corresponding to the given identifier, preserving newlines.
	 * <p>
	 * Parameter replacement will be performed. {1} and {-2} refer to the first argument to this
	 * method, {2} and {-1} to the second.
	 * @param id the identifier of the string to return
	 * @param arg1 the first parameter
	 * @param arg2 the second parameter
	 * @return the localized string with that identifier
	 */
	@Localized String formatMultiline(Identifier id, Object arg1, Object arg2);

	/**
	 * Return the localized string corresponding to the given identifier, preserving newlines.
	 * <p>
	 * Parameter replacement will be performed. {1} refers to the first argument to this method, {2}
	 * to the second, {3} to the third, {4} to the fourth if there is one, etc. {-1} refers to the
	 * last argument, {-2} to the second-to-last, etc.
	 * @param id the identifier of the string to return
	 * @param args the parameters
	 * @return the localized string with that identifier
	 */
	@Localized String formatMultiline(Identifier id, Object... args);


	/**
	 * Return the localized string corresponding to the given identifier as a literal, interpreting
	 * newlines as separators between lines.
	 * <p>
	 * As there are no variable arguments, the resulting string will be cached.
	 * @param id the identifier of the string to return
	 * @return the localized string with that identifier
	 */
	ImmutableList<@Localized String> formatList(Identifier id);

	/**
	 * Return the localized string corresponding to the given identifier, preserving newlines.
	 * <p>
	 * Parameter replacement will be performed. {1} and {-1} both refer to the sole argument to this
	 * method.
	 * @param id the identifier of the string to return
	 * @param arg the first parameter
	 * @return the localized string with that identifier
	 */
	ImmutableList<@Localized String> formatList(Identifier id, Object arg);

	/**
	 * Return the localized string corresponding to the given identifier, preserving newlines.
	 * <p>
	 * Parameter replacement will be performed. {1} and {-2} refer to the first argument to this
	 * method, {2} and {-1} to the second.
	 * @param id the identifier of the string to return
	 * @param arg1 the first parameter
	 * @param arg2 the second parameter
	 * @return the localized string with that identifier
	 */
	ImmutableList<@Localized String> formatList(Identifier id, Object arg1, Object arg2);

	/**
	 * Return the localized string corresponding to the given identifier, preserving newlines.
	 * <p>
	 * Parameter replacement will be performed. {1} refers to the first argument to this method, {2}
	 * to the second, {3} to the third, {4} to the fourth if there is one, etc. {-1} refers to the
	 * last argument, {-2} to the second-to-last, etc.
	 * @param id the identifier of the string to return
	 * @param args the parameters
	 * @return the localized string with that identifier
	 */
	ImmutableList<@Localized String> formatList(Identifier id, Object... args);

}

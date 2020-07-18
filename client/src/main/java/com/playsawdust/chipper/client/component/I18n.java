/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.checkerframework.checker.i18n.qual.Localized;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.localization.Language;
import com.playsawdust.chipper.client.localization.NonLanguage;
import com.playsawdust.chipper.client.localization.NumberBase;
import com.playsawdust.chipper.client.localization.SimpleNumberBase;
import com.playsawdust.chipper.component.Component;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.component.Context.WhiteLotus;

/**
 * Manages internationalization (I18n; there are 18 characters between the first I and the last n).
 * This handles number bases, languages, time formatting, etc. (Or, it will. Currently it's just
 * languages and number bases.)
 * <p>
 * Thread safe.
 */
// TODO lazy loading, merging
public class I18n implements Component, Language, NumberBase {
	private static final Logger log = LoggerFactory.getLogger(I18n.class);

	private final ConcurrentMap<String, Supplier<Language>> languageConstructors = Maps.newConcurrentMap();
	private ConcurrentMap<String, Language> languages = null;

	private String defaultLanguageTag = "x-none";

	private Language defaultLanguage;
	private Language currentLanguage;

	private ConcurrentMap<Identifier, NumberBase> numberBases = Maps.newConcurrentMap();
	private Identifier currentNumberBaseIdentifier = new Identifier("chipper", "decimal");
	private NumberBase currentNumberBase = new SimpleNumberBase(currentNumberBaseIdentifier, SimpleNumberBase.ALPHABET_DECIMAL);

	// prevents race conditions between register and reload
	private final Object reloadMutex = new Object();

	private I18n(WhiteLotus lotus) {
		WhiteLotus.verify(lotus);
		languageConstructors.put("x-none", NonLanguage::new);
		reload();
		numberBases.put(currentNumberBaseIdentifier, currentNumberBase);
	}

	/**
	 * Register a new number base.
	 * @param base the number base to register
	 */
	public void registerNumberBase(NumberBase base) {
		if (base.getIdentifier() == null) {
			throw new IllegalArgumentException("NumberBase::getIdentifier must not return null");
		}
		numberBases.put(base.getIdentifier(), base);
	}

	/**
	 * Update the current number base to a previously registered number base with the given
	 * identifier.
	 * @param id the identifier of the number base to switch to
	 */
	public void setCurrentNumberBase(Identifier id) {
		NumberBase nb = numberBases.get(id);
		if (nb == null) {
			throw new IllegalArgumentException("No number base with identifier "+id+" is registered");
		}
		currentNumberBase = nb;
		currentNumberBaseIdentifier = id;
	}

	/**
	 * @return the currently used number base
	 */
	public NumberBase getCurrentNumberBase() {
		return currentNumberBase;
	}

	/**
	 * @return the currently used number base's identifier
	 */
	public Identifier getCurrentNumberBaseIdentifier() {
		return currentNumberBaseIdentifier;
	}

	/**
	 * @return a list of every registered number base.
	 */
	public List<NumberBase> getAllNumberBases() {
		return Lists.newArrayList(numberBases.values());
	}

	/**
	 * @deprecated Implemented to conform to {@link NumberBase} for convenience.
	 * 		Use {@link #getCurrentNumberBaseIdentifier()} instead.
	 */
	@Deprecated
	@Override
	public Identifier getIdentifier() {
		return currentNumberBaseIdentifier;
	}

	@Override
	public int getRadix() {
		return currentNumberBase.getRadix();
	}

	@Override
	public String format(int n, FormatOptions options) {
		return currentNumberBase.format(n, options);
	}

	@Override
	public String format(int n) {
		return currentNumberBase.format(n);
	}

	@Override
	public String format(long n, FormatOptions options) {
		return currentNumberBase.format(n, options);
	}

	@Override
	public String format(long n) {
		return currentNumberBase.format(n);
	}

	@Override
	public String format(float n, FormatOptions options) {
		return currentNumberBase.format(n, options);
	}

	@Override
	public String format(float n) {
		return currentNumberBase.format(n);
	}

	@Override
	public String format(double n, FormatOptions options) {
		return currentNumberBase.format(n, options);
	}

	@Override
	public String format(double n) {
		return currentNumberBase.format(n);
	}

	/**
	 * Update the default/fallback language that will be used for any unrecognized keys in the
	 * current language.
	 * <p>
	 * The default language in Chipper is {@link NonLanguage}, with tag "x-none".
	 * @param tag the new default language tag
	 */
	public void setDefaultLanguage(String tag) {
		Language lang = languages.get(tag);
		if (lang != null) {
			this.defaultLanguageTag = tag;
			this.defaultLanguage = lang;
		} else {
			throw new IllegalArgumentException("No language with tag "+tag+" is registered");
		}
	}

	/**
	 * Update the current language that will be checked first. Any keys not in this language
	 * will be passed through to the default language.
	 * @param tag the language tag to switch to
	 */
	public void setCurrentLanguage(String tag) {
		Language lang = languages.get(tag);
		if (lang != null) {
			this.currentLanguage = lang;
		} else {
			throw new IllegalArgumentException("No language with tag "+tag+" is registered");
		}
	}

	/**
	 * Register a language constructor for the given tag. The constructor will be called immediately
	 * during registration, and every time languages are reloaded.
	 * <p>
	 * The tag should be a well-formed IETF BCP 47 language tag. For example, US English is "en-US".
	 * Pirate English could be "x-en-PIRATE", Pig Latin could be "x-piglatin".
	 * @param tag the tag for this language
	 * @param lang the language to register
	 */
	public void registerLanguage(String tag, Supplier<Language> lang) {
		synchronized (reloadMutex) {
			if (languageConstructors.containsKey(tag)) {
				throw new IllegalArgumentException("A language with tag "+tag+" is already registered");
			}
			languageConstructors.put(tag, lang);
			languages.put(tag, lang.get());
		}
	}

	/**
	 * Return the language registered for the given tag, or null if none exists.
	 */
	public @Nullable Language getLanguageForTag(String tag) {
		return languages.get(tag);
	}

	/**
	 * Return the current selected language.
	 */
	public Language getCurrentLanguage() {
		return currentLanguage;
	}

	/**
	 * Return the current default/fallback language.
	 */
	public Language getDefaultLanguage() {
		return defaultLanguage;
	}

	/**
	 * Return a list of every registered language.
	 */
	public List<Language> getAllLanguages() {
		return Lists.newArrayList(languages.values());
	}

	/**
	 * Re-invoke all language constructors, reloading any needed data from disk.
	 * <p>
	 * May be called on a worker thread; no language data will be purged until the reload is
	 * complete, so other threads will see stale language data until the reload is complete, after
	 * which they will immediately switch over to the new data with no interruption.
	 */
	public void reload() {
		Stopwatch sw = log.isDebugEnabled() ? Stopwatch.createStarted() : null;
		synchronized (reloadMutex) {
			ConcurrentMap<String, Language> work = Maps.newConcurrentMap();
			for (Map.Entry<String, Supplier<Language>> en : languageConstructors.entrySet()) {
				work.put(en.getKey(), en.getValue().get());
			}
			String currentTag = currentLanguage == null ? "x-none" : getLanguageTag();
			Language defLang = work.get(defaultLanguageTag);
			if (defLang == null) {
				defLang = new NonLanguage();
				defaultLanguageTag = "x-none";
			}
			defaultLanguage = defLang;
			Language curLang = work.get(currentTag);
			currentLanguage = curLang == null ? defLang : curLang;
			languages = work;
			log.debug("Reloaded language data in {}", sw);
		}
	}

	private Object formatIfNum(Object o) {
		if (o instanceof Byte || o instanceof Short || o instanceof Integer) {
			return currentNumberBase.format(((Number) o).intValue());
		} else if (o instanceof Long) {
			return currentNumberBase.format(((Number) o).longValue());
		} else if (o instanceof Float) {
			return currentNumberBase.format(((Number) o).floatValue());
		} else if (o instanceof Number) {
			return currentNumberBase.format(((Number) o).doubleValue());
		} else {
			return o;
		}
	}

	private Object[] formatNums(Object[] arr) {
		Object[] arr2 = new Object[arr.length];
		for (int i = 0; i < arr.length; i++) {
			arr2[i] = formatIfNum(arr[i]);
		}
		return arr2;
	}

	@Override
	public String getLanguageTag() {
		return currentLanguage.getLanguageTag();
	}

	@Override
	public boolean contains(Identifier id) {
		return currentLanguage.contains(id) || defaultLanguage.contains(id);
	}

	@Override
	public @Localized String format(Identifier id) {
		return currentLanguage.contains(id) ? currentLanguage.format(id) : defaultLanguage.format(id);
	}

	/**
	 * <b>I18n note</b>: If the given argument is a number, it will be formatted with the current
	 * number base before being passed to the underlying Language.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public @Localized String format(Identifier id, Object arg) {
		return currentLanguage.contains(id) ? currentLanguage.format(id, formatIfNum(arg)) : defaultLanguage.format(id, formatIfNum(arg));
	}

	/**
	 * <b>I18n note</b>: If any given argument is a number, it will be formatted with the current
	 * number base before being passed to the underlying Language.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public @Localized String format(Identifier id, Object arg1, Object arg2) {
		return currentLanguage.contains(id) ? currentLanguage.format(id, formatIfNum(arg1), formatIfNum(arg2)) : defaultLanguage.format(id, formatIfNum(arg1), formatIfNum(arg2));
	}

	/**
	 * <b>I18n note</b>: If any given argument is a number, it will be formatted with the current
	 * number base before being passed to the underlying Language.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public @Localized String format(Identifier id, Object... args) {
		return currentLanguage.contains(id) ? currentLanguage.format(id, formatNums(args)) : defaultLanguage.format(id, formatNums(args));
	}

	@Override
	public @Localized String formatMultiline(Identifier id) {
		return currentLanguage.contains(id) ? currentLanguage.formatMultiline(id) : defaultLanguage.formatMultiline(id);
	}

	/**
	 * <b>I18n note</b>: If the given argument is a number, it will be formatted with the current
	 * number base before being passed to the underlying Language.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public @Localized String formatMultiline(Identifier id, Object arg) {
		return currentLanguage.contains(id) ? currentLanguage.formatMultiline(id, formatIfNum(arg)) : defaultLanguage.formatMultiline(id, formatIfNum(arg));
	}

	/**
	 * <b>I18n note</b>: If any given argument is a number, it will be formatted with the current
	 * number base before being passed to the underlying Language.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public @Localized String formatMultiline(Identifier id, Object arg1, Object arg2) {
		return currentLanguage.contains(id) ? currentLanguage.formatMultiline(id, formatIfNum(arg1), formatIfNum(arg2)) : defaultLanguage.formatMultiline(id, formatIfNum(arg1), formatIfNum(arg2));
	}

	/**
	 * <b>I18n note</b>: If any given argument is a number, it will be formatted with the current
	 * number base before being passed to the underlying Language.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public @Localized String formatMultiline(Identifier id, Object... args) {
		return currentLanguage.contains(id) ? currentLanguage.formatMultiline(id, formatNums(args)) : defaultLanguage.formatMultiline(id, formatNums(args));
	}

	@Override
	public ImmutableList<@Localized String> formatList(Identifier id) {
		return currentLanguage.contains(id) ? currentLanguage.formatList(id) : defaultLanguage.formatList(id);
	}

	/**
	 * <b>I18n note</b>: If the given argument is a number, it will be formatted with the current
	 * number base before being passed to the underlying Language.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public ImmutableList<@Localized String> formatList(Identifier id, Object arg) {
		return currentLanguage.contains(id) ? currentLanguage.formatList(id, formatIfNum(arg)) : defaultLanguage.formatList(id, formatIfNum(arg));
	}

	/**
	 * <b>I18n note</b>: If any given argument is a number, it will be formatted with the current
	 * number base before being passed to the underlying Language.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public ImmutableList<@Localized String> formatList(Identifier id, Object arg1, Object arg2) {
		return currentLanguage.contains(id) ? currentLanguage.formatList(id, formatIfNum(arg1), formatIfNum(arg2)) : defaultLanguage.formatList(id, formatIfNum(arg1), formatIfNum(arg2));
	}

	/**
	 * <b>I18n note</b>: If any given argument is a number, it will be formatted with the current
	 * number base before being passed to the underlying Language.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public ImmutableList<@Localized String> formatList(Identifier id, Object... args) {
		return currentLanguage.contains(id) ? currentLanguage.formatList(id, formatNums(args)) : defaultLanguage.formatList(id, formatNums(args));
	}

	public static I18n obtain(Context<? extends ClientEngine> ctx) {
		return ctx.getComponent(I18n.class);
	}

	@Override
	public boolean compatibleWith(Engine engine) {
		return engine instanceof ClientEngine;
	}

}

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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.playsawdust.chipper.Identifier;

/**
 * A Language that just returns the identifiers and arguments it's given. Useful for debugging.
 */
public class NonLanguage implements Language {
	private static final Joiner COMMA_JOINER = Joiner.on(", ");
	private static final Joiner NEWLINE_INDENT_JOINER = Joiner.on("\n\t");

	@Override
	public boolean contains(Identifier id) {
		return true;
	}

	@Override
	public String getLanguageTag() {
		return "x-none";
	}

	@Override
	public @Localized String format(Identifier id) {
		return String.valueOf(id);
	}

	@Override
	public @Localized String format(Identifier id, Object arg) {
		return id+"("+arg+")";
	}

	@Override
	public @Localized String format(Identifier id, Object arg1, Object arg2) {
		return id+"("+arg1+", "+arg2+")";
	}

	@Override
	public @Localized String format(Identifier id, Object... args) {
		return id+"("+COMMA_JOINER.join(args)+")";
	}

	@Override
	public @Localized String formatMultiline(Identifier id) {
		return String.valueOf(id);
	}

	@Override
	public @Localized String formatMultiline(Identifier id, Object arg) {
		return id+"\n\t"+arg;
	}

	@Override
	public @Localized String formatMultiline(Identifier id, Object arg1, Object arg2) {
		return id+"\n\t"+arg1+"\n\t"+arg2;
	}

	@Override
	public @Localized String formatMultiline(Identifier id, Object... args) {
		return id+"\n\t"+NEWLINE_INDENT_JOINER.join(args);
	}

	@Override
	public ImmutableList<@Localized String> formatList(Identifier id) {
		return ImmutableList.of(String.valueOf(id));
	}

	@Override
	public ImmutableList<@Localized String> formatList(Identifier id, Object arg) {
		return ImmutableList.of(String.valueOf(id), String.valueOf(arg));
	}

	@Override
	public ImmutableList<@Localized String> formatList(Identifier id, Object arg1, Object arg2) {
		return ImmutableList.of(String.valueOf(id), String.valueOf(arg1), String.valueOf(arg2));
	}

	@Override
	public ImmutableList<@Localized String> formatList(Identifier id, Object... args) {
		ImmutableList.Builder<String> bldr = ImmutableList.builderWithExpectedSize(args.length+1);
		bldr.add(String.valueOf(id));
		for (Object o : args) {
			bldr.add(String.valueOf(o));
		}
		return bldr.build();
	}

}

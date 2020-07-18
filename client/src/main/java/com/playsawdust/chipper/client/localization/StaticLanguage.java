/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.localization;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.checkerframework.checker.i18n.qual.Localized;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.PUAChars;

import com.playsawdust.chipper.client.component.ResourceCache;
import com.playsawdust.chipper.rcl.AccessRules;

/**
 * A {@link Language} loaded from a file.
 */
public class StaticLanguage implements Language {
	private static final Pattern PARAMETER = Pattern.compile("(?<!\\\\)\\{(.*?)(?<!\\\\)\\}");

	private static final ImmutableMap<Identifier, String> FALSE_IDENTIFIERS = ImmutableMap.<Identifier, String>builder()
			.put(new Identifier("chipper", "pua/small_zero"), PUAChars.SMALL_ZERO_STR)
			.put(new Identifier("chipper", "pua/small_one"), PUAChars.SMALL_ONE_STR)
			.put(new Identifier("chipper", "pua/dwiggins_dek"), PUAChars.DWIGGINS_DEK_STR)
			.put(new Identifier("chipper", "pua/dwiggins_el"), PUAChars.DWIGGINS_EL_STR)
			.put(new Identifier("chipper", "pua/negative_one"), PUAChars.NEGATIVE_ONE_STR)
			.put(new Identifier("chipper", "pua/positive_one"), PUAChars.POSITIVE_ONE_STR)
			.put(new Identifier("chipper", "pua/external_link"), PUAChars.EXTERNAL_LINK_STR)
			.put(new Identifier("chipper", "pua/sharing"), PUAChars.SHARING_STR)
			.put(new Identifier("chipper", "pua/network"), PUAChars.NETWORK_STR)
			.put(new Identifier("chipper", "pua/font_renderer_control"), PUAChars.FONT_RENDERER_CONTROL_STR)
			.build();

	private final String tag;

	private final ImmutableListMultimap<Identifier, TranslationPart> translations;

	private final ConcurrentMap<Identifier, @Localized String> literalCacheSingleLine = Maps.newConcurrentMap();
	private final ConcurrentMap<Identifier, @Localized String> literalCacheMultiLine = Maps.newConcurrentMap();
	private final ConcurrentMap<Identifier, ImmutableList<@Localized String>> literalCacheList = Maps.newConcurrentMap();

	public StaticLanguage(String tag, ResourceCache cache, Identifier file) throws IOException {
		this.tag = tag;
		Multimap<Identifier, TranslationPart> mm = ArrayListMultimap.create();
		load(cache, file, mm, Maps.newHashMap());
		translations = ImmutableListMultimap.copyOf(mm);
	}

	@Override
	public String getLanguageTag() {
		return tag;
	}

	private static void load(ResourceCache cache, Identifier file, Multimap<Identifier, TranslationPart> mm, Map<Identifier, String> originalDefinitions) throws IOException {
		try (BufferedReader r = cache.asByteSource(file).asCharSource(Charsets.UTF_8).openBufferedStream()) {
			load(r, file.toString(), mm, originalDefinitions, (filename) -> {
				load(cache, file.sibling(filename), mm, originalDefinitions);
			});
		}
	}

	private static void load(BufferedReader r, String file, Multimap<Identifier, TranslationPart> mm, Map<Identifier, String> originalDefinitions, Importer importer) throws IOException {
		String errorPrefix = "Failed to parse language file with identifier "+file+":";
		Identifier currentId = null;
		int emptyLines = 0;
		int lineNumber = 0;
		while (true) {
			String line = r.readLine();
			if (line == null) break;
			lineNumber++;
			if (line.startsWith("#")) continue;
			if (line.trim().isEmpty()) {
				emptyLines++;
				continue;
			}
			if (line.startsWith(" ")) {
				throw new IOException(errorPrefix+" Line "+lineNumber+" is indented with spaces; tabs must be used, as they are unambiguous");
			}
			if (line.startsWith("@")) {
				importer.accept(line.substring(1));
			} else if (line.startsWith("\t")) {
				if (currentId == null) {
					throw new IOException(errorPrefix+" Line "+lineNumber+" is dangling; there's no identifier before it");
				}
				if (mm.containsKey(currentId)) {
					for (int i = 0; i < emptyLines+1; i++) {
						mm.put(currentId, new NewlineTranslationPart());
					}
					emptyLines = 0;
				}
				line = line.substring(1);
				AccessRules.squelchNextWarning("StringBuffer is required by Pattern's API (ugh)");
				StringBuffer sb = new StringBuffer();
				Matcher params = PARAMETER.matcher(line);
				while (params.find()) {
					sb.setLength(0);
					params.appendReplacement(sb, "");
					if (sb.length() > 0) {
						mm.put(currentId, new LiteralTranslationPart(unescapeBraces(sb.toString())));
					}
					String g = params.group(1);
					Integer idx = Ints.tryParse(g);
					if (idx != null) {
						if (idx == 0) {
							mm.put(currentId, new IdentifierTranslationPart());
						} else {
							mm.put(currentId, new ArgIndexTranslationPart(idx));
						}
					} else {
						Identifier id = Identifier.tryParse(g);
						if (id != null) {
							if (FALSE_IDENTIFIERS.containsKey(id)) {
								mm.put(currentId, new LiteralTranslationPart(FALSE_IDENTIFIERS.get(id)));
							} else {
								mm.put(currentId, new ReferenceTranslationPart(id));
							}
						} else {
							throw new IOException(errorPrefix+" Line "+lineNumber+" contains a parameter that isn't an index or reference: "+params.group());
						}
					}
				}
				sb.setLength(0);
				params.appendTail(sb);
				if (sb.length() > 0) {
					mm.put(currentId, new LiteralTranslationPart(unescapeBraces(sb.toString())));
				}
			} else if (line.contains(":")) {
				Identifier id = Identifier.parse(line);
				if (mm.containsKey(id)) {
					throw new IOException(errorPrefix+" Line "+lineNumber+" redefines a previously seen identifier, "+id+" first defined at "+originalDefinitions.get(id));
				}
				originalDefinitions.put(id, "line "+lineNumber+" in "+file);
				emptyLines = 0;
				currentId = id;
			} else {
				throw new IOException(errorPrefix+" Line "+lineNumber+" has unknown syntax");
			}
		}
	}

	private @Nullable List<TranslationPart> resolveParts(Identifier id) {
		if (id != null && translations.containsKey(id)) {
			return translations.get(id);
		} else {
			return null;
		}
	}

	private @Localized String formatDelimited(Identifier id, char delimiter, BiConsumer<ArgumentativeTranslationPart, StringBuilder> argHandler, int depth) {
		List<TranslationPart> parts = resolveParts(id);
		if (parts == null) {
			return "{"+id+"}";
		} else {
			StringBuilder sb = new StringBuilder();
			for (TranslationPart tp : parts) {
				if (tp instanceof BasicTranslationPart) {
					((BasicTranslationPart) tp).appendTo(sb, id);
				} else if (tp instanceof ArgumentativeTranslationPart) {
					argHandler.accept((ArgumentativeTranslationPart)tp, sb);
				} else if (tp instanceof NewlineTranslationPart) {
					sb.append(delimiter);
				} else if (tp instanceof ReferenceTranslationPart) {
					sb.append(format(((ReferenceTranslationPart) tp).id, depth+1));
				} else {
					throw new RuntimeException("Unrecognized translation part "+tp.getClass());
				}
			}
			return sb.toString();
		}
	}

	private ImmutableList<@Localized String> formatList(Identifier id, BiConsumer<ArgumentativeTranslationPart, StringBuilder> argHandler) {
		ImmutableList.Builder<@Localized String> bldr = ImmutableList.builder();
		StringBuilder sb = new StringBuilder();
		formatListInner(id, argHandler, bldr, sb, 0);
		bldr.add(sb.toString());
		sb.setLength(0);
		return bldr.build();
	}

	private void formatListInner(Identifier id, BiConsumer<ArgumentativeTranslationPart, StringBuilder> argHandler,
			ImmutableList.Builder<@Localized String> bldr, StringBuilder sb, int depth) {
		if (depth > 32) {
			throw new IllegalStateException("Translation key "+id+" contains a recursive reference (or is part of an incredibly long chain)");
		}
		List<TranslationPart> parts = resolveParts(id);
		if (parts == null) {
			bldr.add("{"+id+"}");
		} else {
			for (TranslationPart tp : parts) {
				if (tp instanceof BasicTranslationPart) {
					((BasicTranslationPart) tp).appendTo(sb, id);
				} else if (tp instanceof ArgumentativeTranslationPart) {
					argHandler.accept((ArgumentativeTranslationPart)tp, sb);
				} else if (tp instanceof NewlineTranslationPart) {
					bldr.add(sb.toString());
					sb.setLength(0);
				} else if (tp instanceof ReferenceTranslationPart) {
					formatListInner(((ReferenceTranslationPart) tp).id, argHandler, bldr, sb, depth+1);
				} else {
					throw new RuntimeException("Unrecognized translation part "+tp.getClass());
				}
			}
		}
	}


	@Override
	public boolean contains(Identifier id) {
		return translations.containsKey(id);
	}


	@Override
	public @Localized String format(Identifier id) {
		return format(id, 0);
	}

	private @Localized String format(Identifier id, int depth) {
		if (depth > 32) {
			throw new IllegalStateException("Translation key "+id+" contains a recursive reference (or is part of an incredibly long chain)");
		}
		if (id == null) return "{null}";
		@Localized String res = literalCacheSingleLine.get(id);
		if (res == null) {
			res = formatDelimited(id, ' ', (atp, sb) -> atp.appendTo(sb, id), depth);
			literalCacheSingleLine.putIfAbsent(id, res);
		}
		return res;
	}


	@Override
	public @Localized String format(Identifier id, Object arg) {
		return formatDelimited(id, ' ', (atp, sb) -> atp.appendTo(sb, id, arg), 0);
	}


	@Override
	public @Localized String format(Identifier id, Object arg1, Object arg2) {
		return formatDelimited(id, ' ', (atp, sb) -> atp.appendTo(sb, id, arg1, arg2), 0);
	}


	@Override
	public @Localized String format(Identifier id, Object... args) {
		return formatDelimited(id, ' ', (atp, sb) -> atp.appendTo(sb, id, args), 0);
	}



	@Override
	public @Localized String formatMultiline(Identifier id) {
		if (id == null) return "{null}";
		@Localized String res = literalCacheMultiLine.get(id);
		if (res == null) {
			res = formatDelimited(id, '\n', (atp, sb) -> atp.appendTo(sb, id), 0);
			literalCacheMultiLine.putIfAbsent(id, res);
		}
		return res;
	}


	@Override
	public @Localized String formatMultiline(Identifier id, Object arg) {
		return formatDelimited(id, '\n', (atp, sb) -> atp.appendTo(sb, id, arg), 0);
	}


	@Override
	public @Localized String formatMultiline(Identifier id, Object arg1, Object arg2) {
		return formatDelimited(id, '\n', (atp, sb) -> atp.appendTo(sb, id, arg1, arg2), 0);
	}


	@Override
	public @Localized String formatMultiline(Identifier id, Object... args) {
		return formatDelimited(id, '\n', (atp, sb) -> atp.appendTo(sb, id, args), 0);
	}



	@Override
	public ImmutableList<@Localized String> formatList(Identifier id) {
		if (id == null) return ImmutableList.of("{null}");
		ImmutableList<@Localized String> res = literalCacheList.get(id);
		if (res == null) {
			res = formatList(id, (atp, sb) -> atp.appendTo(sb, id));
			literalCacheList.putIfAbsent(id, res);
		}
		return res;
	}


	@Override
	public ImmutableList<@Localized String> formatList(Identifier id, Object arg) {
		return formatList(id, (atp, sb) -> atp.appendTo(sb, id, arg));
	}


	@Override
	public ImmutableList<@Localized String> formatList(Identifier id, Object arg1, Object arg2) {
		return formatList(id, (atp, sb) -> atp.appendTo(sb, id, arg1, arg2));
	}


	@Override
	public ImmutableList<@Localized String> formatList(Identifier id, Object... args) {
		return formatList(id, (atp, sb) -> atp.appendTo(sb, id, args));
	}


	private static String unescapeBraces(String str) {
		return str.replace("\\{", "{").replace("\\}", "}");
	}

	private static String escapeBraces(String str) {
		return str.replace("{", "\\{").replace("}", "\\}");
	}

	private interface Importer {
		void accept(String filename) throws IOException;
	}

	private interface TranslationPart {}
	private interface BasicTranslationPart extends TranslationPart {
		void appendTo(StringBuilder sb, Identifier id);
	}
	private interface ArgumentativeTranslationPart extends TranslationPart {
		void appendTo(StringBuilder sb, Identifier id);
		void appendTo(StringBuilder sb, Identifier id, Object arg);
		void appendTo(StringBuilder sb, Identifier id, Object arg1, Object arg2);
		void appendTo(StringBuilder sb, Identifier id, Object... arguments);
	}

	private static class LiteralTranslationPart implements BasicTranslationPart {
		private final String str;
		public LiteralTranslationPart(String str) {
			this.str = str;
		}
		@Override
		public void appendTo(StringBuilder sb, Identifier id) {
			sb.append(str);
		}
		@Override
		public String toString() {
			return escapeBraces(str);
		}
	}

	private static class IdentifierTranslationPart implements BasicTranslationPart {
		@Override
		public void appendTo(StringBuilder sb, Identifier id) {
			sb.append(id);
		}
		@Override
		public String toString() {
			return "{0}";
		}
	}

	private static class ArgIndexTranslationPart implements ArgumentativeTranslationPart {
		private final int idx;
		public ArgIndexTranslationPart(int idx) {
			this.idx = idx;
		}
		@Override
		public void appendTo(StringBuilder sb, Identifier id) {
			sb.append("{");
			sb.append(idx);
			sb.append("}");
		}
		@Override
		public void appendTo(StringBuilder sb, Identifier id, Object arg) {
			if (idx == 1 || idx == -1) {
				sb.append(arg);
			} else {
				sb.append("{");
				sb.append(idx);
				sb.append("}");
			}
		}
		@Override
		public void appendTo(StringBuilder sb, Identifier id, Object arg1, Object arg2) {
			if (idx == 1 || idx == -2) {
				sb.append(arg1);
			} else if (idx == 2 || idx == -1) {
				sb.append(arg2);
			} else {
				sb.append("{");
				sb.append(idx);
				sb.append("}");
			}
		}
		@Override
		public void appendTo(StringBuilder sb, Identifier id, Object... arguments) {
			int actualIdx;
			if (idx < 0) {
				actualIdx = arguments.length+idx;
			} else {
				actualIdx = idx-1;
			}
			if (actualIdx < 0 || actualIdx >= arguments.length) {
				sb.append("{");
				sb.append(idx);
				sb.append("}");
			} else {
				sb.append(arguments[actualIdx]);
			}
		}

		@Override
		public String toString() {
			return "{"+idx+"}";
		}
	}

	private static class ReferenceTranslationPart implements TranslationPart {
		public final Identifier id;
		public ReferenceTranslationPart(Identifier id) {
			this.id = id;
		}

		// this unfortunately needs to be special cased a layer higher to properly deal with multiple lines

		@Override
		public String toString() {
			return "{"+id+"}";
		}
	}

	private static class NewlineTranslationPart implements TranslationPart {
		// this may indicate a new list entry, a newline, or a space, depending on which method was called

		@Override
		public String toString() {
			return "\n";
		}
	}

}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.playsawdust.chipper.qual.Namespace;

/**
 * Identifies something by its namespace and its path.
 * <p>
 * Generally, the namespace is the id of the addon providing the thing being identified. <i>Breaking
 * this rule breaks a fundamental assumption made by identifiers, and makes clashes more likely,
 * which is the key thing avoided by the usage of identifiers.</i>
 * <p>
 * Paths must be unambiguous and cannot contain upreferences, redundant segments, or empty segments.
 * Redundant segments are things such as <code>a/b/<b>./</b>c</code>. Empty segments are things such
 * as <code>a<b>//</b>b</code>. Upreferences are things such as <code>a/<b>../</b>b</code>.
 * Additionally, they cannot begin or end with slashes, and must use Unix-style forward-slashes
 * ({@code /}), not Windows-style backslashes ({@code \}). You can avoid accidentally creating these
 * redundant structures by using {@link #child}, {@link #sibling}, and {@link #parent} to manipulate
 * paths instead of manually building path strings.
 * <p>
 * Identifiers are a very common sight throughout Chipper, to require things that would commonly
 * be identified by a bare string or a number be namespaced to prevent conflicts between addons.
 */
public final class Identifier {
	// precomputed has a scary javadoc, but for a small anyOf matcher, it's absolutely an improvement
	// it uses less memory and is O(1) instead of O(log n)
	private static final CharMatcher LOWER_ALPHANUM_UNDSC = CharMatcher.anyOf("abcdefghijklmnopqrstuvwxyz0123456789_").precomputed();

	public final @Namespace String namespace;
	public final String path;

	private boolean calculatedHashCode = false;
	private int hashCode;
	private String toString;

	/**
	 * Note: If you call this constructor with two literal strings, the Chipper class loader will
	 * patch your class to use a private static final field instead to avoid object allocations for
	 * efficiency.
	 * <p>
	 * As such, the recommended way to use one-off identifiers that aren't referenced over and over
	 * in a file, even if the method is called frequently, is just to call this constructor to keep
	 * related code together.
	 * <p>
	 * This is provided as a violation of Chipper's "no magic" rule for convenience, due to the
	 * sheer frequency at which Identifiers are used throughout the codebase. As Identifiers are
	 * immutable, the patched code is semantically equivalent to the unpatched code.
	 */
	public Identifier(@Namespace String namespace, String path) {
		Preconditions.checkArgument(namespace != null, "Namespace cannot be null");
		Preconditions.checkArgument(!namespace.isEmpty(), "Namespace cannot be empty");
		Preconditions.checkArgument(LOWER_ALPHANUM_UNDSC.matchesAllOf(namespace), "Namespace must be only lowercase english alphanumerics plus underscore (got %s)", namespace);

		Preconditions.checkArgument(path != null, "Path cannot be null");
		Preconditions.checkArgument(!path.isEmpty(), "Path cannot be empty");
		// prevent ambiguous paths
		Preconditions.checkArgument(!path.contains("\\"), "Path cannot contain backslashes (got %s)", path);
		Preconditions.checkArgument(!path.contains("../"), "Path cannot contain upreferences (got %s)", path);
		Preconditions.checkArgument(!path.contains("/.."), "Path cannot contain upreferences (got %s)", path);
		Preconditions.checkArgument(!path.contains("./"), "Path cannot contain redundant segments (got %s)", path);
		Preconditions.checkArgument(!path.contains("/."), "Path cannot contain redundant segments (got %s)", path);
		Preconditions.checkArgument(!path.contains("//"), "Path cannot contain empty segments (got %s)", path);
		Preconditions.checkArgument(!path.startsWith("/"), "Path cannot start with a slash (got %s)", path);
		Preconditions.checkArgument(!path.endsWith("/"), "Path cannot end with a slash (got %s)", path);
		this.namespace = namespace;
		this.path = path;
	}

	@Override
	public int hashCode() {
		if (!calculatedHashCode) {
			int result = 1;
			result = 31 * result + namespace.hashCode();
			result = 31 * result + path.hashCode();
			hashCode = result;
			calculatedHashCode = true;
		}
		return hashCode;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (!(obj instanceof Identifier)) return false;
		Identifier that = (Identifier)obj;
		return this.namespace.equals(that.namespace) && this.path.equals(that.path);
	}

	@Override
	public String toString() {
		if (toString == null) toString = namespace+":"+path;
		return toString;
	}

	/**
	 * Returns an Identifier pointing to a child of this one.
	 * <p>
	 * For example, {@code [chipper:test].child("foo")} results in
	 * {@code [chipper:test/foo]}.
	 */
	public Identifier child(String child) {
		return new Identifier(namespace, path+"/"+child);
	}

	/**
	 * Returns an Identifier pointing to the parent of this one.
	 * If this identifier has no parent, {@code null} is returned.
	 * <p>
	 * For example, {@code [chipper:test/foo].parent()} results in
	 * {@code [chipper:test]}. {@code [chipper:test].parent()} results in
	 * {@code null}.
	 */
	public @Nullable Identifier parent() {
		int idx = path.lastIndexOf('/');
		if (idx == -1) return null;
		return new Identifier(namespace, path.substring(0, idx));
	}

	/**
	 * Returns an Identifier pointing to a sibling of this one.
	 * <p>
	 * For example, {@code [chipper:test/foo].sibling("bar")} results in
	 * {@code [chipper:test/bar]}. {@code [chipper:test].sibling("foo")} results in
	 * {@code [chipper:foo]}.
	 */
	public Identifier sibling(String sibling) {
		int idx = path.lastIndexOf('/');
		if (idx == -1) return new Identifier(namespace, sibling);
		return new Identifier(namespace, path.substring(0, idx+1)+sibling);
	}

	/**
	 * Returns an Identifier prefixed with the given path segment.
	 * <p>
	 * For example, {@code [chipper:test/foo].withPrefix("bar")} results in
	 * {@code [chipper:bar/test/foo]}.
	 */
	public Identifier withPrefix(String prefix) {
		return new Identifier(namespace, prefix+"/"+path);
	}

	/**
	 * Construct an Identifier from a bare string. The namespace is assumed to
	 * be everything before the first colon in the string. If the string contains
	 * no colons, or the first colon is at the end of the string, an exception
	 * is thrown.
	 * @param str the string to convert into an identifier
	 * @return an identifier with a namespace and path from the given string
	 * @throws IllegalArgumentException if the string does not represent a valid identifier
	 */
	public static Identifier parse(String str) {
		Identifier id = tryParse(str);
		if (id == null) throw new IllegalArgumentException(str+" is not a valid identifier");
		return id;
	}

	/**
	 * Construct an Identifier from a bare string. The namespace is assumed to
	 * be everything before the first colon in the string. If the string contains
	 * no colons, or the first colon is at the end of the string, {@code null} is returned.
	 * @param str the string to convert into an identifier
	 * @return an identifier with a namespace and path from the given string, or null
	 */
	public static @Nullable Identifier tryParse(String str) {
		int idx = str.indexOf(':');
		if (idx == -1 || idx == str.length()-1) return null;
		return new Identifier(str.substring(0, idx), str.substring(idx+1));
	}

}

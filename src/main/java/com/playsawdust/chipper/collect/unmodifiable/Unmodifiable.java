/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.collect.unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multiset;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.UnmodifiableListIterator;

/**
 * Convenience methods to create unmodifiable views to collections and related
 * classes.
 * <p>
 * Differs from methods in {@link Collections} in that the types are public, so
 * guarantees can be made in APIs and warnings can be generated when calling
 * methods that would attempt to modify such a collection.
 * <p>
 * Differs from {@link ImmutableCollection} in that it doesn't require making
 * potentially expensive copies of collections; instead, comparatively
 * lightweight wrapper objects are used instead.
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class Unmodifiable {

	private static final UnmodifiableList EMPTY_LIST = list(Collections.emptyList());
	private static final UnmodifiableSet EMPTY_SET = set(Collections.emptySet());
	private static final UnmodifiableMap EMPTY_MAP = map(Collections.emptyMap());

	public static <T> UnmodifiableList<T> emptyList() {
		return EMPTY_LIST;
	}

	public static <K, V> UnmodifiableMap<K, V> emptyMap() {
		return EMPTY_MAP;
	}

	public static <T> UnmodifiableSet<T> emptySet() {
		return EMPTY_SET;
	}

	/**
	 * Returns its argument.
	 * @deprecated No need to call this.
	 */
	@Deprecated
	public static <K, V> UnmodifiableMap<K, V> map(UnmodifiableMap<K, V> map) {
		return map;
	}

	/**
	 * Returns a wrapper of the given map that does not support modification.
	 */
	public static <K, V> UnmodifiableMap<K, V> map(Map<K, V> map) {
		return new UnmodifiableMap<>(map);
	}

	/**
	 * Returns its argument.
	 * @deprecated No need to call this.
	 */
	@Deprecated
	public static <T> UnmodifiableMultiset<T> multiset(UnmodifiableMultiset<T> multiset) {
		return multiset;
	}

	/**
	 * Returns a wrapper of the given multiset that does not support modification.
	 */
	public static <T> UnmodifiableMultiset<T> multiset(Multiset<T> multiset) {
		return new UnmodifiableMultiset<>(multiset);
	}

	/**
	 * Returns its argument.
	 * @deprecated No need to call this.
	 */
	@Deprecated
	public static <T> UnmodifiableCollection<T> collection(UnmodifiableCollection<T> collection) {
		return collection;
	}

	/**
	 * Returns a wrapper of the given collection that does not support
	 * modification.
	 */
	public static <T> UnmodifiableCollection<T> collection(Collection<T> collection) {
		return new UnmodifiableCollection<>(collection);
	}

	/**
	 * Returns its argument.
	 * @deprecated No need to call this.
	 */
	@Deprecated
	public static <T> UnmodifiableSet<T> set(UnmodifiableSet<T> set) {
		return set;
	}

	/**
	 * Returns a wrapper of the given set that does not support modification.
	 */
	public static <T> UnmodifiableSet<T> set(Set<T> set) {
		return new UnmodifiableSet<>(set);
	}

	/**
	 * Returns its argument.
	 * @deprecated No need to call this.
	 */
	@Deprecated
	public static <T> UnmodifiableList<T> list(UnmodifiableList<T> list) {
		return list;
	}

	/**
	 * Returns a wrapper of the given list that does not support modification.
	 */
	public static <T> UnmodifiableList<T> list(List<T> list) {
		return new UnmodifiableList<>(list);
	}

	/**
	 * Returns its argument.
	 * @deprecated No need to call this.
	 */
	@Deprecated
	public static <T> UnmodifiableIterator<T> iterator(UnmodifiableIterator<T> iter) {
		return iter;
	}

	/**
	 * Returns a wrapper of the given iterator that does not support
	 * modification.
	 */
	public static <T> UnmodifiableIterator<T> iterator(Iterator<T> iter) {
		return Iterators.unmodifiableIterator(iter);
	}

	/**
	 * Returns its argument.
	 * @deprecated No need to call this.
	 */
	@Deprecated
	public static <T> UnmodifiableListIterator<T> listIterator(UnmodifiableListIterator<T> iter) {
		return iter;
	}

	/**
	 * Returns a wrapper of the given list iterator that does not support
	 * modification.
	 */
	public static <T> UnmodifiableListIterator<T> listIterator(ListIterator<T> iter) {
		return new UnmodifiableListIteratorImpl<>(iter);
	}

	/**
	 * Returns its argument.
	 * @deprecated No need to call this.
	 */
	@Deprecated
	public static <T> UnmodifiableIterable<T> iterable(UnmodifiableIterable<T> iterable) {
		return iterable;
	}

	/**
	 * Returns a wrapper of the given collection that does not support
	 * modification.
	 */
	public static <T> UnmodifiableIterable<T> iterable(Iterable<T> iterable) {
		return new UnmodifiableIterable<>(iterable);
	}

}

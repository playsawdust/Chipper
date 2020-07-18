/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.collect.unmodifiable;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

public class UnmodifiableMap<K, V> implements Map<K, V> {

	private final Map<K, V> delegate;

	UnmodifiableMap(Map<K, V> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Guaranteed to throw an exception and leave the underlying data unmodified.
	 *
	 * @throws UnsupportedOperationException always
	 * @deprecated Unsupported operation.
	 */
	@Override
	@Deprecated
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Guaranteed to throw an exception and leave the underlying data unmodified.
	 *
	 * @throws UnsupportedOperationException always
	 * @deprecated Unsupported operation.
	 */
	@Override
	@Deprecated
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Guaranteed to throw an exception and leave the underlying data unmodified.
	 *
	 * @throws UnsupportedOperationException always
	 * @deprecated Unsupported operation.
	 */
	@Override
	@Deprecated
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Guaranteed to throw an exception and leave the underlying data unmodified.
	 *
	 * @throws UnsupportedOperationException always
	 * @deprecated Unsupported operation.
	 */
	@Override
	@Deprecated
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return delegate.get(key);
	}

	@Override
	public UnmodifiableSet<K> keySet() {
		return Unmodifiable.set(delegate.keySet());
	}

	@Override
	public Collection<V> values() {
		return Unmodifiable.collection(delegate.values());
	}

	@Override
	public UnmodifiableSet<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> entrySet = delegate.entrySet();
		return new UnmodifiableSet<Entry<K, V>>(entrySet) {
			@Override
			public UnmodifiableIterator<Entry<K, V>> iterator() {
				return Unmodifiable.iterator(Iterators.transform(entrySet.iterator(), (en) -> new SimpleImmutableEntry<>(en)));
			}
		};
	}

	@Override
	public boolean equals(Object that) {
		if (that == null) return false;
		if (that == this) return true;
		if (that.getClass() != this.getClass()) return false;
		return ((UnmodifiableMap<?, ?>)that).delegate.equals(this.delegate);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

}

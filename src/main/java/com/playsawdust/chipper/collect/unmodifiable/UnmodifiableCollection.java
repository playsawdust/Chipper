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

public class UnmodifiableCollection<E> extends UnmodifiableIterable<E> implements Collection<E> {
	private final Collection<E> delegate;

	UnmodifiableCollection(Collection<E> delegate) {
		super(delegate);
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
	public final boolean add(E e) {
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
	public final boolean remove(Object o) {
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
	public final boolean addAll(Collection<? extends E> c) {
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
	public final boolean removeAll(Collection<?> c) {
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
	public final boolean retainAll(Collection<?> c) {
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
	public final void clear() {
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
	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	@Override
	public Object[] toArray() {
		return delegate.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return delegate.toArray(a);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	@Override
	public boolean equals(Object that) {
		if (that == null) return false;
		if (that == this) return true;
		if (that.getClass() != this.getClass()) return false;
		return ((UnmodifiableCollection<?>)that).delegate.equals(this.delegate);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}


}

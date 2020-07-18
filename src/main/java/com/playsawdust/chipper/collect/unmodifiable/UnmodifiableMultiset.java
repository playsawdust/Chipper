/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.collect.unmodifiable;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.Multiset;

public class UnmodifiableMultiset<E> extends UnmodifiableCollection<E> implements Multiset<E> {

	private final Multiset<E> delegate;

	UnmodifiableMultiset(Multiset<E> delegate) {
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
	public int add(@Nullable E element, int occurrences) {
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
	public int remove(@Nullable Object element, int occurrences) {
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
	public int setCount(E element, int count) {
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
	public boolean setCount(E element, int oldCount, int newCount) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int count(@Nullable Object element) {
		return delegate.count(element);
	}

	@Override
	public UnmodifiableSet<E> elementSet() {
		return Unmodifiable.set(delegate.elementSet());
	}

	@Override
	public UnmodifiableSet<Entry<E>> entrySet() {
		return Unmodifiable.set(delegate.entrySet());
	}

}

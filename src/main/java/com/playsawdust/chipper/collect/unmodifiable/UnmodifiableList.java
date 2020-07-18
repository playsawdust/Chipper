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
import java.util.List;
import com.google.common.collect.UnmodifiableListIterator;

public class UnmodifiableList<E> extends UnmodifiableCollection<E> implements List<E> {

	private final List<E> delegate;

	UnmodifiableList(List<E> delegate) {
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
	public boolean addAll(int index, Collection<? extends E> c) {
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
	public E set(int index, E element) {
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
	public void add(int index, E element) {
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
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E get(int index) {
		return delegate.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return delegate.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return delegate.lastIndexOf(o);
	}

	@Override
	public UnmodifiableListIterator<E> listIterator() {
		return Unmodifiable.listIterator(delegate.listIterator());
	}

	@Override
	public UnmodifiableListIterator<E> listIterator(int index) {
		return Unmodifiable.listIterator(delegate.listIterator(index));
	}

	@Override
	public UnmodifiableList<E> subList(int fromIndex, int toIndex) {
		return Unmodifiable.list(delegate.subList(fromIndex, toIndex));
	}

}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.collect.unmodifiable;

import com.google.common.collect.UnmodifiableIterator;

public class UnmodifiableIterable<E> implements Iterable<E> {
	private final Iterable<E> delegate;

	UnmodifiableIterable(Iterable<E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public UnmodifiableIterator<E> iterator() {
		return Unmodifiable.iterator(delegate.iterator());
	}

	@Override
	public boolean equals(Object that) {
		if (that == null) return false;
		if (that == this) return true;
		if (that.getClass() != this.getClass()) return false;
		return ((UnmodifiableIterable<?>)that).delegate.equals(this.delegate);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}


}

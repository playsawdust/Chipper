/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.collect.unmodifiable;

import java.util.ListIterator;

import com.google.common.collect.UnmodifiableListIterator;

final class UnmodifiableListIteratorImpl<T> extends UnmodifiableListIterator<T> {
	private final ListIterator<T> iter;

	UnmodifiableListIteratorImpl(ListIterator<T> iter) {
		this.iter = iter;
	}

	@Override
	public boolean hasNext() {
		return iter.hasNext();
	}

	@Override
	public boolean hasPrevious() {
		return iter.hasPrevious();
	}

	@Override
	public T next() {
		return iter.next();
	}

	@Override
	public int nextIndex() {
		return iter.nextIndex();
	}

	@Override
	public T previous() {
		return iter.previous();
	}

	@Override
	public int previousIndex() {
		return iter.previousIndex();
	}
}
/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.collect.unmodifiable;

import java.util.Set;

public class UnmodifiableSet<E> extends UnmodifiableCollection<E> implements Set<E> {

	// Set adds no new methods on top of Collection, just redefines them for new javadocs

	UnmodifiableSet(Set<E> delegate) {
		super(delegate);
	}

}

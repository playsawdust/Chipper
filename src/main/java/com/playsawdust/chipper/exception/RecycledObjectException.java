/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.exception;

import com.playsawdust.chipper.toolbox.pool.PooledObject;

/**
 * Thrown when a {@link PooledObject} is used after it has been
 * {@link PooledObject#recycle recycled}. Thrown on a <i>best-effort</i> basis -
 * this exception must not be relied upon.
 */
public class RecycledObjectException extends RuntimeException {
	public static final long serialVersionUID = 459656496345L;

	public RecycledObjectException(Object o) {
		super("This "+o.getClass().getSimpleName()+" has been recycled!");
	}

}

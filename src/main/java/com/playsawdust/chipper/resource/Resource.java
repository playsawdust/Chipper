/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.resource;

import com.google.common.io.ByteSource;

import com.playsawdust.chipper.Identifier;

/**
 * Represents a resource that can be retrieved from somewhere on demand.
 */
public abstract class Resource extends ByteSource {
	private final ResourceProvider provider;
	private final Identifier id;

	protected Resource(ResourceProvider provider, Identifier id) {
		this.provider = provider;
		this.id = id;
	}

	/**
	 * @return the id of this resource
	 */
	public final Identifier getId() {
		return id;
	}

	/**
	 * @return the provider that created this resource
	 */
	public final ResourceProvider getProvider() {
		return provider;
	}

}
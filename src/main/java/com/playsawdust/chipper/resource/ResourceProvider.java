/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.resource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.playsawdust.chipper.Identifier;

/**
 * Represents something that can generate {@link Resource resources}. May represent a zip file,
 * folder, etc.
 */
public interface ResourceProvider {
	/**
	 * Check if this provider should be visible to users. "Real" providers that come from things
	 * the user has installed should return {@code true}, while providers for addons' built-in
	 * resources and virtual providers should usually return {@code false}.
	 * @return {@code true} if this provider is something the user should be aware of
	 */
	default boolean visibleToUser() { return false; }
	/**
	 * Look for or generate a resource with the given identifier. If this provider does not
	 * recognize the given identifier for any reason, {@code null} should be returned.
	 * @param id the identifier to look for
	 * @return a Resource from this provider with the given id, or {@code null}
	 */
	@Nullable Resource provide(Identifier id);
}
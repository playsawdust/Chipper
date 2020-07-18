/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.component;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.component.Context.WhiteLotus;
import com.playsawdust.chipper.resource.Resource;

public final class ResourceLocator implements Component {

	private ResourceLocator(WhiteLotus lotus) {
		WhiteLotus.verify(lotus);
	}

	/**
	 * Retrieve the "most important" Resource of the given identifier.
	 * <p>
	 * The "most important" resource is the one belonging to the highest priority ResourceProvider,
	 * and this ordering may be configurable by the user.
	 * @param id the identifier of the resource to retrieve
	 * @return the resource
	 */
	public Resource getMostImportant(Identifier id) {
		return null; // TODO
	}


	@Override
	public boolean compatibleWith(Engine engine) {
		return true;
	}

	public static ResourceLocator obtain(Context<?> ctx) {
		return ctx.getComponent(ResourceLocator.class);
	}

}

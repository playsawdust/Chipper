/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network;

public interface Marshallable {
	/**
	 * Write any data contained in this Marshallable into the given Marshaller.
	 */
	void marshal(Marshaller out);
	/**
	 * Read any needed data for this Marshallable from the given Unmarshaller.
	 */
	void unmarshal(Unmarshaller in);
}

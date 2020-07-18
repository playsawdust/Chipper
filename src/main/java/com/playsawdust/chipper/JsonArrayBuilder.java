/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonPrimitive;

public class JsonArrayBuilder {

	private JsonArray array = new JsonArray();

	public JsonArrayBuilder add(String value) {
		array.add(new JsonPrimitive(value));
		return this;
	}

	public JsonArrayBuilder add(int value) {
		array.add(new JsonPrimitive(value));
		return this;
	}

	public JsonArrayBuilder add(double value) {
		array.add(new JsonPrimitive(value));
		return this;
	}

	public JsonArrayBuilder add(boolean value) {
		array.add(new JsonPrimitive(value));
		return this;
	}

	public JsonArrayBuilder add(JsonElement value) {
		array.add(value);
		return this;
	}

	public JsonArray build() {
		return array;
	}

	public String toJson() {
		return array.toJson(JsonGrammar.STRICT);
	}

}

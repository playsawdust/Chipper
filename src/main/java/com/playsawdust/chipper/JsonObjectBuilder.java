/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;

public class JsonObjectBuilder {

	private JsonObject object = new JsonObject();

	public JsonObjectBuilder put(String key, String value) {
		object.put(key, new JsonPrimitive(value));
		return this;
	}

	public JsonObjectBuilder put(String key, int value) {
		object.put(key, new JsonPrimitive(value));
		return this;
	}

	public JsonObjectBuilder put(String key, double value) {
		object.put(key, new JsonPrimitive(value));
		return this;
	}

	public JsonObjectBuilder put(String key, boolean value) {
		object.put(key, new JsonPrimitive(value));
		return this;
	}

	public JsonObjectBuilder put(String key, JsonElement value) {
		object.put(key, value);
		return this;
	}

	public JsonObject build() {
		return object;
	}

	public String toJson() {
		return object.toJson(JsonGrammar.STRICT);
	}

}

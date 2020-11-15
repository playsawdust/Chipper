/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.scene;

import com.playsawdust.chipper.SceneObject;
import com.playsawdust.chipper.math.ProtoColor;

public class Light extends SceneObject {
	private final Type type;
	private ProtoColor color;
	
	public Light(Type type, ProtoColor color) {
		this.type = type;
		this.color = color;
	}
	
	public ProtoColor getColor() {
		return this.color;
	}
	
	public void setColor(ProtoColor color) {
		this.color = color;
	}
	
	public Type getType() {
		return this.type;
	}
	
	public static enum Type {
		/** An infinitely far, collimated light source, such as the sun. Position and falloff will be ignored. */
		DIRECTIONAL,
		/** A point of light, radiating uniformly in all directions. This is the most common light type. */
		POINT,
		/** A directional cone of light, like a flashlight or searchlight. */
		CONE;
	}
}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import java.util.EnumSet;

import org.lwjgl.opengl.GL11;

import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.component.Component;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.component.Engine;

/** All changes to OpenGL state must now go through this class. */
public class GLPipelineManager implements Component {
	private static GLPipelineManager INSTANCE;
	private GLPipelineState currentState = new GLPipelineState();
	
	public GLPipelineState set(GLPipelineState state) {
		GLPipelineState old = currentState;
		
		EnumSet<GLFeatureToggle> features = EnumSet.noneOf(GLFeatureToggle.class);
		features.addAll(old.getEnabledFeatures());
		features.addAll(state.getEnabledFeatures());
		for(GLFeatureToggle toggle : features) {
			if (old.getEnabledFeatures().contains(toggle) ^ state.getEnabledFeatures().contains(toggle)) {
				if (state.getEnabledFeatures().contains(toggle)) {
					GL11.glEnable(toggle.glConstant());
				} else {
					GL11.glDisable(toggle.glConstant());
				}
			}
		}
		
		EnumSet<GLBoolean> setBooleans = EnumSet.noneOf(GLBoolean.class);
		setBooleans.addAll(old.getTrueBooleans());
		setBooleans.addAll(state.getTrueBooleans());
		for(GLBoolean bool : setBooleans) {
			if (old.getTrueBooleans().contains(bool) ^ state.getTrueBooleans().contains(bool)) {
				
				bool.set(state.getTrueBooleans().contains(bool));
				
			}
		}
		
		currentState = state;
		
		return old;
	}

	@Override
	public boolean compatibleWith(Engine engine) {
		return engine.getType().isClient();
	}
	
	public static GLPipelineManager obtain(Context<ClientEngine> ctx) {
		
		
		if (INSTANCE==null) {
			INSTANCE = new GLPipelineManager();
		}
		
		return INSTANCE;
	}
}

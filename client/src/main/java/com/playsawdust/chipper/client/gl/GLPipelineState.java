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

public class GLPipelineState {
	private EnumSet<GLFeatureToggle> enabled = EnumSet.of(GLFeatureToggle.DITHER, GLFeatureToggle.MULTISAMPLE);
	private EnumSet<GLBoolean> trueBools = EnumSet.of(GLBoolean.DEPTH_WRITEMASK);
	
	//private Object2IntMap<GLInteger> ephemeralInt = new Object2IntOpenHashMap<>();
	
	public GLPipelineState() {}
	
	protected EnumSet<GLFeatureToggle> getEnabledFeatures() { return enabled; }
	protected EnumSet<GLBoolean> getTrueBooleans() { return trueBools; }
	
	public static Builder builder() { return new Builder(); }
	
	public static class Builder {
		private boolean built = false;
		private GLPipelineState result = new GLPipelineState();
		public Builder() {}
		
		private void assertMutable() {
			if (built) throw new IllegalStateException("Can't modify a pipeline state once it's built!");
		}
		
		public Builder enable(GLFeatureToggle feature) {
			assertMutable();
			result.enabled.add(feature);
			return this;
		}
		
		public Builder enable(GLFeatureToggle... features) {
			assertMutable();
			for (GLFeatureToggle feature : features) result.enabled.add(feature);
			return this;
		}
		
		public Builder disable(GLFeatureToggle feature) {
			assertMutable();
			result.enabled.remove(feature);
			return this;
		}
		
		public Builder disable(GLFeatureToggle... features) {
			assertMutable();
			for (GLFeatureToggle feature : features) result.enabled.remove(feature);
			return this;
		}
		
		public Builder set(GLBoolean feature, boolean bool) {
			assertMutable();
			if (bool) {
				result.trueBools.add(feature);
			} else {
				result.trueBools.remove(feature);
			}
			return this;
		}
		
		public GLPipelineState build() {
			built = true;
			return result;
		}
	}
}

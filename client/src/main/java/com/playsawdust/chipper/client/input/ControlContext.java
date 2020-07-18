/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.input;

import java.util.ArrayList;

public class ControlContext implements Control {
	protected String localizationKey;
	protected ArrayList<Control> controls = new ArrayList<>();
	
	public ControlContext(String localizationKey) {
		this.localizationKey = localizationKey;
	}
	
	public ControlContext add(Control... controls) {
		for(Control c : controls) this.controls.add(c);
		return this;
	}
	
	public void offerKey(Key key, boolean pressed) {
		for(Control c : controls) c.offerKey(key, pressed);
	}
	
	public void offerClick(int button, boolean pressed) {
		for(Control c : controls) c.offerClick(button, pressed);
	}
}

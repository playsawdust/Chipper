/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.input;

import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;

public class PressControl implements Control {
	private boolean lock = false;
	private Runnable keyTyped;
	private Runnable keyPressed;
	
	private Int2BooleanMap keyStates = new Int2BooleanOpenHashMap();
	private Int2BooleanMap mouseStates = new Int2BooleanOpenHashMap();
	
	public PressControl(int... keyCodes) {
		for(int i : keyCodes) this.keyStates.put(i, false);
	}
	
	public PressControl bindKeys(int... keyCodes) {
		for(int i : keyCodes) this.keyStates.put(i, false);
		return this;
	}
	
	public PressControl bindLeftClick() {
		mouseStates.put(0, false); //GLFW_MOUSE_BUTTON_1
		return this;
	}
	
	public PressControl bindRightClick() {
		mouseStates.put(1, false); //GLFW_MOUSE_BUTTON_2
		return this;
	}
	
	public PressControl bindMouseButtons(int... mouseButtons) {
		for(int i: mouseButtons) this.mouseStates.put(i, false);
		return this;
	}
	
	/**
	 * Checks all the binds for this Control, firing events as needed.
	 * @return true if a "keyTyped" event has fired this poll.
	 */
	public boolean fireEvents() {
		boolean anyActive = isPressed();
		
		if (anyActive && keyPressed!=null) keyPressed.run();
		
		if (lock) {
			if (!anyActive) lock = false;
		} else {
			if (anyActive) {
				if (keyTyped!=null) keyTyped.run();
				lock = true;
				return true;
			}
		}
		
		return false;
	}
	
	public void offerKey(Key key, boolean pressed) {
		if (keyStates.containsKey(key.getGlfwKeyCode())) {
			keyStates.put(key.getGlfwKeyCode(), pressed);
		}
	}
	
	public void offerClick(int button, boolean pressed) {
		if (mouseStates.containsKey(button)) {
			mouseStates.put(button, pressed);
		}
		
	}
	
	/**
	 * Checks all the binds for this Control, and returns true if any are active at this moment.
	 * @return
	 */
	public boolean isPressed() {
		for(boolean b : keyStates.values()) {
			if (b) return true;
		}
		for(boolean b : mouseStates.values()) {
			if (b) return true;
		}
		return false;
	}
	
	/**
	 * Sets the "keyTyped" event for this control to run the provided Runnable. Only one Runnable may be registered at
	 * a time; registering another one will replace the previous call.
	 * 
	 * <p>"keyTyped" happens when a key is initially pressed, but will not fire again for that key unless it's released.
	 * All keys in a Control are considered together, so if two keys are pressed together, both must be released to
	 * allow this Control to fire "keyTyped" events again.
	 */
	public void setKeyTypedCallback(Runnable r) {
		keyTyped = r;
	}
	
	/**
	 * Sets the "keyPressed" callback for this control to call the provided Runnable whenever this Control is polled and
	 * any keybind for this Control is found to be active.
	 * @param r
	 */
	public void setKeyPressedCallback(Runnable r) {
		keyPressed = r;
	}

	
}

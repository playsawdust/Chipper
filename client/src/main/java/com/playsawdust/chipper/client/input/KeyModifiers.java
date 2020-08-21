/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.input;

import static org.lwjgl.glfw.GLFW.*;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.playsawdust.chipper.exception.RecycledObjectException;
import com.playsawdust.chipper.toolbox.pool.ObjectPool;
import com.playsawdust.chipper.toolbox.pool.PooledObject;

/**
 * Represents held modifier keys. This includes Control, Shift, Alt, and Super,
 * as well as Num Lock and Caps Lock for convenience.
 */
public class KeyModifiers implements PooledObject {
	public enum KeyModifier {
		SHIFT(GLFW_MOD_SHIFT),
		CONTROL(GLFW_MOD_CONTROL),
		ALT(GLFW_MOD_ALT),
		SUPER(GLFW_MOD_SUPER),
		NUM_LOCK(GLFW_MOD_NUM_LOCK),
		CAPS_LOCK(GLFW_MOD_CAPS_LOCK),
		;
		private final int bit;
		private KeyModifier(int bit) {
			this.bit = bit;
		}
	}

	private static final ObjectPool<KeyModifiers> pool = new ObjectPool<>(KeyModifiers::new);

	private static final Joiner COMMA_JOINER = Joiner.on(',');
	private static final int RECYCLED = 16384;
	private static final int ALL_GLFW_BITS = GLFW_MOD_SHIFT | GLFW_MOD_CONTROL | GLFW_MOD_ALT |
				GLFW_MOD_SUPER | GLFW_MOD_NUM_LOCK | GLFW_MOD_CAPS_LOCK;

	/**
	 * Special singleton object that indicates no held modifiers. Useful for synthetic events.
	 */
	public static final KeyModifiers NONE = new KeyModifiers() {
		@Override
		protected boolean isSet(int bit) {
			return false;
		}
		@Override
		public void recycle() {
		}
		@Override
		public String toString() {
			return "KeyModifiers.NONE";
		}
	};

	private int bits;

	private KeyModifiers() {}

	protected boolean isSet(int bit) {
		return (bits & bit) != 0;
	}

	protected boolean isRecycled() {
		return isSet(RECYCLED);
	}

	/**
	 * @return the number of held modifier keys
	 */
	public int cardinality() {
		return Integer.bitCount(bits);
	}

	/**
	 * @param bit the bit to check
	 * @return {@code true} if the given modifier is held
	 */
	public boolean isHeld(KeyModifier bit) {
		if (isRecycled()) throw new RecycledObjectException(this);
		return isSet(bit.bit);
	}

	/**
	 * @return {@code true} if one or more Shift keys are held
	 */
	public boolean isShiftHeld() {
		if (isRecycled()) throw new RecycledObjectException(this);
		return isSet(GLFW_MOD_SHIFT);
	}

	/**
	 * @return {@code true} if one or more Control (aka Ctrl) keys are held
	 */
	public boolean isControlHeld() {
		if (isRecycled()) throw new RecycledObjectException(this);
		return isSet(GLFW_MOD_CONTROL);
	}

	/**
	 * @return {@code true} if one or more Alt (aka Option) keys are held
	 */
	public boolean isAltHeld() {
		if (isRecycled()) throw new RecycledObjectException(this);
		return isSet(GLFW_MOD_ALT);
	}

	/**
	 * @return {@code true} if one or more Super (aka Windows, Meta, Command) keys are held
	 */
	public boolean isSuperHeld() {
		if (isRecycled()) throw new RecycledObjectException(this);
		return isSet(GLFW_MOD_SUPER);
	}

	/**
	 * @return {@code true} if Num Lock is enabled
	 */
	public boolean isNumLockEnabled() {
		if (isRecycled()) throw new RecycledObjectException(this);
		return isSet(GLFW_MOD_NUM_LOCK);
	}

	/**
	 * @return {@code true} if Caps Lock is enabled
	 */
	public boolean isCapsLockEnabled() {
		if (isRecycled()) throw new RecycledObjectException(this);
		return isSet(GLFW_MOD_CAPS_LOCK);
	}

	public int toGlfwModifiers() {
		if (isRecycled()) throw new RecycledObjectException(this);
		return bits;
	}



	@Override
	public String toString() {
		if (isRecycled()) return "KeyModifiers[RECYCLED]";
		List<String> li = Lists.newArrayList();
		if (isShiftHeld()) li.add("shift");
		if (isControlHeld()) li.add("control");
		if (isAltHeld()) li.add("alt");
		if (isSuperHeld()) li.add("super");
		if (isNumLockEnabled()) li.add("numlock");
		if (isCapsLockEnabled()) li.add("capslock");
		return "KeyModifiers["+COMMA_JOINER.join(li)+"]";
	}

	@Override
	public void recycle() {
		bits = RECYCLED;
		pool.recycle(this);
	}


	public static KeyModifiers fromGlfw(int mods) {
		KeyModifiers b = pool.get();
		b.bits = mods & ALL_GLFW_BITS;
		return b;
	}

	public static KeyModifiers of(KeyModifier... bits) {
		KeyModifiers b = pool.get();
		b.bits = 0;
		for (KeyModifier bb : bits) {
			b.bits |= bb.bit;
		}
		return b;
	}
}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.component;

/**
 * Represents some kind of component that maintains game state that needs to be shared throughout
 * the engine. Instances of Components are managed by {@link Context}.
 * <p>
 * All Component implementations should offer a static {@code obtain} method that accepts an
 * Engine of the correct type parameter and calls {@link Context#getComponent} on the behalf of the
 * caller. For example:
 * <pre><code>
 * public class MyComponent implements Component {
 *     private MyComponent(WhiteLotus lotus) {
 *         WhiteLotus.verify(lotus);
 *     }
 *
 *     &#64;Override
 *     public boolean compatibleWith(Engine engine) {
 *         return engine instanceof ClientEngine;
 *     }
 *
 *     public static MyComponent obtain(Context<? extends ClientEngine> ctx) {
 *         return ctx.getComponent(MyComponent.class);
 *     }
 * }
 * </code></pre>
 */
public interface Component {

	/**
	 * Check if this Component is compatible with the given Engine.
	 * <p>
	 * Client-only components will generally want to override this with
	 * {@code return engine instanceof ClientEngine}. More generally, this method should check the
	 * same constraints imposed by the generics on your {@code obtain} method, to ensure bare calls
	 * to {@link Context#getComponent} are correct.
	 * @param engine the engine to test
	 * @return {@code true} if this Component is compatible with the given Engine
	 */
	boolean compatibleWith(Engine engine);

}

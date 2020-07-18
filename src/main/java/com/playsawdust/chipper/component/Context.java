/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.playsawdust.chipper.Addon;
import com.playsawdust.chipper.client.ClientEngine;

import com.playsawdust.chipper.qual.unification.ClientOnly;
import com.playsawdust.chipper.qual.unification.ServerOnly;
import com.playsawdust.chipper.qual.unification.Unified;
import com.playsawdust.chipper.server.ServerEngine;

/**
 * Manages holding and intializing engine components.
 */
/*
 * "Oh god, really? A Context!? What is this, Android!?"
 * Not quite. Android's Context is a "god object" - i.e. it is exceedingly large and has too many
 * responsibilities. Chipper's Context is the exact opposite.
 *
 * One of the rules of Chipper's design is "no global state". However, virtually everything has
 * some level of shared state that it needs, and passing around all the individual objects you need
 * is a chore and introduces an absurd amount of boilerplate. Previously, my solution to this
 * problem would be static classes or singletons, but global state makes the codebase inflexible.
 * This Context system means we can run multiple clients in one process, run a server and client
 * together, etc, without needing any special code to keep things separate. It allows Chipper
 * processes to have an arbitrary topology.
 *
 * A "service locator", as this pattern is called, is used instead of "dependency injection", as
 * dependency injection is magic. Where your objects come from after using an @Inject is unclear and
 * intentionally obscured. Another of Chipper's design rules is "no magic". The codebase should be
 * easily explorable by anyone with moderate knowledge of Java, not expert knowledge. And, even as
 * an expert, excessive reflection is simply bad. It's confusing and fragile.
 *
 * My particular implementation of a service locator feels more like static classes, due to the
 * inverse usage of an "obtain" static and the discouragement of using getComponent directly.
 *
 * I think this is a good solution, but of course, you may disagree.
 *
 * -- Una
 */
public final class Context<E extends Engine> {
	/**
	 * <i>Who knocks at the guarded gate?</i><br/>
	 * <i>One who has eaten the fruit and tasted its mysteries.</i>
	 * <p>
	 * Prevents creation of Components except by Context. (For this to work,
	 * you must call WhiteLotus.verify in your constructor.)
	 */
	public static final class WhiteLotus {
		private static final WhiteLotus instance = new WhiteLotus();
		private WhiteLotus() {}

		public static void verify(WhiteLotus lotus) {
			if (lotus == null) {
				throw new IllegalArgumentException("Attempt to initialize Component with null lotus");
			}
			if (lotus != instance) {
				throw new IllegalArgumentException("Attempt to initialize Component with invalid lotus");
			}
		}
	}

	private static final Logger log = LoggerFactory.getLogger(Context.class);

	private final E engine;
	private final Map<Class<? extends Component>, Component> cache = Maps.newHashMap();

	private Context(E engine) {
		this.engine = engine;
	}

	/**
	 * Retrieve the engine object. Usually only needed by internal code.
	 * <p>
	 * On clients, this is ClientEngine. On servers, this is ServerEngine.
	 * @see #getEngineType()
	 */
	public E getEngine() {
		return engine;
	}

	/**
	 * @return the type of this Context's Engine, useful for guarding component access
	 */
	public EngineType getEngineType() {
		return getEngine().getType();
	}

	/**
	 * Search for a loaded addon of the given type, and return it if it exists, otherwise
	 * {@code null}.
	 * @param clazz the type to search for
	 * @return the found addon, or null if none exists
	 */
	public <T extends Addon> @Nullable T peekAddon(Class<T> clazz) {
		// TODO use an addon loader system
		Addon a = engine.getDefaultAddon();
		if (clazz.isInstance(a)) {
			return (T)a;
		}
		return null;
	}

	/**
	 * Search for a loaded addon of the given type, and return it if it exists, otherwise throw an
	 * exception.
	 * @param clazz the type to search for
	 * @return the found addon
	 * @throws IllegalArgumentException if no addon of the given class is loaded
	 */
	public <T extends Addon> @NonNull T getAddon(Class<T> clazz) {
		T a = peekAddon(clazz);
		if (a == null) throw new IllegalArgumentException("No addon with class "+clazz+" is loaded");
		return a;
	}

	/**
	 * Safely cast this Context to one containing a ClientEngine.
	 * <p>
	 * Should always be guarded; this will only be needed in {@link Unified unified} methods that
	 * accept a {@code Context<?>}. For example:
	 * <pre>
	 * {@code @Unified}
	 * public void doTheThing(Context<?> ctx) {
	 *     if (ctx.getEngineType().isClient()) {
	 *         Context&lt;ClientEngine> clientCtx = ctx.asClientContext();
	 *         // initialize client-only resources, do OpenGL stuff, ...
	 *     } else {
	 *         Context&lt;ServerEngine> serverCtx = ctx.asServerContext();
	 *         // do other stuff
	 *     }
	 * }
	 * </pre>
	 * @return this Context as a a client context
	 * @throws ClassCastException if this Context is the wrong type
	 * @see #asServerContext()
	 * @see #getEngineType()
	 */
	@ClientOnly
	public Context<ClientEngine> asClientContext() {
		if (engine instanceof ClientEngine) {
			return (Context<ClientEngine>)this;
		}
		throw new ClassCastException("Cannot cast Context<"+engine.getClass().getName()+"> to Context<ClientEngine>");
	}

	/**
	 * Safely cast this Context to one containing a ServerEngine.
	 * <p>
	 * Should always be guarded; this will only be needed in {@link Unified unified} methods that
	 * accept a {@code Context<?>}. For example:
	 * <pre>
	 * {@code @Unified}
	 * public void doTheThing(Context<?> ctx) {
	 *     if (ctx.getEngineType().isServer()) {
	 *         Context&lt;ServerEngine> serverCtx = ctx.asServerContext();
	 *         // initialize game logic objects, load data files, ...
	 *     } else {
	 *         Context&lt;ClientEngine> clientCtx = ctx.asClientContext();
	 *         // do other stuff
	 *     }
	 * }
	 * </pre>
	 * @return this Context as a a server context
	 * @throws ClassCastException if this Context is the wrong type
	 * @see #asClientContext()
	 * @see #getEngineType()
	 */
	@ServerOnly
	public Context<ServerEngine> asServerContext() {
		if (engine instanceof ServerEngine) {
			return (Context<ServerEngine>)this;
		}
		throw new ClassCastException("Cannot cast Context<"+engine.getClass().getName()+"> to Context<ServerEngine>");
	}

	/**
	 * Retrieve a shared instance of the given component.
	 * <p>
	 * Generally, you should not call this method directly. Components should offer a static
	 * {@code obtain} method that takes a Context of the appropriate type for safety.
	 * @param clazz the class of the component to retrieve
	 */
	@SuppressWarnings("unchecked") // cast is safe
	public <T extends Component> T getComponent(Class<T> clazz) {
		return (T)cache.computeIfAbsent(clazz, this::createComponent);
	}

	private <T extends Component> T createComponent(Class<T> clazz) {
		try {
			boolean explain = false;
			Constructor<T> cons;
			try {
				cons = clazz.getDeclaredConstructor();
				log.warn("Component {} has a no-args constructor; it should instead accept a WhiteLotus and call WhiteLotus.verify to prevent invalid instantiation", clazz);
				explain = true;
			} catch (NoSuchMethodException e) {
				cons = clazz.getDeclaredConstructor(WhiteLotus.class);
			}
			if (Modifier.isPublic(cons.getModifiers())) {
				log.warn("Component {} has a public constructor; Context has reflection permission and will use it to call any constructors", clazz);
				explain = true;
			}
			if (explain) {
				log.warn("A proper constructor for this Component would look like this:\n"
						+ "private {}(WhiteLotus lotus) {\n"
						+ "    WhiteLotus.verify(lotus);\n"
						+ "}", clazz.getSimpleName().substring(clazz.getSimpleName().indexOf('$')+1));
			}
			cons.setAccessible(true);
			T inst = cons.getParameterCount() == 0 ? cons.newInstance() : cons.newInstance(WhiteLotus.instance);
			if (!inst.compatibleWith(engine)) {
				String likelyIntendedMethod = getEngineType().isServer() ? "Client" : "Server";
				throw new IllegalArgumentException("Component "+clazz.getName()+" is not compatible with "+engine.getClass().getName()+" - did you forget a ctx.getEngineType().is"+likelyIntendedMethod+"() check?");
			}
			return inst;
		} catch (NoSuchMethodException e) {
			String reason = "could not find a no-args or lotus constructor";
			if (clazz.isMemberClass()) {
				Class<?> dec = clazz.getDeclaringClass();
				try {
					clazz.getConstructor(dec);
					reason = "it is an inner class and isn't static";
				} catch (Throwable t) {}
			} else if (clazz.isAnonymousClass()) {
				reason = "it is an anonymous class";
			}
			log.warn("Failed to instantiate component {} - {}", clazz, reason);
			return null;
		} catch (InstantiationException e) {
			log.warn("Failed to instantiate component {} - it is an abstract class", clazz);
			return null;
		} catch (InvocationTargetException e) {
			log.warn("Failed to instantiate component {} - an exception was thrown by the constructor", clazz, e.getCause());
			return null;
		} catch (IllegalAccessException e) {
			throw new AssertionError("Illegal access should be impossible", e);
		} catch (IllegalArgumentException e) {
			throw new AssertionError("Illegal argument should be impossible", e);
		}
	}


	/**
	 * Obtain a brand-new Context, with an independent Component cache.
	 * @deprecated You probably don't want to call this method; it is a factory
	 * 		method used during engine initialization to create a global shared
	 * 		Context, and using it outside of engine initialization probably won't
	 * 		do what you intend.
	 * @param engine the engine object to use (usually ClientEngine or ServerEngine, or a subclass thereof)
	 * @return a new Context
	 */
	@Deprecated
	public static <E extends Engine> Context<E> createNew(E engine) {
		return new Context<>(engine);
	}


}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.exception.ProtocolViolationException;
import com.playsawdust.chipper.network.Message.Direction;
import com.playsawdust.chipper.network.protocol.base.message.GoodbyeMessage;
import com.playsawdust.chipper.network.protocol.base.message.WelcomeMessage;

import com.playsawdust.chipper.toolbox.lipstick.SharedRandom;

/**
 * Represents a mode that a Connection can be in, dictating what kinds of Messages can be sent and
 * received. Extend this class and call {@link #register} in your constructor for every valid
 * message. Exposing your Protocol subclass instance can allow other addons to extend your Protocol.
 * <p>
 * If you do not want your protocol to be extensible, call {@link #freeze} at the end of your
 * constructor.
 * <p>
 * Protocols must be registered with the {@link ProtocolRegistry}.
 */
public abstract class Protocol {

	private final MRegistry registryClientbound = new MRegistry();
	private final MRegistry registryServerbound = new MRegistry();
	private final MRegistry registryBidirectional = new MRegistry();

	private boolean frozen = false;

	public Protocol() {
		register(GoodbyeMessage::new);
	}

	private MRegistry registryForDirection(Direction dir) {
		switch (dir) {
			case CLIENTBOUND: return registryClientbound;
			case SERVERBOUND: return registryServerbound;
			case BIDIRECTIONAL: return registryBidirectional;
			default: throw new AssertionError("Missing case for "+dir);
		}
	}

	/**
	 * Retrieve the identifier of this protocol's "start message". Most protocols have one, which
	 * defines which message must be sent after {@link WelcomeMessage} to switch to it, as well as
	 * what message is advertised in WelcomeMessage to show support for this protocol.
	 * <p>
	 * If this Protocol must be switched into explicitly through some other means, such as a custom
	 * handshake, you may return {@code null} to opt out of the Chipper Base Protocol handshake.
	 * @return the identifier of this protocol's "start message"
	 */
	public abstract @Nullable Identifier getStartMessage();

	/**
	 * "Freeze" this Protocol, preventing further messages from being registered.
	 */
	protected final void freeze() {
		frozen = true;
	}

	/**
	 * Register a new Message with this Protocol. The constructor will be immediately called to
	 * create a dummy object to call various methods on to put it in the right registry.
	 * @param constructor the constructor of the Message to register, such as {@code HelloMessage::new}
	 * @throws IllegalStateException if this Protocol has been {@link #freeze frozen}
	 */
	public final <T extends Message> void register(Supplier<T> constructor) {
		if (frozen) throw new IllegalStateException("Protocol is frozen");
		T m = constructor.get();
		if (m == null) throw new IllegalArgumentException("Message constructor cannot return null");
		Class<T> clazz = (Class<T>)m.getClass();
		Direction dir = m.getDirection();
		if (dir == null) throw new IllegalArgumentException("Message.getDirection cannot return null");
		registryForDirection(dir).put(m.getId(), new RegisteredMessage<>(clazz, constructor));
	}

	// we use iterables for convenience
	// pre-defined for efficiency (varargs allocates an array)
	private final List<MRegistry> registriesServer = ImmutableList.of(registryServerbound, registryBidirectional);
	private final List<MRegistry> registriesClient = ImmutableList.of(registryClientbound, registryBidirectional);
	private final List<MRegistry> registries = ImmutableList.<MRegistry>builder()
			// randomize to make bugs related to relying on createIndiscrimiately's undefined behavior more obvious
			.addAll(SharedRandom.shuffle(Arrays.asList(registryServerbound, registryClientbound)))
			.add(registryBidirectional)
			.build();

	/**
	 * Create a new Message instance from the given identifier, checking for serverbound or
	 * bidirectional messages, in that order. If none is found, an exception is thrown.
	 * @param id the identifier of the message to construct
	 * @return a newly constructed message
	 * @throws ProtocolViolationException if there is no message with this id that can be received on
	 * 		the server
	 */
	public final Message createForServer(Identifier id) {
		return create(registriesServer, id);
	}

	/**
	 * Create a new Message instance from the given identifier, checking for clientbound or
	 * bidirectional messages, in that order. If none is found, an exception is thrown.
	 * @param id the identifier of the message to construct
	 * @return a newly constructed message
	 * @throws ProtocolViolationException if there is no message with this id that can be received on
	 * 		the client
	 */
	public final Message createForClient(Identifier id) {
		return create(registriesClient, id);
	}


	/**
	 * Create a new Message instance from the given identifier, checking for any kind of message, no
	 * matter its intended direction. If there are serverbound and clientbound messages with the same
	 * identifier, which one is returned is undefined. <i>Generally, this method is not useful and
	 * should be avoided.</i> If no message is found, an exception is thrown.
	 * @param id the identifier of the message to construct
	 * @return a newly constructed message
	 * @throws ProtocolViolationException if there is no message with this id
	 */
	public final Message createIndiscriminately(Identifier id) {
		return create(registries, id);
	}

	private Message create(Iterable<MRegistry> registries, Identifier id) {
		for (MRegistry reg : registries) {
			RegisteredMessage<?> r = reg.get(id);
			if (r != null) {
				Message m = r.constructor.get();
				if (m == null) {
					throw new RuntimeException("Constructor for Message type "+r.clazz+" returned null! This is not allowed!");
				}
				return m;
			}
		}
		throw new ProtocolViolationException("Found no registered messages for id "+id);
	}

	// re-typing this generic over and over is DRIVING ME NUTS.
	// java doesn't have typedefs but this is CLOSE ENOUGH.
	private static final class MRegistry extends HashMap<Identifier, RegisteredMessage<?>> {
		private static final long serialVersionUID = 306504947559840962L;
	}

	private static class RegisteredMessage<T extends Message> {
		public final Class<T> clazz;
		public final Supplier<T> constructor;
		private RegisteredMessage(Class<T> clazz, Supplier<T> constructor) {
			this.clazz = clazz;
			this.constructor = constructor;
		}
	}

}

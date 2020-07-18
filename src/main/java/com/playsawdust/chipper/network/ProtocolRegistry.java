/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network;

import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.collect.unmodifiable.Unmodifiable;
import com.playsawdust.chipper.collect.unmodifiable.UnmodifiableList;
import com.playsawdust.chipper.collect.unmodifiable.UnmodifiableSet;
import com.playsawdust.chipper.component.Component;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.component.Context.WhiteLotus;
import com.playsawdust.chipper.network.protocol.base.BaseProtocol;

public class ProtocolRegistry implements Component {

	private ProtocolRegistry(WhiteLotus lotus) {
		WhiteLotus.verify(lotus);
	}

	private final List<Protocol> protocols = Lists.newArrayList(new BaseProtocol());
	private final UnmodifiableList<Protocol> protocolsUnmodifiable = Unmodifiable.list(protocols);

	private final Map<Identifier, Protocol> protocolsByStartMessage = Maps.newHashMap();
	private final Map<Class<? extends Protocol>, Protocol> protocolsByClass = Maps.newHashMap();
	private final UnmodifiableSet<Identifier> protocolsByStartMessageKeysUnmodifiable = Unmodifiable.set(protocolsByStartMessage.keySet());

	/**
	 * Register a new Protocol with this ProtocolRegistry, making it available for use for
	 * Connections.
	 */
	public void register(Protocol protocol) {
		Class<? extends Protocol> clazz = protocol.getClass();
		if (protocolsByClass.containsKey(clazz)) {
			throw new IllegalArgumentException("A protocol of class "+clazz+" has already been registered");
		}
		Identifier start = protocol.getStartMessage();
		if (start != null) {
			if (protocolsByStartMessage.containsKey(start)) {
				throw new IllegalArgumentException("A protocol with a start message of "+start+" has already been registered");
			}
			protocolsByStartMessage.put(start, protocol);
		}
		protocols.add(protocol);
		protocolsByClass.put(clazz, protocol);
	}

	/**
	 * @return the instance of BaseProtocol constructed when this ProtocolRegistry was
	 */
	public BaseProtocol getBaseProtocol() {
		return (BaseProtocol)protocols.get(0);
	}

	/**
	 * @return a list of every registered protocol
	 */
	public UnmodifiableList<Protocol> getProtocols() {
		return protocolsUnmodifiable;
	}

	/**
	 * @param id the identifier of the start message
	 * @return a protocol with that start message, or null if none exists
	 */
	public @Nullable Protocol getProtocolByStartMessage(Identifier id) {
		return protocolsByStartMessage.get(id);
	}

	/**
	 * @param clazz the class of the protocol to look up
	 * @return the instance of that protocol class held by this registry, or null if it hasn't been
	 * 		registered
	 */
	public <T extends Protocol> @Nullable T getProtocolByClass(Class<T> clazz) {
		return (T)protocolsByClass.get(clazz);
	}

	/**
	 * @return a set of all valid start messages for each registered protocol
	 */
	public UnmodifiableSet<Identifier> getStartMessages() {
		return protocolsByStartMessageKeysUnmodifiable;
	}

	@Override
	public boolean compatibleWith(Engine engine) {
		return true;
	}

	public static ProtocolRegistry obtain(Context<?> ctx) {
		return ctx.getComponent(ProtocolRegistry.class);
	}

}

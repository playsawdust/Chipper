/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network.protocol.base.message;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.network.Connection;
import com.playsawdust.chipper.network.Marshaller;
import com.playsawdust.chipper.network.Message.ClientboundMessage;
import com.playsawdust.chipper.network.Packet;
import com.playsawdust.chipper.network.Unmarshaller;

import com.playsawdust.chipper.client.ClientEngine;

import com.playsawdust.chipper.toolbox.lipstick.SharedRandom;

/**
 * The WelcomeMessage is the second message sent on a Chipper connection, from the server to the
 * client. It informs the client of all valid next messages that can be sent to switch the
 * connection into a more useful mode than Base Protocol, as Base Protocol can do nothing other
 * than negotiate these next modes.
 * <p>
 * Once it is received, the client looks for a message type it understands, and then sends that to
 * initiate the handshake for that next protocol. After this, the only Chipper Base Protocol
 * message that will be used is {@link GoodbyeMessage}; all other messages are completely out of
 * the jurisdiction of the engine, other than the {@link Packet message encapsulation format}. If
 * the client does not find any message types it understands, or only finds message types it does
 * not want to send, it will disconnect with the reason "chipper:not_interested".
 * <p>
 * A commonly available choice in WelcomeMessage is "chipper:ping", a simple similarly barebones
 * protocol for doing pings. <i>However, Chipper-based games are not required to support the ping
 * protocol</i>, it is considered an extension like any other.
 * <p>
 * <b>This packet is part of the <i>Chipper Base Protocol</i></b>. Its wire format is frozen and
 * will never be changed.
 */
public class WelcomeMessage extends ClientboundMessage {

	/**
	 * The set of identifiers of all valid next messages, other than "chipper:goodbye". On the wire,
	 * this set is shuffled to reduce the likelihood of a protocol consumer accidentally relying on
	 * some form of sorting.
	 */
	public final Set<Identifier> choices = Sets.newHashSet();

	public WelcomeMessage() {
		this(Collections.emptySet());
	}

	public WelcomeMessage(Collection<Identifier> choices) {
		super(new Identifier("chipper", "welcome"));
		this.choices.addAll(choices);
	}

	@Override
	public void marshal(Marshaller out) {
		out.writeIVar32(choices.size());
		// write values in a random order to ensure the other side actually speaks our protocol and
		// won't break if more choices are introduced
		for (Identifier id : SharedRandom.shuffleCopy(choices)) {
			out.writeIdentifier(id);
		}
	}

	@Override
	public void unmarshal(Unmarshaller in) {
		int count = in.readIVar32();
		choices.clear();
		for (int i = 0; i < count; i++) {
			choices.add(in.readIdentifier());
		}
	}

	@Override
	protected void processClient(Context<ClientEngine> ctx, Connection c) {
		c.goodbye(new Identifier("chipper", "not_interested"));
	}

	@Override
	public String toString() {
		return "WelcomeMessage[choices="+choices+"]";
	}

}

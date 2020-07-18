/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network.protocol.base.message;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.network.Connection;
import com.playsawdust.chipper.network.Marshaller;
import com.playsawdust.chipper.network.ProtocolRegistry;
import com.playsawdust.chipper.network.Unmarshaller;
import com.playsawdust.chipper.network.Message.ServerboundMessage;
import com.playsawdust.chipper.server.ServerEngine;

/**
 * The HelloMessage is the first message sent on a Chipper connection, from the client to the
 * server.
 * <p>
 * Once it is received, the "chipper:hello_received" flag is set on the connection. If another hello
 * is received while this flag is set, the connection is dropped for the reason
 * "chipper:duplicate_hello".
 * <p>
 * The server immediately responds with a {@link WelcomeMessage}.
 * <p>
 * <b>This packet is part of the <i>Chipper Base Protocol</i></b>. Its wire format is frozen and
 * will never be changed.
 */
public class HelloMessage extends ServerboundMessage {

	/**
	 * The "correlation id" for this connection, used to associate UDP packets with TCP connections.
	 */
	public int correlationId;

	public HelloMessage() {
		this(0);
	}

	public HelloMessage(int correlationId) {
		super(new Identifier("chipper", "hello"));
		this.correlationId = correlationId;
	}

	@Override
	public void marshal(Marshaller out) {
		out.writeI32(correlationId);
	}

	@Override
	public void unmarshal(Unmarshaller in) {
		this.correlationId = in.readI32();
	}

	@Override
	protected void processServer(Context<ServerEngine> ctx, Connection c) {
		if (c.hasFlag(new Identifier("chipper", "hello_received"))) {
			c.goodbye(new Identifier("chipper", "duplicate_hello"));
		} else {
			c.setFlag(new Identifier("chipper", "hello_received"));
			c.sendMessage(new WelcomeMessage(ProtocolRegistry.obtain(ctx).getStartMessages()));
		}
	}

	@Override
	public String toString() {
		return "HelloMessage[correlationId="+correlationId+"]";
	}

}

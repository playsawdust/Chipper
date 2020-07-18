/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network.protocol.base.message;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.network.Connection;
import com.playsawdust.chipper.network.Marshaller;
import com.playsawdust.chipper.network.Message;
import com.playsawdust.chipper.network.Unmarshaller;
import com.playsawdust.chipper.server.ServerEngine;

/**
 * The GoodbyeMessage can be sent on a connection at any time, and it indicates the other side is no
 * longer interested in continuing this connection for any reason. The reason is elaborated upon as
 * an Identifier.
 * <p>
 * Once this message is received, the connection will be closed. If a GoodbyeMessage is sent by a
 * side and the connection is still open after 5 seconds, it will be forcibly closed.
 * <p>
 * GoodbyeMessages should never be constructed or sent directly; use {@link Connection#goodbye} to
 * perform all the needed bookkeeping and properly switch the connection state.
 * <p>
 * <b>This packet is part of the <i>Chipper Base Protocol</i></b>. Its wire format is frozen and
 * will never be changed.
 */
public class GoodbyeMessage extends Message {

	/**
	 * An arbitrary Identifier explaining why the connection was dropped, such as
	 * "chipper:not_interested".
	 */
	public Identifier reason;
	/**
	 * A list of extra strings with arbitrary meaning depending on the reason. May be used as
	 * formatting parameters for a localized string derived from the reason identifier. Order is
	 * preserved.
	 */
	public final List<String> extra = Lists.newArrayList();

	public GoodbyeMessage() {
		super(new Identifier("chipper", "goodbye"));
	}

	public GoodbyeMessage(Identifier reason, Iterable<String> extra) {
		super(new Identifier("chipper", "goodbye"));
		Preconditions.checkNotNull(reason);
		this.reason = reason;
		for (String s : extra) {
			this.extra.add(s);
		}
	}

	@Override
	public void marshal(Marshaller out) {
		out.writeIdentifier(reason);
		out.writeIVar32(extra.size());
		for (String s : extra) {
			out.writeString(s);
		}
	}

	@Override
	public void unmarshal(Unmarshaller in) {
		reason = in.readIdentifier();
		extra.clear();
		int size = in.readIVar32();
		for (int i = 0; i < size; i++) {
			extra.add(in.readString());
		}
	}

	@Override
	protected void processClient(Context<ClientEngine> ctx, Connection c) {
		c.onRemoteGoodbye(reason, extra);
		c.disconnect();
	}

	@Override
	protected void processServer(Context<ServerEngine> ctx, Connection c) {
		c.onRemoteGoodbye(reason, extra);
		c.disconnect();
	}

	@Override
	public String toString() {
		return "GoodbyeMessage[reason="+reason+",extra="+extra+"]";
	}

}

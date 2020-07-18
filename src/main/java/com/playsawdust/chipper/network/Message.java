/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.qual.unification.ClientOnly;
import com.playsawdust.chipper.qual.unification.ServerOnly;

import com.playsawdust.chipper.server.ServerEngine;

/**
 * Represents a logical Message that can be sent to the other side through various
 * transports, as a {@link Packet}.
 */
public abstract class Message implements Marshallable {

	public enum Direction {
		CLIENTBOUND,
		SERVERBOUND,
		BIDIRECTIONAL,
		;
	}

	public enum SendMode {
		/**
		 * Send this Message over the reliable TCP channel. It may take longer
		 * to send than {@link #UNRELIABLE}, but it is guaranteed to be received
		 * eventually, and it will be delivered in order.
		 * <p>
		 * Good for low-frequency or medium-frequency packets that need to make
		 * it to the other side, such as chat messages and connection state
		 * changes.
		 */
		RELIABLE,
		/**
		 * Send this Message over the unreliable UDP channel, if it is available.
		 * It will be sent as fast as possible, but it may never be received
		 * depending on numerous factors, and if it is received it may be
		 * out-of-order.
		 * <p>
		 * If UDP cannot be used for any reason, it will be sent over the TCP
		 * channel and behave like {@link #RELIABLE}. If this message would
		 * clog the TCP channel and should be completely ignored if UDP is
		 * unavailable, use {@link #UNIMPORTANT}.
		 * <p>
		 * Good for medium-high-frequency packets that can be missed with no
		 * lasting ill effects, but are still important enough to warrant being
		 * sent over the TCP fallback, such as some movement updates, effects,
		 * and voice packets.
		 */
		UNRELIABLE,
		/**
		 * Send this Message over the unreliable UDP channel, if it is available.
		 * If UDP is not available, the Message is silently dropped.
		 * <p>
		 * Good for high-frequency packets that can be missed with no ill
		 * effects, such as most movement updates and unimportant effects.
		 */
		UNIMPORTANT,
		;
	}

	protected final Identifier id;

	public Message(Identifier id) {
		this.id = id;
	}

	public final Identifier getId() {
		return id;
	}

	/**
	 * Write any data this Message contains into the given Marshaller.
	 * @param out the Marshaller to write to
	 */
	@Override
	public abstract void marshal(Marshaller out);
	/**
	 * Read any data this Message needs from the given Unmarshaller.
	 * @param in the Unmarshaller to read from
	 */
	@Override
	public abstract void unmarshal(Unmarshaller in);

	public Direction getDirection() {
		return Direction.BIDIRECTIONAL;
	}

	public SendMode getSendMode() {
		return SendMode.RELIABLE;
	}

	@ClientOnly
	protected abstract void processClient(Context<ClientEngine> ctx, Connection c);
	@ServerOnly
	protected abstract void processServer(Context<ServerEngine> ctx, Connection c);

	public final void process(Context<?> ctx, Connection c) {
		if (ctx.getEngineType().isClient()) {
			processClient(ctx.asClientContext(), c);
		} else if (ctx.getEngineType().isServer()) {
			processServer(ctx.asServerContext(), c);
		} else {
			throw new IllegalArgumentException("Context is neither client or server?");
		}
	}

	@Override
	public abstract String toString();

	public static abstract class ClientboundMessage extends Message {
		public ClientboundMessage(Identifier id) {
			super(id);
		}

		@Override
		protected final void processServer(Context<ServerEngine> ctx, Connection c) {
			throw new AssertionError();
		}

		@Override
		public final Direction getDirection() {
			return Direction.CLIENTBOUND;
		}
	}

	public static abstract class ServerboundMessage extends Message {
		public ServerboundMessage(Identifier id) {
			super(id);
		}

		@Override
		protected final void processClient(Context<ClientEngine> ctx, Connection c) {
			throw new AssertionError();
		}

		@Override
		public final Direction getDirection() {
			return Direction.SERVERBOUND;
		}
	}


}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.toolbox.lipstick.BraceFormatter;

/**
 * Represents a low-level Packet, as sent or received on the wire.
 */
public class Packet implements Marshallable {

	/**
	 * The "short" ID for this packet. Dynamically assigned per-connection. 0 means there is no
	 * short ID, as packets with the given long ID have not been sent enough times for it to be
	 * worth allocating a short ID.
	 */
	public int shortId;
	/**
	 * The "long" ID for this packet. Stays the same for the same Messages between connections. Null
	 * when a short ID is already known by the receiving side.
	 */
	public @Nullable Identifier longId;
	/**
	 * This packet's payload.
	 */
	public byte[] payload = null;

	/**
	 * Read in a packet header and payload from the given Unmarshaller, into this Packet object.
	 * @param u the unmarshaller to read from
	 * @throws BufferUnderflowException if there isn't a complete packet available
	 */
	@Override
	public void unmarshal(Unmarshaller u) throws BufferUnderflowException {
		int shortId = u.readIVar32();
		boolean hasLongId = shortId <= 0;
		Identifier longId = null;
		if (hasLongId) {
			longId = u.readIdentifier();
			shortId *= -1;
		}
		byte[] payload = new byte[u.readIVar32()];
		u.read(payload);

		this.shortId = shortId;
		this.longId = longId;
		this.payload = payload;
	}

	/**
	 * Write a packet header and payload to the given Marshaller, from this Packet object.
	 * @param m the marshaller to write to
	 * @throws BufferOverflowException if there isn't enough space in the marshaller for this packet
	 */
	@Override
	public void marshal(Marshaller m) throws BufferOverflowException {
		// store the longId's presence in the sign bit of the shortId to save a byte
		m.writeIVar32(longId != null ? -shortId : shortId);
		if (longId != null) {
			m.writeIdentifier(longId);
		}
		m.writeIVar32(payload.length);
		m.write(payload);
	}

	@Override
	public String toString() {
		return BraceFormatter.format("Packet[shortId={},longId={},payload=<{} bytes>]", shortId, longId, payload.length);
	}

}

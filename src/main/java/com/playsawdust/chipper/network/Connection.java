/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network;

import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.exception.ProtocolViolationException;
import com.playsawdust.chipper.network.Message.SendMode;
import com.playsawdust.chipper.network.protocol.base.message.GoodbyeMessage;

import com.playsawdust.chipper.toolbox.Hexdump;
import com.playsawdust.chipper.toolbox.concurrent.SharedThreadPool;
import com.playsawdust.chipper.toolbox.lipstick.BraceFormatter;
import com.playsawdust.chipper.toolbox.lipstick.MonotonicTime;
import com.playsawdust.chipper.toolbox.lipstick.SharedRandom;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public final class Connection {
	private static final Logger log = LoggerFactory.getLogger(Connection.class);

	public interface Describer {
		@Nullable String describe(Connection c) throws IOException;
	}

	public static final Identifier FLAG_SAID_GOODBYE = new Identifier("chipper", "said_goodbye");

	private final Context<?> ctx;
	private final Runnable writeableNotify;
	private final long epoch = MonotonicTime.nanos();

	private final SocketChannel tcpChannel;

	private int simulatedPacketLossOut = 0;
	private int simulatedPacketLossIn = 0;

	// self-synchronized {
	private final Set<Identifier> flags = Sets.newHashSet();
	private final List<Describer> describers = Lists.newArrayList();
	// }

	// concurrent {
	private final Queue<@NonNull Message> incomingMessages = Queues.newLinkedBlockingDeque();
	private final Queue<@NonNull Message> outgoingMessages = Queues.newLinkedBlockingDeque();
	// }

	// network thread only {
	private ByteBuffer readBuffer = memAlloc(8*1024).order(ByteOrder.BIG_ENDIAN);
	private final ByteBuffer writeBuffer = memAlloc(8*1024).order(ByteOrder.BIG_ENDIAN);
	private final Int2ObjectMap<Identifier> incomingShortIds = new Int2ObjectOpenHashMap<>();
	private int nextShortId = 1;
	private final Set<Identifier> messagesSentBefore = Sets.newHashSet();
	private final Object2IntMap<Identifier> outgoingShortIds = new Object2IntOpenHashMap<>();
	private Protocol currentProtocol;
	// }

	// synchronized (disconnectMutex) {
	private final Object disconnectMutex = new Object();
	private Identifier disconnectReason;
	private ImmutableList<String> disconnectExtra = ImmutableList.of();
	private boolean remoteDisconnected = false;
	// }

	// avoids calling describe, which is potentially expensive, if a log line isn't printed
	private final Object describeFacade = new Object() {
		@Override
		public String toString() {
			return describe();
		}
	};

	public Connection(Context<?> ctx, Runnable writeableNotify, SocketChannel tcpChannel, DatagramChannel udpChannel) {
		this.ctx = ctx;
		this.writeableNotify = writeableNotify;
		this.tcpChannel = tcpChannel;
		currentProtocol = ProtocolRegistry.obtain(ctx).getBaseProtocol();
	}

	public SocketAddress getLocalAddress() throws IOException {
		return tcpChannel.getLocalAddress();
	}

	public SocketAddress getRemoteAddress() throws IOException {
		return tcpChannel.getRemoteAddress();
	}

	public long nanosSinceStart() {
		return MonotonicTime.deltaNanos(epoch);
	}

	/**
	 * Check if a flag with the given identifier is currently set. This can be used to track
	 * arbitrary state information about the protocol, such as whether or not a certain message has
	 * already been received.
	 * @param id the identifier to check
	 * @return {@code true} if this flag has been set
	 * @see #setFlag
	 * @see #clearFlag
	 */
	public boolean hasFlag(Identifier id) {
		synchronized (flags) {
			return flags.contains(id);
		}
	}

	/**
	 * Set a flag with the given identifier on this connection.
	 * @see #hasFlag
	 * @param id the identifier to set
	 */
	public void setFlag(Identifier id) {
		synchronized (flags) {
			flags.add(id);
		}
	}

	/**
	 * Clear a flag with the given identifier on this connection.
	 * @see #hasFlag
	 * @param id the identifier to clear
	 */
	public void clearFlag(Identifier id) {
		synchronized (flags) {
			flags.add(id);
		}
	}

	/**
	 * For testing. If set, every 1 in {@code rate} messages that have a send mode of
	 * {@code UNRELIABLE} or {@code UNIMPORTANT} will be dropped.
	 * <p>
	 * A {@code rate} of 0 turns off packet loss simulation. A {@code rate} of 1 makes every
	 * non-{@code RELIABLE} message be dropped. A {@code rate} of 2 makes approximately 50% of all
	 * non-{@code RELIABLE} messages be dropped, 3 is 33.3333%, etc.
	 * @param outRate the loss rate for outgoing messages
	 * @param inRate the loss rate for incoming messages
	 */
	public void setSimulatedPacketLossRate(int outRate, int inRate) {
		this.simulatedPacketLossOut = outRate;
		this.simulatedPacketLossIn = inRate;
	}

	/**
	 * Queue the given Message for sending to the other side.
	 * @param msg the Message to queue for sending
	 */
	public void sendMessage(Message msg) {
		Preconditions.checkNotNull(msg);
		if (hasFlag(FLAG_SAID_GOODBYE)) {
			log.trace("[{}] Ignoring new message after having already said goodbye", describeFacade);
			return;
		}
		if (simulatedPacketLossOut > 0 && msg.getSendMode() != SendMode.RELIABLE && SharedRandom.chance(simulatedPacketLossOut)) {
			log.debug("[{}] Simulating lost packet for outgoing {}", describeFacade, msg.getId());
			return;
		}
		log.trace("[{}] Queue new message {}", describeFacade, msg);
		outgoingMessages.add(msg);
		writeableNotify.run();
	}

	/**
	 * Send a GoodbyeMessage to the other side, asking them to close the connection, and disable
	 * message processing and sending.
	 * <p>
	 * The connection will be forcibly closed if the other side doesn't close it within 5 seconds.
	 * @param reason an arbitrary Identifier explaining why the connection was dropped, such as
	 * 		"chipper:not_interested".
	 * @param extra an array of extra strings with arbitrary meaning depending on the reason. May
	 * 		be used as formatting parameters for a localized string derived from the reason
	 * 		identifier.
	 */
	public void goodbye(Identifier reason, String... extra) {
		if (hasFlag(FLAG_SAID_GOODBYE)) return;
		log.trace("[{}] Saying goodbye: {}", describeFacade, reason);
		synchronized (disconnectMutex) {
			this.disconnectReason = reason;
			this.disconnectExtra = ImmutableList.copyOf(extra);
			this.remoteDisconnected = false;
		}
		sendMessage(new GoodbyeMessage(reason, Arrays.asList(extra)));
		setFlag(FLAG_SAID_GOODBYE);
		SharedThreadPool.schedule(() -> {
			if (isConnected()) {
				log.warn("[{}] Connection is still open 5 seconds after saying goodbye; closing", describeFacade);
				disconnect();
			}
		}, 5, TimeUnit.SECONDS);
	}

	/**
	 * @deprecated <b>Internal. For use by GoodbyeMessage only.</b>
	 */
	@Deprecated
	public void onRemoteGoodbye(Identifier reason, List<String> extra) {
		log.trace("[{}] Remote said goodbye: {}", describeFacade, reason);
		synchronized (disconnectMutex) {
			this.disconnectReason = reason;
			this.disconnectExtra = ImmutableList.copyOf(extra);
			this.remoteDisconnected = true;
		}
	}

	/**
	 * Immediately drop this connection without telling the other side why.
	 */
	public void disconnect() {
		try {
			tcpChannel.close();
			synchronized (disconnectMutex) {
				this.remoteDisconnected = false;
			}
		} catch (IOException e) {
			log.warn("[{}] Failed to close socket", e);
		}
	}

	public boolean isConnected() {
		return tcpChannel.isOpen();
	}

	/**
	 * @return the identifier explaining why we disconnected; {@code null} if not known or we're
	 * 		still connected
	 * @see #goodbye
	 */
	public @Nullable Identifier getDisconnectReason() {
		return disconnectReason;
	}

	/**
	 * @return a list of arbitrary "extra" string data for the
	 * 		{@link #getDisconnectReason disconnect reason}; empty if not known or we're still
	 * 		connected
	 * @see #goodbye
	 */
	public ImmutableList<String> getDisconnectExtras() {
		return disconnectExtra;
	}

	/**
	 * @return {@code true} if the connection was dropped by the remote, {@code false} if it was
	 * 		dropped by us or we're still connected
	 */
	public boolean didRemoteDisconnect() {
		return remoteDisconnected;
	}

	/**
	 * Derive a concise, human-readable description of this connection, using any available
	 * information in an attempt to be useful, such as a player name or the remote IP address.
	 * @return a short description of this connection in the best human-readable form available
	 */
	public String describe() {
		synchronized (describers) {
			for (Describer d : describers) {
				try {
					String s = d.describe(this);
					if (s != null) return s;
				} catch (IOException e) {
					log.debug("Describer {} threw an exception", d, e);
				}
			}
		}
		return getRemoteAddrString();
	}

	/**
	 * Add a "describer" to this Connection; something that, given this Connection, can produce a
	 * human-readable summary to return from {@link #describe}.
	 * <p>
	 * If a describer returns null or throws an exception, the next describer is tried. If all
	 * describers return null or throw, the remote address is used as this connection's description.
	 * Describers are tried in the reverse order they were added, so that more useful describers
	 * using information only available later, such as account usernames, are tried first.
	 */
	public void addDescriber(Describer describer) {
		synchronized (describers) {
			describers.add(describer);
		}
	}

	private String getRemoteAddrString() {
		try {
			return getAddrString(tcpChannel.getRemoteAddress());
		} catch (IOException e) {
			log.debug("Error while attempting to retrieve remote addr", e);
			return "<error>";
		}
	}

	private String getLocalAddrString() {
		try {
			return getAddrString(tcpChannel.getLocalAddress());
		} catch (IOException e) {
			log.debug("Error while attempting to retrieve local addr", e);
			return "<error>";
		}
	}

	private static String getAddrString(SocketAddress addr) {
		InetSocketAddress inet = ((InetSocketAddress)addr);
		if (inet.getAddress() instanceof Inet6Address) {
			return "["+InetAddresses.toAddrString(inet.getAddress())+"]:"+inet.getPort();
		} else {
			return inet.getAddress().getHostAddress()+":"+inet.getPort();
		}
	}

	@Override
	public String toString() {
		return BraceFormatter.format(
				"Connection[connected={},remoteAddr={},localAddr={},incomingMessages=<{} messages>,outgoingMessages=<{} messages>]",
				isConnected(), getRemoteAddrString(), getLocalAddrString(), incomingMessages.size(), outgoingMessages.size());
	}

	// called on client thread or server thread
	public void processPackets() {
		if (incomingMessages.size() > 100) {
			log.warn("[{}] Connection is falling majorly behind! ({} unprocessed messages since last network update) - DoS attack? extreme lag?", describeFacade, incomingMessages.size());
		} else if (incomingMessages.size() > 10) {
			log.debug("[{}] Connection is falling behind! ({} unprocessed messages since last network update)", describeFacade, incomingMessages.size());
		}
		// don't do too much work at once - up to 20 messages
		for (int i = 0; i < 20; i++) {
			Message msg = incomingMessages.poll();
			if (msg == null) break;
			msg.process(ctx, this);
		}
	}

	// network thread only {
	/**
	 * @deprecated <b>Internal. For use by NetworkThread only.</b>
	 */
	@Deprecated
	public boolean writePending() {
		if (!isConnected()) return true;
		if (outgoingMessages.size() > 100) {
			log.warn("[{}] Connection is falling majorly behind! ({} unsent messages since last network update)", describeFacade, outgoingMessages.size());
		} else if (outgoingMessages.size() > 10) {
			log.debug("[{}] Connection is falling behind! ({} unsent messages since last network update)", describeFacade, outgoingMessages.size());
		}
		// don't do too much work at once - up to 20 messages
		for (int i = 0; i < 20; i++) {
			Message msg = outgoingMessages.poll();
			if (msg == null) break;
			Identifier id = msg.getId();
			Packet p = new Packet();
			if (outgoingShortIds.containsKey(id)) {
				p.shortId = outgoingShortIds.getInt(id);
			} else {
				if (messagesSentBefore.contains(id)) {
					int shortId = nextShortId++;
					outgoingShortIds.put(id, shortId);
					p.shortId = shortId;
				} else {
					messagesSentBefore.add(id);
				}
				p.longId = id;
			}
			writeBuffer.rewind().limit(writeBuffer.capacity());
			Marshaller m = new Marshaller(writeBuffer);
			msg.marshal(m);
			ByteBuffer payload = m.finish();
			byte[] payloadBys = new byte[payload.limit()];
			payload.get(payloadBys);
			p.payload = payloadBys;

			log.trace("[{}] Write packet {}", describeFacade, p);

			writeBuffer.rewind().limit(writeBuffer.capacity());
			p.marshal(m);
			try {
				ByteBuffer fin = m.finish();
				log.trace("[{}] OUT TCP\n{}", describeFacade, Hexdump.encode(fin));
				tcpChannel.write(fin);
			} catch (IOException e) {
				log.warn("[{}] Failed to write packet", describeFacade, e);
			}
		}
		return false;
	}

	/**
	 * @deprecated <b>Internal. For use by NetworkThread only.</b>
	 */
	@Deprecated
	public void feedQueued(ByteBuffer buffer) {
		log.trace("[{}] IN TCP\n{}", describeFacade, Hexdump.encode(buffer));
		readBuffer.limit(readBuffer.capacity());
		if (buffer.remaining() > readBuffer.remaining()) {
			int diff = buffer.remaining()-readBuffer.remaining();
			int newLimit = ((readBuffer.limit()+diff)*3)/2; // *1.5 (three halves) without floating point
			log.debug("[{}] Reallocating receive buffer from {}K to {}K", describeFacade, readBuffer.limit()/1024, newLimit/1024);
			readBuffer = memRealloc(readBuffer, newLimit);
		}
		readBuffer.put(buffer);
		tryReadPackets();
		if (!incomingMessages.isEmpty() && ctx.getEngineType().isServer()) {
			ctx.asServerContext().getEngine().enqueueProcessing(this);
		}
	}

	/**
	 * @deprecated <b>Internal. For use by NetworkThread only.</b>
	 */
	@Deprecated
	public void feedImmediate(ByteBuffer buffer) {
		log.trace("[{}] IN UDP\n{}", describeFacade, Hexdump.encode(buffer));
		Unmarshaller u = new Unmarshaller(buffer);
		Packet p = new Packet();
		p.unmarshal(u);
		log.trace("[{}] Received packet {} ({}) with a {} byte payload over UDP", describeFacade, p.longId, p.shortId, p.payload.length);
		Message msg = convertToMessage(p);
		if (msg != null) {
			incomingMessages.add(msg);
			if (ctx.getEngineType().isServer()) {
				ctx.asServerContext().getEngine().enqueueProcessing(this);
			}
		}
	}

	private void tryReadPackets() {
		ByteBuffer dup = readBuffer.duplicate();
		dup.flip();
		Unmarshaller u = new Unmarshaller(dup);
		try {
			// keep going until underflow
			while (true) {
				Packet p = new Packet();
				p.unmarshal(u);
				log.trace("[{}] Received packet {} ({}) with a {} byte payload over TCP", describeFacade, p.longId, p.shortId, p.payload.length);
				Message msg = convertToMessage(p);
				if (msg != null) {
					incomingMessages.add(msg);
				}
				// update position only after a successful read
				readBuffer.position(dup.position());
			}
		} catch (BufferUnderflowException e) {}
		readBuffer.compact();
	}
	// }

	private @Nullable Message convertToMessage(Packet p) {
		int shortId = p.shortId;
		Identifier longId = p.longId;
		if (longId != null) {
			if (incomingShortIds.containsKey(shortId)) {
				if (!Objects.equals(incomingShortIds.get(shortId), longId)) {
					throw new ProtocolViolationException("Cannot redefine an existing short ID (tried to redefine "+shortId+" to mean "+longId+" when it already means "+incomingShortIds.get(shortId)+")");
				}
			} else if (shortId != 0) {
				log.trace("[{}] Defining new short ID {} to mean {}", describeFacade, shortId, longId);
				incomingShortIds.put(shortId, longId);
			}
		} else {
			if (shortId == 0) {
				throw new ProtocolViolationException("Got packet with no short ID or long ID");
			}
			longId = incomingShortIds.get(shortId);
			if (longId == null) {
				throw new ProtocolViolationException("Got unknown short ID "+shortId);
			}
		}
		if (hasFlag(FLAG_SAID_GOODBYE) && !longId.equals(new Identifier("chipper", "goodbye"))) {
			log.trace("[{}] Ignoring message received post-goodbye: {}", describeFacade, longId);
			return null;
		}
		Message msg;
		if (ctx.getEngineType().isClient()) {
			msg = currentProtocol.createForClient(longId);
		} else if (ctx.getEngineType().isServer()) {
			msg = currentProtocol.createForServer(longId);
		} else {
			log.warn("[{}] Current context doesn't seem to be a client or a server?", describeFacade);
			msg = currentProtocol.createIndiscriminately(longId);
		}
		ByteBuffer buf = ByteBuffer.wrap(p.payload);
		Unmarshaller un = new Unmarshaller(buf);
		msg.unmarshal(un);
		if (buf.remaining() > 0) {
			log.debug("[{}] Packet with ID {} under-read by {} bytes", describeFacade, longId, buf.remaining());
		}
		if (simulatedPacketLossOut > 0 && msg.getSendMode() != SendMode.RELIABLE && SharedRandom.chance(simulatedPacketLossIn)) {
			log.debug("[{}] Simulating lost packet for incoming {}", describeFacade, msg.getId());
			return null;
		}
		return msg;
	}

}

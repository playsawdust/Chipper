/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.server;

import static java.nio.channels.SelectionKey.*;
import static java.net.StandardSocketOptions.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.rpmalloc.RPmalloc.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.network.Connection;

public class ServerNetworkThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(ServerNetworkThread.class);
	/** @see DatagramSocket#setTrafficClass */
	private static final int IPTOS_LOWDELAY = 0x10;

	private final Context<ServerEngine> ctx;
	private final Selector selector;
	private final DatagramChannel udpChannel;

	private ByteBuffer buffer;

	private boolean run = true;

	private final List<Connection> connections = Lists.newArrayList();
	private final Table<InetAddress, Integer, Connection> connectionsByCorrelationId = HashBasedTable.create();

	public ServerNetworkThread(Context<ServerEngine> ctx, ServerSocketChannel tcpChannel, DatagramChannel udpChannel) throws IOException {
		this.ctx = ctx;
		this.selector = Selector.open();
		this.udpChannel = udpChannel;
		if (((InetSocketAddress)udpChannel.getLocalAddress()).getAddress() instanceof Inet4Address) {
			// behavior of this option is "undefined" for IPv6 sockets
			// (Undefined behavior? In my Java!? It's more likely than you'd think!)
			udpChannel.setOption(IP_TOS, IPTOS_LOWDELAY);
		}
		tcpChannel.register(selector, OP_ACCEPT);
		udpChannel.register(selector, OP_READ);
		setDaemon(true);
		setName("Network thread");
	}

	@Override
	public void run() {
		rpmalloc_thread_initialize();
		try {
			buffer = memAlloc(8*1024).order(ByteOrder.BIG_ENDIAN);
			while (run) {
				try {
					selector.select();
				} catch (IOException e) {
					log.warn("Select failed", e);
					continue;
				}
				for (SelectionKey key : selector.selectedKeys()) {
					buffer.rewind().limit(buffer.capacity());
					try {
						if (key.isAcceptable()) {
							ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
							try {
								SocketChannel sc = ssc.accept();
								if (sc == null) continue;
								sc.configureBlocking(false);
								sc.setOption(TCP_NODELAY, true);
								InetSocketAddress src = (InetSocketAddress)sc.getRemoteAddress();
								log.debug("Accepted connection from TCP {}:{}", src.getAddress().getHostAddress(), src.getPort());
								if (ctx.getEngine().isPortcheckServer(src.getAddress())) {
									ctx.getEngine().onPortcheckResponseTCP();
									sc.close();
								} else {
									Connection conn = new Connection(ctx, selector::wakeup, sc, udpChannel);
									connections.add(conn);
									sc.register(selector, OP_READ, conn);
								}
							} catch (IOException e) {
								log.warn("TCP accept failed", e);
								continue;
							}
						} else if (key.isReadable()) {
							if (key.channel() instanceof DatagramChannel) {
								DatagramChannel dc = (DatagramChannel)key.channel();
								try {
									InetSocketAddress src = (InetSocketAddress)dc.receive(buffer);
									if (src == null) continue;
									buffer.flip();
									log.debug("Read {} bytes from UDP {}:{}", buffer.limit(), src.getAddress().getHostAddress(), src.getPort());
									if (ctx.getEngine().isPortcheckServer(src.getAddress())) {
										int i = buffer.getInt();
										if (i == 1347374663) {
											long token = buffer.getLong();
											if (ctx.getEngine().isPortcheckToken(token)) {
												CharBuffer cb = Charsets.UTF_8.decode(buffer);
												char[] chr = new char[cb.remaining()];
												cb.get(chr);
												ctx.getEngine().onPortcheckResponseUDP(new String(chr));
											}
										}
									} else {
										int correlationId = buffer.getInt();
										Connection c = connectionsByCorrelationId.get(src.getAddress(), correlationId);
										if (c != null) {
											c.feedImmediate(buffer);
										} else {
											log.warn("Received UDP packet with bad correlation ID");
										}
									}
								} catch (IOException e) {
									log.warn("UDP receive failed", e);
									continue;
								}
							} else {
								SocketChannel sc = (SocketChannel)key.channel();
								try {
									sc.read(buffer);
									buffer.flip();
									InetSocketAddress src = (InetSocketAddress)sc.getRemoteAddress();
									if (buffer.limit() == 0) {
										if (!key.isValid()) {
											log.debug("TCP {}:{} disconnected", src.getAddress().getHostAddress(), src.getPort());
											// TODO optimize
											Iterator<Connection> iter = connectionsByCorrelationId.values().iterator();
											while (iter.hasNext()) {
												if (iter.next() == key.attachment()) {
													iter.remove();
												}
											}
											key.cancel();
										}
									} else {
										log.debug("Read {} bytes from TCP {}:{}", buffer.limit(), src.getAddress().getHostAddress(), src.getPort());
										Object attachment = key.attachment();
										if (attachment instanceof Connection) {
											((Connection)attachment).feedQueued(buffer);
										} else {
											if (attachment == null) {
												log.warn("No attachment for TCP {}:{}", src.getAddress().getHostAddress(), src.getPort());
											} else {
												log.warn("Unknown attachment for TCP {}:{} ({})", src.getAddress().getHostAddress(), src.getPort(), attachment);
											}
										}
									}
								} catch (IOException e) {
									log.warn("TCP read failed", e);
									continue;
								}
							}
						}
					} catch (Error e) {
						throw e;
					} catch (Throwable e) {
						String desc;
						if (key.attachment() instanceof Connection) {
							desc = ((Connection)key.attachment()).describe();
						} else {
							desc = key.channel().toString();
						}
						log.warn("Exception while processing event for {}", desc, e);
					}
				}
				Iterator<Connection> iter = connections.iterator();
				while (iter.hasNext()) {
					Connection c = iter.next();
					try {
						if (c.writePending()) {
							log.debug("{} disconnected", c.describe());
							iter.remove();
						}
					} catch (Error e) {
						throw e;
					} catch (Throwable e) {
						log.warn("Exception while processing event for {}", c.describe(), e);
					}
				}
			}
		} finally {
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			memFree(buffer);
			buffer = null;
			rpmalloc_thread_finalize();
		}
	}



}

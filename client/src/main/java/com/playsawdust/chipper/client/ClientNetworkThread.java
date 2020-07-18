/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import static java.nio.channels.SelectionKey.*;
import static java.net.StandardSocketOptions.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.rpmalloc.RPmalloc.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.network.Connection;

public class ClientNetworkThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(ClientNetworkThread.class);
	/** @see DatagramSocket#setTrafficClass */
	private static final int IPTOS_LOWDELAY = 0x10;

	private final Context<ClientEngine> ctx;
	private final Selector selector;

	private final Connection conn;

	private ByteBuffer buffer;

	private boolean run = true;

	public ClientNetworkThread(Context<ClientEngine> ctx, SocketChannel tcpChannel, DatagramChannel udpChannel) throws IOException {
		this.ctx = ctx;
		this.selector = Selector.open();
		this.conn = new Connection(ctx, selector::wakeup, tcpChannel, udpChannel);
		if (((InetSocketAddress)udpChannel.getLocalAddress()).getAddress() instanceof Inet4Address) {
			// behavior of this option is "undefined" for IPv6 sockets
			// (Undefined behavior? In my Java!? It's more likely than you'd think!)
			udpChannel.setOption(IP_TOS, IPTOS_LOWDELAY);
		}
		tcpChannel.register(selector, OP_READ);
		udpChannel.register(selector, OP_READ);
		setDaemon(true);
		setName("Network thread");
	}

	public Connection getConnection() {
		return conn;
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
					if (key.isReadable()) {
						if (key.channel() instanceof DatagramChannel) {
							DatagramChannel dc = (DatagramChannel)key.channel();
							try {
								InetSocketAddress src = (InetSocketAddress)dc.receive(buffer);
								if (src == null) continue;
								buffer.flip();
								log.debug("Read {} bytes from UDP {}:{}", buffer.limit(), src.getAddress().getHostAddress(), src.getPort());
								conn.feedImmediate(buffer);
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
										return;
									} else {
										log.debug("Read 0 bytes from TCP {}:{}...?", src.getAddress().getHostAddress(), src.getPort());
									}
								} else {
									log.debug("Read {} bytes from TCP {}:{}", buffer.limit(), src.getAddress().getHostAddress(), src.getPort());
									conn.feedQueued(buffer);
								}
							} catch (IOException e) {
								log.warn("TCP read failed", e);
								continue;
							}
						}
					}
				}
				conn.writePending();
			}
		} catch (Throwable t) {
			log.error("Error in network thread", t);
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

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.server.dedicated;

import static org.lwjgl.system.rpmalloc.RPmalloc.*;
import static org.lwjgl.util.simd.SSE.*;
import static org.lwjgl.util.simd.SSE3.*;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.lwjgl.system.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.playsawdust.chipper.Addon;
import com.playsawdust.chipper.Distribution;
import com.playsawdust.chipper.MoreLibC;
import com.playsawdust.chipper.Parachute;
import com.playsawdust.chipper.Greeting;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.component.EngineType;
import com.playsawdust.chipper.network.Connection;

import com.playsawdust.chipper.server.ServerNetworkThread;
import com.playsawdust.chipper.server.ServerEngine;
import com.playsawdust.chipper.toolbox.io.Directories;
import com.playsawdust.chipper.toolbox.lipstick.SharedRandom;
import com.sun.jna.Platform;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.win32.Kernel32;

public class DedicatedServerEngine extends ServerEngine {
	private static final Logger log = LoggerFactory.getLogger(Distribution.NAME);

	private boolean ran = false;

	protected InetAddress portcheckServer;
	protected int udpPingbacks = 0;
	protected boolean tcpPingbackSuccess = false;
	protected long pingbackToken;
	protected String publicAddress;

	private Context<ServerEngine> context;

	private final ScheduledThreadPoolExecutor executor;

	public DedicatedServerEngine() {
		executor = new ScheduledThreadPoolExecutor(1, (r) -> new Thread(r, "Server thread"));
		// thread pool executor is re-used to save time, but we only want one thread
		executor.setMaximumPoolSize(1);
		// avoids a possible memory leak (exasperated by lambda capturing)
		executor.setRemoveOnCancelPolicy(true);
	}

	@Override
	public int run(String... args) {
		if (ran) {
			throw new IllegalStateException("DedicatedServerEngine cannot be started more than once, even after being stopped");
		}
		ran = true;
		Thread.currentThread().setName("Init thread");
		Greeting.print(this, log);

		try {
			log.debug("Checking if it's safe to run in this directory...");
			File configFile = new File("config/chipper.jkson");
			if (!configFile.exists()) {
				// hmmm... did they delete this config to generate defaults?
				// check for a few other signs of our presence in this directory
				File addonsHome = new File("addons");
				File dataHome = new File("data");
				if (addonsHome.isDirectory() && dataHome.isDirectory()) {
					// we probably were here before; continue
					log.debug("Found addons and data directories, continuing");
				} else {
					// okay... is this a new directory with very few files in it?
					// (give them room for the game executable and a run script, plus some leeway)
					File cwd = new File(".");
					int fileCount = cwd.list().length;
					if (fileCount < 4) {
						// we can bootstrap in this directory with little risk
						log.debug("Directory has few files, continuing");
					} else {
						// we probably got ran inside of the Downloads directory or similar
						// exit instead of polluting the directory
						// TODO add a command-line switch to override?
						log.error("Couldn't find any files suggesting a previous run of the {} server.\n"
								+ "This directory additionally has {} files in it.\n"
								+ "To avoid polluting this directory, the server will now exit.\n"
								+ "Please run the server in a new empty directory - various files and directories will be created automatically.",
									Distribution.NAME, fileCount);
						return 1;
					}
				}
			} else {
				// this directory definitely belongs to us; continue
				log.debug("Found Chipper config file, continuing");
			}

			context = Context.createNew(this);

			String bindAddrStr = "0.0.0.0";
			if (!bindAddrStr.contains(":")) {
				System.setProperty("java.net.preferIPv4Stack", "true");
			}
			int port = Distribution.DEFAULT_PORT;
			InetAddress bindAddr;
			try {
				bindAddr = InetAddress.getByName(bindAddrStr);
			} catch (UnknownHostException e) {
				log.error("Failed to resolve {}", bindAddrStr, e);
				return 2;
			}
			DatagramChannel udpChannel;
			ServerSocketChannel tcpChannel;
			try {
				if (port == 0) {
					log.info("Binding to a random port, as the configured port number is 0\n"
							+ "(Hint: The in-use port number is written to ./run/port)");
				}
				udpChannel = DatagramChannel.open().bind(new InetSocketAddress(bindAddr, port));
				udpChannel.configureBlocking(false);
			} catch (Exception e) {
				readableBindError(bindAddr, port, e, "UDP");
				return 4;
			}
			InetSocketAddress bound;
			try {
				bound = (InetSocketAddress)udpChannel.getLocalAddress();
			} catch (IOException e) {
				log.error("Failed to get bound address", e);
				return 4;
			}
			try {
				tcpChannel = ServerSocketChannel.open().bind(bound);
				tcpChannel.configureBlocking(false);
			} catch (Exception e) {
				readableBindError(bindAddr, port, e, "TCP");
				return 4;
			}
			writeRunFile("port", bound.getPort());
			if (Platform.isWindows()) {
				writeRunFile("pid", Kernel32.INSTANCE.GetCurrentProcessId());
			} else if (Platform.isMac()) {
				writeRunFile("pid", SystemB.INSTANCE.getpid());
			} else {
				writeRunFile("pid", MoreLibC.INSTANCE.getpid());
			}

			// initializing LWJGL seems to touch something in java.net
			// since we conditionally set preferIPv4Stack above, this needs to happen here
			Configuration.MEMORY_ALLOCATOR.set("rpmalloc");
			_MM_SET_FLUSH_ZERO_MODE(_MM_FLUSH_ZERO_ON);
			_MM_SET_DENORMALS_ZERO_MODE(_MM_DENORMALS_ZERO_ON);

			rpmalloc_initialize();
			rpmalloc_thread_initialize();

			Parachute.allocate();

			executor.prestartCoreThread();

			try {
				try {
					new ServerNetworkThread(context, tcpChannel, udpChannel).start();
				} catch (IOException e) {
					log.error("Failed to create receive thread", e);
					return 4;
				}
				log.info("Listening on UDP+TCP {}:{}", bound.getAddress().getHostAddress(), bound.getPort());
				int boundPort = bound.getPort();
				if (bound.getAddress().isLoopbackAddress()) {
					log.debug("Skipping portcheck for local server");
				} else {
					portcheck(boundPort);
				}
				Thread.currentThread().setName("Command processing thread");
				// TODO use JLine
				try (Scanner scanner = new Scanner(System.in)) {
					while (true) {
						String line = scanner.nextLine();
						if ("stop".equals(line)) break;
					}
				}
			} finally {
				rpmalloc_thread_finalize();
			}
		} finally {
			log.info("Waiting for any tasks to finish...");
			executor.shutdown();
			while (!executor.isTerminated()) {
				try {
					if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
						log.warn("30 seconds later, tasks still have not finished. Giving up, exiting now.");
						executor.shutdownNow();
					}
				} catch (InterruptedException e) {
				}
			}
			log.info("Exiting");
		}
		return 0;
	}

	@Override
	public Addon getDefaultAddon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void enqueueProcessing(Connection connection) {
		executor.execute(connection::processPackets);
	}

	@Override
	public EngineType getType() {
		return EngineType.DEDICATED_SERVER;
	}

	private void writeRunFile(String name, int value) {
		writeRunFile(name, String.valueOf(value));
	}

	private void writeRunFile(String name, String value) {
		try {
			File f = new File(Directories.getRuntimeDir(), name);
			Files.asCharSink(f, Charsets.UTF_8).write(value+(Platform.isWindows() ? "\r\n" : "\n"));
		} catch (IOException e) {
			log.warn("Failed to write runtime file {}", name, e);
		}
	}

	private void portcheck(int boundPort) {
		new Thread(() -> {
			try {
				ByteBuffer buf = ByteBuffer.allocateDirect(2048);
				List<InetAddress> allPortchecks = Lists.newArrayList(InetAddress.getAllByName("portcheck.playsawdust.com"));
				SharedRandom.shuffle(allPortchecks);
				for (InetAddress ia : allPortchecks) {
					try (Socket sock = new Socket()) {
						sock.connect(new InetSocketAddress(ia, 32239), 2500);
						portcheckServer = ia;
						break;
					} catch (SocketTimeoutException | ConnectException e) {
						continue;
					}
				}
				if (portcheckServer == null) {
					if (allPortchecks.isEmpty()) {
						log.info("Couldn't find any portcheck servers. Skipping portcheck.");
					} else if (allPortchecks.size() == 1) {
						log.info("The portcheck server appears to be down. Skipping portcheck.");
					} else {
						log.info("All portcheck servers appear to be down. Skipping portcheck.");
					}
					return;
				}
				pingbackToken = SharedRandom.uniformLong();
				SocketAddress sa = new InetSocketAddress(portcheckServer, 32239);
				log.debug("Asking for a ping from the anonymous portcheck server to verify connectivity...");
				DatagramChannel ch = DatagramChannel.open().connect(sa);
				buf.putInt(1346981447);
				buf.putShort((short)(boundPort));
				buf.putLong(pingbackToken);
				buf.put((byte)(1));
				buf.flip();
				for (int i = 0; i < 10; i++) {
					ch.send(buf.duplicate(), sa);
				}
				ch.close();
				Stopwatch sw = Stopwatch.createStarted();
				while (sw.elapsed(TimeUnit.MILLISECONDS) < 5000) {
					// in the time it takes for the 40 UDP pings, the TCP connection will have happened
					// if it hasn't, we can safely assume TCP is closed without waiting the full 5 seconds
					if (udpPingbacks >= 40) break;
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
				}
				sw.stop();
				if (udpPingbacks > 40) udpPingbacks = 40;
				int packetLoss = (int)((1-(udpPingbacks/40D))*100);
				String packetLossMsg = "";
				log.debug("{}% packet loss ({} packets, expected {})", packetLoss, udpPingbacks, 40);
				if (packetLoss > 70) {
					packetLossMsg = ", with extremely bad packet loss. (The game probably won't be playable)";
				} else if (packetLoss > 40) {
					packetLossMsg = ", with very bad packet loss";
				} else if (packetLoss > 20) {
					packetLossMsg = ", with bad packet loss";
				} else if (packetLoss == 0) {
					packetLossMsg = ", with no packet loss";
				}
				boolean udpPingbackSuccess = (udpPingbacks > 0);
				if (udpPingbackSuccess && tcpPingbackSuccess) {
					boolean defaultPort = (boundPort == Distribution.DEFAULT_PORT);
					String addr = defaultPort ? "just "+publicAddress : publicAddress+":"+boundPort;
					String whyAddr = defaultPort ? "since you're using the default port" : "since you're not using the default port";
					log.info("Portcheck server was able to reach us on TCP and UDP.\n"
							+ "You're good to go{}.\n"
							+ "Your public address is {}, {}.", packetLossMsg, addr, whyAddr);
				} else {
					// "router" is more understandable for home connections
					// "firewall" is more correct for servers in data centers
					// this is a pointless miniscule detail but i care so yeah
					boolean probablyBehindNAT = isProbablyBehindNAT();
					String openAPort = probablyBehindNAT ? "forward a port on your router" : "open a port in your firewall";
					String openIn = probablyBehindNAT ? "forwarded on your router" : "open in your firewall";
					if (!udpPingbackSuccess && tcpPingbackSuccess) {
						log.warn("Portcheck server was able to reach us on TCP, but not UDP.\n"
								+ "UDP makes the game more efficient. Make sure UDP is {}.", openIn);
					} else if (udpPingbackSuccess && !tcpPingbackSuccess) {
						log.warn("Portcheck server was able to reach us on UDP, but not TCP.\n"
								+ "TCP is required. Make sure TCP is {}.", openIn);
					} else if (!udpPingbackSuccess && !tcpPingbackSuccess) {
						log.warn("Portcheck server couldn't reach us on TCP or UDP.\n"
								+ "Do you need to {}?", openAPort);
					}
				}
			} catch (UnknownHostException e) {
				log.debug("Unable to resolve portcheck server", e);
			} catch (IOException e) {
				log.debug("Unable to ping portcheck server", e);
			}
		}, "Portcheck thread").start();
	}

	@Override
	public boolean isPortcheckServer(InetAddress address) {
		return address.equals(portcheckServer);
	}

	@Override
	public boolean isPortcheckToken(long token) {
		return token == pingbackToken;
	}

	@Override
	public void onPortcheckResponseTCP() {
		tcpPingbackSuccess = true;
	}

	@Override
	public void onPortcheckResponseUDP(String publicAddress) {
		udpPingbacks++;
		this.publicAddress = publicAddress;
	}

	private boolean isProbablyBehindNAT() {
		try {
			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (InetAddress a : Collections.list(ni.getInetAddresses())) {
					if (!a.isLinkLocalAddress() && !a.isLoopbackAddress() && !a.isSiteLocalAddress()) {
						// public IP on an interface suggests direct connection
						return false;
					}
				}
			}
			// no public IPs on any interfaces suggests a NAT
			return true;
		} catch (Exception e) {
			// increased security suggests a server or well-informed user
			return false;
		}
	}

	private void readableBindError(InetAddress addr, int port, Exception e, String type) {
		String readable = null;
		String msg = e.getMessage();
		if (msg != null && msg.toLowerCase(Locale.ENGLISH).equals("cannot assign requested address")) {
			try {
				List<String> addrs = Lists.newArrayList();
				boolean anyIp6 = false;
				for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
					for (InetAddress a : Collections.list(ni.getInetAddresses())) {
						if (a instanceof Inet6Address) anyIp6 = true;
						addrs.add(a.getHostAddress()+" ("+ni.getDisplayName()+")");
					}
				}
				if (addrs.isEmpty()) throw new SocketException();
				String prelude;
				if (anyIp6) {
					prelude = "- 0.0.0.0 (wildcard)\n"
							+ "- ::0 (wildcard)";
				} else {
					prelude = "- 0.0.0.0 (wildcard)";
				}
				readable = "Bind address is invalid.\n"
						+ "Is "+addr.getHostAddress()+" an address owned by this computer?\n"
						+ "The valid addresses for this computer seem to be:\n"
						+ prelude+"\n"
						+ "- "+Joiner.on("\n- ").join(addrs);
			} catch (SocketException e2) {
				readable = "Bind address is invalid.\nIs "+addr.getHostAddress()+" an address owned by this computer?\n"
						+ "We couldn't enumerate your network interfaces, so we don't know what addresses are valid.\n"
						+ "Try 0.0.0.0 or ::0 as the IPv4 or IPv6 wildcard, respectively.";
			}
		} else if (msg != null && msg.toLowerCase(Locale.ENGLISH).equals("permission denied")) {
			if (System.getProperty("os.name").contains("Win")) {
				if (port <= 1024) {
					readable = "Port bind permission denied. "+port+" is a well-known port.\n"
							+ "Only administrators can bind well-known ports.";
				}
			} else {
				if (port <= 1024) {
					if (System.getProperty("os.name").contains("Linux")) {
						readable = "Port bind permission denied. "+port+" is a well-known port.\n"
								+ "Only root can bind well-known ports, or programs with CAP_NET_BIND_SERVICE.";
					} else {
						readable = "Port bind permission denied. "+port+" is a well-known port.\n"
								+ "Only root can bind well-known ports.";
					}
				}
			}
		} else if (msg != null && msg.toLowerCase(Locale.ENGLISH).startsWith("port out of range")) {
			readable = "Port number out of range.\n"
					+ "Port numbers must be within the range 0 - 65535, inclusive.";
		} else if (msg != null && msg.toLowerCase(Locale.ENGLISH).equals("address already in use")) {
			readable = "Port is already bound.\n"
					+ "Is another "+Distribution.NAME+" server already running?";
		}
		if (readable == null) {
			log.error("Failed to bind {} {}:{}: No concise human-readable error is available.", type, addr.getHostAddress(), port, e);
		} else {
			log.error("Failed to bind {} {}:{}: {}", type, addr.getHostAddress(), port, readable);
		}
	}

}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Security;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.bridge.SLF4JBridgeHandler;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.playsawdust.chipper.exception.ForbiddenClassError;
import com.playsawdust.chipper.rcl.RuledClassLoader;

import com.playsawdust.chipper.toolbox.io.Directories;
import com.playsawdust.chipper.toolbox.io.LoggerPrintStream;
import com.playsawdust.chipper.toolbox.lipstick.MonotonicTime;
import com.playsawdust.chipper.toolbox.pool.ObjectPool;
import com.sun.jna.Memory;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.IPHlpAPI.FIXED_INFO;
import com.sun.jna.platform.win32.IPHlpAPI.IP_ADDR_STRING;
import com.sun.jna.ptr.IntByReference;
import com.unascribed.asyncsimplelog.AsyncSimpleLog;
import com.unascribed.asyncsimplelog.AsyncSimpleLog.LogLevel;

public class Bootstrap {

	public static class Stage2 {
		protected static void preStart() {
			if (!skipRcl() && !Stage2.class.getClassLoader().getClass().getName().equals(RuledClassLoader.class.getName())) {
				throw new AssertionError("Stage2 must be called within the ruled classloader");
			}
			// initialize MonotonicTime's epoch
			MonotonicTime.nanos();
			SLF4JBridgeHandler.removeHandlersForRootLogger();
			SLF4JBridgeHandler.install();
			ObjectPool.enableStats(true);
			Security.addProvider(new BouncyCastleProvider());
			if (Platform.isWindows()) {
				IntByReference bufferSize = new IntByReference();
				if (IPHlpAPI.INSTANCE.GetNetworkParams(null, bufferSize) == WinError.ERROR_BUFFER_OVERFLOW) {
					Memory buffer = new Memory(bufferSize.getValue());
					if (IPHlpAPI.INSTANCE.GetNetworkParams(null, bufferSize) == WinError.ERROR_SUCCESS) {
						FIXED_INFO info = new FIXED_INFO(buffer);
						List<String> dnsServers = Lists.newArrayList();
						IP_ADDR_STRING cur = info.DnsServerList;
						while (cur != null) {
							String addr = new String(cur.IpAddress.String);
							int nullPos = addr.indexOf(0);
							if (nullPos != -1) {
								addr = addr.substring(0, nullPos);
							}
							dnsServers.add(addr);
							cur = cur.Next;
						}
						// will get picked up by DNSJava, allowing it to skip its ipconfig parsing
						System.setProperty("dns.server", Joiner.on(',').join(dnsServers));
					}
				}
			}
			// TODO check if the terminal supports ansi codes, and enable by default
			if (System.getenv("CHIPPER_ANSI") != null) {
				AsyncSimpleLog.setAnsi(true);
			}
			// this, however, should stay guarded behind some kind of option
			if (System.getenv("CHIPPER_POWERLINE") != null) {
				AsyncSimpleLog.setPowerline(true);
			}
			AsyncSimpleLog.startLogging();
			AsyncSimpleLog.setMinLogLevel(LogLevel.TRACE);
			System.setOut(new LoggerPrintStream("STDOUT", false));
			System.setErr(new LoggerPrintStream("STDERR", true));
			Directories.setAppName(Distribution.ID, Distribution.NAME);
		}
	}

	protected static boolean skipRcl() {
		return System.getenv("CHIPPER_SKIP_RCL") != null;
	}

	protected static void preStart() {
		if (System.getenv("CHIPPER_CLS_ON_START") != null) {
			System.out.print("\u001Bc");
		}
	}

	protected static void startStage2(ClassLoader cl, String clazz, String[] args, PrintStream realOut) {
		try {
			cl.loadClass(clazz)
				.getDeclaredMethod("run", String[].class).invoke(null, (Object)args);
		} catch (Throwable e) {
			if (e instanceof ForbiddenClassError) return;
			e.printStackTrace(realOut);
			realOut.println("Failed to invoke stage 2.");
			System.exit(255);
		}
	}

	protected static URL[] getClasspath(ClassLoader cl) {
		if (cl instanceof URLClassLoader) {
			// easy
			return ((URLClassLoader)cl).getURLs();
		} else {
			// hmm.
			Class<?> clazz = cl.getClass();
			try {
				Class<?> builtin = Class.forName("jdk.internal.loader.BuiltinClassLoader");
				if (builtin.isAssignableFrom(clazz)) {
					// J9 built-in loader. no API to determine classpath; reflect into it
					Field ucpF = builtin.getDeclaredField("ucp");
					ucpF.setAccessible(true);
					Class<?> ucpClazz = Class.forName("jdk.internal.loader.URLClassPath");
					Object ucp = ucpF.get(cl);
					if (ucp == null) {
						// only modules are loaded, which we cannot adequately represent
						return new URL[0];
					} else {
						Method getUrls = ucpClazz.getDeclaredMethod("getURLs");
						getUrls.setAccessible(true);
						return (URL[])getUrls.invoke(ucp);
					}
				} else {
					throw new IllegalArgumentException("Don't know how to get class path for class loader "+clazz.getCanonicalName());
				}
			} catch (Throwable t) {
				throw new IllegalArgumentException("Don't know how to get class path for class loader "+clazz.getCanonicalName(), t);
			}
		}
	}

	protected static void defaultClientRules(RuledClassLoader.Builder bldr) {
		defaultRules(bldr, true);
	}

	protected static void defaultServerRules(RuledClassLoader.Builder bldr) {
		defaultRules(bldr, false);
	}

	private static void defaultRules(RuledClassLoader.Builder bldr, boolean client) {
		String tkReason;
		if (client) {
			tkReason = "Do not use $$.\n"
					+ "Some parts of ## conflict with LWJGL, and opening separate windows with disparate designs is bad UX.\n"
					+ "All UI must be implemented through the Widget system.";
		} else {
			tkReason = "Do not use $$.\n"
					+ "The dedicated server is designed for completely headless operation and must not open windows.";
		}
		String oldCollReason = "$$ is a legacy collection from Java 1, available for compatibility only.\n"
				+ "Use the Java 2 equivalent ## instead.";
		String notApiReason = "$$ is not public API.";
		String mathReason = "Use FastMath instead of $$. If the method you need is not available, please open an issue.";
		String lqRandReason = "$$$ is inflexible and uses a low-quality, slow random number generator.\n"
				+ "Use SharedRandom.## instead.";
		String objectIoReason = "Object IO is a security risk and incredibly fragile. Use Marshallable instead.";
		String dontGc = "Do not call $$$; explicit GCs are slow and almost never useful.\n"
				+ "If you're attempting to mitigate a low-memory condition, use Parachute.free instead.";
		bldr
			.addExemptPackage("sun")
			.addExemptPackage("java")
			.addExemptPackage("com.sun")
			.addExemptPackage("org.lwjgl")
			.addExemptPackage("com.sixlegs.png")
			.addExemptPackage("com.google.common")
			.addExemptPackage("org.joml")
			.addExemptPackage("org.h2")
			.addExemptPackage("org.xbill.DNS")
			.addExemptPackage("it.unimi.dsi.fastutil")

			.addExemptClass("com.unascribed.random.RandomXoshiro256StarStar")
			.addExemptClass("com.playsawdust.chipper.math.FastMath")
			.addExemptClass("com.playsawdust.chipper.rcl.RuledClassLoader")
			.addExemptClass("com.playsawdust.chipper.Parachute")

			.addPermittedPackage("com.sun.jna")
			.addPermittedMethod("java.lang.Math", "max")
			.addPermittedMethod("java.lang.Math", "min")
			.addPermittedMethod("java.lang.Math", "abs")
			.addPermittedMethod("java.lang.Math", "signum")

			.addForbiddenPackage("javafx", tkReason.replace("##", "JavaFX"))
			.addForbiddenPackage("java.awt", tkReason.replace("##", "AWT"))
			.addForbiddenPackage("javax.swing", tkReason.replace("##", "AWT"))
			.addForbiddenPackage("sun", notApiReason)
			.addForbiddenPackage("com.sun", notApiReason)

			.addForbiddenClass("java.io.ObjectInputStream", objectIoReason)
			.addForbiddenClass("java.io.ObjectOutputStream", objectIoReason)

			.addForbiddenMethod("java.lang.System", "gc", dontGc)
			.addForbiddenMethod("java.lang.Runtime", "gc", dontGc)

			.addDiscouragedClass("java.util.Vector", oldCollReason.replace("##", "ArrayList"))
			.addDiscouragedClass("java.util.Hashtable", oldCollReason.replace("##", "HashMap"))
			.addDiscouragedClass("java.util.Stack", oldCollReason.replace("##", "ArrayDeque"))
			.addDiscouragedClass("java.lang.StringBuffer",
					"$$ is a legacy class from Java 1, available for compatibility only.\n"
					+ "Use the Java 5 equivalent StringBuilder instead.")
			.addDiscouragedClass("java.util.Random",
					"$$ is a low-quality, slow random number generator.\n"
					+ "Use BetterRandom or SharedRandom instead.")
			.addDiscouragedClass("java.util.concurrent.ThreadLocalRandom",
					"$$ uses a low-quality, slow random number generator.\n"
					+ "Use SharedRandom instead, which is similarly thread-safe.")
			.addDiscouragedClass("java.lang.Math", mathReason)

			.addDiscouragedMethod("java.lang.Math", "random", lqRandReason.replace("##", "uniformDouble"))
			.addDiscouragedMethod("java.util.Collections", "shuffle(Ljava/util/List;)V", lqRandReason.replace("##", "shuffle"))
			.addDiscouragedMethod("java.lang.System","currentTimeMillis",
					"$$$ uses the system clock time, and can be unreliable for timing. Use MonotonicTime.millis() instead.\n"
					+ "If you actually want wall clock time, use Date or Calendar instead.")
		;
	}

}

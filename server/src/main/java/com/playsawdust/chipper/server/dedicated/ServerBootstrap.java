/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.server.dedicated;

import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.playsawdust.chipper.Bootstrap;
import com.playsawdust.chipper.exception.ForbiddenClassError;
import com.playsawdust.chipper.rcl.RuledClassLoader;
import com.playsawdust.chipper.server.dedicated.jediterm.JediTermController;

public class ServerBootstrap extends Bootstrap {
	public static void main(String[] args) {
		preStart();
		PrintStream out = System.out;
		if (skipRcl()) {
			Stage2.run(args);
		} else {
			RuledClassLoader cl = RuledClassLoader.builder()
					.addSources(getClasspath(ClassLoader.getSystemClassLoader()))
					.addRules(Bootstrap::defaultServerRules)
					.addExemptPackage("com.jediterm")
					.addExemptPackage("com.playsawdust.chipper.server.dedicated.jediterm")
					.build();
			startStage2(cl, "com.playsawdust.chipper.server.dedicated.ServerBootstrap$Stage2", args, out);
		}
	}

	public static class Stage2 extends Bootstrap.Stage2 {
		private static final Logger log = LoggerFactory.getLogger(ServerBootstrap.class);

		public static void run(String[] args) {
			JediTermController jtc = null;
			if (System.console() == null) {
				jtc = new JediTermController();
				jtc.start();
			}
			preStart();
			try {
				System.exit(new DedicatedServerEngine().run(args));
			} catch (Throwable t) {
				if (t instanceof ForbiddenClassError) return;
				log.error("Failed to run the engine", t);
			}
			if (jtc != null) {
				jtc.stop();
			}
		}
	}

}

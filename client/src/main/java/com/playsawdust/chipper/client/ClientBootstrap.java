/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.playsawdust.chipper.Bootstrap;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.exception.ForbiddenClassError;
import com.playsawdust.chipper.rcl.RuledClassLoader;

public class ClientBootstrap extends Bootstrap {
	public static void main(String[] args) {
		preStart();
		PrintStream out = System.out;
		if (skipRcl()) {
			Stage2.run(args);
		} else {
			RuledClassLoader cl = RuledClassLoader.builder()
					.addSources(getClasspath(ClassLoader.getSystemClassLoader()))
					.addRules(Bootstrap::defaultClientRules)
					.build();
			startStage2(cl, "com.playsawdust.chipper.client.ClientBootstrap$Stage2", args, out);
		}
	}

	public static class Stage2 extends Bootstrap.Stage2 {
		private static final Logger log = LoggerFactory.getLogger(ClientBootstrap.class);

		public static void run(String[] args) {
			preStart();
			try {
				System.exit(new ClientEngine().run(args));
			} catch (Throwable t) {
				if (t instanceof ForbiddenClassError) return;
				log.error("Failed to run the engine", t);
			}
		}
	}

}

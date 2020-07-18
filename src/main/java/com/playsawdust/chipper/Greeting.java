/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Calendar;
import java.util.Map;
import org.slf4j.Logger;

import com.sun.jna.Platform;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

import com.playsawdust.chipper.component.Engine;

import com.playsawdust.chipper.toolbox.io.Directories;
import com.playsawdust.chipper.toolbox.lipstick.SharedRandom;

public class Greeting {

	public static void print(Engine engine, Logger log) {
		log.info("{}! {} v{} starting as a {}... (Powered by Chipper v{})",
				randomGreeting(), Distribution.NAME, Distribution.VERSION, engine.getType(), Distribution.CHIPPER_VERSION);
		if (Platform.isLinux()) {
			try {
				File osReleaseFile = new File("/etc/os-release");
				String s = Files.asCharSource(osReleaseFile, Charsets.UTF_8).read().replace("\r\n", "\n");
				Map<String, String> osRelease = Splitter.on('\n').omitEmptyStrings().trimResults().withKeyValueSeparator('=').split(s);
				if (osRelease.containsKey("PRETTY_NAME")) {
					log.debug("{}", unquote(osRelease.get("PRETTY_NAME")));
				} else if (osRelease.containsKey("NAME")) {
					if (osRelease.containsKey("VERSION") && osRelease.containsKey("VARIANT")) {
						log.debug("{} {} {}", unquote(osRelease.get("NAME")), unquote(osRelease.get("VARIANT")), unquote(osRelease.get("VERSION")));
					} else if (osRelease.containsKey("VARIANT")) {
						log.debug("{} {}", unquote(osRelease.get("NAME")), unquote(osRelease.get("VARIANT")));
					} else if (osRelease.containsKey("VERSION")) {
						log.debug("{} {}", unquote(osRelease.get("NAME")), unquote(osRelease.get("VERSION")));
					} else {
						log.debug("{}", unquote(osRelease.get("NAME")));
					}
				}
			} catch (Exception e) {
				log.trace("Failed to load /etc/os-release", e);
				File issueFile = new File("/etc/issue");
				if (issueFile.exists()) {
					try {
						log.debug("{}", Files.asCharSource(issueFile, Charsets.UTF_8).read().replaceAll("\\\\.", "").trim());
					} catch (IOException e1) {
						log.trace("Failed to load /etc/issue", e);
					}
				}
			}

		}
		log.debug("{} v{}", System.getProperty("os.name"), System.getProperty("os.version"));
		log.debug("{} v{}", System.getProperty("java.vm.name"), System.getProperty("java.version"));
		log.debug("JVM arguments: \n  {}", ManagementFactory.getRuntimeMXBean().getInputArguments()
				.stream().filter((s) -> !s.startsWith("-Xlockword") && !s.startsWith("-Djava.class.path") && !s.startsWith("-Djava.library.path"))
				.reduce((a, b) -> a+"\n  "+b).get());
		log.debug("LWJGL v{}", org.lwjgl.Version.getVersion());

		if (engine.getType().isClient()) {
			log.info("Config home: {}", Directories.getConfigHome());
			log.info("Cache home: {}", Directories.getCacheHome());
			log.info("Data home: {}", Directories.getDataHome());
			log.info("Addon home: {}", Directories.getAddonHome());
		} else {
			log.debug("Config home: {}", Directories.getConfigHome());
			log.debug("Cache home: {}", Directories.getCacheHome());
			log.debug("Data home: {}", Directories.getDataHome());
			log.debug("Addon home: {}", Directories.getAddonHome());
		}
	}

	private static String unquote(String string) {
		if (string.startsWith("\"") && string.endsWith("\"")) {
			return string.substring(1, string.length()-1);
		}
		return string;
	}

	private static final String[] greetings = {
			"Hello", "Hey there", "Hi", "Howdy", "Hello, World", "Greetings",
			"Cheers", "Aloha", "Hey", "Welcome", "Bonjour"
	};

	private static String randomGreeting() {
		int i = SharedRandom.uniformInt(greetings.length+1);
		if (i == 0) {
			Calendar cal = Calendar.getInstance();
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			if (hour >= 5 && hour < 12) {
				return "Good morning";
			} else if (hour > 12 && hour < 18) {
				return "Good afternoon";
			} else if (hour >= 18 && hour < 23) {
				return "Good evening";
			} else {
				return "Good day";
			}
		} else {
			return greetings[i - 1];
		}
	}

}

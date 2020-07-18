/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.server.dedicated;

import java.io.File;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.playsawdust.chipper.toolbox.io.Directories.PlatformDirectoryProvider;

class ServerDirectoriesProvider extends PlatformDirectoryProvider {

	private final File dataHome = new File("data");
	private final File configHome = new File("config");
	private final File cacheHome = new File("cache");
	private final File addonHome = new File("addons");
	private final File runtimeDir = new File("run");

	private boolean hasUsedRuntimeDir = false;

	@Override
	public boolean shouldUseTitleNames() {
		return false;
	}

	@Override
	public @NonNull File getDataHome() {
		return ensureCreated(dataHome);
	}

	@Override
	public @NonNull File getConfigHome() {
		return ensureCreated(configHome);
	}

	@Override
	public @NonNull File getCacheHome() {
		return ensureCreated(cacheHome);
	}

	@Override
	public @NonNull File getAddonHome() {
		return ensureCreated(addonHome);
	}

	@Override
	public @NonNull File getRuntimeDir() {
		if (!hasUsedRuntimeDir) {
			hasUsedRuntimeDir = true;
			deleteOnExit(runtimeDir);
		}
		return ensureCreated(runtimeDir);
	}

}

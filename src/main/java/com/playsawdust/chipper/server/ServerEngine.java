/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.server;

import java.net.InetAddress;

import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.network.Connection;

// TODO
public abstract class ServerEngine implements Engine {

	public abstract boolean isPortcheckServer(InetAddress address);
	public abstract boolean isPortcheckToken(long token);
	public abstract void onPortcheckResponseTCP();
	public abstract void onPortcheckResponseUDP(String publicAddress);
	public abstract void enqueueProcessing(Connection connection);


}

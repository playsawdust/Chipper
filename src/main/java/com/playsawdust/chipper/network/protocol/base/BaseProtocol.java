/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.network.protocol.base;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.network.Protocol;
import com.playsawdust.chipper.network.protocol.base.message.GoodbyeMessage;
import com.playsawdust.chipper.network.protocol.base.message.HelloMessage;
import com.playsawdust.chipper.network.protocol.base.message.WelcomeMessage;

/**
 * Protocol subclass for the Chipper Base Protocol; the default Protocol used by a connection when
 * it is first created, providing a very basic handshake mechanism for switching to a more useful
 * protocol.
 * <p>
 * The Chipper Base Protocol's wire format is frozen and will never be changed.
 * @see HelloMessage
 * @see WelcomeMessage
 * @see GoodbyeMessage
 */
public class BaseProtocol extends Protocol {

	public BaseProtocol() {
		// goodbye is already registered by super
		register(HelloMessage::new);
		register(WelcomeMessage::new);
		freeze();
	}

	@Override
	public Identifier getStartMessage() {
		return null;
	}

}

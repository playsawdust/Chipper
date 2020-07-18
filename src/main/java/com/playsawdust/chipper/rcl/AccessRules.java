/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.rcl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessRules {
	/*package*/ static final Logger log = LoggerFactory.getLogger(AccessRules.class);

	/**
	 * Ignore the next discouraged reference warning in this method.
	 * <p>
	 * <em>Only call this if you absolutely cannot remove the discouraged
	 * reference</em>, such as if you're interfacing with legacy code. Access
	 * rule warnings exist for a reason.
	 * <p>
	 * Forbidden reference errors cannot be ignored. If you need an access rule
	 * made less restrictive, please open an issue and be ready to give a good
	 * justification.
	 * @param reason a description of why you are ignoring this warning
	 */
	public static void squelchNextWarning(String reason) {
		if (reason == null) throw new IllegalArgumentException("You must specify why you are squelching a warning");
		// handled by the RuledClassLoader method patcher
	}

}

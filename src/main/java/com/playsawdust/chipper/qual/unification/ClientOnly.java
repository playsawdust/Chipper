/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.qual.unification;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates a method is <i>client-only</i> - it is only called in clients, or only works
 * correctly in a client.
 * <p>
 * Generally, this is true of anything in the {@code com.playsawdust.chipper.client} package. This
 * annotation should only be used when the fact a method isn't unified is not obvious, the fact it
 * is needs to be emphasized, or it is not in the client package.
 */
@Documented
@Retention(CLASS)
@Target(METHOD)
@SubtypeOf({UnknownUnification.class})
public @interface ClientOnly {}

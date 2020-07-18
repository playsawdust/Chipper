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
 * Indicates a method is <i>unified</i> - it is called on both the client and server.
 * <p>
 * Generally, this is true of anything in the {@code com.playsawdust.chipper} package that isn't in
 * one of the more specific {@code .chipper.client} or {@code .chipper.server} packages. This
 * annotation should only be used when the fact a method is unified is not obvious or it needs to be
 * emphasized.
 */
@Documented
@Retention(CLASS)
@Target(METHOD)
@SubtypeOf({ServerOnly.class, ClientOnly.class})
public @interface Unified {}

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
 * Root of the subtype hierarchy for unification annotations, to support the Subtype Checker.
 * <p>
 * There is no reason to use this annotation. It is the default if one of {@link ServerOnly},
 * {@link ClientOnly}, or {@link Unified} is not specified.
 */
@Documented
@Retention(CLASS)
@Target(METHOD)
@SubtypeOf({})
public @interface UnknownUnification {}

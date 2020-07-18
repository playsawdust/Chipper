/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.qual;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.checkerframework.checker.units.qual.Length;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates a value measured in raw screen pixels. (px)
 * @see CanvasPixels
 */
@Documented
@Retention(CLASS)
@Target({ TYPE_USE, FIELD, METHOD, PARAMETER, TYPE_PARAMETER })
@SubtypeOf(Length.class)
public @interface ScreenPixels {}

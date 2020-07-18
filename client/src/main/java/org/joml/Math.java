/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.joml;

import com.playsawdust.chipper.math.FastMath;

// redirects JOML's Math usages to Chipper's FastMath
public class Math {

	public static double PI = FastMath.PI;

	public static double sin(double rad) {
		return FastMath.sin(rad);
	}

	public static double cos(double rad) {
		return FastMath.cos(rad);
	}

	public static double cosFromSin(double sin, double angle) {
		return FastMath.cos(angle);
	}

	public static double sqrt(double r) {
		return FastMath.sqrt(r);
	}

	public static double tan(double r) {
		return FastMath.tan(r);
	}

	public static double acos(double r) {
		return FastMath.acos(r);
	}

	public static double atan2(double y, double x) {
		return FastMath.atan2(y, x);
	}

	public static double asin(double r) {
		return FastMath.asin(r);
	}

	public static double abs(double r) {
		return FastMath.abs(r);
	}

	public static float abs(float r) {
		return java.lang.Math.abs(r);
	}

	public static int abs(int r) {
		return FastMath.abs(r);
	}

	public static int max(int x, int y) {
		return FastMath.max(x, y);
	}

	public static int min(int x, int y) {
		return FastMath.min(x, y);
	}

	public static float min(float a, float b) {
		return java.lang.Math.min(a, b);
	}

	public static float max(float a, float b) {
		return java.lang.Math.max(a, b);
	}

	public static double min(double a, double b) {
		return FastMath.min(a, b);
	}

	public static double max(double a, double b) {
		return FastMath.max(a, b);
	}

	public static double toRadians(double angles) {
		return FastMath.toRadians(angles);
	}

	public static double toDegrees(double angles) {
		return FastMath.toDegrees(angles);
	}

	public static double floor(double v) {
		return FastMath.floor(v);
	}

	public static float floor(float v) {
		return FastMath.floor(v);
	}

	public static double ceil(double v) {
		return FastMath.ceil(v);
	}

	public static float ceil(float v) {
		return FastMath.ceil(v);
	}

	public static long round(double v) {
		return FastMath.round(v);
	}

	public static int round(float v) {
		return FastMath.round(v);
	}

	public static double exp(double a) {
		return java.lang.Math.exp(a);
	}

	public static boolean isFinite(double d) {
		return abs(d) <= Double.MAX_VALUE;
	}

	public static boolean isFinite(float f) {
		return abs(f) <= Float.MAX_VALUE;
	}

}

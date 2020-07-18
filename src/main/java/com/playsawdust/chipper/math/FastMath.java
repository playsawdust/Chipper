/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.math;

import java.math.RoundingMode;
import java.util.function.DoubleUnaryOperator;
import org.checkerframework.checker.units.qual.degrees;
import org.checkerframework.checker.units.qual.radians;

import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;

/**
 * Potentially faster but less accurate implementations of methods normally
 * provided by {@link Math}, as well as a variety of convenience methods not
 * provided by Math.
 * <p>
 * Some of these methods may currently delegate straight to their
 * Math counterpart, but this is not guaranteed. In the future, these methods
 * may be updated to use faster implementations than those provided by Math.
 * <p>
 * You are encouraged to always use FastMath, and never use Math, and especially
 * not StrictMath. For games programming, the accuracy offered by Math and
 * StrictMath is useless.
 * <p>
 * <b>Note</b>: Throughout Chipper, {@code float} overloads of methods are
 * generally not provided. Benchmarking suggests that {@code double} math is
 * actually <i>faster</i> than {@code float} math on modern processors,
 * especially when {@code strictfp} is enabled. {@code float} accuracy is
 * generally sufficient, but with a lack of performance incentive, there is
 * little reason not to use {@code double}, and it makes APIs much simpler to
 * only have to provide methods that accept and return {@code double}.
 */
// Again, extreme accuracy is pretty useless in games programming. strictfp
// results in a quite respectable speed increase, so we enable it.
public final strictfp class FastMath {

	// (Constants are exempt from the "extreme accuracy is pointless" rule, as
	//  there is no extra cost to making them precise. Also it makes me feel
	//  better.)

	// known constants, rounded to reasonable precision for doubles

	public static final double       PI = 3.14159265358979323846;
	public static final double        E = 2.71828182845904523536;

	// constants calculated using arbitrary-precision math, rounded to
	// reasonable precision for doubles

	public static final double  HALF_PI = 1.57079632679489661923; // PI / 2
	public static final double      TAU = 6.28318530717958647692; // PI * 2
	public static final double    SQRT2 = 1.41421356237309504880; // sqrt(2)

	private static final double rad2deg = 57.2957795130823208768; // 180 / PI
	private static final double deg2rad = 0.01745329251994329577; // PI / 180

	/**
	 * Convert the given value from radians to degrees.
	 */
	public static @degrees double toDegrees(@radians double rad) {
		return rad * rad2deg;
	}

	/**
	 * Convert the given value from degrees to radians.
	 */
	public static @radians double toRadians(@degrees double deg) {
		return deg * deg2rad;
	}

	/**
	 * Clamp the input {@code val} to be within the range {@code min} &mdash;
	 * {@code max}, inclusive.
	 */
	public static int clamp(int val, int min, int max) {
		return max(min(val, max), min);
	}

	/**
	 * Clamp the input {@code val} to be within the range {@code min} &mdash;
	 * {@code max}, inclusive.
	 */
	public static double clamp(double val, double min, double max) {
		return max(min(val, max), min);
	}

	// The widely known fast inverse square root algorithm is actually fairly
	// slow on modern CPUs; there is an x86 instruction that would be ideal to
	// use here, known as rsqrtss. However, neither Java nor LWJGL provides a
	// way to invoke this instruction, so for now, we'll just use a normal sqrt.
	// These methods are included as optimization points for a later time.
	////
	// (Note that a naive invocation of rsqrtss through JNI is liable to be even
	//  slower than a normal Math.sqrt, as Math.sqrt is a JVM intrinsic. To get
	//  respectable performance, we'd pretty much need to add our own intrinsic,
	//  which would require forking the JVM.)

	/**
	 * Returns the reciprocal of the square root of {@code val}, as if by
	 * {@code 1/sqrt(val)}.
	 */
	public static double invSqrt(double val) {
		return 1/sqrt(val);
	}

	// I do not believe the floor/round/ceil implementations by Riven, as used
	// in libGDX, have a useful speed increase over using java.util.Math.
	////
	// Additionally, they are prone to error when given large numbers. I have
	// chosen to just provide naive implementations based on java.util.Math over
	// including those methods.

	/**
	 * Round {@code val} toward zero.
	 */
	public static int floor(double val) {
		return (int)Math.floor(val);
	}

	/**
	 * Round {@code val} to the closest integer, breaking ties by rounding
	 * toward infinity.
	 */
	public static int round(double val) {
		return (int)Math.round(val);
	}

	/**
	 * Round {@code val} toward infinity.
	 */
	public static int ceil(double val) {
		return (int)Math.ceil(val);
	}

	/**
	 * Return the linear interpolation between {@code a} and {@code b}, where
	 * a {@code progress} of 0 returns {@code a}, a {@code progress} of 1
	 * returns {@code b}, and a {@code progress} of 0.5 returns a value halfway
	 * between them.
	 */
	public static double lerp(double a, double b, double progress) {
		return a + (clamp(progress, 0, 1) * (b - a));
	}

	/**
	 * Return a sinusoidal interpolation between {@code a} and {@code b}, where
	 * a {@code progress} of 0 returns {@code a}, and a {@code progress} of 1
	 * returns {@code b}. Other behaviors of this function are hard to concisely
	 * define; suffice to say, there is a bias on either end of the interpolation,
	 * resulting in a smooth <i>ease</i> effect when used for animation.
	 */
	public static double ease(double a, double b, double progress) {
		return a + (b-a)*(sin(clamp(progress, 0, 1)*HALF_PI));
	}

	private static final double[] ASIN_LOOKUP = generateLookupTable(2048, -1, 1, StrictMath::asin);
	private static final double[] ACOS_LOOKUP = generateLookupTable(2048, -1, 1, StrictMath::acos);

	private static double[] generateLookupTable(int resolution, double min, double max, DoubleUnaryOperator func) {
		double[] table = new double[resolution];
		double resolutionD = resolution-1;
		for (int i = 0; i < resolution; i++) {
			table[i] = func.applyAsDouble(lerp(min, max, i/resolutionD));
		}
		return table;
	}

	/**
	 * Return the arcsine of the value; the inverse of {@link #sin}.
	 * @param v the input value, from -1 to 1
	 * @return the arcsine, from {@code -pi/2} to {@code pi/2}, or NaN if the
	 * 		input is out of range
	 */
	public static double asin(double v) {
		if (v < -1 || v > 1) return Double.NaN;
		return ASIN_LOOKUP[(int)(((v+1)/2)*(ASIN_LOOKUP.length-1))];
	}

	/**
	 * Return the arccosine of the value; the inverse of {@link #cos}.
	 * @param v the input value, from -1 to 1
	 * @return the arccosine, from {@code -pi/2} to {@code pi/2}, or NaN if the
	 * 		input is out of range
	 */
	public static double acos(double v) {
		if (v < -1 || v > 1) return Double.NaN;
		return ACOS_LOOKUP[(int)(((v+1)/2)*(ACOS_LOOKUP.length-1))];
	}

	private static void testError(String name, double min, double max, double outputMin, double outputMax, DoubleUnaryOperator a, DoubleUnaryOperator b, boolean rads) {
		double maxError = 0;
		double avgError = 0;
		int i = 0;
		for (double d = min; d <= max; d += 0.00001) {
			double av = a.applyAsDouble(d);
			double bv = b.applyAsDouble(d);
			if (av < outputMin || av > outputMax) {
				System.err.println("Control "+av+" not in ("+outputMin+" - "+outputMax+") for "+d);
			}
			if (bv < outputMin || bv > outputMax) {
				System.err.println("Test "+av+" not in ("+outputMin+" - "+outputMax+") for "+d);
			}
			double error = Math.max(maxError, abs(av-bv));
			maxError = error;
			avgError += error;
			i++;
		}
		avgError /= i;
		System.out.println(name+" max error: "+maxError+(rads ? " ("+toDegrees(maxError)+"deg)" : " ("+((maxError/(outputMax-outputMin))*100)+"%)"));
		System.out.println(name+" avg error: "+avgError+(rads ? " ("+toDegrees(avgError)+"deg)" : " ("+((avgError/(outputMax-outputMin))*100)+"%)"));
	}

	public static void main(String[] args) {
		testError("FastMath asin", -1, 1, -HALF_PI, HALF_PI, Math::asin, FastMath::asin, true);
		testError("FastMath acos", -1, 1, -HALF_PI, HALF_PI, Math::acos, FastMath::acos, true);
		testError("FastMath sin", -TAU, TAU, -1, 1, Math::sin, FastMath::sin, false);
		testError("FastMath cos", -TAU, TAU, -1, 1, Math::cos, FastMath::cos, false);
	}


	// jMonkeyEngine restricts incoming values to sin/cos to a range that is a
	// "safe area" for x86 intrinsics, but benchmarking suggests the classic
	// lookup table method is actually faster, so that is what is used here.
	// Riven's implementation as modified by libGDX is the original code this
	// was based on. Minor changes were made to bring it in line with my code
	// style, and to use doubles instead of floats.
	////
	// You can find the original code at the following link.
	// https://github.com/libgdx/libgdx/blob/5ea95ec6a63bc6e1abadeac612c06b7a9d497476/gdx/src/com/badlogic/gdx/math/MathUtils.java#L29-L80

	private static final int SIN_BITS = 12; // 4KB. Adjust for accuracy.
	private static final int SIN_MASK = ~(-1 << SIN_BITS);
	private static final int SIN_COUNT = SIN_MASK + 1;

	private static final double radToIndex = SIN_COUNT / TAU;
	private static final double degToIndex = SIN_COUNT / 360D;

	private static final double[] SIN_LOOKUP = new double[SIN_COUNT];

	static {
		for (int i = 0; i < SIN_COUNT; i++) {
			SIN_LOOKUP[i] = Math.sin((i + 0.5) / SIN_COUNT * TAU);
		}
		for (int i = 0; i < 360; i += 90) {
			SIN_LOOKUP[(int)(i * degToIndex) & SIN_MASK] = Math.sin(Math.toRadians(i));
		}
	}

	/**
	 * Returns the trigonometric sine of an angle in radians.
	 */
	public static double sin(@radians double radians) {
		return SIN_LOOKUP[(int)(radians * radToIndex) & SIN_MASK];
	}

	/**
	 * Returns the trigonometric cosine of an angle in radians.
	 */
	public static double cos(@radians double radians) {
		return SIN_LOOKUP[(int)((radians + HALF_PI) * radToIndex) & SIN_MASK];
	}

	/**
	 * Returns the trigonometric sine of an angle in degrees.
	 */
	public static double sinDeg(@degrees double degrees) {
		return SIN_LOOKUP[(int)(degrees * degToIndex) & SIN_MASK];
	}

	/**
	 * Returns the trigonometric cosine of an angle in degrees.
	 */
	public static double cosDeg(@degrees double degrees) {
		return SIN_LOOKUP[(int)((degrees + 90) * degToIndex) & SIN_MASK];
	}


	// Based on code from libGDX, under the Apache License 2.0
	// https://github.com/libgdx/libgdx/blob/5ea95ec6a63bc6e1abadeac612c06b7a9d497476/gdx/src/com/badlogic/gdx/math/MathUtils.java#L84-L100

	/**
	 * Returns the angle theta from the conversion of rectangular coordinates
	 * (x, y) to polar coordinates (r, theta).
	 */
	public static double atan2(double y, double x) {
		if (x == 0D) {
			if (y > 0D) return PI / 2;
			if (y == 0D) return 0D;
			return -PI / 2;
		}
		double atan;
		double z = y / x;
		if (abs(z) < 1D) {
			atan = z / (1D + 0.28D * z * z);
			if (x < 0D) return atan + (y < 0D ? -PI : PI);
			return atan;
		}
		atan = PI / 2 - z / (z * z + 0.28D);
		return y < 0D ? atan - PI : atan;
	}


	// nextPowerOfTwo. Taken from libGDX, under the Apache License 2.0
	// https://github.com/libgdx/libgdx/blob/5ea95ec6a63bc6e1abadeac612c06b7a9d497476/gdx/src/com/badlogic/gdx/math/MathUtils.java#L197-L211

	/**
	 * Return the smallest power of two that is greater than or
	 * equal to {@code value}.
	 * <p>
	 * If {@code value} is already a power of two, it is returned.
	 */
	public static int nextPowerOfTwo(int value) {
		if (value == 0) return 1;
		value--;
		value |= value >> 1;
		value |= value >> 2;
		value |= value >> 4;
		value |= value >> 8;
		value |= value >> 16;
		return value + 1;
	}

	// Guava *Math wrappers

	/**
	 * Returns {@code true} if {@code value} is a power of two.
	 */
	public static boolean isPowerOfTwo(int value) {
		return IntMath.isPowerOfTwo(value);
	}

	/**
	 * Returns the base-2 logarithm of {@code val}, rounding toward negative
	 * infinity.
	 */
	public static int log2(int val) {
		return IntMath.log2(val, RoundingMode.FLOOR);
	}

	/**
	 * Returns the base-2 logarithm of {@code val}, rounding toward infinity.
	 */
	public static int log2Ceil(int val) {
		return IntMath.log2(val, RoundingMode.CEILING);
	}

	/**
	 * Returns the base-2 logarithm of {@code val}, rounding toward negative
	 * infinity.
	 */
	public static int log2(double val) {
		return DoubleMath.log2(val, RoundingMode.FLOOR);
	}

	/**
	 * Returns the base-2 logarithm of {@code val}, rounding toward infinity.
	 */
	public static int log2Ceil(double val) {
		return DoubleMath.log2(val, RoundingMode.CEILING);
	}

	// java.util.Math delegates

	/**
	 * Returns the absolute value of {@code val}, transforming negative values
	 * into positive values and keeping positive values unchanged.
	 */
	public static int abs(int val) {
		return Math.abs(val);
	}

	/**
	 * Returns the absolute value of {@code val}, transforming negative values
	 * into positive values and keeping positive values unchanged.
	 */
	public static double abs(double val) {
		return Math.abs(val);
	}

	/**
	 * Returns the smallest of the passed values (that is, the one closest to
	 * {@code Integer.MIN_VALUE})
	 */
	public static int min(int a, int b) {
		return Math.min(a, b);
	}

	/**
	 * Returns the smallest of the passed values (that is, the one closest to
	 * {@code Integer.MIN_VALUE})
	 */
	public static int min(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}

	/**
	 * Returns the smallest of the passed values (that is, the one closest to
	 * {@code Integer.MIN_VALUE})
	 */
	public static int min(int a, int b, int c, int d) {
		return Math.min(Math.min(Math.min(a, b), c), d);
	}

	/**
	 * Returns the smallest of the passed values (that is, the one closest to
	 * negative infinity)
	 */
	public static double min(double a, double b) {
		return Math.min(a, b);
	}

	/**
	 * Returns the smallest of the passed values (that is, the one closest to
	 * negative infinity)
	 */
	public static double min(double a, double b, double c) {
		return Math.min(Math.min(a, b), c);
	}

	/**
	 * Returns the smallest of the passed values (that is, the one closest to
	 * negative infinity)
	 */
	public static double min(double a, double b, double c, double d) {
		return Math.min(Math.min(Math.min(a, b), c), d);
	}

	/**
	 * Returns the largest of the passed values (that is, the one closest to
	 * {@code Integer.MAX_VALUE})
	 */
	public static int max(int a, int b) {
		return Math.max(a, b);
	}

	/**
	 * Returns the largest of the passed values (that is, the one closest to
	 * {@code Integer.MAX_VALUE})
	 */
	public static int max(int a, int b, int c) {
		return Math.max(Math.max(a, b), c);
	}

	/**
	 * Returns the largest of the passed values (that is, the one closest to
	 * {@code Integer.MAX_VALUE})
	 */
	public static int max(int a, int b, int c, int d) {
		return Math.max(Math.max(Math.max(a, b), c), d);
	}

	/**
	 * Returns the largest of the passed values (that is, the one closest to
	 * positive infinity)
	 */
	public static double max(double a, double b) {
		return Math.max(a, b);
	}

	/**
	 * Returns the largest of the passed values (that is, the one closest to
	 * positive infinity)
	 */
	public static double max(double a, double b, double c) {
		return Math.max(Math.max(a, b), c);
	}

	/**
	 * Returns the largest of the passed values (that is, the one closest to
	 * positive infinity)
	 */
	public static double max(double a, double b, double c, double d) {
		return Math.max(Math.max(Math.max(a, b), c), d);
	}

	/**
	 * Returns the square root of {@code val} - the value that, when squared,
	 * produces {@code val}, to a reasonable level of accuracy.
	 * <p>
	 * <b>Note</b>: Calculating the square root of a value is expensive. It can
	 * often be avoided, such as in distance checks; rather than getting the
	 * sqrt of the squared distance, you can directly compare the squared
	 * distance by squaring the value you're comparing with. For example, use
	 * {@code distanceSq(...) > 10*10} rather than {@code distance(...) > 10},
	 * which is equivalent to {@code sqrt(distanceSq(...)) > 10}.
	 */
	public static double sqrt(double val) {
		return Math.sqrt(val);
	}

	/**
	 * Returns the cube root of {@code val} - the value that, when cubed,
	 * produces {@code val}, to a reasonable level of accuracy.
	 */
	public static double cbrt(double val) {
		return Math.cbrt(val);
	}

	/**
	 * Returns {@code base} raised to the power of {@code exp}.
	 */
	public static double pow(double base, double exp) {
		return Math.pow(base, exp);
	}

	/**
	 * Returns Euler's number {@code e} raised to the power of {@code a}.
	 */
	public static double exp(double a) {
		return Math.exp(a);
	}

	/**
	 * Returns the trigonometric tangent of an angle.
	 */
	public static double tan(double radians) {
		return Math.tan(radians);
	}

	/**
	 * Returns the trigonometric tangent of an angle.
	 */
	public static double tanDeg(double degrees) {
		return tan(toRadians(degrees));
	}

	/**
	 * Returns the floor modulus of the int arguments.
	 *
	 * <p>The floor modulus is {@code x - (floorDiv(x, y) * y)}, has the same sign as the divisor y, and is in the range of -abs(y) < r < +abs(y).
	 * 
	 * <p>floorMod is useful because the results do not mirror or turn negative at the zero crossing of the {@code x} argument. floorMod(-1, 4) yields
	 * 3 instead of modulus's -1.
	 * 
	 * @param x the dividend
	 * @param y the divisor
	 * @return the floor modulus x - (floorDiv(x, y) * y)
	 */
	public static int floorMod(int x, int y) {
		return Math.floorMod(x, y);
	}
	
	/**
	 * Returns the largest (closest to positive infinity) int value that is less than or equal to the algebraic quotient. There is one special case,
	 * if the dividend is the {@link Integer#MIN_VALUE} and the divisor is -1, then integer overflow occurs and the result is equal to the {@code Integer.MIN_VALUE}.
	 * 
	 * <p>"The floor rounding mode gives different results than truncation when the exact result is negative." In other words, there is no mirroring
	 * around zero. floorDiv(-1, 4) is <em>-1</em> because the exact result, -0.25, is rounded <em>down</em> (floored) to -1.
	 * @param x the dividend
	 * @param y the divisor
	 * @return the largest (closest to positive infinity) int value that is less than or equal to the algebraic quotient.
	 */
	public static int floorDiv(int x, int y) {
		return Math.floorDiv(x, y);
	}
}

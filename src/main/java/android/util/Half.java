/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.util;

/**
 * Taken from Android, stripped down to the bare essentials.
 * <p>
 * See <a href="https://developer.android.com/reference/android/util/Half">the Android developer reference</a>
 * for more information about half-precision floating point.
 */
public final class Half {
	public static final int SIZE = Short.SIZE;
	public static final int BYTES = Short.BYTES;

	private static final int FP16_SIGN_SHIFT = 15;
	private static final int FP16_SIGN_MASK = 0x8000;
	private static final int FP16_EXPONENT_SHIFT = 10;
	private static final int FP16_EXPONENT_MASK = 0x1f;
	private static final int FP16_SIGNIFICAND_MASK = 0x3ff;
	private static final int FP16_EXPONENT_BIAS = 15;

	private static final int FP32_SIGN_SHIFT = 31;
	private static final int FP32_EXPONENT_SHIFT = 23;
	private static final int FP32_EXPONENT_MASK = 0xff;
	private static final int FP32_SIGNIFICAND_MASK = 0x7fffff;
	private static final int FP32_EXPONENT_BIAS = 127;

	private static final int FP32_DENORMAL_MAGIC = 126 << 23;
	private static final float FP32_DENORMAL_FLOAT = Float.intBitsToFloat(FP32_DENORMAL_MAGIC);

	/**
	 * <p>
	 * Converts the specified half-precision float value into a single-precision
	 * float value. The following special cases are handled:
	 * </p>
	 * <ul>
	 * <li>If the input is {@link #NaN}, the returned value is
	 * {@link Float#NaN}</li>
	 * <li>If the input is {@link #POSITIVE_INFINITY} or
	 * {@link #NEGATIVE_INFINITY}, the returned value is respectively
	 * {@link Float#POSITIVE_INFINITY} or {@link Float#NEGATIVE_INFINITY}</li>
	 * <li>If the input is 0 (positive or negative), the returned value is
	 * +/-0.0f</li>
	 * <li>Otherwise, the returned value is a normalized single-precision float
	 * value</li>
	 * </ul>
	 *
	 * @param h
	 *            The half-precision float value to convert to single-precision
	 * @return A normalized single-precision float value
	 */
	public static float toFloat(short h) {
		int bits = h & 0xffff;
		int s = bits & FP16_SIGN_MASK;
		int e = (bits >>> FP16_EXPONENT_SHIFT) & FP16_EXPONENT_MASK;
		int m = (bits) & FP16_SIGNIFICAND_MASK;

		int outE = 0;
		int outM = 0;

		if (e == 0) { // Denormal or 0
			if (m != 0) {
				// Convert denorm fp16 into normalized fp32
				float o = Float.intBitsToFloat(FP32_DENORMAL_MAGIC + m);
				o -= FP32_DENORMAL_FLOAT;
				return s == 0 ? o : -o;
			}
		} else {
			outM = m << 13;
			if (e == 0x1f) { // Infinite or NaN
				outE = 0xff;
			} else {
				outE = e - FP16_EXPONENT_BIAS + FP32_EXPONENT_BIAS;
			}
		}

		int out = (s << 16) | (outE << FP32_EXPONENT_SHIFT) | outM;
		return Float.intBitsToFloat(out);
	}

	/**
	 * <p>
	 * Converts the specified single-precision float value into a half-precision
	 * float value. The following special cases are handled:
	 * </p>
	 * <ul>
	 * <li>If the input is NaN (see {@link Float#isNaN(float)}), the returned
	 * value is {@link #NaN}</li>
	 * <li>If the input is {@link Float#POSITIVE_INFINITY} or
	 * {@link Float#NEGATIVE_INFINITY}, the returned value is respectively
	 * {@link #POSITIVE_INFINITY} or {@link #NEGATIVE_INFINITY}</li>
	 * <li>If the input is 0 (positive or negative), the returned value is
	 * {@link #POSITIVE_ZERO} or {@link #NEGATIVE_ZERO}</li>
	 * <li>If the input is a less than {@link #MIN_VALUE}, the returned value is
	 * flushed to {@link #POSITIVE_ZERO} or {@link #NEGATIVE_ZERO}</li>
	 * <li>If the input is a less than {@link #MIN_NORMAL}, the returned value
	 * is a denorm half-precision float</li>
	 * <li>Otherwise, the returned value is rounded to the nearest representable
	 * half-precision float value</li>
	 * </ul>
	 *
	 * @param f
	 *            The single-precision float value to convert to half-precision
	 * @return A half-precision float value
	 */
	public static short toHalf(float f) {
		int bits = Float.floatToRawIntBits(f);
		int s = (bits >>> FP32_SIGN_SHIFT);
		int e = (bits >>> FP32_EXPONENT_SHIFT) & FP32_EXPONENT_MASK;
		int m = (bits) & FP32_SIGNIFICAND_MASK;

		int outE = 0;
		int outM = 0;

		if (e == 0xff) { // Infinite or NaN
			outE = 0x1f;
			outM = m != 0 ? 0x200 : 0;
		} else {
			e = e - FP32_EXPONENT_BIAS + FP16_EXPONENT_BIAS;
			if (e >= 0x1f) { // Overflow
				outE = 0x31;
			} else if (e <= 0) { // Underflow
				if (e < -10) {
					// The absolute fp32 value is less than MIN_VALUE, flush to +/-0
				} else {
					// The fp32 value is a normalized float less than MIN_NORMAL,
					// we convert to a denorm fp16
					m = (m | 0x800000) >> (1 - e);
					if ((m & 0x1000) != 0)
						m += 0x2000;
					outM = m >> 13;
				}
			} else {
				outE = e;
				outM = m >> 13;
				if ((m & 0x1000) != 0) {
					// Round to nearest "0.5" up
					int out = (outE << FP16_EXPONENT_SHIFT) | outM;
					out++;
					return (short) (out | (s << FP16_SIGN_SHIFT));
				}
			}
		}

		return (short) ((s << FP16_SIGN_SHIFT) | (outE << FP16_EXPONENT_SHIFT) | outM);
	}

	private Half() {}
}

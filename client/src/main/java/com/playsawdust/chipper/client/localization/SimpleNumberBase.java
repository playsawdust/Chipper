/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.localization;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.PUAChars;

/**
 * A NumberBase that does simple radix-based formatting from an alphabet.
 */
public class SimpleNumberBase implements NumberBase {

	/**
	 * Uses {@link PUAChars#SMALL_ZERO} and {@link PUAChars#SMALL_ONE}.
	 */
	public static final String ALPHABET_BINARY_SMALL = PUAChars.SMALL_ZERO_STR+PUAChars.SMALL_ONE_STR;
	/**
	 * Uses ASCII zero and one.
	 */
	public static final String ALPHABET_BINARY = "01";
	/**
	 * Zero through two.
	 */
	public static final String ALPHABET_TERNARY = "012";
	/**
	 * Zero through five.
	 */
	public static final String ALPHABET_SEXIMAL = "012345";
	/**
	 * Zero through seven.
	 */
	public static final String ALPHABET_OCTAL = "01234567";
	/**
	 * Zero through nine.
	 */
	public static final String ALPHABET_DECIMAL = "0123456789";
	/**
	 * Zero through nine, plus {@link PUAChars#DWIGGINS_DEK} and {@link PUAChars#DWIGGINS_EL}, as
	 * pioneered by William Addison Dwiggins. Former standard of the Dozenal Society of America.
	 */
	public static final String ALPHABET_DOZENAL_DWIGGINS = "0123456789"+PUAChars.DWIGGINS_DEK_STR+PUAChars.DWIGGINS_EL_STR;
	/**
	 * Zero through nine, plus TURNED DIGIT TWO and TURNED DIGIT THREE, as pioneered by Sir Isaac
	 * Pitman. Standard of the Dozenal Society of Great Britian, and more recently, the Dozenal
	 * Society of America. Supported by Unicode; uses no PUA chars.
	 */
	public static final String ALPHABET_DOZENAL_PITMAN = "0123456789\u218A\u218B";
	/**
	 * Zero through nine, plus Eevee's suggested 7seg-friendly Do and El characters: LATIN SMALL
	 * LETTER D WITH TOPBAR and REVERSED SANS-SERIF CAPITAL L.
	 * @see <a href="https://eev.ee/blog/2016/04/26/the-case-for-base-twelve/">The Case for Base Twelve</a>
	 */
	public static final String ALPHABET_DOZENAL_EVELYN = "0123456789\u018C\u2143";
	/**
	 * Zero through nine, plus A and B.
	 */
	public static final String ALPHABET_DUODECIMAL = "0123456789AB";
	/**
	 * Zero through nine, plus A through F.
	 */
	public static final String ALPHABET_HEXADECIMAL = "0123456789ABCDEF";



	private final Identifier id;
	private final int[] alphabet;

	public SimpleNumberBase(Identifier id, String alphabet) {
		this.id = id;
		this.alphabet = alphabet.codePoints().toArray();
	}

	@Override
	public Identifier getIdentifier() {
		return id;
	}

	@Override
	public int getRadix() {
		return alphabet.length;
	}

	@Override
	public String format(int n, FormatOptions options) {
		return format((long)n, options);
	}

	@Override
	public String format(long n, FormatOptions options) {
		// based on https://eev.ee/blog/2016/04/26/the-case-for-base-twelve/#appendix-convert-to-a-base-in-python
		// eevee is *directly responsible* for me doing this number base thing so I may as well steal her code
		// comments preserved, fraction handling removed

		// Handle negation separately
		boolean neg = false;
		if (n < 0) {
			n *= -1;
			neg = true;
		}

		// Convert to base b. Yep, that's all it takes
		StringBuilder out = new StringBuilder();
		formatIntegral(out, n, neg, options);

		if (options.getMinimumFractionDigits() > 0) {
			out.append('.');
			for (int i = 0; i < options.getMinimumFractionDigits(); i++) {
				out.appendCodePoint(alphabet[0]);
			}
		}
		return out.toString();
	}

	private void formatIntegral(StringBuilder out, long n, boolean neg, FormatOptions options) {
		int b = alphabet.length;

		// Convert to base b. Yep, that's all it takes
		int iDigits = 0;
		List<Integer> commaPositions = options.isGroupingUsed() ? Lists.newArrayList() : Collections.emptyList();
		while (n > 0) {
			long r = (n % b);
			n = n / b;
			out.appendCodePoint(alphabet[(int)r]);
			iDigits++;
			if (options.isGroupingUsed() && iDigits%3 == 0) {
				// We can't iterate the StringBuilder after-the-fact and add commas as some digits
				// may be two chars, and naively adding a comma would cleave a surrogate pair in
				// half. Instead, we can add absolute character offsets to a list, and use those
				// later, allowing us to skip the cost of properly iterating a UTF-16 string.
				// Since commas aren't counted in iDigits, we have to additionally add the number of
				// commas we've appended so far.
				commaPositions.add(out.length()+(commaPositions.size()));
			}
		}
		// Handle minimum/maximum formatting rules (not in original Python)
		while (iDigits < options.getMinimumIntegerDigits()) {
			out.appendCodePoint(alphabet[0]);
			iDigits++;
			if (options.isGroupingUsed() && iDigits%3 == 0) {
				commaPositions.add(out.length()+(commaPositions.size()));
			}
		}
		while (iDigits > options.getMaximumIntegerDigits()) {
			if (out.length() >= 2 && Character.isSurrogatePair(out.charAt(out.length()-2), out.charAt(out.length()-1))) {
				out.deleteCharAt(out.length()-1);
			}
			out.deleteCharAt(out.length()-1);
			iDigits--;
		}
		for (int i : commaPositions) {
			if (i < out.length()) {
				out.insert(i, ',');
			}
		}

		if (neg) {
			out.append('-');
		}
		// Converting to a base moves away from the decimal point, so these digits
		// are in reverse order and need flipping
		out.reverse();
	}

	@Override
	public String format(float n, FormatOptions options) {
		return format((double)n, options);
	}

	@Override
	public String format(double d, FormatOptions options) {
		// based on https://eev.ee/blog/2016/04/26/the-case-for-base-twelve/#appendix-convert-to-a-base-in-python
		// eevee is *directly responsible* for me doing this number base thing so I may as well steal her code
		// comments preserved

		int b = alphabet.length;

		long n = (long)d;
		double frac = Math.abs(d)%1;

		// Handle negation separately
		boolean neg = false;
		if (n < 0) {
			n *= -1;
			neg = true;
		}

		StringBuilder out = new StringBuilder();
		formatIntegral(out, n, neg, options);

		if (frac > 0 || options.getMinimumFractionDigits() > 0) {
			// Leading zero if necessary
			if (out.length() == 0) {
				out.appendCodePoint(alphabet[0]);
			}
			out.append('.');
			int fDigits = 0;
			for (int i = 0; i < options.getMaximumFractionDigits(); i++) {
				if (frac == 0) {
					break;
				}
				double j = frac * b;
				int k = (int) (j);
				frac = j % 1;
				out.appendCodePoint(alphabet[k]);
				fDigits++;
			}
			while (fDigits < options.getMinimumFractionDigits()) {
				out.appendCodePoint(alphabet[0]);
				fDigits++;
			}
		}
		return out.toString();
	}

}

/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.localization;

import com.playsawdust.chipper.Identifier;

public interface NumberBase {

	/**
	 * @return the identifier of this NumberBase, such as chipper:decimal
	 */
	Identifier getIdentifier();

	/**
	 * @return the radix or base represented by this number base; ten is decimal, or 0 if the
	 * 		question makes no sense (e.g. roman numerals)
	 */
	int getRadix();

	/**
	 * Format the given integer in this base according to the given format options.
	 * @param n the int to format
	 * @param options the format options to use
	 * @return the formatted string
	 */
	String format(int n, FormatOptions options);

	/**
	 * Format the given integer in this base using the default {@link FormatOptions#BARE BARE}
	 * format options.
	 * @param n the int to format
	 * @return the formatted string
	 */
	default String format(int n) {
		return format(n, FormatOptions.BARE);
	}

	/**
	 * Format the given long integer in this base according to the given format options.
	 * @param n the long to format
	 * @param options the format options to use
	 * @return the formatted string
	 */
	String format(long n, FormatOptions options);

	/**
	 * Format the given long integer in this base using the default {@link FormatOptions#BARE BARE}
	 * format options.
	 * @param n the long to format
	 * @return the formatted string
	 */
	default String format(long n) {
		return format(n, FormatOptions.BARE);
	}

	/**
	 * Format the given float in this base according to the given format options.
	 * @param n the float to format
	 * @param options the format options to use
	 * @return the formatted string
	 */
	String format(float n, FormatOptions options);

	/**
	 * Format the given double in this base using the default {@link FormatOptions#BARE BARE}
	 * format options.
	 * @param n the float to format
	 * @return the formatted string
	 */
	default String format(float n) {
		return format(n, FormatOptions.BARE);
	}

	/**
	 * Format the given double in this base according to the given format options.
	 * @param n the double to format
	 * @param options the format options to use
	 * @return the formatted string
	 */
	String format(double n, FormatOptions options);

	/**
	 * Format the given double in this base using the default {@link FormatOptions#BARE BARE}
	 * format options.
	 * @param n the double to format
	 * @return the formatted string
	 */
	default String format(double n) {
		return format(n, FormatOptions.BARE);
	}


	/**
	 * Represents generic formatting options, similar to NumberFormat, but designed for the Chipper
	 * NumberBase system. Follows the builder pattern; for custom options not provided by the two
	 * default constants, you may do something like this:
	 * <code>
	 * private static final FormatOptions GROUPED_PAD2I_PAD2F = new FormatOptions()
	 * 				.minimumIntegerDigits(2)
	 * 				.minimumFractionDigits(2)
	 * 				.groupingUsed(true);
	 * </code>
	 * This class is basically just a holder for the exact same values offered by NumberFormat. As
	 * such, most Javadocs are copied directly from NumberFormat, as that system too deals with
	 * abstract number formatting concerns. However, the Java system is less easily extensible and
	 * comes with legacy baggage, hence the choice to create a new system.
	 */
	public static final class FormatOptions {

		/**
		 * No decorations will be added, other than a decimal point, if necessary. Only as many
		 * digits as necessary to represent the number will be used, with no padding.
		 * <p>
		 * For example, in decimal, {@code 1234} will become {@code "1234"}, and {@code 123.4} will
		 * become {@code "123.4"}.
		 */
		public static final FormatOptions BARE = new FormatOptions().freeze();

		/**
		 * Grouping will be added, and a decimal point, if necessary. Only as many digits as
		 * necessary to represent the number will be used, with no padding.
		 * <p>
		 * For example, in decimal, {@code 1234} will become {@code "1,234"}, and {@code 123.4} will
		 * become {@code "123.4"}.
		 */
		public static final FormatOptions GROUPED = new FormatOptions().groupingUsed(true).freeze();

		private static final String FROZEN = "This FormatOptions has been frozen and cannot be modified";

		private boolean frozen = false;

		private boolean groupingUsed = false;

		private int maximumIntegerDigits = 40;
		private int minimumIntegerDigits = 1;

		private int maximumFractionDigits = 3;
		private int minimumFractionDigits = 0;

		/**
		 * "Freeze" this FormatOptions object, preventing further changes.
		 */
		public FormatOptions freeze() {
			frozen = true;
			return this;
		}

		/**
		 * Returns true if grouping is used in this format. For example, in the
		 * English locale, with grouping on, the number 1234567 might be
		 * formatted as "1,234,567". The grouping separator as well as the size
		 * of each group is locale dependant and is determined by sub-classes of
		 * NumberFormat.
		 *
		 * @return {@code true} if grouping is used; {@code false} otherwise
		 * @see #setGroupingUsed
		 */
		public boolean isGroupingUsed() {
			return groupingUsed;
		}

		/**
		 * Set whether or not grouping will be used in this format.
		 *
		 * @param newValue
		 *            {@code true} if grouping is used; {@code false} otherwise
		 * @see #isGroupingUsed
		 */
		public FormatOptions groupingUsed(boolean newValue) {
			if (frozen) throw new IllegalStateException(FROZEN);
			groupingUsed = newValue;
			return this;
		}

		/**
		 * Returns the maximum number of digits allowed in the integer portion
		 * of a number.
		 *
		 * @return the maximum number of digits
		 * @see #setMaximumIntegerDigits
		 */
		public int getMaximumIntegerDigits() {
			return maximumIntegerDigits;
		}

		/**
		 * Sets the maximum number of digits allowed in the integer portion of a
		 * number. maximumIntegerDigits must be &ge; minimumIntegerDigits. If
		 * the new value for maximumIntegerDigits is less than the current value
		 * of minimumIntegerDigits, then minimumIntegerDigits will also be set
		 * to the new value.
		 *
		 * @param newValue
		 *            the maximum number of integer digits to be shown; if less
		 *            than zero, then zero is used. The concrete subclass may
		 *            enforce an upper limit to this value appropriate to the
		 *            numeric type being formatted.
		 * @see #getMaximumIntegerDigits
		 */
		public FormatOptions maximumIntegerDigits(int newValue) {
			if (frozen) throw new IllegalStateException(FROZEN);
			maximumIntegerDigits = Math.max(0, newValue);
			if (minimumIntegerDigits > maximumIntegerDigits) {
				minimumIntegerDigits = maximumIntegerDigits;
			}
			return this;
		}

		/**
		 * Returns the minimum number of digits allowed in the integer portion
		 * of a number.
		 *
		 * @return the minimum number of digits
		 * @see #setMinimumIntegerDigits
		 */
		public int getMinimumIntegerDigits() {
			return minimumIntegerDigits;
		}

		/**
		 * Sets the minimum number of digits allowed in the integer portion of a
		 * number. minimumIntegerDigits must be &le; maximumIntegerDigits. If
		 * the new value for minimumIntegerDigits exceeds the current value of
		 * maximumIntegerDigits, then maximumIntegerDigits will also be set to
		 * the new value
		 *
		 * @param newValue
		 *            the minimum number of integer digits to be shown; if less
		 *            than zero, then zero is used. The concrete subclass may
		 *            enforce an upper limit to this value appropriate to the
		 *            numeric type being formatted.
		 * @see #getMinimumIntegerDigits
		 */
		public FormatOptions minimumIntegerDigits(int newValue) {
			if (frozen) throw new IllegalStateException(FROZEN);
			minimumIntegerDigits = Math.max(0, newValue);
			if (minimumIntegerDigits > maximumIntegerDigits) {
				maximumIntegerDigits = minimumIntegerDigits;
			}
			return this;
		}

		/**
		 * Returns the maximum number of digits allowed in the fraction portion
		 * of a number.
		 *
		 * @return the maximum number of digits.
		 * @see #setMaximumFractionDigits
		 */
		public int getMaximumFractionDigits() {
			return maximumFractionDigits;
		}

		/**
		 * Sets the maximum number of digits allowed in the fraction portion of
		 * a number. maximumFractionDigits must be &ge; minimumFractionDigits.
		 * If the new value for maximumFractionDigits is less than the current
		 * value of minimumFractionDigits, then minimumFractionDigits will also
		 * be set to the new value.
		 *
		 * @param newValue
		 *            the maximum number of fraction digits to be shown; if less
		 *            than zero, then zero is used. The concrete subclass may
		 *            enforce an upper limit to this value appropriate to the
		 *            numeric type being formatted.
		 * @see #getMaximumFractionDigits
		 */
		public FormatOptions maximumFractionDigits(int newValue) {
			if (frozen) throw new IllegalStateException(FROZEN);
			maximumFractionDigits = Math.max(0, newValue);
			if (maximumFractionDigits < minimumFractionDigits) {
				minimumFractionDigits = maximumFractionDigits;
			}
			return this;
		}

		/**
		 * Returns the minimum number of digits allowed in the fraction portion
		 * of a number.
		 *
		 * @return the minimum number of digits
		 * @see #setMinimumFractionDigits
		 */
		public int getMinimumFractionDigits() {
			return minimumFractionDigits;
		}

		/**
		 * Sets the minimum number of digits allowed in the fraction portion of
		 * a number. minimumFractionDigits must be &le; maximumFractionDigits.
		 * If the new value for minimumFractionDigits exceeds the current value
		 * of maximumFractionDigits, then maximumIntegerDigits will also be set
		 * to the new value
		 *
		 * @param newValue
		 *            the minimum number of fraction digits to be shown; if less
		 *            than zero, then zero is used. The concrete subclass may
		 *            enforce an upper limit to this value appropriate to the
		 *            numeric type being formatted.
		 * @see #getMinimumFractionDigits
		 */
		public FormatOptions minimumFractionDigits(int newValue) {
			if (frozen) throw new IllegalStateException(FROZEN);
			minimumFractionDigits = Math.max(0, newValue);
			if (maximumFractionDigits < minimumFractionDigits) {
				maximumFractionDigits = minimumFractionDigits;
			}
			return this;
		}

	}

}

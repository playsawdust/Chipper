/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.math;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.units.qual.degrees;
import org.checkerframework.common.value.qual.IntRange;

import com.google.common.collect.ImmutableMap;
import com.playsawdust.chipper.exception.RecycledObjectException;
import com.playsawdust.chipper.qual.PackedRGB;
import com.playsawdust.chipper.qual.Unit;

import com.playsawdust.chipper.toolbox.pool.ObjectPool;
import com.playsawdust.chipper.toolbox.pool.PooledObject;

/**
 * ProtoColor represents the <i>prototype</i> of a color. It holds an unknown
 * and arbitrary colorspace, and will automatically convert to any desired
 * colorspace.
 * <p>
 * A ProtoColor instance is <i>effectively immutable</i> - it internally keeps
 * state in the form of cached colorspace transformations, so converting to a
 * given colorspace will not create a new object, but changing the actual color
 * value held by a ProtoColor requires a new object. Note that due to this,
 * <i>ProtoColor instances are not thread safe</i> and must be cloned if they
 * will be used from more than one thread.
 * <p>
 * For conversions to/from absolute colorspaces such as XYZ, ProtoColor always
 * assumes sRGB with the D65 illuminant. These assumptions are generally
 * correct on computer monitors, and are <i>good enough</i> on non-Samsung
 * phones. (Samsung phones tend to have absurdly oversaturated displays because
 * laypeople perceive them as better, similar to how consumer headphones have
 * overdriven bass.)
 * <p>
 * HSV/HSL color math is ported from the <a href="https://github.com/toish/chromatism">Chromatism</a>
 * library. CIELAB/XYZ/LCH color math is ported from <a href="https://github.com/apache/commons-imaging/blob/b2eda69/src/main/java/org/apache/commons/imaging/color/ColorConversions.java">Apache
 * Commons Imaging</a>.
 */
public final class ProtoColor implements Cloneable, PooledObject {
	private static final ObjectPool<ProtoColor> pool = new ObjectPool<>(ProtoColor::new);
	private static final ObjectPool<double[]> arrayPool = new ObjectPool<>(() -> new double[3]);

	public enum ColorSpace {
		/**
		 * The standard additive RGB color space. ProtoColor assumes sRGB when
		 * conversions from this space are required.
		 */
		RGB,
		/**
		 * A cylindrical transformation of the RGB color space, where the final
		 * component, V, represents the "value" of a color. 0 is black, 1 is
		 * the fully saturated color.
		 */
		HSV,
		/**
		 * A cylindrical transformation of the RGB color space, where the final
		 * component, L, represents the "lightness" of a color. 0 is black, 1 is
		 * white, 0.5 is the fully saturated color.
		 */
		HSL,
		/**
		 * An <i>absolute</i> color space, based on how the human visual system
		 * actually works. Standard transforms to CIEXYZ exist from common color
		 * spaces such as sRGB, so while this is an old and no longer current
		 * color space, it is still important for conversions. Y is luminance,
		 * which is why various other colorspaces use "Y" to indicate luma. Z is
		 * S-cone response, X is "a mix of response curves chosen to be
		 * non-negative".
		 * <p>
		 * CIEXYZ was defined in 1931 as part of very important foundational
		 * work toward bettering the understanding of human color vision.
		 * <p>
		 * Generally, you should only modify Y if working with XYZ directly.
		 * More complex transforms should be done in {@link #CIELCH}.
		 * <p>
		 * <b>Further reading</b>: <a href=
		 * "https://en.wikipedia.org/wiki/CIE_1931_color_space">Wikipedia:CIE
		 * 1931 color space</a>
		 */
		CIEXYZ,
		/**
		 * A <i>perceptual</i> absolute color space, based on how the human
		 * visual system actually works. A newer version of CIELUV (not
		 * currently supported by ProtoColor), which is a transformation of
		 * CIEXYZ to make it perceptually uniform. L is lightness, A is
		 * green/red, B is blue/yellow.
		 * <p>
		 * It is generally more convenient to perform transforms in
		 * {@link #CIELCH} than directy in CIELAB.
		 */
		CIELAB,
		/**
		 * A cylindrical transformation of the {@link #CIELAB} color space,
		 * where the components are Lightness, Chroma, and Hue. Unlike HSV and
		 * HSL, these components are perceptually uniform. The meaning of this
		 * is difficult to explain succintly, and is best learned by experience.
		 * Try opening the Colorspace Toy and comparing how the CIELCH sliders
		 * react compared to the HSL/HSV sliders.
		 * <p>
		 * Lightness maps directly to CIELAB Lightness, and Chroma is a similar
		 * concept to Saturation in HSV/HSL.
		 * <p>
		 * <b>See</b>:
		 * <a href="https://unascribed.com/junk/colorspaces.html">Una's
		 * Colorspace Toy</a>
		 */
		CIELCH,
		/*
		XYY,
		CMYK,
		YIQ,
		LMS,
		CIELUV,
		*/// TODO
		;
	}

	private static final double oneThird = 1/3D;
	private static final double twoThirds = 2/3D;

	private boolean recyclable = true;
	private boolean recycled = false;

	private ColorSpace originalSpace = null;

	private double[] rgb = null;
	private double[] hsv = null;
	private double[] hsl = null;
	private double[] ciexyz = null;
	private double[] cielab = null;
	private double[] cielch = null;
	/*
	private double[] xyy;
	private double[] cmyk;
	private double[] yiq;
	private double[] lms;
	private double[] cieluv;
	private double alpha;
	*/// TODO

	private ProtoColor() {}

	private static ProtoColor get(ColorSpace originalSpace) {
		ProtoColor pc = pool.get();
		pc.originalSpace = originalSpace;
		pc.recycled = false;
		return pc;
	}

	private static double[] pooledArray(double a, double b, double c) {
		double[] arr = arrayPool.get();
		arr[0] = a;
		arr[1] = b;
		arr[2] = c;
		return arr;
	}

	private double[] maybePooledArray(double a, double b, double c) {
		if (recyclable) {
			return pooledArray(a, b, c);
		} else {
			return new double[] { a, b, c };
		}
	}

	private static @PolyNull double[] pooledClone(@PolyNull double[] arr) {
		if (arr == null) return null;
		return pooledArray(arr[0], arr[1], arr[2]);
	}

	private static ProtoColor unpooledConstant(int rgb) {
		ProtoColor pc = new ProtoColor();
		pc.recyclable = false;
		pc.originalSpace = ColorSpace.RGB;
		pc.rgb = new double[] {
			((rgb>>16)&0xFF)/255D,
			((rgb>>8 )&0xFF)/255D,
			((rgb>>0 )&0xFF)/255D
		};
		pc.populateAll();
		return pc;
	}

	/**
	 * Obtain an RGB ProtoColor with the given 24-bit packed RGB int.
	 * @see #getPackedRGB
	 * @param rgb the 24-bit packed int to retrieve values from
	 * @return an RGB ProtoColor from the shared pool
	 */
	public static ProtoColor fromRGB(@PackedRGB int rgb) {
		ProtoColor pc = get(ColorSpace.RGB);
		pc.rgb = pooledArray(
			((rgb>>16)&0xFF)/255D,
			((rgb>>8 )&0xFF)/255D,
			((rgb>>0 )&0xFF)/255D
		);
		return pc;
	}

	/**
	 * Obtain an RGB ProtoColor from the given 24-bit packed BGR int.
	 * @see #getPackedBGR
	 * @param bgr the 24-bit packed int to retrieve values from
	 * @return an RGB ProtoColor from the shared pool
	 */
	public static ProtoColor fromBGR(int bgr) {
		ProtoColor pc = get(ColorSpace.RGB);
		pc.rgb = pooledArray(
				((bgr>>8 )&0xFF)/255D,
				((bgr>>16)&0xFF)/255D,
				((bgr>>24)&0xFF)/255D
		);
		return pc;
	}

	/**
	 * Obtain an RGB ProtoColor with the given components.
	 * @param r the red component, from 0 to 1
	 * @param g the green component, from 0 to 1
	 * @param b the blue component, from 0 to 1
	 * @return an RGB ProtoColor from the shared pool
	 */
	public static ProtoColor fromRGB(@Unit double r, @Unit double g, @Unit double b) {
		r = FastMath.clamp(r, 0, 1);
		g = FastMath.clamp(g, 0, 1);
		b = FastMath.clamp(b, 0, 1);
		ProtoColor pc = get(ColorSpace.RGB);
		pc.rgb = pooledArray(r, g, b);
		return pc;
	}

	/**
	 * Obtain an HSL ProtoColor with the given components.
	 * @param h the hue component, in degrees
	 * @param s the saturation component, from 0 to 1
	 * @param l the lightness component, from 0 to 1
	 * @return an HSL ProtoColor from the shared pool
	 */
	public static ProtoColor fromHSL(@degrees double h, @Unit double s, @Unit double l) {
		h = negativeModulo(h, 360)/360D;
		s = FastMath.clamp(s, 0, 1);
		l = FastMath.clamp(l, 0, 1);
		ProtoColor pc = get(ColorSpace.HSL);
		pc.hsl = pooledArray(h, s, l);
		return pc;
	}

	/**
	 * Obtain an HSV ProtoColor from the given components.
	 * @param h the hue component, in degrees
	 * @param s the saturation component, from 0 to 1
	 * @param v the value component, from 0 to 1
	 * @return an HSV ProtoColor from the shared pool
	 */
	public static ProtoColor fromHSV(@degrees double h, @Unit double s, @Unit double v) {
		h = negativeModulo(h, 360)/360D;
		s = FastMath.clamp(s, 0, 1);
		v = FastMath.clamp(v, 0, 1);
		ProtoColor pc = get(ColorSpace.HSV);
		pc.hsv = pooledArray(h, s, v);
		return pc;
	}

	/**
	 * Obtain a CIEXYZ ProtoColor from the given components.
	 * @param x the x component
	 * @param y the y component
	 * @param z the z component
	 * @return a CIEXYZ ProtoColor from the shared pool
	 */
	public static ProtoColor fromCIEXYZ(double x, double y, double z) {
		ProtoColor pc = get(ColorSpace.CIEXYZ);
		pc.ciexyz = pooledArray(x, y, z);
		return pc;
	}

	/**
	 * Obtain a CIELAB ProtoColor from the given components.
	 * @param l the L* component
	 * @param a the a* component
	 * @param b the b* component
	 * @return a CIELAB ProtoColor from the shared pool
	 */
	public static ProtoColor fromCIELAB(double l, double a, double b) {
		ProtoColor pc = get(ColorSpace.CIELAB);
		pc.cielab = pooledArray(l, a, b);
		return pc;
	}

	/**
	 * Obtain a CIELCH ProtoColor from the given components.
	 * @param l the lightness component
	 * @param c the chroma component
	 * @param h the hue component, in degrees
	 * @return a CIELCH ProtoColor from the shared pool
	 */
	public static ProtoColor fromCIELCH(double l, double c, @degrees double h) {
		h = negativeModulo(h, 360)/360D;
		ProtoColor pc = get(ColorSpace.CIELCH);
		pc.hsv = pooledArray(l, c, h);
		return pc;
	}

	// matches strings looking like CSS-y triplets
	// supports comma-separated and space-separated syntax
	// most of this metacharacter vomit is due to enforcing at least one
	// space if a comma is not found
	private static final Pattern TRIPLET = Pattern.compile("^(\\w+)\\(([0-9]+%?)(?:,\\s*|\\s+)([0-9]+%?)(?:,\\s*|\\s+)([0-9]+%?)\\)$");

	/**
	 * Attempt to parse the given string as a color in one of the following
	 * supported formats:
	 * <ul>
	 * <li><b>12-bit RGB hex</b>: {@code #RGB}</li>
	 * <li><b>24-bit RGB hex</b>: {@code #RRGGBB}</li>
	 * <li><b>RGB tuple</b>: {@code rgb(0-255, 0-255, 0-255)}</li>
	 * <li><b>RGB tuple (percentage)</b>: {@code rgb(0-100%, 0-100%, 0-100%)}</li>
	 * <li><b>HSL tuple</b>: {@code hsl(0-360, 0-100%, 0-100%)}</li>
	 * <li><b>HSV tuple</b>: {@code hsv(0-360, 0-100%, 0-100%)}</li>
	 * <li><b>Case-insensitive CSS color name</b>: <i>See <a href= "https://developer.mozilla.org/en-US/docs/Web/CSS/color_value#Color_keywords">MDN</a></i></li>
	 * </ul>
	 * This is, more or less, compatible with CSS Level 4. Notable missing
	 * features are the <i>absolutely bizarre</i> color guessing behavior with
	 * unrecognized color names (this implementation chooses to throw
	 * NumberFormatException instead) support for alpha (which ProtoColor lacks
	 * entirely at this time), and rad/turn support for the hue component in HSL
	 * and HSV. Vendor-specific and deprecated behaviors are additionally not
	 * supported.
	 *
	 * @param str the string to parse
	 * @return the parsed color
	 * @throws NumberFormatException if the string could not be parsed
	 */
	public static ProtoColor parse(String str) throws NumberFormatException {
		if (str.startsWith("#")) {
			int r;
			int g;
			int b;
			if (str.length() == 4) {
				r = Character.digit(str.charAt(1), 16);
				g = Character.digit(str.charAt(2), 16);
				b = Character.digit(str.charAt(3), 16);
				r |= r << 4;
				g |= g << 4;
				b |= b << 4;
			} else if (str.length() == 7) {
				r = Integer.parseInt(str.substring(1, 3), 16);
				g = Integer.parseInt(str.substring(3, 5), 16);
				b = Integer.parseInt(str.substring(5, 7), 16);
			} else {
				throw new NumberFormatException("Invalid color (started with #, but was not 12-bit or 24-bit): "+str);
			}
			return fromRGB(r/255D, g/255D, b/255D);
		}
		ProtoColor named = CSSColors.asMap().get(str.toLowerCase(Locale.ROOT));
		if (named != null) return named;
		Matcher triplet = TRIPLET.matcher(str);
		if (triplet.matches()) {
			String type = triplet.group(1);
			if ("rgb".equals(type)) {
				String rStr = triplet.group(2);
				String gStr = triplet.group(3);
				String bStr = triplet.group(4);
				int amtPct = 0;
				if (rStr.endsWith("%")) amtPct++;
				if (gStr.endsWith("%")) amtPct++;
				if (bStr.endsWith("%")) amtPct++;
				double r;
				double g;
				double b;
				if (amtPct == 0) {
					r = Double.parseDouble(rStr)/255D;
					g = Double.parseDouble(gStr)/255D;
					b = Double.parseDouble(bStr)/255D;
				} else if (amtPct == 3) {
					rStr = rStr.substring(0, rStr.length()-1);
					gStr = gStr.substring(0, gStr.length()-1);
					bStr = bStr.substring(0, bStr.length()-1);
					r = Double.parseDouble(rStr)/100D;
					g = Double.parseDouble(gStr)/100D;
					b = Double.parseDouble(bStr)/100D;
				} else {
					throw new NumberFormatException("Invalid color (either all or none of the components must be percentages): "+str);
				}
				return fromRGB(r, g, b);
			} else if ("hsl".equals(type) || "hsv".equals(type)) {
				String hStr = triplet.group(2);
				String sStr = triplet.group(3);
				String lvStr = triplet.group(4);
				if (hStr.endsWith("%")) throw new NumberFormatException("Invalid color (hue cannot be a percentage): "+str);
				if (!sStr.endsWith("%")) throw new NumberFormatException("Invalid color (saturation must be a percentage): "+str);
				if (!lvStr.endsWith("%")) {
					if (type.endsWith("v")) {
						throw new NumberFormatException("Invalid color (value must be a percentage): "+str);
					} else {
						throw new NumberFormatException("Invalid color (lightness must be a percentage): "+str);
					}
				}
				sStr = sStr.substring(0, sStr.length()-1);
				lvStr = lvStr.substring(0, lvStr.length()-1);
				double h = Double.parseDouble(hStr);
				double s = Double.parseDouble(sStr)/100D;
				double lv = Double.parseDouble(lvStr)/100D;
				if (type.endsWith("v")) {
					return fromHSV(h, s, lv);
				} else {
					return fromHSL(h, s, lv);
				}
			} else {
				throw new NumberFormatException("Invalid color (unrecognized triplet type "+triplet.group(1)+"): "+str);
			}
		} else {
			throw new NumberFormatException("Invalid color (not in any recognized format): "+str);
		}
	}

	@Override
	public String toString() {
		if (recycled) throw new RecycledObjectException(this);
		switch (originalSpace) {
			case RGB: return "ProtoColor(RGB){r="+rgb[0]+", g="+rgb[1]+", b="+rgb[2]+"}";
			case HSV: return "ProtoColor(HSV){h="+hsv[0]+", s="+hsv[1]+", v="+hsv[2]+"}";
			case HSL: return "ProtoColor(HSL){h="+hsl[0]+", s="+hsl[1]+", l="+hsl[2]+"}";
			case CIEXYZ: return "ProtoColor(CIEXYZ){x="+ciexyz[0]+", y="+ciexyz[1]+", z="+ciexyz[2]+"}";
			case CIELAB: return "ProtoColor(CIELAB){L*="+cielab[0]+", a*="+cielab[1]+", b*="+cielab[2]+"}";
			case CIELCH: return "ProtoColor(CIELCH){L*="+cielch[0]+", c="+cielch[1]+", h="+cielch[2]+"}";
			default: throw new AssertionError("missing case for "+originalSpace);
		}
	}

	/**
	 * @return this ProtoColor as a 24-bit RGB hex string, such as #FFAA00
	 */
	public String toHexString() {
		return "#"+Integer.toHexString(getPackedRGB()).substring(2);
	}

	@Override
	public int hashCode() {
		if (recycled) throw new RecycledObjectException(this);
		switch (originalSpace) {
			case RGB: return 29*Arrays.hashCode(rgb);
			case HSV: return 31*Arrays.hashCode(hsv);
			case HSL: return 37*Arrays.hashCode(hsl);
			case CIEXYZ: return 41*Arrays.hashCode(hsl);
			case CIELAB: return 43*Arrays.hashCode(hsl);
			case CIELCH: return 47*Arrays.hashCode(hsl);
			default: throw new AssertionError("missing case for "+originalSpace);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (recycled) throw new RecycledObjectException(this);
		if (obj == null) return false;
		if (obj == this) return true;
		if (!(obj instanceof ProtoColor)) return false;
		ProtoColor that = (ProtoColor)obj;
		switch (originalSpace) {
			case RGB: {
				that.populateRGB();
				return Arrays.equals(this.rgb, that.rgb);
			}
			case HSV: {
				that.populateHSV();
				return Arrays.equals(this.hsv, that.hsv);
			}
			case HSL: {
				that.populateHSL();
				return Arrays.equals(this.hsl, that.hsl);
			}
			case CIEXYZ: {
				that.populateCIEXYZ();
				return Arrays.equals(this.ciexyz, that.ciexyz);
			}
			case CIELAB: {
				that.populateCIELAB();
				return Arrays.equals(this.cielab, that.cielab);
			}
			case CIELCH: {
				that.populateCIELCH();
				return Arrays.equals(this.cielch, that.cielch);
			}
			default: throw new AssertionError("missing case for "+originalSpace);
		}
	}

	@Override
	public ProtoColor clone() {
		if (recycled) throw new RecycledObjectException(this);
		ProtoColor pc = get(originalSpace);
		pc.rgb = pooledClone(rgb);
		pc.hsl = pooledClone(hsl);
		pc.hsv = pooledClone(hsv);
		return pc;
	}

	@Override
	public void recycle() {
		if (!recyclable) {
			// this is an error condition, but there's no harm in being quiet
			// this way shared code that expects recyclable protocolors can
			// operate on constants without issue
			return;
		}
		recycled = true;
		if (rgb != null) arrayPool.recycle(rgb);
		if (hsl != null) arrayPool.recycle(hsl);
		if (hsv != null) arrayPool.recycle(hsv);
		if (ciexyz != null) arrayPool.recycle(ciexyz);
		if (cielab != null) arrayPool.recycle(cielab);
		if (cielch != null) arrayPool.recycle(cielch);
		rgb = null;
		hsl = null;
		hsv = null;
		ciexyz = null;
		cielab = null;
		cielch = null;
		originalSpace = null;
		pool.recycle(this);
	}

	/**
	 * Convert this ProtoColor to RGB if needed, and then return the red
	 * component.
	 * @return the red component, from 0 to 1
	 */
	public @Unit double getRed() {
		populateRGB();
		return rgb[0];
	}
	/**
	 * Convert this ProtoColor to RGB if needed, and then return the green
	 * component.
	 * @return the green component, from 0 to 1
	 */
	public @Unit double getGreen() {
		populateRGB();
		return rgb[1];
	}
	/**
	 * Convert this ProtoColor to RGB if needed, and then return the blue
	 * component.
	 * @return the blue component, from 0 to 1
	 */
	public @Unit double getBlue() {
		populateRGB();
		return rgb[2];
	}

	/**
	 * Convert this ProtoColor to RGB if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the red
	 * component.
	 * @param r the new red component
	 * @return a new ProtoColor
	 */
	public ProtoColor withRed(double r) {
		return fromRGB(r, getGreen(), getBlue());
	}

	/**
	 * Convert this ProtoColor to RGB if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the green
	 * component.
	 * @param g the new green component
	 * @return a new ProtoColor
	 */
	public ProtoColor withGreen(double g) {
		return fromRGB(getRed(), g, getBlue());
	}

	/**
	 * Convert this ProtoColor to RGB if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the blue
	 * component.
	 * @param b the new blue component
	 * @return a new ProtoColor
	 */
	public ProtoColor withBlue(double b) {
		return fromRGB(getRed(), getGreen(), b);
	}

	/**
	 * Convert this ProtoColor to RGB if needed, and then return the red
	 * component.
	 * @return the red component, from 0 to 255
	 */
	public @IntRange(from=0, to=255) int getRedByte() {
		return (int)(getRed()*255);
	}
	/**
	 * Convert this ProtoColor to RGB if needed, and then return the green
	 * component.
	 * @return the green component, from 0 to 255
	 */
	public @IntRange(from=0, to=255) int getGreenByte() {
		return (int)(getGreen()*255);
	}
	/**
	 * Convert this ProtoColor to RGB if needed, and then return the blue
	 * component.
	 * @return the blue component, from 0 to 255
	 */
	public @IntRange(from=0, to=255) int getBlueByte() {
		return (int)(getBlue()*255);
	}
	/**
	 * Convert this ProtoColor to RGB if needed, and then return the red, green,
	 * and blue components packed into a 24-bit value. The most-significant
	 * 8 bits (what would be the alpha component) will be set to 0xFF.
	 * @return a packed RGB int
	 */
	public @PackedRGB int getPackedRGB() {
		int r = getRedByte();
		int g = getGreenByte();
		int b = getBlueByte();
		int c = 0xFF000000;
		c |= (r << 16);
		c |= (g << 8);
		c |= (b << 0);
		return c;
	}
	/**
	 * Convert this ProtoColor to RGB if needed, and then return the red, green,
	 * and blue components packed into a 24-bit value in reverse order, for use
	 * with little-endian APIs. The least-significant 8 bits (what would be the
	 * alpha component) will be set to 0xFF.
	 * @return a packed BGR int
	 */
	public int getPackedBGR() {
		int r = getRedByte();
		int g = getGreenByte();
		int b = getBlueByte();
		int c = 0x000000FF;
		c |= (b << 24);
		c |= (g << 16);
		c |= (r << 8);
		return c;
	}


	/**
	 * Convert this ProtoColor to HSL if needed, then return the hue component.
	 * @return the hue component, in degrees
	 */
	public @degrees double getHSHue() {
		if (hsv != null && hsl == null) return hsv[0] * 360;
		populateHSL();
		return hsl[0] * 360;
	}

	/**
	 * Convert this ProtoColor to HSL if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the hue
	 * component.
	 * @param h the new hue component
	 * @return a new ProtoColor
	 */
	public ProtoColor withHSHue(@degrees double h) {
		if (hsv != null && hsl == null) return fromHSV(h, getHSVSaturation(), getHSVValue());
		return fromHSL(h, getHSLSaturation(), getHSLLightness());
	}

	/**
	 * Convert this ProtoColor to HSL if needed, then return the saturation
	 * component.
	 * @return the saturation component, from 0 to 1
	 */
	public @Unit double getHSLSaturation() {
		populateHSL();
		return hsl[1];
	}
	/**
	 * Convert this ProtoColor to HSL if needed, then return the lightness
	 * component.
	 * @return the lightness component, from 0 to 1
	 */
	public @Unit double getHSLLightness() {
		populateHSL();
		return hsl[2];
	}

	/**
	 * Convert this ProtoColor to HSL if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the saturation
	 * component.
	 * @param s the new saturation component
	 * @return a new ProtoColor
	 */
	public ProtoColor withHSLSaturation(@Unit double s) {
		return fromHSL(getHSHue(), s, getHSLLightness());
	}

	/**
	 * Convert this ProtoColor to HSL if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the lightness
	 * component.
	 * @param l the new lightness component
	 * @return a new ProtoColor
	 */
	public ProtoColor withHSLLightness(@Unit double l) {
		return fromHSL(getHSHue(), getHSLSaturation(), l);
	}

	/**
	 * Convert this ProtoColor to HSV if needed, then return the saturation
	 * component.
	 * @return the saturation component, from 0 to 1
	 */
	public @Unit double getHSVSaturation() {
		populateHSV();
		return hsv[1];
	}
	/**
	 * Convert this ProtoColor to HSV if needed, then return the value
	 * component.
	 * @return the value component, from 0 to 1
	 */
	public @Unit double getHSVValue() {
		populateHSV();
		return hsv[2];
	}

	/**
	 * Convert this ProtoColor to HSV if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the saturation
	 * component.
	 * @param s the new saturation component
	 * @return a new ProtoColor
	 */
	public ProtoColor withHSVSaturation(@Unit double s) {
		return fromHSV(getHSHue(), s, getHSVValue());
	}

	/**
	 * Convert this ProtoColor to HSV if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the value
	 * component.
	 * @param v the new value component
	 * @return a new ProtoColor
	 */
	public ProtoColor withHSVValue(@Unit double v) {
		return fromHSV(getHSHue(), getHSVSaturation(), v);
	}

	/**
	 * Convert this ProtoColor to CIEXYZ if needed, and then return the X
	 * component.
	 * @return the X component
	 */
	public double getCIEX() {
		populateCIEXYZ();
		return ciexyz[0];
	}

	/**
	 * Convert this ProtoColor to CIEXYZ if needed, and then return the Y
	 * component.
	 * @return the Y component
	 */
	public double getCIEY() {
		populateCIEXYZ();
		return ciexyz[1];
	}

	/**
	 * Convert this ProtoColor to CIEXYZ if needed, and then return the Z
	 * component.
	 * @return the Z component
	 */
	public double getCIEZ() {
		populateCIEXYZ();
		return ciexyz[2];
	}

	/**
	 * Convert this ProtoColor to CIEXYZ if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the X component.
	 * @param x the new X component
	 * @return a new ProtoColor
	 */
	public ProtoColor withCIEX(double x) {
		return fromCIEXYZ(x, getCIEY(), getCIEZ());
	}

	/**
	 * Convert this ProtoColor to CIEXYZ if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the Y component.
	 * @param y the new Y component
	 * @return a new ProtoColor
	 */
	public ProtoColor withCIEY(double y) {
		return fromCIEXYZ(getCIEX(), y, getCIEZ());
	}

	/**
	 * Convert this ProtoColor to CIEXYZ if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the Z component.
	 * @param z the new Z component
	 * @return a new ProtoColor
	 */
	public ProtoColor withCIEZ(double z) {
		return fromCIEXYZ(getCIEX(), getCIEY(), z);
	}

	/**
	 * Convert this ProtoColor to CIELAB if needed, and then return the
	 * lightness component. This is the same L component as in CIELCH.
	 * @return the lightness component
	 */
	public double getCIELightness() {
		if (cielch != null) return cielch[0];
		populateCIELAB();
		return cielab[0];
	}

	/**
	 * Convert this ProtoColor to CIELAB if needed, and then return the
	 * a* component.
	 * @return the a* component
	 */
	public double getCIEA() {
		populateCIELAB();
		return cielab[1];
	}

	/**
	 * Convert this ProtoColor to CIELAB if needed, and then return the
	 * b* component.
	 * @return the b* component
	 */
	public double getCIEB() {
		populateCIELAB();
		return cielab[2];
	}

	/**
	 * Convert this ProtoColor to CIELAB if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the lightness
	 * component.
	 * @param l the new lightness component
	 * @return a new ProtoColor
	 */
	public ProtoColor withCIELABLightness(double l) {
		return fromCIELAB(l, getCIEA(), getCIEB());
	}

	/**
	 * Convert this ProtoColor to CIELAB if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the a*
	 * component.
	 * @param a the new a* component
	 * @return a new ProtoColor
	 */
	public ProtoColor withCIEA(double a) {
		return fromCIELAB(getCIELightness(), a, getCIEB());
	}

	/**
	 * Convert this ProtoColor to CIELAB if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the b*
	 * component.
	 * @param b the new b* component
	 * @return a new ProtoColor
	 */
	public ProtoColor withCIEB(double b) {
		return fromCIELAB(getCIELightness(), getCIEA(), b);
	}

	/**
	 * Convert this ProtoColor to CIELCH if needed, and then return the
	 * chroma component.
	 * @return the chroma component
	 */
	public double getCIEChroma() {
		populateCIELCH();
		return cielch[1];
	}

	/**
	 * Convert this ProtoColor to CIELCH if needed, and then return the
	 * hue component.
	 * @return the hue component, in degrees
	 */
	public @degrees double getCIEHue() {
		populateCIELCH();
		return cielch[2];
	}

	/**
	 * Convert this ProtoColor to CIELCH if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the lightness
	 * component.
	 * @param l the new lightness component
	 * @return a new ProtoColor
	 */
	public ProtoColor withCIELCHLightness(double l) {
		return fromCIELCH(l, getCIEChroma(), getCIEHue());
	}

	/**
	 * Convert this ProtoColor to CIELCH if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the chroma
	 * component.
	 * @param c the new chroma component
	 * @return a new ProtoColor
	 */
	public ProtoColor withCIEChroma(double c) {
		return fromCIELCH(getCIELightness(), c, getCIEHue());
	}

	/**
	 * Convert this ProtoColor to CIELCH if needed, and then return a new
	 * ProtoColor with the same color as this one, except for the hue
	 * component.
	 * @param h the new hue component
	 * @return a new ProtoColor
	 */
	public ProtoColor withCIEHue(@degrees double h) {
		return fromCIELCH(getCIELightness(), getCIEChroma(), h);
	}

	/**
	 * Ensure that a cached RGB value is available.
	 * <p>
	 * May also populate other colorspaces if they are needed for the
	 * conversion.
	 */
	public void populateRGB() {
		if (recycled) throw new RecycledObjectException(this);
		if (rgb == null) {
			if (hsv != null) {
				populateHSL();
			}
			if (hsl != null) {
				if (Double.compare(hsl[1], 0) == 0) {
					double l = hsl[2];
					rgb = maybePooledArray(l, l, l);
					return;
				}
				double tempOne, tempTwo, tempHue;
				if (hsl[2] >= 0.5D) {
					tempOne = (hsl[2] + hsl[1]) - (hsl[2] * hsl[1]);
				} else {
					tempOne = hsl[2] * (1 + hsl[1]);
				}
				tempTwo = (2 * hsl[2]) - tempOne;
				tempHue = hsl[0];
				double tempR = (tempHue + oneThird) % 1;
				double tempG = tempHue;
				double tempB = negativeModulo((tempHue - oneThird), 1);
				double r, g, b;
				if ((6 * tempR) < 1) {
					r = tempTwo + ((tempOne - tempTwo) * 6 * tempR);
				} else if ((2 * tempR) < 1) {
					r = tempOne;
				} else if ((3 * tempR) < 2) {
					r = tempTwo
							+ ((tempOne - tempTwo) * ((twoThirds - tempR) * 6));
				} else {
					r = tempTwo;
				}
				if ((6 * tempG) < 1) {
					g = tempTwo + ((tempOne - tempTwo) * 6 * tempG);
				} else if ((2 * tempG) < 1) {
					g = tempOne;
				} else if ((3 * tempG) < 2) {
					g = tempTwo
							+ ((tempOne - tempTwo) * ((twoThirds - tempG) * 6));
				} else {
					g = tempTwo;
				}
				if ((6 * tempB) < 1) {
					b = tempTwo + ((tempOne - tempTwo) * 6 * tempB);
				} else if ((2 * tempB) < 1) {
					b = tempOne;
				} else if ((3 * tempB) < 2) {
					b = tempTwo
							+ ((tempOne - tempTwo) * ((twoThirds - tempB) * 6));
				} else {
					b = tempTwo;
				}
				if (r < 0) {
					r = 0;
				}
				if (g < 0) {
					g = 0;
				}
				if (b < 0) {
					b = 0;
				}
				rgb = maybePooledArray(r, g, b);
				return;
			}
			populateCIEXYZ();
			double x = ciexyz[0] / 100;
			double y = ciexyz[1] / 100;
			double z = ciexyz[2] / 100;

			double r = x * 3.2406 + y * -1.5372 + z * -0.4986;
			double g = x * -0.9689 + y * 1.8758 + z * 0.0415;
			double b = x * 0.0557 + y * -0.2040 + z * 1.0570;

			if (r > 0.0031308) {
				r = 1.055 * Math.pow(r, (1 / 2.4)) - 0.055;
			} else {
				r = 12.92 * r;
			}
			if (g > 0.0031308) {
				g = 1.055 * Math.pow(g, (1 / 2.4)) - 0.055;
			} else {
				g = 12.92 * g;
			}
			if (b > 0.0031308) {
				b = 1.055 * Math.pow(b, (1 / 2.4)) - 0.055;
			} else {
				b = 12.92 * b;
			}
			r = FastMath.clamp(r, 0, 1);
			g = FastMath.clamp(g, 0, 1);
			b = FastMath.clamp(b, 0, 1);
			rgb = maybePooledArray(r, g, b);
			return;
		}
	}

	/**
	 * Ensure that a cached HSL value is available.
	 * <p>
	 * May also populate other colorspaces if they are needed for the
	 * conversion.
	 */
	public void populateHSL() {
		if (recycled) throw new RecycledObjectException(this);
		if (hsl == null) {
			if (hsv != null) {
				double s;
				if ((2 - hsv[1]) * hsv[2] < 1) {
					s = hsv[1] * hsv[2] / ((2 - hsv[1]) * hsv[2]);
				} else {
					s = hsv[1] * hsv[2] / (2 - (2 - hsv[1]) * hsv[2]);
				}
				double l = ((2 - hsv[1]) * hsv[2]) / 2;
				hsl = maybePooledArray(hsv[0], s, l);
				return;
			}
			populateRGB();
			double[] rgbOrdered = Arrays.copyOf(rgb, 3);
			Arrays.sort(rgbOrdered);
			double l = ((rgbOrdered[0] + rgbOrdered[2]) / 2);
			double s, h;
			if (Double.compare(rgbOrdered[0], rgbOrdered[2]) == 0) {
				s = 0;
				h = 0;
			} else {
				if (l >= 50) {
					s = ((rgbOrdered[2] - rgbOrdered[0]) / ((2D - rgbOrdered[2]) - rgbOrdered[0]));
				} else {
					s = ((rgbOrdered[2] - rgbOrdered[0]) / (rgbOrdered[2] + rgbOrdered[0]));
				}
				if (Double.compare(rgbOrdered[2], rgb[0]) == 0) {
					h = ((rgb[1] - rgb[2]) / (rgbOrdered[2] - rgbOrdered[0])) / 6;
				} else if (Double.compare(rgbOrdered[2], rgb[1]) == 0) {
					h = (2 + ((rgb[2] - rgb[0]) / (rgbOrdered[2] - rgbOrdered[0]))) / 6;
				} else {
					h = (4 + ((rgb[0] - rgb[1]) / (rgbOrdered[2] - rgbOrdered[0]))) / 6;
				}
				if (h < 0) {
					h += 1;
				} else if (h > 1) {
					h = h % 1;
				}
			}
			hsl = maybePooledArray(h, s, l);
			return;
		}
	}

	/**
	 * Ensure that a cached HSV value is available.
	 * <p>
	 * May also populate other colorspaces if they are needed for the
	 * conversion.
	 */
	public void populateHSV() {
		if (recycled) throw new RecycledObjectException(this);
		if (hsv == null) {
			populateHSL();
			double luminance = hsl[1] * (hsl[2] < 0.5 ? hsl[2] : 1 - hsl[2]);
			double h = hsl[0];
			double s = (2 * luminance) / (hsl[2] + luminance);
			double v = hsl[2] + luminance;
			hsv = maybePooledArray(h, s, v);
			return;
		}
	}

	// Observer = 2°, Illuminant = D65
	private static final double REF_X = 95.047;
	private static final double REF_Y = 100.000;
	private static final double REF_Z = 108.883;

	/**
	 * Ensure that a cached CIEXYZ value is available.
	 * <p>
	 * May also populate other colorspaces if they are needed for the
	 * conversion.
	 */
	public void populateCIEXYZ() {
		if (recycled) throw new RecycledObjectException(this);
		if (ciexyz == null) {
			if (hasRGBIshColorspace()) {
				populateRGB();
				double sr = rgb[0];
				sr = sr < 0.04045 ? sr / 12.92 : Math.pow((sr + 0.055) / 1.055, 2.4);
				double sg = rgb[1];
				sg = sg < 0.04045 ? sg / 12.92 : Math.pow((sg + 0.055) / 1.055, 2.4);
				double sb = rgb[2];
				sb = sb < 0.04045 ? sb / 12.92 : Math.pow((sb + 0.055) / 1.055, 2.4);
				double x = 100 * (sr * 0.4124 + sg * 0.3576 + sb * 0.1805);
				double y = 100 * (sr * 0.2126 + sg * 0.7152 + sb * 0.0722);
				double z = 100 * (sr * 0.0193 + sg * 0.1192 + sb * 0.9505);
				ciexyz = maybePooledArray(x, y, z);
				return;
			}
			populateCIELAB();
			double l = cielab[0];
			double a = cielab[1];
			double b = cielab[2];

			double y = (l + 16) / 116.0;
			double x = a / 500 + y;
			double z = y - b / 200.0;

			if (Math.pow(y, 3) > 0.008856) {
				y = Math.pow(y, 3);
			} else {
				y = (y - 16 / 116.0) / 7.787;
			}
			if (Math.pow(x, 3) > 0.008856) {
				x = Math.pow(x, 3);
			} else {
				x = (x - 16 / 116.0) / 7.787;
			}
			if (Math.pow(z, 3) > 0.008856) {
				z = Math.pow(z, 3);
			} else {
				z = (z - 16 / 116.0) / 7.787;
			}

			x *= REF_X;
			y *= REF_Y;
			z *= REF_Z;
			ciexyz = maybePooledArray(x, y, z);
			return;
		}
	}

	/**
	 * Ensure that a cached CIELAB value is available.
	 * <p>
	 * May also populate other colorspaces if they are needed for the
	 * conversion.
	 */
	public void populateCIELAB() {
		if (recycled) throw new RecycledObjectException(this);
		if (cielab == null) {
			if (cielch != null) {
				double l = cielch[0];
				double c = cielch[1];
				double h = cielch[2];

				double a = FastMath.cosDeg(h) * c;
				double b = FastMath.sinDeg(h) * c;
				cielab = maybePooledArray(l, a, b);
				return;
			}
			populateCIEXYZ();
			double x = ciexyz[0] / REF_X;
			double y = ciexyz[1] / REF_Y;
			double z = ciexyz[2] / REF_Z;

			if (x > 0.008856) {
				x = Math.pow(x, (1 / 3.0));
			} else {
				x = (7.787 * x) + (16 / 116.0);
			}
			if (y > 0.008856) {
				y = Math.pow(y, 1 / 3.0);
			} else {
				y = (7.787 * y) + (16 / 116.0);
			}
			if (z > 0.008856) {
				z = Math.pow(z, 1 / 3.0);
			} else {
				z = (7.787 * z) + (16 / 116.0);
			}

			double l = (116 * y) - 16;
			double a = 500 * (x - y);
			double b = 200 * (y - z);
			cielab = maybePooledArray(l, a, b);
			return;
		}
	}

	/**
	 * Ensure that a cached CIELCH value is available.
	 * <p>
	 * May also populate other colorspaces if they are needed for the
	 * conversion.
	 */
	public void populateCIELCH() {
		if (recycled) throw new RecycledObjectException(this);
		if (cielch == null) {
			populateCIELAB();
			double l = cielab[0];
			double a = cielab[1];
			double b = cielab[2];

			double c = FastMath.sqrt((a * a) + (b * b));
			double h = FastMath.atan2(b, a);

			if (h > 0) {
				h = (h / FastMath.PI) * 180.0;
			} else {
				h = 360 - FastMath.toDegrees(Math.abs(h));
			}

			cielch = maybePooledArray(l, c, h);
			return;
		}
	}

	private boolean hasRGBIshColorspace() {
		return rgb != null || hsv != null || hsl != null;
	}

	private ProtoColor populateAll() {
		populateRGB();
		populateHSL();
		populateHSV();
		populateCIEXYZ();
		populateCIELAB();
		populateCIELCH();
		return this;
	}

	private static double negativeModulo(double n, double m) {
		return ((n % m) + m) % m;
	}

	public static final ProtoColor BLACK = unpooledConstant(0x000000);
	public static final ProtoColor WHITE = unpooledConstant(0xFFFFFF);

	public static final ProtoColor RED = unpooledConstant(0xFF0000);
	public static final ProtoColor GREEN = unpooledConstant(0x00FF00);
	public static final ProtoColor BLUE = unpooledConstant(0x0000FF);

	public static final ProtoColor YELLOW = unpooledConstant(0xFFFF00);
	public static final ProtoColor MAGENTA = unpooledConstant(0xFF00FF);
	public static final ProtoColor CYAN = unpooledConstant(0x00FFFF);

	public static final class CSSColors {
		public static final ProtoColor ALICEBLUE = unpooledConstant(0xF0F8FF);
		public static final ProtoColor ANTIQUEWHITE = unpooledConstant(0xFAEBD7);
		public static final ProtoColor AQUA = unpooledConstant(0x00FFFF);
		public static final ProtoColor AQUAMARINE = unpooledConstant(0x7FFFD4);
		public static final ProtoColor AZURE = unpooledConstant(0xF0FFFF);
		public static final ProtoColor BEIGE = unpooledConstant(0xF5F5DC);
		public static final ProtoColor BISQUE = unpooledConstant(0xFFE4C4);
		public static final ProtoColor BLACK = unpooledConstant(0x000000);
		public static final ProtoColor BLANCHEDALMOND = unpooledConstant(0xFFEBCD);
		public static final ProtoColor BLUE = unpooledConstant(0x0000FF);
		public static final ProtoColor BLUEVIOLET = unpooledConstant(0x8A2BE2);
		public static final ProtoColor BROWN = unpooledConstant(0xA52A2A);
		public static final ProtoColor BURLYWOOD = unpooledConstant(0xDEB887);
		public static final ProtoColor CADETBLUE = unpooledConstant(0x5F9EA0);
		public static final ProtoColor CHARTREUSE = unpooledConstant(0x7FFF00);
		public static final ProtoColor CHOCOLATE = unpooledConstant(0xD2691E);
		public static final ProtoColor CORAL = unpooledConstant(0xFF7F50);
		public static final ProtoColor CORNFLOWERBLUE = unpooledConstant(0x6495ED);
		public static final ProtoColor CORNSILK = unpooledConstant(0xFFF8DC);
		public static final ProtoColor CRIMSON = unpooledConstant(0xDC143C);
		public static final ProtoColor CYAN = unpooledConstant(0x00FFFF);
		public static final ProtoColor DARKBLUE = unpooledConstant(0x00008B);
		public static final ProtoColor DARKCYAN = unpooledConstant(0x008B8B);
		public static final ProtoColor DARKGOLDENROD = unpooledConstant(0xB8860B);
		public static final ProtoColor DARKGRAY = unpooledConstant(0xA9A9A9);
		public static final ProtoColor DARKGREEN = unpooledConstant(0x006400);
		public static final ProtoColor DARKGREY = unpooledConstant(0xA9A9A9);
		public static final ProtoColor DARKKHAKI = unpooledConstant(0xBDB76B);
		public static final ProtoColor DARKMAGENTA = unpooledConstant(0x8B008B);
		public static final ProtoColor DARKOLIVEGREEN = unpooledConstant(0x556B2F);
		public static final ProtoColor DARKORANGE = unpooledConstant(0xFF8C00);
		public static final ProtoColor DARKORCHID = unpooledConstant(0x9932CC);
		public static final ProtoColor DARKRED = unpooledConstant(0x8B0000);
		public static final ProtoColor DARKSALMON = unpooledConstant(0xE9967A);
		public static final ProtoColor DARKSEAGREEN = unpooledConstant(0x8FBC8F);
		public static final ProtoColor DARKSLATEBLUE = unpooledConstant(0x483D8B);
		public static final ProtoColor DARKSLATEGRAY = unpooledConstant(0x2F4F4F);
		public static final ProtoColor DARKSLATEGREY = unpooledConstant(0x2F4F4F);
		public static final ProtoColor DARKTURQUOISE = unpooledConstant(0x00CED1);
		public static final ProtoColor DARKVIOLET = unpooledConstant(0x9400D3);
		public static final ProtoColor DEEPPINK = unpooledConstant(0xFF1493);
		public static final ProtoColor DEEPSKYBLUE = unpooledConstant(0x00BFFF);
		public static final ProtoColor DIMGRAY = unpooledConstant(0x696969);
		public static final ProtoColor DIMGREY = unpooledConstant(0x696969);
		public static final ProtoColor DODGERBLUE = unpooledConstant(0x1E90FF);
		public static final ProtoColor FIREBRICK = unpooledConstant(0xB22222);
		public static final ProtoColor FLORALWHITE = unpooledConstant(0xFFFAF0);
		public static final ProtoColor FORESTGREEN = unpooledConstant(0x228B22);
		public static final ProtoColor FUCHSIA = unpooledConstant(0xFF00FF);
		public static final ProtoColor GAINSBORO = unpooledConstant(0xDCDCDC);
		public static final ProtoColor GHOSTWHITE = unpooledConstant(0xF8F8FF);
		public static final ProtoColor GOLD = unpooledConstant(0xFFD700);
		public static final ProtoColor GOLDENROD = unpooledConstant(0xDAA520);
		public static final ProtoColor GRAY = unpooledConstant(0x808080);
		public static final ProtoColor GREEN = unpooledConstant(0x008000);
		public static final ProtoColor GREENYELLOW = unpooledConstant(0xADFF2F);
		public static final ProtoColor GREY = unpooledConstant(0x808080);
		public static final ProtoColor HONEYDEW = unpooledConstant(0xF0FFF0);
		public static final ProtoColor HOTPINK = unpooledConstant(0xFF69B4);
		public static final ProtoColor INDIANRED = unpooledConstant(0xCD5C5C);
		public static final ProtoColor INDIGO = unpooledConstant(0x4B0082);
		public static final ProtoColor IVORY = unpooledConstant(0xFFFFF0);
		public static final ProtoColor KHAKI = unpooledConstant(0xF0E68C);
		public static final ProtoColor LAVENDER = unpooledConstant(0xE6E6FA);
		public static final ProtoColor LAVENDERBLUSH = unpooledConstant(0xFFF0F5);
		public static final ProtoColor LAWNGREEN = unpooledConstant(0x7CFC00);
		public static final ProtoColor LEMONCHIFFON = unpooledConstant(0xFFFACD);
		public static final ProtoColor LIGHTBLUE = unpooledConstant(0xADD8E6);
		public static final ProtoColor LIGHTCORAL = unpooledConstant(0xF08080);
		public static final ProtoColor LIGHTCYAN = unpooledConstant(0xE0FFFF);
		public static final ProtoColor LIGHTGOLDENRODYELLOW = unpooledConstant(0xFAFAD2);
		public static final ProtoColor LIGHTGRAY = unpooledConstant(0xD3D3D3);
		public static final ProtoColor LIGHTGREEN = unpooledConstant(0x90EE90);
		public static final ProtoColor LIGHTGREY = unpooledConstant(0xD3D3D3);
		public static final ProtoColor LIGHTPINK = unpooledConstant(0xFFB6C1);
		public static final ProtoColor LIGHTSALMON = unpooledConstant(0xFFA07A);
		public static final ProtoColor LIGHTSEAGREEN = unpooledConstant(0x20B2AA);
		public static final ProtoColor LIGHTSKYBLUE = unpooledConstant(0x87CEFA);
		public static final ProtoColor LIGHTSLATEGRAY = unpooledConstant(0x778899);
		public static final ProtoColor LIGHTSLATEGREY = unpooledConstant(0x778899);
		public static final ProtoColor LIGHTSTEELBLUE = unpooledConstant(0xB0C4DE);
		public static final ProtoColor LIGHTYELLOW = unpooledConstant(0xFFFFE0);
		public static final ProtoColor LIME = unpooledConstant(0x00FF00);
		public static final ProtoColor LIMEGREEN = unpooledConstant(0x32CD32);
		public static final ProtoColor LINEN = unpooledConstant(0xFAF0E6);
		public static final ProtoColor MAGENTA = unpooledConstant(0xFF00FF);
		public static final ProtoColor MAROON = unpooledConstant(0x800000);
		public static final ProtoColor MEDIUMAQUAMARINE = unpooledConstant(0x66CDAA);
		public static final ProtoColor MEDIUMBLUE = unpooledConstant(0x0000CD);
		public static final ProtoColor MEDIUMORCHID = unpooledConstant(0xBA55D3);
		public static final ProtoColor MEDIUMPURPLE = unpooledConstant(0x9370DB);
		public static final ProtoColor MEDIUMSEAGREEN = unpooledConstant(0x3CB371);
		public static final ProtoColor MEDIUMSLATEBLUE = unpooledConstant(0x7B68EE);
		public static final ProtoColor MEDIUMSPRINGGREEN = unpooledConstant(0x00FA9A);
		public static final ProtoColor MEDIUMTURQUOISE = unpooledConstant(0x48D1CC);
		public static final ProtoColor MEDIUMVIOLETRED = unpooledConstant(0xC71585);
		public static final ProtoColor MIDNIGHTBLUE = unpooledConstant(0x191970);
		public static final ProtoColor MINTCREAM = unpooledConstant(0xF5FFFA);
		public static final ProtoColor MISTYROSE = unpooledConstant(0xFFE4E1);
		public static final ProtoColor MOCCASIN = unpooledConstant(0xFFE4B5);
		public static final ProtoColor NAVAJOWHITE = unpooledConstant(0xFFDEAD);
		public static final ProtoColor NAVY = unpooledConstant(0x000080);
		public static final ProtoColor OLDLACE = unpooledConstant(0xFDF5E6);
		public static final ProtoColor OLIVE = unpooledConstant(0x808000);
		public static final ProtoColor OLIVEDRAB = unpooledConstant(0x6B8E23);
		public static final ProtoColor ORANGE = unpooledConstant(0xFFA500);
		public static final ProtoColor ORANGERED = unpooledConstant(0xFF4500);
		public static final ProtoColor ORCHID = unpooledConstant(0xDA70D6);
		public static final ProtoColor PALEGOLDENROD = unpooledConstant(0xEEE8AA);
		public static final ProtoColor PALEGREEN = unpooledConstant(0x98FB98);
		public static final ProtoColor PALETURQUOISE = unpooledConstant(0xAFEEEE);
		public static final ProtoColor PALEVIOLETRED = unpooledConstant(0xDB7093);
		public static final ProtoColor PAPAYAWHIP = unpooledConstant(0xFFEFD5);
		public static final ProtoColor PEACHPUFF = unpooledConstant(0xFFDAB9);
		public static final ProtoColor PERU = unpooledConstant(0xCD853F);
		public static final ProtoColor PINK = unpooledConstant(0xFFC0CB);
		public static final ProtoColor PLUM = unpooledConstant(0xDDA0DD);
		public static final ProtoColor POWDERBLUE = unpooledConstant(0xB0E0E6);
		public static final ProtoColor PURPLE = unpooledConstant(0x800080);
		public static final ProtoColor REBECCAPURPLE = unpooledConstant(0x663399);
		public static final ProtoColor RED = unpooledConstant(0xFF0000);
		public static final ProtoColor ROSYBROWN = unpooledConstant(0xBC8F8F);
		public static final ProtoColor ROYALBLUE = unpooledConstant(0x4169E1);
		public static final ProtoColor SADDLEBROWN = unpooledConstant(0x8B4513);
		public static final ProtoColor SALMON = unpooledConstant(0xFA8072);
		public static final ProtoColor SANDYBROWN = unpooledConstant(0xF4A460);
		public static final ProtoColor SEAGREEN = unpooledConstant(0x2E8B57);
		public static final ProtoColor SEASHELL = unpooledConstant(0xFFF5EE);
		public static final ProtoColor SIENNA = unpooledConstant(0xA0522D);
		public static final ProtoColor SILVER = unpooledConstant(0xC0C0C0);
		public static final ProtoColor SKYBLUE = unpooledConstant(0x87CEEB);
		public static final ProtoColor SLATEBLUE = unpooledConstant(0x6A5ACD);
		public static final ProtoColor SLATEGRAY = unpooledConstant(0x708090);
		public static final ProtoColor SLATEGREY = unpooledConstant(0x708090);
		public static final ProtoColor SNOW = unpooledConstant(0xFFFAFA);
		public static final ProtoColor SPRINGGREEN = unpooledConstant(0x00FF7F);
		public static final ProtoColor STEELBLUE = unpooledConstant(0x4682B4);
		public static final ProtoColor TAN = unpooledConstant(0xD2B48C);
		public static final ProtoColor TEAL = unpooledConstant(0x008080);
		public static final ProtoColor THISTLE = unpooledConstant(0xD8BFD8);
		public static final ProtoColor TOMATO = unpooledConstant(0xFF6347);
		public static final ProtoColor TURQUOISE = unpooledConstant(0x40E0D0);
		public static final ProtoColor VIOLET = unpooledConstant(0xEE82EE);
		public static final ProtoColor WHEAT = unpooledConstant(0xF5DEB3);
		public static final ProtoColor WHITE = unpooledConstant(0xFFFFFF);
		public static final ProtoColor WHITESMOKE = unpooledConstant(0xF5F5F5);
		public static final ProtoColor YELLOW = unpooledConstant(0xFFFF00);
		public static final ProtoColor YELLOWGREEN = unpooledConstant(0x9ACD32);

		private static final ImmutableMap<String, ProtoColor> MAP;

		static {
			// I am not writing all that again
			// go go gadget reflection
			ImmutableMap.Builder<String, ProtoColor> bldr = ImmutableMap.builder();
			for (Field f : CSSColors.class.getDeclaredFields()) {
				if (f.getType() == ProtoColor.class) {
					try {
						bldr.put(f.getName().toLowerCase(Locale.ROOT), (ProtoColor) Preconditions.checkNotNull(f.get(null)));
					} catch (Exception e) {
						throw new AssertionError(e);
					}
				}
			}
			MAP = bldr.build();
		}

		/**
		 * Returns a view of all the colors defined in this class, in alphabetical
		 * order. All keys are lowercase.
		 */
		public static ImmutableMap<String, ProtoColor> asMap() {
			return MAP;
		}

		private CSSColors() {}
	}

	public static final class MaterialColors {
		public static final ProtoColor RED_50 = unpooledConstant(0xFFEBEE);
		public static final ProtoColor RED_100 = unpooledConstant(0xFFCDD2);
		public static final ProtoColor RED_200 = unpooledConstant(0xEF9A9A);
		public static final ProtoColor RED_300 = unpooledConstant(0xE57373);
		public static final ProtoColor RED_400 = unpooledConstant(0xEF5350);
		public static final ProtoColor RED_500 = unpooledConstant(0xF44336);
		public static final ProtoColor RED_600 = unpooledConstant(0xE53935);
		public static final ProtoColor RED_700 = unpooledConstant(0xD32F2F);
		public static final ProtoColor RED_800 = unpooledConstant(0xC62828);
		public static final ProtoColor RED_900 = unpooledConstant(0xB71C1C);

		public static final ProtoColor RED_A100 = unpooledConstant(0xFF8A80);
		public static final ProtoColor RED_A200 = unpooledConstant(0xFF5252);
		public static final ProtoColor RED_A400 = unpooledConstant(0xFF1744);
		public static final ProtoColor RED_A700 = unpooledConstant(0xD50000);


		public static final ProtoColor PINK_50 = unpooledConstant(0xFCE4EC);
		public static final ProtoColor PINK_100 = unpooledConstant(0xF8BBD0);
		public static final ProtoColor PINK_200 = unpooledConstant(0xF48FB1);
		public static final ProtoColor PINK_300 = unpooledConstant(0xF06292);
		public static final ProtoColor PINK_400 = unpooledConstant(0xEC407A);
		public static final ProtoColor PINK_500 = unpooledConstant(0xE91E63);
		public static final ProtoColor PINK_600 = unpooledConstant(0xD81D60);
		public static final ProtoColor PINK_700 = unpooledConstant(0xC2185B);
		public static final ProtoColor PINK_800 = unpooledConstant(0xAD1457);
		public static final ProtoColor PINK_900 = unpooledConstant(0x880E4F);

		public static final ProtoColor PINK_A100 = unpooledConstant(0xFF80AB);
		public static final ProtoColor PINK_A200 = unpooledConstant(0xFF4081);
		public static final ProtoColor PINK_A400 = unpooledConstant(0xF50057);
		public static final ProtoColor PINK_A700 = unpooledConstant(0xC51162);


		public static final ProtoColor PURPLE_50 = unpooledConstant(0xF3E5F5);
		public static final ProtoColor PURPLE_100 = unpooledConstant(0xE1BEE7);
		public static final ProtoColor PURPLE_200 = unpooledConstant(0xCE93D8);
		public static final ProtoColor PURPLE_300 = unpooledConstant(0xBA68C8);
		public static final ProtoColor PURPLE_400 = unpooledConstant(0xAB47BC);
		public static final ProtoColor PURPLE_500 = unpooledConstant(0x9C27B0);
		public static final ProtoColor PURPLE_600 = unpooledConstant(0x8324AA);
		public static final ProtoColor PURPLE_700 = unpooledConstant(0x7B1FA2);
		public static final ProtoColor PURPLE_800 = unpooledConstant(0x6A1B9A);
		public static final ProtoColor PURPLE_900 = unpooledConstant(0x4A178C);

		public static final ProtoColor PURPLE_A100 = unpooledConstant(0xEA80FC);
		public static final ProtoColor PURPLE_A200 = unpooledConstant(0xE040FB);
		public static final ProtoColor PURPLE_A400 = unpooledConstant(0xD500F9);
		public static final ProtoColor PURPLE_A700 = unpooledConstant(0xAA00FF);


		public static final ProtoColor DEEP_PURPLE_50 = unpooledConstant(0xEDE7F6);
		public static final ProtoColor DEEP_PURPLE_100 = unpooledConstant(0xD1C4E9);
		public static final ProtoColor DEEP_PURPLE_200 = unpooledConstant(0xB39DDB);
		public static final ProtoColor DEEP_PURPLE_300 = unpooledConstant(0x9575CD);
		public static final ProtoColor DEEP_PURPLE_400 = unpooledConstant(0x7E57C2);
		public static final ProtoColor DEEP_PURPLE_500 = unpooledConstant(0x673AB7);
		public static final ProtoColor DEEP_PURPLE_600 = unpooledConstant(0x5E35B1);
		public static final ProtoColor DEEP_PURPLE_700 = unpooledConstant(0x512DA8);
		public static final ProtoColor DEEP_PURPLE_800 = unpooledConstant(0x4527A0);
		public static final ProtoColor DEEP_PURPLE_900 = unpooledConstant(0x311B92);

		public static final ProtoColor DEEP_PURPLE_A100 = unpooledConstant(0xB388FF);
		public static final ProtoColor DEEP_PURPLE_A200 = unpooledConstant(0x7C4DFF);
		public static final ProtoColor DEEP_PURPLE_A400 = unpooledConstant(0x651FFF);
		public static final ProtoColor DEEP_PURPLE_A700 = unpooledConstant(0x6200EA);


		public static final ProtoColor INDIGO_50 = unpooledConstant(0xE8EAF6);
		public static final ProtoColor INDIGO_100 = unpooledConstant(0xC5CAE9);
		public static final ProtoColor INDIGO_200 = unpooledConstant(0x9FA8DA);
		public static final ProtoColor INDIGO_300 = unpooledConstant(0x7986CB);
		public static final ProtoColor INDIGO_400 = unpooledConstant(0x5C6BC0);
		public static final ProtoColor INDIGO_500 = unpooledConstant(0x3F51B5);
		public static final ProtoColor INDIGO_600 = unpooledConstant(0x3949AB);
		public static final ProtoColor INDIGO_700 = unpooledConstant(0x303F9F);
		public static final ProtoColor INDIGO_800 = unpooledConstant(0x283593);
		public static final ProtoColor INDIGO_900 = unpooledConstant(0x1A237E);

		public static final ProtoColor INDIGO_A100 = unpooledConstant(0x8C9EFF);
		public static final ProtoColor INDIGO_A200 = unpooledConstant(0x536DFE);
		public static final ProtoColor INDIGO_A400 = unpooledConstant(0x3D5AFE);
		public static final ProtoColor INDIGO_A700 = unpooledConstant(0x304FFE);


		public static final ProtoColor BLUE_50 = unpooledConstant(0xE3F2FD);
		public static final ProtoColor BLUE_100 = unpooledConstant(0xBBDEFB);
		public static final ProtoColor BLUE_200 = unpooledConstant(0x90CAF9);
		public static final ProtoColor BLUE_300 = unpooledConstant(0x64B5F6);
		public static final ProtoColor BLUE_400 = unpooledConstant(0x42A5F5);
		public static final ProtoColor BLUE_500 = unpooledConstant(0x2196F3);
		public static final ProtoColor BLUE_600 = unpooledConstant(0x1E88E5);
		public static final ProtoColor BLUE_700 = unpooledConstant(0x1976D2);
		public static final ProtoColor BLUE_800 = unpooledConstant(0x1565C0);
		public static final ProtoColor BLUE_900 = unpooledConstant(0x0D47A1);

		public static final ProtoColor BLUE_A100 = unpooledConstant(0x82B1FF);
		public static final ProtoColor BLUE_A200 = unpooledConstant(0x448AFF);
		public static final ProtoColor BLUE_A400 = unpooledConstant(0x2979FF);
		public static final ProtoColor BLUE_A700 = unpooledConstant(0x2962FF);


		public static final ProtoColor LIGHT_BLUE_50 = unpooledConstant(0xE1F5FE);
		public static final ProtoColor LIGHT_BLUE_100 = unpooledConstant(0xB3E5FC);
		public static final ProtoColor LIGHT_BLUE_200 = unpooledConstant(0x81D4FA);
		public static final ProtoColor LIGHT_BLUE_300 = unpooledConstant(0x4FC3F7);
		public static final ProtoColor LIGHT_BLUE_400 = unpooledConstant(0x29B6F6);
		public static final ProtoColor LIGHT_BLUE_500 = unpooledConstant(0x03A9F4);
		public static final ProtoColor LIGHT_BLUE_600 = unpooledConstant(0x039BE5);
		public static final ProtoColor LIGHT_BLUE_700 = unpooledConstant(0x0288D1);
		public static final ProtoColor LIGHT_BLUE_800 = unpooledConstant(0x0277BD);
		public static final ProtoColor LIGHT_BLUE_900 = unpooledConstant(0x01579B);

		public static final ProtoColor LIGHT_BLUE_A100 = unpooledConstant(0x80D8FF);
		public static final ProtoColor LIGHT_BLUE_A200 = unpooledConstant(0x40C4FF);
		public static final ProtoColor LIGHT_BLUE_A400 = unpooledConstant(0x00B0FF);
		public static final ProtoColor LIGHT_BLUE_A700 = unpooledConstant(0x0091EA);


		public static final ProtoColor CYAN_50 = unpooledConstant(0xE0F7FA);
		public static final ProtoColor CYAN_100 = unpooledConstant(0xB2EBF2);
		public static final ProtoColor CYAN_200 = unpooledConstant(0x80DEEA);
		public static final ProtoColor CYAN_300 = unpooledConstant(0x4DD0E1);
		public static final ProtoColor CYAN_400 = unpooledConstant(0x26C6DA);
		public static final ProtoColor CYAN_500 = unpooledConstant(0x00BCD4);
		public static final ProtoColor CYAN_600 = unpooledConstant(0x00ACC1);
		public static final ProtoColor CYAN_700 = unpooledConstant(0x0097A7);
		public static final ProtoColor CYAN_800 = unpooledConstant(0x00838F);
		public static final ProtoColor CYAN_900 = unpooledConstant(0x006064);

		public static final ProtoColor CYAN_A100 = unpooledConstant(0x84FFFF);
		public static final ProtoColor CYAN_A200 = unpooledConstant(0x18FFFF);
		public static final ProtoColor CYAN_A400 = unpooledConstant(0x00E5FF);
		public static final ProtoColor CYAN_A700 = unpooledConstant(0x00B8D4);


		public static final ProtoColor TEAL_50 = unpooledConstant(0xE0F2F1);
		public static final ProtoColor TEAL_100 = unpooledConstant(0xB2DFDB);
		public static final ProtoColor TEAL_200 = unpooledConstant(0x80CBC4);
		public static final ProtoColor TEAL_300 = unpooledConstant(0x4DB6AC);
		public static final ProtoColor TEAL_400 = unpooledConstant(0x26A69A);
		public static final ProtoColor TEAL_500 = unpooledConstant(0x009688);
		public static final ProtoColor TEAL_600 = unpooledConstant(0x00897B);
		public static final ProtoColor TEAL_700 = unpooledConstant(0x00769B);
		public static final ProtoColor TEAL_800 = unpooledConstant(0x00695C);
		public static final ProtoColor TEAL_900 = unpooledConstant(0x004D40);

		public static final ProtoColor TEAL_A100 = unpooledConstant(0xA7FFEB);
		public static final ProtoColor TEAL_A200 = unpooledConstant(0x64FFDA);
		public static final ProtoColor TEAL_A400 = unpooledConstant(0x1DE9B6);
		public static final ProtoColor TEAL_A700 = unpooledConstant(0x00BFA5);


		public static final ProtoColor GREEN_50 = unpooledConstant(0xE8F5E9);
		public static final ProtoColor GREEN_100 = unpooledConstant(0xC8E6C9);
		public static final ProtoColor GREEN_200 = unpooledConstant(0xA5D6A7);
		public static final ProtoColor GREEN_300 = unpooledConstant(0x81C784);
		public static final ProtoColor GREEN_400 = unpooledConstant(0x66BB6A);
		public static final ProtoColor GREEN_500 = unpooledConstant(0x4CAF50);
		public static final ProtoColor GREEN_600 = unpooledConstant(0x43A047);
		public static final ProtoColor GREEN_700 = unpooledConstant(0x388E3C);
		public static final ProtoColor GREEN_800 = unpooledConstant(0x237D32);
		public static final ProtoColor GREEN_900 = unpooledConstant(0x1B5E20);

		public static final ProtoColor GREEN_A100 = unpooledConstant(0xB9F6CA);
		public static final ProtoColor GREEN_A200 = unpooledConstant(0x69F0AE);
		public static final ProtoColor GREEN_A400 = unpooledConstant(0x00E676);
		public static final ProtoColor GREEN_A700 = unpooledConstant(0x00C853);


		public static final ProtoColor LIGHT_GREEN_50 = unpooledConstant(0xF1F8E9);
		public static final ProtoColor LIGHT_GREEN_100 = unpooledConstant(0xDCEDC8);
		public static final ProtoColor LIGHT_GREEN_200 = unpooledConstant(0xC5E1A5);
		public static final ProtoColor LIGHT_GREEN_300 = unpooledConstant(0xAED581);
		public static final ProtoColor LIGHT_GREEN_400 = unpooledConstant(0x9CCC65);
		public static final ProtoColor LIGHT_GREEN_500 = unpooledConstant(0x8BC34A);
		public static final ProtoColor LIGHT_GREEN_600 = unpooledConstant(0x7CB342);
		public static final ProtoColor LIGHT_GREEN_700 = unpooledConstant(0x689F38);
		public static final ProtoColor LIGHT_GREEN_800 = unpooledConstant(0x558B2F);
		public static final ProtoColor LIGHT_GREEN_900 = unpooledConstant(0x33691E);

		public static final ProtoColor LIGHT_GREEN_A100 = unpooledConstant(0xCCFF90);
		public static final ProtoColor LIGHT_GREEN_A200 = unpooledConstant(0xB2FF59);
		public static final ProtoColor LIGHT_GREEN_A400 = unpooledConstant(0x76FF03);
		public static final ProtoColor LIGHT_GREEN_A700 = unpooledConstant(0x64DD17);


		public static final ProtoColor LIME_50 = unpooledConstant(0xF9FBE7);
		public static final ProtoColor LIME_100 = unpooledConstant(0xF0F4C3);
		public static final ProtoColor LIME_200 = unpooledConstant(0xE6EE9C);
		public static final ProtoColor LIME_300 = unpooledConstant(0xDCE775);
		public static final ProtoColor LIME_400 = unpooledConstant(0xD4E157);
		public static final ProtoColor LIME_500 = unpooledConstant(0xCDDC39);
		public static final ProtoColor LIME_600 = unpooledConstant(0xC0CA33);
		public static final ProtoColor LIME_700 = unpooledConstant(0xAFB42B);
		public static final ProtoColor LIME_800 = unpooledConstant(0x9E9D24);
		public static final ProtoColor LIME_900 = unpooledConstant(0x827717);

		public static final ProtoColor LIME_A100 = unpooledConstant(0xF4FF81);
		public static final ProtoColor LIME_A200 = unpooledConstant(0xEEFF41);
		public static final ProtoColor LIME_A400 = unpooledConstant(0xC6FF00);
		public static final ProtoColor LIME_A700 = unpooledConstant(0xAEEA00);


		public static final ProtoColor YELLOW_50 = unpooledConstant(0xFFFDE7);
		public static final ProtoColor YELLOW_100 = unpooledConstant(0xFFF9C4);
		public static final ProtoColor YELLOW_200 = unpooledConstant(0xFFF59D);
		public static final ProtoColor YELLOW_300 = unpooledConstant(0xFFF176);
		public static final ProtoColor YELLOW_400 = unpooledConstant(0xFFEE58);
		public static final ProtoColor YELLOW_500 = unpooledConstant(0xFFEB3B);
		public static final ProtoColor YELLOW_600 = unpooledConstant(0xFFD835);
		public static final ProtoColor YELLOW_700 = unpooledConstant(0xFBC02D);
		public static final ProtoColor YELLOW_800 = unpooledConstant(0xF9A825);
		public static final ProtoColor YELLOW_900 = unpooledConstant(0xF57F17);

		public static final ProtoColor YELLOW_A100 = unpooledConstant(0xFFFF8D);
		public static final ProtoColor YELLOW_A200 = unpooledConstant(0xFFFF00);
		public static final ProtoColor YELLOW_A400 = unpooledConstant(0xFFEA00);
		public static final ProtoColor YELLOW_A700 = unpooledConstant(0xFFD600);


		public static final ProtoColor AMBER_50 = unpooledConstant(0xFFF8E1);
		public static final ProtoColor AMBER_100 = unpooledConstant(0xFFECB3);
		public static final ProtoColor AMBER_200 = unpooledConstant(0xFFE082);
		public static final ProtoColor AMBER_300 = unpooledConstant(0xFFD54F);
		public static final ProtoColor AMBER_400 = unpooledConstant(0xFFCA28);
		public static final ProtoColor AMBER_500 = unpooledConstant(0xFFC107);
		public static final ProtoColor AMBER_600 = unpooledConstant(0xFFB300);
		public static final ProtoColor AMBER_700 = unpooledConstant(0xFFA000);
		public static final ProtoColor AMBER_800 = unpooledConstant(0xFF8F00);
		public static final ProtoColor AMBER_900 = unpooledConstant(0xFF6F00);

		public static final ProtoColor AMBER_A100 = unpooledConstant(0xFFE57F);
		public static final ProtoColor AMBER_A200 = unpooledConstant(0xFFD740);
		public static final ProtoColor AMBER_A400 = unpooledConstant(0xFFC400);
		public static final ProtoColor AMBER_A700 = unpooledConstant(0xFFAB00);


		public static final ProtoColor ORANGE_50 = unpooledConstant(0xFFF3E0);
		public static final ProtoColor ORANGE_100 = unpooledConstant(0xFFE0B2);
		public static final ProtoColor ORANGE_200 = unpooledConstant(0xFFCC80);
		public static final ProtoColor ORANGE_300 = unpooledConstant(0xFFB74D);
		public static final ProtoColor ORANGE_400 = unpooledConstant(0xFFA726);
		public static final ProtoColor ORANGE_500 = unpooledConstant(0xFF9800);
		public static final ProtoColor ORANGE_600 = unpooledConstant(0xFB8C00);
		public static final ProtoColor ORANGE_700 = unpooledConstant(0xF57C00);
		public static final ProtoColor ORANGE_800 = unpooledConstant(0xEF6C00);
		public static final ProtoColor ORANGE_900 = unpooledConstant(0xE65100);

		public static final ProtoColor ORANGE_A100 = unpooledConstant(0xFFD180);
		public static final ProtoColor ORANGE_A200 = unpooledConstant(0xFFAB40);
		public static final ProtoColor ORANGE_A400 = unpooledConstant(0xFF9100);
		public static final ProtoColor ORANGE_A700 = unpooledConstant(0xFF6D00);


		public static final ProtoColor DEEP_ORANGE_50 = unpooledConstant(0xFBE9E7);
		public static final ProtoColor DEEP_ORANGE_100 = unpooledConstant(0xFFCCBC);
		public static final ProtoColor DEEP_ORANGE_200 = unpooledConstant(0xFFAB91);
		public static final ProtoColor DEEP_ORANGE_300 = unpooledConstant(0xFF8A65);
		public static final ProtoColor DEEP_ORANGE_400 = unpooledConstant(0xFF7043);
		public static final ProtoColor DEEP_ORANGE_500 = unpooledConstant(0xFF5722);
		public static final ProtoColor DEEP_ORANGE_600 = unpooledConstant(0xF4511E);
		public static final ProtoColor DEEP_ORANGE_700 = unpooledConstant(0xE64A19);
		public static final ProtoColor DEEP_ORANGE_800 = unpooledConstant(0xD84315);
		public static final ProtoColor DEEP_ORANGE_900 = unpooledConstant(0xBF360C);

		public static final ProtoColor DEEP_ORANGE_A100 = unpooledConstant(0xFF9E80);
		public static final ProtoColor DEEP_ORANGE_A200 = unpooledConstant(0xFF6E40);
		public static final ProtoColor DEEP_ORANGE_A400 = unpooledConstant(0xFF3D00);
		public static final ProtoColor DEEP_ORANGE_A700 = unpooledConstant(0xDD2C00);


		public static final ProtoColor BROWN_50 = unpooledConstant(0xEFEBE9);
		public static final ProtoColor BROWN_100 = unpooledConstant(0xD7CCC8);
		public static final ProtoColor BROWN_200 = unpooledConstant(0xBCAAA4);
		public static final ProtoColor BROWN_300 = unpooledConstant(0xA1887F);
		public static final ProtoColor BROWN_400 = unpooledConstant(0x8D6E63);
		public static final ProtoColor BROWN_500 = unpooledConstant(0x795548);
		public static final ProtoColor BROWN_600 = unpooledConstant(0x6D4C41);
		public static final ProtoColor BROWN_700 = unpooledConstant(0x5D4037);
		public static final ProtoColor BROWN_800 = unpooledConstant(0x4E342E);
		public static final ProtoColor BROWN_900 = unpooledConstant(0x3E2723);


		public static final ProtoColor GREY_50 = unpooledConstant(0xFAFAFA);
		public static final ProtoColor GREY_100 = unpooledConstant(0xF5F5F5);
		public static final ProtoColor GREY_200 = unpooledConstant(0xEEEEEE);
		public static final ProtoColor GREY_300 = unpooledConstant(0xE0E0E0);
		public static final ProtoColor GREY_400 = unpooledConstant(0xBDBDBD);
		public static final ProtoColor GREY_500 = unpooledConstant(0x9E9E9E);
		public static final ProtoColor GREY_600 = unpooledConstant(0x757575);
		public static final ProtoColor GREY_700 = unpooledConstant(0x616161);
		public static final ProtoColor GREY_800 = unpooledConstant(0x424242);
		public static final ProtoColor GREY_900 = unpooledConstant(0x212121);


		public static final ProtoColor BLUE_GREY_50 = unpooledConstant(0xECEFF1);
		public static final ProtoColor BLUE_GREY_100 = unpooledConstant(0xCFD8DC);
		public static final ProtoColor BLUE_GREY_200 = unpooledConstant(0xB0BEC5);
		public static final ProtoColor BLUE_GREY_300 = unpooledConstant(0x90A4AE);
		public static final ProtoColor BLUE_GREY_400 = unpooledConstant(0x78909C);
		public static final ProtoColor BLUE_GREY_500 = unpooledConstant(0x607D8B);
		public static final ProtoColor BLUE_GREY_600 = unpooledConstant(0x546E7A);
		public static final ProtoColor BLUE_GREY_700 = unpooledConstant(0x455A64);
		public static final ProtoColor BLUE_GREY_800 = unpooledConstant(0x37474F);
		public static final ProtoColor BLUE_GREY_900 = unpooledConstant(0x263238);

		private MaterialColors() {}
	}

}

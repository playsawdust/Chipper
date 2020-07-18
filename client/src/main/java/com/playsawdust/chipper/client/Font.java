/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.IntBuffer;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.playsawdust.chipper.AbstractNativeResource;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.PUAChars;
import com.playsawdust.chipper.exception.ResourceNotFoundException;

import com.playsawdust.chipper.client.component.ResourceCache;

import com.playsawdust.chipper.img.BufferedImage;
import com.playsawdust.chipper.math.ProtoColor;
import com.playsawdust.chipper.math.RectI;

import com.playsawdust.chipper.toolbox.concurrent.SharedThreadPool;
import com.playsawdust.chipper.toolbox.lipstick.MonotonicTime;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;

// TODO this is pretty much the first thing I wrote, so it predates Canvas
// should probably get ported to Canvas at some point and stop using raw GL
public class Font extends AbstractNativeResource {
	private static final Logger log = LoggerFactory.getLogger(Font.class);

	public final class PreparedString implements CharSequence {
		private final String text;
		private final List<Glyph> glyphs;
		private final int width;
		private final int height;
		private int token;
		private long lastRetrievedFromCache = 0;

		private PreparedString(String text, List<Glyph> glyphs, int width, int height, int token) {
			this.text = text;
			this.glyphs = glyphs;
			this.width = width;
			this.height = height;
			this.token = token;
		}
		public String getText() {
			return text;
		}
		public int getWidth() {
			return width;
		}
		public int getHeight() {
			return height;
		}
		public boolean isValid() {
			return Font.this.isValid(this);
		}
		public boolean belongsTo(Font font) {
			return font == Font.this;
		}

		@Override
		public char charAt(int index) {
			return text.charAt(index);
		}
		@Override
		public int length() {
			return text.length();
		}
		@Override
		public CharSequence subSequence(int start, int end) {
			return text.subSequence(start, end);
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (Glyph g : glyphs) {
				sb.append(g);
			}
			return sb.toString();
		}
	}

	private static class Glyph {
		public Block block;
		public int u;
		public int v;
		public int width;
		public int height;
		public boolean colored;
		public boolean replacement;
		private final String desc;

		public boolean overline;
		public boolean underline;
		public boolean bold;

		public Glyph(String desc, Block block, int u, int v, int width, int height) {
			this.desc = desc;
			this.block = block;
			this.u = u;
			this.v = v;
			this.width = width;
			this.height = height;
		}

		public void draw(Font font, double r, double g, double b, double a) {
			glBindTexture(GL_TEXTURE_2D, block.textureId);
			float minU = u/(float)block.width;
			float minV = v/(float)block.height;
			float maxU = (u+width)/(float)block.width;
			float maxV = (v+height)/(float)block.height;
			if (!colored) {
				glColor4d(r, g, b, a);
			} else {
				glColor4d(1, 1, 1, a);
			}
			glBegin(GL_QUADS);
				for (int i = 0; i < (bold ? 2 : 1); i++) {
					glTexCoord2f(minU, minV);
					glVertex2f(i, 0);
					glTexCoord2f(maxU, minV);
					glVertex2f(width+i, 0);
					glTexCoord2f(maxU, maxV);
					glVertex2f(width+i, height);
					glTexCoord2f(minU, maxV);
					glVertex2f(i, height);
				}
			glEnd();
			if (overline || underline) {
				glDisable(GL_TEXTURE_2D);
			}
			if (overline) {
				glBegin(GL_QUADS);
					glVertex2f(-font.lineExtend, font.overlineOffset);
					glVertex2f(width+font.lineExtend+font.characterSpacing, font.overlineOffset);
					glVertex2f(width+font.lineExtend+font.characterSpacing, font.overlineOffset+1);
					glVertex2f(-font.lineExtend, font.overlineOffset+1);
				glEnd();
			}
			if (underline) {
				glBegin(GL_QUADS);
					glVertex2f(-font.lineExtend, height-1+font.underlineOffset);
					glVertex2f(width+font.lineExtend+font.characterSpacing, height-1+font.underlineOffset);
					glVertex2f(width+font.lineExtend+font.characterSpacing, height+font.underlineOffset);
					glVertex2f(-font.lineExtend, height+font.underlineOffset);
				glEnd();
			}
			if (overline || underline) {
				glEnable(GL_TEXTURE_2D);
			}
		}

		@Override
		public String toString() {
			return desc;
		}

	}

	private static class Space extends Glyph {
		public Space(int width, int height) {
			super("\u2334", null, 0, 0, width, height);
		}

		@Override
		public void draw(Font font, double r, double g, double b, double a) {}
	}

	private static class Newline extends Glyph {
		public Newline() {
			super("\u2B92", null, 0, 0, 0, 0);
		}

		@Override
		public void draw(Font font, double r, double g, double b, double a) {}
	}

	private static class Block {
		public String name;
		public int textureId;
		public int width;
		public int height;

		public Block(String name, int width, int height) {
			this.name = name;
			this.width = width;
			this.height = height;
		}

	}

	private static class StandardBlock extends Block {
		public final BitSet supportedGlyphs = new BitSet(256);
		public final BitSet colorGlyphs = new BitSet(256);
		public Map<Integer, Integer> widths;
		public boolean loaded = false;
		public boolean loading = false;
		public boolean uploaded = false;

		private int imgWidth;
		private int imgHeight;
		private int[] argbBuf;

		public StandardBlock(String name, int width, int height) {
			super(name, width, height);
		}

		public void load(Font font) throws ResourceNotFoundException {
			if (loaded) return;
			loading = true;
			Stopwatch sw = font.lazy && log.isDebugEnabled() ? Stopwatch.createStarted() : null;
			BufferedImage img = font.resourceCache.loadImage(font.prefix.child(name+".png"));
			int[] argbBuf = new int[img.getWidth()*img.getHeight()];
			img.getARGB(0, 0, img.getWidth(), img.getHeight(), argbBuf, 0, img.getWidth());
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					boolean allTransparent = true;
					// initialize to true if there are no color glyphs to short-circuit the check
					// makes things way faster, especially for large fonts like Unifont
					boolean anyColored = font.noColorGlyphs;
					int furthestOpaquePixel = 0;
					out: for (int cX = font.defaultCharWidth-1; cX >= 0; cX--) {
						for (int cY = 0; cY < font.defaultCharHeight; cY++) {
							if (furthestOpaquePixel > cX && !allTransparent && anyColored) {
								// we've learned all we can
								break out;
							}
							int argb = argbBuf[(((y*font.defaultCharHeight)+cY)*img.getWidth())+((x*font.defaultCharWidth)+cX)];
							if ((argb >> 24) != 0) {
								allTransparent = false;
								furthestOpaquePixel = Math.max(furthestOpaquePixel, cX);
								int rgb = (argb & 0x00FFFFFF);
								if (rgb != 0xFFFFFF && rgb != 0x000000) {
									anyColored = true;
								}
							}
						}
					}
					if (!allTransparent) {
						int idx = (y*16)+x;
						if (font.proportional) {
							widths.put(idx, furthestOpaquePixel+1);
						}
						supportedGlyphs.set(idx);
						if (!font.noColorGlyphs && anyColored) {
							colorGlyphs.set(idx);
						}
					}
				}
			}
			imgWidth = img.getWidth();
			imgHeight = img.getHeight();
			img.free();
			this.argbBuf = argbBuf;
			loaded = true;
			loading = false;
			if (sw != null) {
				sw.stop();
				log.debug("Loaded block {} for font {} in {}", name, font.name, sw);
			}
		}

		public void upload(Font font) {
			if (!loaded) throw new IllegalStateException("Cannot upload before loading");
			if (uploaded) return;
			Stopwatch sw = font.lazy && log.isDebugEnabled() ? Stopwatch.createStarted() : null;
			textureId = font.uploadTexture(argbBuf, imgWidth, imgHeight, colorGlyphs.isEmpty() ? GL_ALPHA8 : GL_RGBA8);
			uploaded = true;
			argbBuf = null;
			imgWidth = 0;
			imgHeight = 0;
			if (sw != null) {
				sw.stop();
				log.debug("Uploaded block {} for font {} in {}", name, font.name, sw);
			}
		}
	}

	private final ResourceCache resourceCache;
	private final String name;
	private final Identifier prefix;
	private final ImmutableMap<String, StandardBlock> blocks;
	private final ImmutableSet<String> supportedFlags;
	private final int defaultCharWidth;
	private final int defaultCharHeight;
	private final int flagWidth;
	private final int flagHeight;
	private final boolean proportional;
	private final int spaceWidth;
	private final int characterSpacing;
	private final int lineSpacing;
	private final boolean lazy;
	private final boolean noColorGlyphs;

	private final int lineExtend;
	private final int overlineOffset;
	private final int underlineOffset;

	private final Block flagBlock;

	private final Map<String, PreparedString> stringCache = Maps.newHashMap();
	private final CopyOnWriteArraySet<String> badBlocks = new CopyOnWriteArraySet<>();
	private int stringCacheToken = 0;
	private int token = 0;

	/**
	 * @deprecated <b>Do not call this constructor directly</b>. Use {@link ResourceCache#getFont}.
	 */
	@Deprecated
	public Font(ResourceCache cache, JsonObject meta, Identifier prefix) throws ResourceNotFoundException {
		this.resourceCache = cache;
		this.prefix = prefix;
		this.name = ((JsonPrimitive)meta.get("name")).asString();
		this.defaultCharWidth = ((Number)((JsonPrimitive)meta.get("width")).getValue()).intValue();
		this.defaultCharHeight = ((Number)((JsonPrimitive)meta.get("height")).getValue()).intValue();
		this.proportional = meta.containsKey("proportional") && ((Boolean)((JsonPrimitive)meta.get("proportional")).getValue());
		this.spaceWidth = meta.containsKey("space_width") ? ((Number)((JsonPrimitive)meta.get("space_width")).getValue()).intValue() : defaultCharWidth;
		this.characterSpacing = meta.containsKey("character_spacing") ? ((Number)((JsonPrimitive)meta.get("character_spacing")).getValue()).intValue() : (proportional?1:0);
		this.lineSpacing = meta.containsKey("line_spacing") ? ((Number)((JsonPrimitive)meta.get("line_spacing")).getValue()).intValue() : 0;
		this.lazy = meta.containsKey("lazy") && ((Boolean)((JsonPrimitive)meta.get("lazy")).getValue());
		this.noColorGlyphs = meta.containsKey("no_color_glyphs") && ((Boolean)((JsonPrimitive)meta.get("no_color_glyphs")).getValue());
		this.lineExtend = meta.containsKey("line_extend") ? ((Number)((JsonPrimitive)meta.get("line_extend")).getValue()).intValue() : 1;
		this.overlineOffset = meta.containsKey("overline_y_offset") ? ((Number)((JsonPrimitive)meta.get("overline_y_offset")).getValue()).intValue() : 0;
		this.underlineOffset = meta.containsKey("underline_y_offset") ? ((Number)((JsonPrimitive)meta.get("underline_y_offset")).getValue()).intValue() : 0;
		JsonArray supportedBlocksArr = (JsonArray)meta.get("supported_blocks");
		List<String> supportedBlocks = Lists.newArrayList();
		boolean supportsFlags = false;
		for (JsonElement je : supportedBlocksArr) {
			String str = ((JsonPrimitive)je).asString();
			if ("flags".equals(str)) {
				supportsFlags = true;
			} else {
				supportedBlocks.add(str);
			}
		}
		if (supportsFlags) {
			this.flagWidth = ((Number)((JsonPrimitive)meta.get("flag_width")).getValue()).intValue();
			this.flagHeight = ((Number)((JsonPrimitive)meta.get("flag_height")).getValue()).intValue();
			ImmutableSet.Builder<String> supportedFlagsBldr = ImmutableSet.builder();
			int[] argbBuf = new int[flagWidth*flagHeight];
			BufferedImage img = cache.loadImage(prefix.child("flags.png"));
			for (int x = 0; x < 26; x++) {
				for (int y = 0; y < 26; y++) {
					img.getARGB(x*flagWidth, y*flagHeight, flagWidth, flagHeight, argbBuf, 0, flagWidth);
					for (int argb : argbBuf) {
						if ((argb >> 24) != 0) {
							char first = (char)('A'+y);
							char last = (char)('A'+x);
							supportedFlagsBldr.add(""+first+last);
							break;
						}
					}
				}
			}
			this.supportedFlags = supportedFlagsBldr.build();
			flagBlock = new Block("flags", flagWidth*26, flagHeight*26);
			flagBlock.textureId = uploadTexture(img, GL_RGBA8);
			img.free();
		} else {
			this.flagWidth = 0;
			this.flagHeight = 0;
			this.supportedFlags = ImmutableSet.of();
			this.flagBlock = null;
		}
		ImmutableMap.Builder<String, StandardBlock> blocksBldr = ImmutableMap.builderWithExpectedSize(supportedBlocks.size());
		boolean firstBlock = true;
		for (String block : supportedBlocks) {
			StandardBlock b = new StandardBlock(block, defaultCharWidth*16, defaultCharHeight*16);
			if (proportional) {
				b.widths = Maps.newHashMap();
			}
			if (firstBlock) {
				b.load(this);
				b.upload(this);
				firstBlock = false;
			}
			blocksBldr.put(block, b);
		}
		this.blocks = blocksBldr.build();
		if (!lazy) {
			for (StandardBlock b : blocks.values()) {
				b.load(this);
				b.upload(this);
			}
		}
	}

	private int uploadTexture(BufferedImage img, int format) {
		int w = img.getWidth();
		int h = img.getHeight();
		int[] buf = new int[w*h];
		img.getARGB(0, 0, w, h, buf, 0, w);
		return uploadTexture(buf, w, h, format);
	}

	private int uploadTexture(int[] buf, int w, int h, int format) {
		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, id);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		IntBuffer nativeBuf = memAllocInt(buf.length);
		nativeBuf.put(buf);
		nativeBuf.flip();
		glTexImage2D(GL_TEXTURE_2D, 0, format, w, h, 0, GL_BGRA, GL_UNSIGNED_BYTE, nativeBuf);
		memFree(nativeBuf);
		return id;
	}

	public String getName() {
		checkFreed();
		return name;
	}

	public int getGlyphWidth() {
		checkFreed();
		return defaultCharWidth;
	}

	public int getGlyphHeight() {
		checkFreed();
		return defaultCharHeight;
	}

	public int measure(String str) {
		checkFreed();
		if (str.isEmpty()) return 0;
		return prepare(str).getWidth();
	}

	public int measure(PreparedString str) {
		checkFreed();
		return str.getWidth();
	}

	public PreparedString prepare(String str) {
		checkFreed();
		if (stringCacheToken != token) {
			stringCache.clear();
			stringCacheToken = token;
		}
		PreparedString prep = stringCache.get(str);
		if (prep == null || prep.token != this.token) {
			prep = doPrepare(str);
			stringCache.put(str, prep);
		}
		prep.lastRetrievedFromCache = MonotonicTime.millis();
		while (stringCache.size() > 64) {
			long lruTime = Long.MAX_VALUE;
			String lru = null;
			for (Map.Entry<String, PreparedString> en : stringCache.entrySet()) {
				if (en.getValue().lastRetrievedFromCache < lruTime) {
					lru = en.getKey();
					lruTime = en.getValue().lastRetrievedFromCache;
				}
			}
			stringCache.remove(lru);
		}
		return prep;
	}

	public boolean isValid(PreparedString str) {
		return str.belongsTo(this) && str.token == this.token;
	}

	/**
	 * Prepare the given codepoint for rendering, to reduce flickering when encountering non-BMP
	 * characters.
	 */
	public void preload(int codepoint) {
		String block = Integer.toHexString(codepoint >> 8);
		if (block.length() == 1) block = "0".concat(block);
		if (!badBlocks.contains(block) && blocks.containsKey(block)) {
			StandardBlock b = blocks.get(block);
			if (!b.loaded) {
				b.loading = true;
				final String fblock = block;
				Runnable loader = () -> {
					try {
						b.load(this);
						token++;
					} catch (Throwable t) {
						log.error("Couldn't load block {} for font {}", fblock, name);
						badBlocks.add(fblock);
					} finally {
						b.loading = false;
					}
				};
				SharedThreadPool.submit(loader);
			}
		}
	}

	/**
	 * Prepare all the codepoints in the string for rendering, to reduce flickering when
	 * encountering non-BMP characters.
	 */
	public void preload(String s) {
		s.codePoints().forEach(this::preload);
	}

	private PreparedString doPrepare(String str) {
		checkFreed();
		List<Glyph> glyphs = toGlyphs(str);
		RectI size = measureGlyphs(glyphs);
		int width = size.getWidth();
		int height = size.getHeight();
		size.recycle();
		return new PreparedString(str, glyphs, width, height, token);
	}

	private RectI measureGlyphs(List<Glyph> glyphs) {
		int width = 0;
		int height = 0;
		int widthThisLine = 0;
		int heightThisLine = 0;
		for (Glyph g : glyphs) {
			if (g instanceof Newline) {
				width = Math.max(width, widthThisLine);
				height += heightThisLine+lineSpacing;
				widthThisLine = 0;
				heightThisLine = 0;
			} else {
				widthThisLine += g.width+characterSpacing;
				heightThisLine = Math.max(g.height, heightThisLine);
			}
		}
		width = Math.max(width, widthThisLine);
		height += heightThisLine;
		return RectI.fromSize(0, 0, width, height);
	}

	public void drawShadowedString(double x, double y, String str, ProtoColor color, double alpha) {
		drawShadowedString(x, y, prepare(str), color, alpha);
	}

	public void drawString(double x, double y, String str, int color) {
		drawString(x, y, prepare(str), color);
	}

	public void drawString(double x, double y, String str, ProtoColor color, double alpha) {
		drawString(x, y, prepare(str), color, alpha);
	}

	public void drawString(double x, double y, String str, double r, double g, double b, double a) {
		drawString(x, y, prepare(str), r, g, b, a);
	}

	public void drawShadowedString(double x, double y, PreparedString str, ProtoColor color, double alpha) {
		checkFreed();
		ProtoColor shadowColor = color.withHSVValue(color.getHSVValue()/4);
		drawString(x, y+1, str, shadowColor, alpha);
		drawString(x+1, y+1, str, shadowColor, alpha);
		drawString(x, y, str, color, alpha);
		shadowColor.recycle();
	}

	public void drawString(double x, double y, PreparedString str, int color) {
		ProtoColor pc = ProtoColor.fromRGB(color);
		drawString(x, y, str, pc, (color >> 24)/255D);
		pc.recycle();
	}

	public void drawString(double x, double y, PreparedString str, ProtoColor color, double alpha) {
		drawString(x, y, str, color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	public void drawString(double x, double y, PreparedString str, double r, double g, double b, double a) {
		checkFreed();
		drawGlyphs(x, y, str.glyphs, r, g, b, a);
	}

	public PreparedString wordWrap(String str, int maxWidth) {
		return wordWrap(prepare(str), maxWidth);
	}

	public PreparedString wordWrap(PreparedString str, int maxWidth) {
		checkFreed();
		List<Glyph> glyphs = Lists.newArrayList(str.glyphs);
		List<List<Glyph>> words = Lists.newArrayList();
		List<Glyph> work = Lists.newArrayList();
		for (Glyph g : glyphs) {
			work.add(g);
			if (g instanceof Space || g instanceof Newline) {
				words.add(work);
				work = Lists.newArrayList();
			}
		}
		if (!work.isEmpty()) {
			words.add(work);
		}
		work = Lists.newArrayList();
		int width = 0;
		for (List<Glyph> word : words) {
			try (RectI size = measureGlyphs(word)) {
//				System.out.println(glyphsToString(word)+" is "+size.getWidth()+"x"+size.getHeight()+". width is "+width+", maxWidth is "+maxWidth+", postwidth would be "+(width+size.getWidth()));
				if (width+size.getWidth() >= maxWidth) {
//					System.out.println(glyphsToString(word)+" overflows");
					if (size.getWidth() > maxWidth) {
						for (Glyph g : word) {
							if (width+g.width+characterSpacing > maxWidth) {
								width = 0;
								work.add(new Newline());
							}
							work.add(g);
							width += g.width+characterSpacing;
						}
						word.clear();
					} else {
						work.add(new Newline());
						width = size.getWidth();
					}
				} else {
					width += size.getWidth();
				}
			}
			work.addAll(word);
			if (!word.isEmpty() && word.get(word.size()-1) instanceof Newline) {
//				System.out.println(glyphsToString(word)+" ends in newline, setting width to 0");
				width = 0;
			}
		}
		int height;
		try (RectI size = measureGlyphs(work)) {
			width = size.getWidth();
			height = size.getHeight();
		}
		return new PreparedString(str.text, work, width, height, str.token);
	}

	private String glyphsToString(Iterable<Glyph> glyphs) {
		return Joiner.on("").join(glyphs);
	}

	private void drawGlyphs(double x, double y, List<Glyph> glyphs, double r, double g, double b, double a) {
		glPushMatrix();
		glTranslated(x, y, 0);
		glPushMatrix();
		try {
			glEnable(GL_DEPTH_TEST);
			glEnable(GL_ALPHA_TEST);
			glClearDepth(0);
			glClear(GL_DEPTH_BUFFER_BIT);
			glDepthFunc(GL_NOTEQUAL);
			glEnable(GL_TEXTURE_2D);
			int maxHeight = defaultCharHeight;
			for (Glyph glyph : glyphs) {
				if (glyph.height < defaultCharHeight) {
					glPushMatrix();
					glTranslatef(0, (defaultCharHeight-glyph.height)/2f, 0);
				}
				maxHeight = Math.max(maxHeight, glyph.height);
				glyph.draw(this, r, g, b, a);
				if (glyph.height < defaultCharHeight) {
					glPopMatrix();
				}
				if (glyph instanceof Newline) {
					glPopMatrix();
					glTranslatef(0, maxHeight+lineSpacing, 0);
					maxHeight = defaultCharHeight;
					glPushMatrix();
				} else {
					glTranslatef(glyph.width+characterSpacing, 0, 0);
				}
			}
			glDisable(GL_TEXTURE_2D);
			glDisable(GL_DEPTH_TEST);
			glDisable(GL_ALPHA_TEST);
		} finally {
			glPopMatrix();
			glPopMatrix();
		}
	}

	private List<Glyph> toGlyphs(String str) {
		String normal = Normalizer.normalize(str, Form.NFC);
		PrimitiveIterator.OfInt iter = normal.codePoints().iterator();
		List<Glyph> out = Lists.newArrayList();
		List<Glyph> work = Lists.newArrayList();
		boolean readingControlCommand = false;
		StringBuilder controlCommand = new StringBuilder();
		char lastRegionalIndicator = '\0';
		boolean overline = false;
		boolean underline = false;
		boolean bold = false;
		while (iter.hasNext()) {
			work.clear();
			int codepoint = iter.nextInt();
			if (codepoint == PUAChars.FONT_RENDERER_CONTROL) {
				readingControlCommand = true;
			} else if (codepoint >= 0xE0020 && codepoint < 0xE007F) {
				// TAGS block
				if (readingControlCommand) {
					controlCommand.append((char)(codepoint-0xE0000));
				}
			} else if (codepoint == 0xE007F) {
				// TAG END
				if (readingControlCommand) {
					readingControlCommand = false;
					if (controlCommand.charAt(0) == 'f') {
						// Flags
						Boolean state = null;
						for (int i = 1; i < controlCommand.length(); i++) {
							char c = controlCommand.charAt(i);
							if (c == '+') {
								if (state == Boolean.TRUE) {
									log.debug("Control command f (Flags) + and - are stateful; you can set multiple flags at once with only one plus\nCommand was: {}", controlCommand);
								}
								state = true;
							} else if (c == '-') {
								if (state == Boolean.FALSE) {
									log.debug("Control command f (Flags) + and - are stateful; you can clear multiple flags at once with only one minus\nCommand was: {}", controlCommand);
								}
								state = false;
							} else if (c == 'b' || c == 'u' || c == 'o') {
								if (state == null) {
									work.addAll(toGlyphs("[Error: Malformed control command; no state (+ or -) specified for "+c+"]"));
								} else {
									switch (c) {
										case 'b':
											bold = state;
											break;
										case 'u':
											underline = state;
											break;
										case 'o':
											overline = state;
											break;
									}
								}
							} else {
								work.addAll(toGlyphs("[Error: Malformed control command; no feature with char "+c+"]"));
							}
						}
					} else {
						work.addAll(toGlyphs("[Error: Unrecognized control command: "+controlCommand+"]"));
					}
					controlCommand.setLength(0);
				}
			} else if (readingControlCommand) {
				readingControlCommand = false;
				work.addAll(toGlyphs("[Error: Unterminated control command]"));
			} else if (codepoint == 0xFE0F && !work.isEmpty()) {
				Glyph last = work.get(work.size()-1);
				String emojified = last.block.name+"_E";
				if (blocks.containsKey(emojified)) {
					last.block = blocks.get(emojified);
				}
			} else if (codepoint == 0xFE0E && !work.isEmpty()) {
				Glyph last = work.get(work.size()-1);
				if (last.block.name.endsWith("_E")) {
					String deemojified = last.block.name.substring(0, last.block.name.length()-2);
					last.block = blocks.get(deemojified);
				}
			} else if (flagBlock != null && codepoint >= 0x1F1E6 && codepoint <= 0x1F1FF) {
				// REGIONAL INDICATOR A-Z
				char alpha = (char)('A'+(codepoint-0x1F1E6));
				if (lastRegionalIndicator != '\0') {
					String country = ""+lastRegionalIndicator+alpha;
					if (supportedFlags.contains(country)) {
						Glyph g = new Glyph("FLAG", flagBlock, (alpha-'A')*flagWidth, (lastRegionalIndicator-'A')*flagHeight, flagWidth, flagHeight);
						g.colored = true;
						work.add(g);
					} else {
						addBasicCodepoint(0x1F1E6+(lastRegionalIndicator-'A'), work, false);
						addBasicCodepoint(0x1F1E6+(alpha-'A'), work, false);
					}
					lastRegionalIndicator = '\0';
				} else {
					lastRegionalIndicator = alpha;
				}
			} else if (codepoint == ' ') {
				work.add(new Space(spaceWidth, defaultCharHeight));
			} else if (codepoint == '\t') {
				work.add(new Space(spaceWidth*4, defaultCharHeight));
			} else if (codepoint == '\n') {
				work.add(new Newline());
			} else {
				addBasicCodepoint(codepoint, work, false);
			}
			for (Glyph g : work) {
				g.underline = underline;
				g.overline = overline;
				g.bold = bold;
			}
			out.addAll(work);
		}
		return out;
	}

	private void addBasicCodepoint(int codepoint, List<Glyph> li, boolean replacement) {
		String block = Integer.toHexString(codepoint >> 8);
		if (block.length() == 1) block = "0".concat(block);
		if (!badBlocks.contains(block) && blocks.containsKey(block)) {
			int chr = codepoint & 0xFF;
			int u = chr&0x0F;
			int v = (chr >> 4)&0x0F;
			StandardBlock b = blocks.get(block);
			if (!b.loaded) {
				if (b.loading) {
					addBasicCodepoint('\uFFFD', li, true);
					return;
				}
				b.loading = true;
				final String fblock = block;
				Runnable loader = () -> {
					try {
						b.load(this);
						token++;
					} catch (Throwable t) {
						log.error("Couldn't lazy-load block {} for font {}", fblock, name);
						badBlocks.add(fblock);
					} finally {
						b.loading = false;
					}
				};
				if (replacement) {
					loader.run();
					if (badBlocks.contains(block)) {
						if (codepoint == '?') {
							throw new RuntimeException("Failed to lazy-load block for replacement character");
						} else {
							addBasicCodepoint('?', li, true);
							return;
						}
					}
				} else {
					SharedThreadPool.submit(loader);
					addBasicCodepoint('\uFFFD', li, true);
					return;
				}
			}
			if (!b.uploaded) {
				b.upload(this);
			}
			int idx = (v*16)+u;
			if (b.supportedGlyphs.get(idx)) {
				Glyph g = new Glyph(new String(Character.toChars(codepoint)), blocks.get(block), u*defaultCharWidth, v*defaultCharHeight, b.widths == null ? defaultCharWidth : b.widths.get(idx), defaultCharHeight);
				g.replacement = replacement;
				if (b.colorGlyphs.get(idx)) {
					g.colored = true;
				}
				li.add(g);
			} else if (!replacement) {
				addBasicCodepoint('\uFFFD', li, true);
			}
		} else if (!replacement) {
			addBasicCodepoint('\uFFFD', li, true);
		} else {
			addBasicCodepoint('?', li, true);
		}
	}

	@Override
	protected void _free() {
		for (Block b : blocks.values()) {
			glDeleteTextures(b.textureId);
		}
		if (flagBlock != null) {
			glDeleteTextures(flagBlock.textureId);
		}
	}

}

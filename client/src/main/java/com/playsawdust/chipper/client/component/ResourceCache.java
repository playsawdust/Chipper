/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.Font;
import com.playsawdust.chipper.client.al.ALBuffer;
import com.playsawdust.chipper.client.audio.OggOpusDecoder;
import com.playsawdust.chipper.client.gl.GLCompileException;
import com.playsawdust.chipper.client.gl.GLShader;
import com.playsawdust.chipper.client.gl.GLTexture2D;
import com.playsawdust.chipper.client.gl.PixelFormat;
import com.playsawdust.chipper.client.gl.GLShader.ShaderType;
import com.playsawdust.chipper.component.Component;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.component.Context.WhiteLotus;
import com.playsawdust.chipper.exception.ResourceNotFoundException;
import com.playsawdust.chipper.img.BufferedImage;
import com.playsawdust.chipper.img.LWImage;
import com.playsawdust.chipper.toolbox.io.Slice;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;

/**
 * Entry point to obtaining resources from any registered sources, and caching
 * them for easy use.
 */
public final class ResourceCache implements Component {
	private static final Logger log = LoggerFactory.getLogger(ResourceCache.class);

	private static final Jankson jkson = Jankson.builder().build();

	private final ConcurrentMap<Identifier, Slice> resourceBytes = Maps.newConcurrentMap();
	private final ConcurrentMap<Identifier, String> resourceStrings = Maps.newConcurrentMap();
	private final ConcurrentMap<Identifier, JsonObject> janksonObjects = Maps.newConcurrentMap();

	private final Map<Identifier, GLTexture2D> textures = Maps.newHashMap();
	private final Map<Identifier, GLShader> shaders = Maps.newHashMap();
	private final Map<Identifier, ALBuffer> clips = Maps.newHashMap();
	private final Map<Identifier, Font> fonts = Maps.newHashMap();

	private ResourceCache(WhiteLotus lotus) {
		WhiteLotus.verify(lotus);
	}

	/**
	 * Clears the resource cache. Additionally, all textures and clips will be
	 * freed and will become immediately invalid.
	 */
	public void clear() {
		log.debug("Dropping all caches");
		resourceBytes.clear();
		resourceStrings.clear();
		janksonObjects.clear();

		freeAll(textures);
		freeAll(shaders);
		freeAll(clips);
		freeAll(fonts);
	}

	private void freeAll(Map<?, ? extends NativeResource> map) {
		freeAll(map.values());
	}

	private void freeAll(Collection<? extends NativeResource> c) {
		c.forEach(NativeResource::free);
		c.clear();
	}

	/**
	 * Open an input stream for the resource with the given identifier. <b>Does
	 * not cache</b>. Meant as plumbing to implement systems that <i>do</i>
	 * cache.
	 * @param id the identifier of the resource to be opened
	 * @return an InputStream of the contents of the resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier
	 */
	public InputStream openResource(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		// TODO currently just delegates to the ClassLoader
		// there needs to be some kind of resource provider system in the future
		InputStream stream = ClassLoader.getSystemResourceAsStream("res/"+id.namespace+"/"+id.path);
		if (stream == null) throw new ResourceNotFoundException("No such resource", id);
		return stream;
	}

	/**
	 * Returns a {@link ByteSource} wrapper for {@link #openResource}.
	 * @param id the identifier of the resource to be opened
	 * @return a ByteSource that will open the given resource
	 */
	public ByteSource asByteSource(Identifier id) {
		return new ByteSource() {
			@Override
			public InputStream openStream() throws IOException {
				return openResource(id);
			}
		};
	}

	/**
	 * Slurp the resource with the given identifier entirely into memory and
	 * return it. <b>Does not cache</b>. Meant as plumbing to implement systems
	 * that <i>do</i> cache. Note: As an optimization, this method shares the
	 * cache used by {@link #getResourceBytes}. As such, this method cannot be
	 * used as a way to bypass the cache. If the cache is stale, {@link #clear}
	 * must be called.
	 * @param id the identifier of the resource to be slurped
	 * @return the bytes contained in the resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or loading it fails
	 */
	public byte[] slurpResource(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		try (InputStream in = openResource(id)) {
			// if somebody else already called getResourceBytes for this resource,
			// no need to waste our time reading it again
			Slice cached = resourceBytes.get(id);
			if (cached != null) return cached.toByteArray();
			byte[] bys = ByteStreams.toByteArray(in);
			return bys;
		} catch (IOException e) {
			throw new ResourceNotFoundException("Failed to load resource", id, e);
		}
	}

	/**
	 * Slurp the resource with the given identifier entirely into memory and
	 * return it decoded as a UTF-8 string. <b>Does not cache</b>. Meant as
	 * plumbing to implement systems that <i>do</i> cache. Note: As an
	 * optimization, this method shares the cache used by {@link #getResourceText}.
	 * As such, this method cannot be used as a way to bypass the cache. If the
	 * cache is stale, {@link #clear} must be called.
	 * @param id the identifier of the resource to be slurped
	 * @return the string read from the resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or loading it fails
	 */
	public String slurpResourceText(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		String cached = resourceStrings.get(id);
		if (cached != null) return cached;
		return new String(slurpResource(id), Charsets.UTF_8);
	}

	/**
	 * Retrieve the byte string consisting of the contents of the resource with
	 * the given identifier. Cached.
	 * @param id the identifier of the resource to be retrieved
	 * @return the byte string consisting of the contents of the resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or loading it fails
	 */
	public Slice getResourceBytes(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		// containsKey would introduce a race
		Slice cached = resourceBytes.get(id);
		if (cached != null) return cached;
		byte[] bys = slurpResource(id);
		Slice str = new Slice(bys);
		resourceBytes.putIfAbsent(id, str);
		// technically, Slices aren't immutable, as they have a method that
		// takes untrusted OutputStreams and passes the underlying byte[] into them
		////
		// methods in this class generally protect from accidental cache poisoning,
		// not intentional or malicious cache poisoning. those working with security
		// critical data should never rely on caches.
		return str;
	}

	public String getResourceText(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		// containsKey would introduce a race
		String cached = resourceStrings.get(id);
		if (cached != null) return cached;
		String str = slurpResourceText(id);
		resourceStrings.putIfAbsent(id, str);
		return str;
	}

	/**
	 * Retrieve and decode the given resource as an image. <b>Does not cache</b>.
	 * Supported formats, in order of preference:
	 * <ul>
	 * <li>ORA</li>
	 * <li>PNG</li>
	 * <li>GIF</li>
	 * <li>BMP</li>
	 * <li>PSD</li>
	 * </ul>
	 * Formats that will load, but are discouraged, in no particular order:
	 * <ul>
	 * <li>JPEG</li>
	 * <li>PIC</li>
	 * <li>TGA</li>
	 * <li>HDR</li>
	 * <li>PNM</li>
	 * </ul>
	 * @param id the identifier of the resource to be decoded
	 * @return a BufferedImage containing the pixels decoded from the given
	 * 		resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or loading it fails
	 */
	public BufferedImage loadImage(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		byte[] bys = null;
		try (PushbackInputStream is = new PushbackInputStream(openResource(id), 4)) {
			byte[] hdr = new byte[4];
			ByteStreams.readFully(is, hdr);
			if (ZIP_HEADER.equals(hdr)) {
				// it's a zip file
				is.unread(hdr);
				ZipInputStream zis = new ZipInputStream(is);
				ZipEntry first = zis.getNextEntry();
				// the first entry in an ORA file must be an uncompressed file named mimetype...
				if ("mimetype".equals(first.getName())) {
					// ...with the sole content "image/openraster"
					String mime = new String(ByteStreams.toByteArray(zis), Charsets.UTF_8);
					if ("image/openraster".equals(mime)) {
						// ok, we've got an openraster file. look for the mergedimage.png
						while (true) {
							ZipEntry ent = zis.getNextEntry();
							if (ent == null) {
								// reached EOF and no mergedimage
								throw new ResourceNotFoundException("Cannot load ORA file without mergedimage.png in resource ", id);
							}
							if (ent.getName().equals("mergedimage.png")) {
								bys = ByteStreams.toByteArray(zis);
								break;
							}
						}
					}
				}
			}
		} catch (IOException e) {
			// guess it's not an OpenRaster
		}
		if (bys == null) {
			bys = slurpResource(id);
		}
		if (bys.length==0) throw new ResourceNotFoundException("Resource was zero-length!", id);
		ByteBuffer buffer = MemoryUtil.memAlloc(bys.length);
		buffer.put(bys);
		buffer.flip();
		try {
			// buffer freeing is handled by BufferedImage
			return new BufferedImage(buffer);
		} catch (IOException e) {
			MemoryUtil.memFree(buffer);
			throw new ResourceNotFoundException("Failed to load resource", id, e);
		}
	}

	private static final Slice ZIP_HEADER = Slice.fromHex("504B0304");

	/**
	 * Retrieve and decode the given resource as an image. <b>Does not cache</b>. The resulting
	 * image holds no offheap memory.
	 * @param id the identifier of the resource to be decoded
	 * @return a LWImage containing the pixels decoded from the given resource
	 * @throws ResourceNotFoundException if there is no resource with this identifier,
	 *         or if loading it fails.
	 */
	public LWImage loadLWImage(Identifier id) throws ResourceNotFoundException {
		try (BufferedImage result = loadImage(id)) {
			return new LWImage(result);
		} catch (IOException e) {
			throw new ResourceNotFoundException("Failed to load resource", id, e);
		}
	}

	/**
	 * Retrieve and decode the given resource as a <a href="https://github.com/falkreon/Jankson">Jankson</a>
	 * file (.jkson), and return the decoded object. <b>Does not cache</b>.
	 * @param id the identifier of the resource to be decoded
	 * @return a JsonObject containing the data decoded from the given resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or loading it fails
	 */
	public JsonObject loadJanksonObject(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		JsonObject cached = janksonObjects.get(id);
		if (cached != null) return cached.clone();
		String text = slurpResourceText(id);
		// temporary hack until Jankson gets support for braceless files
		if (!text.startsWith("{")) text = "{"+text+"}";
		try {
			return jkson.load(text);
		} catch (SyntaxError e) {
			throw new ResourceNotFoundException("Jankson syntax error: "+e.getCompleteMessage()+" in resource", id);
		}
	}

	/**
	 * Retrieve and decode the given resource as a <a href="https://github.com/falkreon/Jankson">Jankson</a>
	 * file (.jkson), and return the decoded object. Caches.
	 * @param id the identifier of the resource to be decoded
	 * @return a JsonObject containing the data decoded from the given resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or loading it fails
	 */
	public JsonObject getJanksonObject(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		JsonObject cached = janksonObjects.get(id);
		if (cached != null) return cached.clone();
		JsonObject obj = loadJanksonObject(id);
		janksonObjects.putIfAbsent(id, obj);
		// always clone output objects to prevent accidental cache poisoning
		return obj.clone();
	}

	/**
	 * Retrieve and decode the given resource as an image, and then upload it
	 * to the GPU and return a handle to the created texture. Caches, unless the
	 * GLTexture2D object is {@link GLTexture2D#free freed}.
	 * @param id the identifier of the resource to be decoded
	 * @return a GLTexture2D containing the pixels decoded from the given resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or loading it fails
	 * @see #loadImage
	 */
	@UIEffect
	public GLTexture2D getTexture(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		GLTexture2D cached = textures.get(id);
		if (cached != null && !cached.isFreed()) return cached;
		GLTexture2D tex = GLTexture2D.allocate();
		try (BufferedImage img = loadImage(id)) {
			tex.upload(PixelFormat.RGBA, img);
		}
		textures.put(id, tex);
		return tex;
	}

	/**
	 * Retrieve and compile the given resource as a GLSL shader of the given
	 * type, and return a handle to the created  shader. Caches, unless the
	 * GLShader object is {@link GLShader#free freed}.
	 * @param id the identifier of the resource to be compiled
	 * @return a GLShader containing the compiled shader
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or compiling it fails
	 */
	@UIEffect
	public GLShader getShader(ShaderType type, Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		GLShader cached = shaders.get(id);
		if (cached != null && !cached.isFreed()) return cached;
		String src = slurpResourceText(id);
		GLShader shader = GLShader.allocate(type);
		try {
			shader.compile(src);
		} catch (GLCompileException e) {
			throw new ResourceNotFoundException("Failed to compile shader", id, e);
		}
		shaders.put(id, shader);
		return shader;
	}

	/**
	 * Retrieve and decode the given resource as an Ogg Opus file, and then send
	 * it to OpenAL and return a handle to the created buffer. Caches, unless
	 * the ALBuffer object is {@link ALBuffer#free freed}.
	 * @param id the identifier of the resource to be decoded
	 * @return an ALBuffer containing the samples decoded from the given resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or loading it fails
	 */
	@UIEffect
	public ALBuffer getClip(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		ALBuffer cached = clips.get(id);
		if (cached != null && !cached.isFreed()) return cached;
		try (InputStream is = openResource(id); OggOpusDecoder dec = new OggOpusDecoder(is)) {
			if (dec.getChannels() != 1) {
				log.warn("Loaded a stereo resource with path {} in namespace {} as a clip - clips should be mono", id.path, id.namespace);
			}
			ShortBuffer samples = dec.decodeAll();
			ALBuffer buf = ALBuffer.allocate();
			buf.upload(48000, dec.getChannels(), samples);
			clips.put(id, buf);
			return buf;
		} catch (IOException e) {
			throw new ResourceNotFoundException("Failed to load resource", id, e);
		}
	}

	/**
	 * Retrieve and decode a font in the given directory, looking for its font.jkson
	 * for more information on how to load it. Caches.
	 * <p>
	 * Documentation on fonts is TODO.
	 * @param id the identifier of the resource to be decoded
	 * @return an Font containing the glyphs decoded from the given resource
	 * @throws ResourceNotFoundException if there is no resource with this
	 * 		identifier, or loading it fails
	 */
	@UIEffect
	public Font getFont(Identifier id) throws ResourceNotFoundException {
		Preconditions.checkArgument(id != null, "id cannot be null");
		Font cached = fonts.get(id);
		if (cached != null && !cached.isFreed()) return cached;
		JsonObject meta = loadJanksonObject(id.child("font.jkson"));
		Font font = new Font(this, meta, id);
		fonts.put(id, font);
		return font;
	}


	public static ResourceCache obtain(Context<? extends ClientEngine> ctx) {
		return ctx.getComponent(ResourceCache.class);
	}

	@Override
	public boolean compatibleWith(Engine engine) {
		return engine instanceof ClientEngine;
	}

}

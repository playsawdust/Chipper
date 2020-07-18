/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.lwjgl.system.MemoryUtil;

import com.google.common.base.Preconditions;
import com.playsawdust.chipper.AbstractNativeResource;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.img.BufferedImage;
import com.playsawdust.chipper.img.Image;
import com.playsawdust.chipper.img.LWImage;

public class MaterialAtlas extends AbstractNativeResource {
	private ArrayList<LWImage> diffuse = new ArrayList<>();
	private ArrayList<LWImage> specEmissive = new ArrayList<>();
	private ArrayList<LWImage> normal = new ArrayList<>();

	private LinkedHashMap<Identifier, MaterialEntry> entries = new LinkedHashMap<>();
	private boolean compiled = false;
	private GLTexture2D texture = null;
	private ShaderBuffer materialsBuffer = null;
	private ShaderBuffer framesBuffer = null;
	private int tileWidth = -1;
	private int tileHeight = -1;
	private int tilesWide = -1;

	private int[] constantSpec = new int[0]; //indices relative to normal start
	private int constantNormal = 0;


	public MaterialAtlas(int tileWidth, int tileHeight) {
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}

	/** Adds no-emissive specularity textures to approximate constant specularity values.
	 * @param count how many specEmissive textures to generate, from 1 to 256
	 */
	public void addConstantSpec(int count) {
		Preconditions.checkArgument(count>0);
		constantSpec = new int[count];

		for(int i=0; i<count; i++) {
			int specValue = (int)(i * (255.0/count));
			specValue &= 0xFF; //just in case
			LWImage im = new LWImage(tileWidth, tileHeight);

			int col = 0xFF000000 | (specValue<<16) | (specValue<<8) | specValue;

			im.fill(col);
			specEmissive.add(im);
			constantSpec[i] = specEmissive.size()-1;
		}
	}

	public void addMaterial(Identifier id, Image diffuse) {
		addMaterial(id, diffuse, 0.1);
	}

	/** Add a flat material with no emissivity and a constant specularity value
	 * @param diffuse An image representing the diffuse colors of the material to add
	 * @param spec    A value between 0 and 1 inclusive which describes how strong the material's specularity should be. May be rounded off.
	 */
	public void addMaterial(Identifier id, Image diffuse, double spec) {
		LWImage preScaling = (diffuse instanceof LWImage) ? (LWImage)diffuse : new LWImage(diffuse);
		LWImage scaled = (preScaling.getWidth()==tileWidth && preScaling.getHeight()==tileHeight) ? preScaling : preScaling.getScaled(tileWidth, tileHeight);

		this.diffuse.add(scaled);
		int diffuseIndex = this.diffuse.size()-1;

		MaterialEntry entry = new MaterialEntry();
		entry.diffuse.add(diffuseIndex);
		entry.constantSpec = spec;
		entry.id = id;
		entries.put(id, entry);
	}

	public void bind(GLProgram program) {
		if (!compiled) {
			compile(program);
		}

		texture.bind();

		if (materialsBuffer!=null) program.bindShaderBuffer(materialsBuffer, "materials");
		program.getUniform("atlas_size").setInt(tileWidth, tileHeight, tilesWide, tilesWide);
	}

	/** Make sure your program is bound first! */
	public void compile(GLProgram program) {
		compiled = true;

		if (specEmissive.size()==0) {
			LWImage noSpec = new LWImage(tileWidth, tileHeight);
			noSpec.fill(0xFF_000000);
			specEmissive.add(noSpec);
			constantSpec = new int[1];
			constantSpec[0] = 0;
		}

		LWImage flatNormal = new LWImage(tileWidth, tileHeight);
		flatNormal.fill(0xFF_00FF00);
		normal.add(0, flatNormal);
		constantNormal = 0;

		int atlasSize = diffuse.size()+specEmissive.size()+normal.size();
		tilesWide = (int)Math.ceil(Math.sqrt(atlasSize));
		System.out.println("Stitching "+tilesWide+"x"+tilesWide+" material atlas.");
		LWImage stitched = new LWImage(tilesWide*tileWidth, tilesWide*tileHeight);

		int index = 0;

		int diffuseStart = index;
		for(int i=0; i<diffuse.size(); i++) {
			int tileX = (index % tilesWide) * tileWidth;
			int tileY = (index / tilesWide) * tileHeight;

			stitched.paintImage(diffuse.get(i), tileX, tileY);

			index++;
		}

		int specStart = index;
		for(int i=0; i<specEmissive.size(); i++) {
			int tileX = (index % tilesWide) * tileWidth;
			int tileY = (index / tilesWide) * tileHeight;

			stitched.paintImage(specEmissive.get(i), tileX, tileY);

			index++;
		}

		int normalStart = index;
		for(int i=0; i<normal.size(); i++) {
			int tileX = (index % tilesWide) * tileWidth;
			int tileY = (index / tilesWide) * tileHeight;

			stitched.paintImage(normal.get(i), tileX, tileY);

			index++;
		}


		if (texture==null) {
			texture = GLTexture2D.allocate();
		}

		try (BufferedImage buffered = stitched.asBufferedImage()) {
			texture.upload(PixelFormat.RGBA, buffered);
		}

		if (materialsBuffer==null) materialsBuffer = ShaderBuffer.allocate(ShaderBuffer.Type.UNIFORM_BUFFER);
		{
			ByteBuffer buf = MemoryUtil.memAlloc(entries.size()*Integer.BYTES*4);
			for(MaterialEntry entry : entries.values()) {
				/*
				 * `ofs` is actually meant to be an offset into `frames`, the other uniform buffer, which holds the diffuse, normal, specEmissive indices for each frame of the block's material.
				 * For now, we're just pointing to the atlas index of the diffuse texture so we can use one uniform buffer.
				 */
				buf.putInt(diffuseStart + entry.diffuse.get(0)); //ofs
				buf.putInt(1); //count
				buf.putInt(0); //delay
				buf.putInt(0); //flags
			}
			materialsBuffer.upload(buf, GLVertexBuffer.HintFrequency.STATIC, GLVertexBuffer.HintNature.DRAW);
			MemoryUtil.memFree(buf);
		}

	}

	public int getIndex(Identifier id) {
		MaterialEntry entry = entries.get(id);
		if (entry==null) return 0;
		return entry.diffuse.get(0); //TODO: When the material indirection layer is up, return the material index instead of the diffuse index
	}

	private static class MaterialEntry {
		public Identifier id;

		public double constantSpec = 0.1;

		public ArrayList<Integer> diffuse = new ArrayList<>();      //indices relative to diffuse start
		public ArrayList<Integer> specEmissive = new ArrayList<>(); //indices relative to specEmissive start
		public ArrayList<Integer> normal = new ArrayList<>();       //indices relative to normal start
	}

	@Override
	protected void _free() {
		if (texture!=null) {
			texture.free();
			texture = null;
		}
		if (materialsBuffer!=null) {
			materialsBuffer.free();
			materialsBuffer = null;
		}
	}
}

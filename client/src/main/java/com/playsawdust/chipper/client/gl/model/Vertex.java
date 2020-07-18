/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl.model;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.playsawdust.chipper.math.ProtoColor;

import com.playsawdust.chipper.client.gl.builder.VertexBuilder;

public class Vertex {
	private Vector3d pos;
	private Vector3d norm;
	private Vector3d rawNorm;
	private Vector3d color;
	private Vector3d uv;
	
	public Vertex(Vector3d position, Vector3d tex, Vector3d color, Vector3d normal) {
		this.pos = position;
		this.uv = tex;
		this.color = color;
		this.norm = normal;
		this.rawNorm = normal;
	}
	
	public Vector3dc position() {
		return pos;
	}
	
	public Vector3dc normal() {
		return norm;
	}
	
	protected Vector3dc rawNormal() {
		return rawNorm;
	}
	
	public ProtoColor color() {
		return ProtoColor.fromRGB(color.x, color.y, color.z);
	}
	
	public Vector3dc colorVector() {
		return color;
	}
	
	public Vector3dc uv() {
		return uv;
	}
	
	public void get(VertexBuilder.Next buf) {
		buf.next()
			.vertex(pos)
			.tex(uv.x, uv.y, uv.z)
			.color(color())
			.normal(norm)
			;
	}

	public void setNormal(Vector3d normal) {
		norm = normal;
		rawNorm = normal;
	}
	
	public void setPosition(Vector3d position) {
		pos = position;
	}
	
	public void setTexCoord(Vector3d texCoord) {
		this.uv = texCoord;
	}
	
	@Override
	public String toString() {
		return this.pos.toString();
	}
}

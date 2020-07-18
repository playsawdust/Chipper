/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl.model;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.playsawdust.chipper.client.gl.builder.VertexBuilder;

public class Face {
	private Vertex a;
	private Vertex b;
	private Vertex c;
	
	public Face(Vertex a, Vertex b, Vertex c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	@Nullable
	public Vertex getVertex(Vector3dc vec) {
		if (a.position().equals(vec)) return a;
		if (b.position().equals(vec)) return b;
		if (c.position().equals(vec)) return c;
		return null;
	}
	
	public Vertex a() { return a; }
	public Vertex b() { return b; }
	public Vertex c() { return c; }
	
	/**
	 * Calculates a normal for this face
	 * @return a mutable vector representing a calculated normal for this face
	 */
	public Vector3d getNormal() {
		Vector3d u = new Vector3d(b.position()).sub(a.position()).normalize();
		Vector3d v = new Vector3d(c.position()).sub(a.position()).normalize();
		return new Vector3d(u).cross(v).normalize();
	}
	
	public void genFaceNormal() {
		Vector3d normal = getNormal();
		//System.out.println("Generated new normal: "+normal);
		a.setNormal(normal);
		b.setNormal(normal);
		c.setNormal(normal);
	}
	
	public void get(VertexBuilder.Next buf) {
		a.get(buf);
		b.get(buf);
		c.get(buf);
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}
}

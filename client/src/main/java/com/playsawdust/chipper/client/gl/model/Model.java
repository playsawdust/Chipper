/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.google.common.collect.ImmutableSet;
import com.playsawdust.chipper.client.gl.builder.VertexBuilder;

public class Model {
	private ArrayList<Face> faces = new ArrayList<>();
	private ArrayList<Vertex> vertices = new ArrayList<>();
	private HashMap<Vector3dc, HashSet<Face>> adjacentFaces = new HashMap<>();
	
	private double minX = 0;
	private double minY = 0;
	private double minZ = 0;
	private double maxX = 0;
	private double maxY = 0;
	private double maxZ = 0;
	
	public Model(Collection<Face> faces) {
		for(Face face: faces) {
			this.faces.add(face);
			
			this.vertices.add(face.a());
			this.vertices.add(face.b());
			this.vertices.add(face.c());
			
			addAdjacentFace(face.a(), face);
			addAdjacentFace(face.b(), face);
			addAdjacentFace(face.c(), face);
			
			this.minX = min(
					minX,
					face.a().position().x(),
					face.b().position().x(),
					face.c().position().x()
				);
			
			this.minY = min(
					minY,
					face.a().position().y(),
					face.b().position().y(),
					face.c().position().y()
				);
			
			this.minZ =min(
					minZ,
					face.a().position().z(),
					face.b().position().z(),
					face.c().position().z()
				);
			
			this.maxX = max(
					maxX,
					face.a().position().x(),
					face.b().position().x(),
					face.c().position().x()
				);
			
			this.maxY = max(
					maxY,
					face.a().position().y(),
					face.b().position().y(),
					face.c().position().y()
				);
			
			this.maxZ = max(
					maxZ,
					face.a().position().z(),
					face.b().position().z(),
					face.c().position().z()
				);
		}
		
		//System.out.println("Added "+this.faces.size()+" faces");
	}
	
	
	public void get(VertexBuilder.Next buf) {
		for(Face face : faces) {
			face.get(buf);
		}
	}
	
	public void smoothNormals() {
		for(Map.Entry<Vector3dc, HashSet<Face>> entry : adjacentFaces.entrySet()) {
			Vector3d avg = new Vector3d(0,0,0);
			for(Face face : entry.getValue()) {
				Vertex cur = face.getVertex(entry.getKey());
				if (cur!=null) {
					avg.add(cur.normal());
				}
			}
			avg.normalize();
			for(Face face : entry.getValue()) {
				Vertex cur = face.getVertex(entry.getKey());
				if (cur!=null) cur.setNormal(avg);
			}
		}
	}
	
	/** This doesn't work yet - I think orthogonalize doesn't do what I think it does, so for now the radius is disabled and it does a planar projection instead of a cylindrical one. */
	public void cylinderUnwrap() {
		Vector3dc up = new Vector3d(0,1,0);
		for(Vertex v : vertices) {
			Vector3d tmp = new Vector3d(0,0,0);
			v.position().orthogonalize(up, tmp);
			double x = tmp.x();
			double z = tmp.z();
			double radians = Math.atan2(z, x) + Math.PI;
			double tu = v.position().x();//radians / (Math.PI*2.0);
			double tv = v.position().y() + (-minY);
			double extent = Math.abs(maxY-minY);
			if (extent!=0) tv/=extent;
			v.setTexCoord(new Vector3d(tu, tv, 0.0));
		}
	}
	
	public Collection<Face> getAdjacentFaces(Vector3dc vec) {
		HashSet<Face> set = adjacentFaces.get(vec);
		if (set==null) return ImmutableSet.of();
		return ImmutableSet.copyOf(set);
	}
	
	private void addAdjacentFace(Vertex v, Face f) {
		if (!adjacentFaces.containsKey(v.position())) adjacentFaces.put(v.position(), new HashSet<>());
		HashSet<Face> faces = adjacentFaces.get(v.position());
		faces.add(f);
	}
	
	private static double max(double a, double b, double c, double d) {
		return
			Math.max(
				Math.max(a,b),
				Math.max(c,d)
			);
	}
	
	private static double min(double a, double b, double c, double d) {
		return
			Math.min(
				Math.min(a,b),
				Math.min(c,d)
			);
	}
}

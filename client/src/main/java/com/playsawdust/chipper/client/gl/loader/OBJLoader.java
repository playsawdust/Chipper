/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.joml.Vector3d;

import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.component.ResourceCache;
import com.playsawdust.chipper.client.gl.GLVertexBuffer;
import com.playsawdust.chipper.client.gl.builder.VertexBuilder;
import com.playsawdust.chipper.client.gl.model.Face;
import com.playsawdust.chipper.client.gl.model.Model;
import com.playsawdust.chipper.client.gl.model.Vertex;
import com.playsawdust.chipper.component.Context;

public class OBJLoader {
	public static Model load(Context<ClientEngine> ctx, Identifier id) throws IOException {
		String file = ResourceCache.obtain(ctx).slurpResourceText(id);
		
		ArrayList<Vector3d> positions = new ArrayList<>();
		ArrayList<Vector3d> textures = new ArrayList<>();
		ArrayList<Vector3d> normals = new ArrayList<>();
		ArrayList<IndexedFace> indexedFaces = new ArrayList<>();
		ArrayList<Face> faces = new ArrayList<>();
		//HashMap<Vector3d, HashSet<Face>> adjacentFaces = new HashMap<>();
		
		for(String line : file.split("\\n")) { //TODO: Lazy split into a Stream<String> would handle huge models much better
			if (line.startsWith("#")) continue;
			if (line.trim().isEmpty()) continue;
			
			String[] parts = line.split(" ");
			if (parts[0].equals("v")) {
				if (parts.length>=4) {
					Vector3d vertexPos = new Vector3d(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
					positions.add(vertexPos);
				}
			} else if (parts[0].equals("vn")) {
				if (parts.length>=4) {
					Vector3d vertexNormal = new Vector3d(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
					normals.add(vertexNormal);
				}
			} else if (parts[0].equals("vt")) {
				if (parts.length==4) {
					Vector3d vertexTexcoord = new Vector3d(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
					normals.add(vertexTexcoord);
				} else if (parts.length==3) {
					Vector3d vertexTexcoord = new Vector3d(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), 0);
					textures.add(vertexTexcoord);
				}
			} else if (parts[0].equals("f")) {
				IndexedFace.parse(line, indexedFaces);
			}
			
		}
		
		//System.out.println("OBJLoader de-indexing and uploading "+indexedFaces.size()+" faces, "+positions.size()+" positions, "+normals.size()+" normals, and "+textures.size()+" textureUVs...");
		
		GLVertexBuffer result = GLVertexBuffer.allocate();
		result.setUsageHints(GLVertexBuffer.HintFrequency.STATIC, GLVertexBuffer.HintNature.DRAW);
		VertexBuilder.Next buf = result.builder().triangles().smooth();
		for(IndexedFace face : indexedFaces) {
			if (face.a==null || face.b==null || face.c==null) {
				System.out.println("INVALID FACE");
				continue;
			}
			//System.out.println("Deindexing ("+face.a.v+"|"+face.a.vt+"|"+face.a.vn+"), ("+face.b.v+"|"+face.b.vt+"|"+face.b.vn+"), "+face.c.v+"|"+face.c.vt+"|"+face.c.vn+")");
			Face deIndexed = deref(face, positions, textures, normals);
			faces.add(deIndexed);
		}
		
		//Generate data for vertexes which don't have it
		for(Face face : faces) {
			if (face.a().position().equals(face.b().position())) System.out.println("A-B Degenerate");
			if (face.b().position().equals(face.c().position())) System.out.println("B-C Degenerate");
			if (face.a().position().equals(face.c().position())) System.out.println("A-C Degenerate");
			
			
			//Normals from face windings...
			if (face.a().normal().length()==0 || face.b().normal().length()==0 || face.c().normal().length()==0) {
				face.genFaceNormal();
			}
			
			//Cylindrical projection for missing UVs...
			//if (face.a.tex==ZERO || face.b.tex==ZERO || face.c.tex==ZERO) {
				
			//}
		}
		
		//Smooth normals
		//for(Face face : faces) {
		//	smoothNormal(face.a, adjacentFaces);
		//	smoothNormal(face.b, adjacentFaces);
		//	smoothNormal(face.c, adjacentFaces);
		//}
		
		//Emit them
		//for(Face face : faces) {
		//	emit(buf.next(), face.a);
		//	emit(buf.next(), face.b);
		//	emit(buf.next(), face.c);
		//}
		//((NextOrEnd) buf).end();
		return new Model(faces);
		
		//return result;
	}
	
	private static final Vector3d ZERO = new Vector3d(0,0,0);
	private static Vector3d deref(int pos, ArrayList<Vector3d> list) {
		if (pos<0 || pos>=list.size()) return ZERO;
		return list.get(pos);
	}
	
	//private static void emit(VertexBuilder.Vertex buf, Vertex v) {
	//	buf
	//		.vertex(v.pos.x, v.pos.y, v.pos.z)
	//		.tex(v.tex.x, v.tex.y)
	//		.color(1, 1, 1)
	//		.normal(v.norm.x, v.norm.y, v.norm.z);
		//if (v.tex==ZERO) System.out.println("ZERO TEX");
		/*
		VertexBuilder.TexOrColorOrNormalOrNextOrEnd tmp = buf.vertex(v.pos.x, v.pos.y, v.pos.z);
		if (v.tex!=ZERO) tmp.tex(v.tex.x, v.tex.y);
		tmp.color(1, 1, 1);
		//if (v.norm!=ZERO)
			tmp.normal(v.norm.x, v.norm.y, v.norm.z);
		*/
		//System.out.println("    "+v.pos+"    "+v.norm+"    "+v.tex);
	//}
	
	//private static void adjacentFace(Face f, Map<Vector3f, HashSet<Face>> map) {
	//	adjacentFace(f.a.pos, f, map);
	//	adjacentFace(f.b.pos, f, map);
	//	adjacentFace(f.c.pos, f, map);
	//}
	
	//private static void adjacentFace(Vector3f vec, Face f, Map<Vector3f, HashSet<Face>> map) {
	//	HashSet<Face> faceSet = map.get(vec);
	//	if (faceSet==null) {
	//		faceSet = new HashSet<Face>();
	//		map.put(vec, faceSet);
	//	}
	//	
	//	faceSet.add(f);
	//}
	
	/*
	private static void smoothNormal(Vertex vertex, Map<Vector3f, HashSet<Face>> adjacentFaces) {
		HashSet<Face> adjacent = adjacentFaces.get(vertex.pos);
		if (adjacent==null || adjacent.size()<2) return;
		Vector3f vec = new Vector3f(0,0,0);
		for(Face f: adjacent) {
			Vertex related = f.getVertex(vertex.pos);
			vec.add(related.rawNorm);
		}
		vec.normalize();
		vertex.norm = vec;
	}*/
	
	private static class IndexedVertex {
		public int v  = -1; //pos
		public int vt = -1; //tex
		public int vn = -1; //norm
		
		public static IndexedVertex of(String def) {
			IndexedVertex result = new IndexedVertex();
			String[] parts = def.split("/");
			if (!parts[0].trim().isEmpty()) {
				result.v = Integer.parseUnsignedInt(parts[0].trim());
			}
			if (parts.length>1 && !parts[1].trim().isEmpty()) {
				result.vt = Integer.parseUnsignedInt(parts[1].trim());
			}
			if (parts.length>2 && !parts[2].trim().isEmpty()) {
				result.vn = Integer.parseUnsignedInt(parts[2].trim());
			}
			return result;
		}
	}
	
	private static class IndexedFace {
		public IndexedVertex a;
		public IndexedVertex b;
		public IndexedVertex c;
		
		public static void parse(String def, Collection<IndexedFace> faces) {
			if (!def.startsWith("f ")) throw new IllegalArgumentException("Face declaration must start with f");
			String[] parts = def.split(" ");
			if (parts.length-1<3 || parts.length-1>4) throw new IllegalArgumentException("Face cannot have less than 3 or more than 4 vertices - this one has "+(parts.length-1));
			
			
			if (parts.length-1==3) {
				IndexedFace face = new IndexedFace();
				face.a = IndexedVertex.of(parts[1]);
				face.b = IndexedVertex.of(parts[2]);
				face.c = IndexedVertex.of(parts[3]);
				faces.add(face);
			} else {
				IndexedVertex a = IndexedVertex.of(parts[1]);
				IndexedVertex b = IndexedVertex.of(parts[2]);
				IndexedVertex c = IndexedVertex.of(parts[3]);
				IndexedVertex d = IndexedVertex.of(parts[4]);
				
				IndexedFace face1 = new IndexedFace();
				face1.a = a;
				face1.b = b;
				face1.c = c;
				faces.add(face1);
				IndexedFace face2 = new IndexedFace();
				face2.a = a;
				face2.b = c;
				face2.c = d;
				faces.add(face2);
			}
		}
	}
	
	private static Vertex deref(IndexedVertex v,  ArrayList<Vector3d> positions, ArrayList<Vector3d> textures, ArrayList<Vector3d> normals) {
		Vector3d pos = deref(v.v-1, positions);
		Vector3d tex = deref(v.vt-1, textures);
		Vector3d col = new Vector3d(1,1,1);
		Vector3d normal = deref(v.vn-1, normals);
		
		return new Vertex(pos, tex, col, normal);
	}
	
	private static Face deref(IndexedFace f, ArrayList<Vector3d> positions, ArrayList<Vector3d> textures, ArrayList<Vector3d> normals) {
		Vertex a = deref(f.a, positions, textures, normals);
		Vertex b = deref(f.b, positions, textures, normals);
		Vertex c = deref(f.c, positions, textures, normals);
		
		//Face result = new Face(a,b,c);
		//result.genFaceNormal();
		//return result;
		return new Face(a,b,c);
	}
	
	/*
	private static class Vertex {
		Vector3f pos;
		Vector3f tex;
		Vector3f norm;
		Vector3f rawNorm;
		
		public static Vertex of(IndexedVertex v, ArrayList<Vector3f> positions, ArrayList<Vector3f> textures, ArrayList<Vector3f> normals) {
			Vertex result = new Vertex();
			result.pos = deref(v.v-1, positions);
			result.tex = deref(v.vt-1, textures);
			result.norm= deref(v.vn-1, normals);
			result.rawNorm = result.norm;
			
			return result;
		}
	}
	
	private static class Face {
		Vertex a;
		Vertex b;
		Vertex c;
		
		public Vertex getVertex(Vector3f pos) {
			if (a.pos.equals(pos)) return a;
			if (b.pos.equals(pos)) return b;
			if (c.pos.equals(pos)) return c;
			return a;
		}
		
		public static Face of(IndexedFace f, ArrayList<Vector3f> positions, ArrayList<Vector3f> textures, ArrayList<Vector3f> normals, HashMap<Vector3f, HashSet<Face>> adjacentFaces) {
			Face result = new Face();
			
			result.a = Vertex.of(f.a, positions, textures, normals);
			result.b = Vertex.of(f.b, positions, textures, normals);
			result.c = Vertex.of(f.c, positions, textures, normals);
			adjacentFace(result, adjacentFaces);
			
			return result;
		}
	}*/
}

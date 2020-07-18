/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

/**
 * Metaphone and DoubleMetaphone are originally by Lawrence Phillips ((c) 1998, 1999),
 * and source code in both C/C++ and tablized form are available under the LGPL.
 *
 * The code herein is a total rewrite.
 * @author Isaac Ellingson (Falkreon)
 */
public class Metaphone {
	private String s = "";
	private StringBuilder out = new StringBuilder();

	public Metaphone() {}

	public String apply(String s) {
		String[] pieces = s.split(" ");
		StringBuilder result = new StringBuilder();
		for(String q : pieces) {
			this.s = q;
			out.setLength(0);

			apply();

			result.append(out.toString());
			result.append(' ');
		}
		return result.toString().trim();
	}

	/**
	 * Drop the first character of the working String
	 */
	private void dropFirst() {
		if (out.length()<1) return;
		int codePoints = Character.charCount(out.codePointAt(0));
		for(int i=0; i<codePoints; i++) {
			if (out.length()==0) return;
			out.deleteCharAt(0);
		}
	}

	/**
	 * Drop the first 'n' characters from the working String
	 */
	private void dropFirst(int n) {
		for(int i=0; i<n; i++) dropFirst();
	}

	private void prepend(char ch) {
		out.insert(0, ch);
	}

	private void apply() {
		s = s.toLowerCase();
		out.append(s);

		//start with some start-of-word rules

		//e.g. "Aebersold", "Gnagy", "Knuth", "Pniewski", "Wright"
		if (out.toString().startsWith("gn")
			|| s.startsWith("kn")
			|| s.startsWith("pn")
			|| s.startsWith("wr")
				) {
			dropFirst();
		}

		//e.g. "Deng Xiaopeng"
		if (out.toString().startsWith("x")) {
			dropFirst();
			prepend('s');
		}

		//e.g. "Whalen"
		if (out.toString().startsWith("wh")) {
			dropFirst(2);
			prepend('w');
		}

		//Leading vowels are important, but not distinct from each other.
		char ch = out.charAt(0);
		if (ch=='a' || ch=='e' || ch=='i' || ch=='o' || ch=='u' || ch=='y') {
			dropFirst();
			prepend('*');
		}

		//special cases: sometimes "ough" has an "f" sound
		String tmp = out.toString();
		if (tmp.equals("cough") || tmp.equals("rough") || tmp.equals("tough")) {
			out.setLength(0);

			out.append(tmp.charAt(0));
			out.append('f');
			return;
		}

		if (tmp.equals("*nough")) {
			out.setLength(0);
			out.append("*nf");
			return;
		}

		//now that we're sorted out, we can apply more general-case rules.
		//For this we're going to re-convert out into a String and char-by-char rebuild it
		tmp = out.toString();
		out.setLength(0);

		char pre   = 0;
		char cur   = (tmp.length()>0) ? tmp.charAt(0) : 0;
		char post  = (tmp.length()>1) ? tmp.charAt(1) : 0;
		char post2 = (tmp.length()>2) ? tmp.charAt(2) : 0;
		out.append(processBeginning(cur,post));
		//System.out.println("::"+out.toString());
		if (tmp.length()>0) tmp = tmp.substring(1);
		while(tmp.length()>0) {
			pre = cur;
			cur = post;
			post = post2;
			post2 = (tmp.length()>2) ? tmp.charAt(2) : 0;
			if (post!=0) out.append(processMid(pre, cur, post, post2));
			else out.append(processLast(pre, cur));
			//System.out.println("::"+out.toString());
			tmp = tmp.substring(1);
		}
		//processLast(cur, post);
	}

	public String processBeginning(char cur, char post) {
		//System.out.println("PRE: - "+cur+" "+post+" -");

		switch(cur) {
		case 'a': return "*";
		case 'c': return "k";
		case 'd': return "t";
		case 'e': return "*";
		case 'g': return "k";
		case 'i': return "*";
		case 'j': return "k";
		case 'o': return "*";
		case 'p': return (post=='h') ? "f":"p";
		case 'q': return "k";
		case 's':
			if (post=='h') return "x";
			else return "s";
		case 't':
			if (post=='h') return "0"; //0 == thorn
			else return "t";
		case 'u': return "*";
		case 'w':
			if (vowel(post)) return "w";
			else return "";
		case 'x': return "ks";
		case 'y':
			if (vowel(post)) return "y";
			else return "";
		case 'z': return "s";
		default:
			return ""+cur;
		}
	}

	public String processMid(char pre, char cur, char post, char post2) {
		//System.out.println("MID: "+pre+" "+cur+" "+post+" "+post2);

		switch(cur) {
		case 'a': return "";
		case 'c':
			if (post=='i' && post2=='a') return "x";
			if (post=='h' && pre!='s') return "x";
			if (post=='i' || post=='e' || post=='y') {
				return (pre=='s') ? "" : "s";
			}
			if (post=='q') return "";
			return "k";
		case 'd':
			if (post=='g' && post2=='e') return "k";
			if (post=='g' && post2=='y') return "k";
			if (post=='g' && post2=='i') return "k";
			return "t";
		case 'e': return "";
		case 'g':
			if (post=='h' && !vowel(post2)) return "";
			if (post=='n' && post2==0) return "";
			if (post=='n' && post2=='e') return "";
			if (post=='n' && post2=='s') return "";
			if (pre=='d' && post=='e' && post2!=0) return "";
			if (pre!='g' && (post=='i'||post=='e'||post=='y')) return "k";
			return "k";
		case 'h':
			if (vowel(pre) && (!vowel(post) || post=='y')) return "";
			if (pre=='c' || pre=='s' || pre=='p' || pre=='t' || pre=='g' || pre=='r') return "";
			return "h";
		case 'i': return "";
		case 'j': return "k";
		case 'k':
			if (pre=='c') return "";
			return "k";
		case 'm':
			if (pre=='m') return "";
			return "m";
		case 'n':
			if (pre=='n') return "";
			return "n";
		case 'o': return "";
		case 'p':
			if (pre=='f') return "";
			return "p";
		case 'q': return "k";
		case 'r':
			if (vowel(pre) && (!vowel(post) || post=='y')) return "";
			return "r";
		case 's':
			if (post=='h' && post2=='e') return "sk";
			if (post=='h' && post2=='o') return "sk";
			if (post=='h') return "x";
			if (post=='i' && post2=='o') return "x";
			if (post=='i' && post2=='a') return "x";
			return "s";
		case 't':
			if (post=='i' && post2=='o') return "x";
			if (post=='i' && post2=='a') return "x";
			if (post=='c' && post2=='h') return "";
			if (post=='h') return "0"; //thorn
			return "t";
		case 'u': return "";
		case 'v': return "f";
		case 'w':
			if (vowel(post)) return "w";
			return "";
		case 'x': return "ks";
		case 'y':
			if (vowel(post)) return "y";
			return "";
		case 'z': return "s";
		default:
			return ""+cur;
		}
	}

	public String processLast(char pre, char cur) {
		//System.out.println("END: "+pre+" "+cur+" - -");

		switch(cur) {
		case 'a': return "";
		case 'b':
			if (pre=='m') return "";
			return "b";
		case 'c': return "k";
		case 'd': return "t";
		case 'e': return "";
		case 'g': return "k";
		case 'h':
			if (vowel(pre)) return "";
			return "h";
		case 'i': return "";
		case 'j': return "k";
		case 'k':
			if (pre=='c') return "";
			return "k";
		case 'm':
			if (pre=='m') return "";
			return "m";
		case 'n':
			if (pre=='n') return "";
			return "n";
		case 'o': return "";
		case 'q': return "k";
		case 'r':
			if (vowel(pre)) return "";
			return "r";
		case 'u': return "";
		case 'v': return "f";
		case 'w':
			if (vowel(pre)) return "";
			return "w";
		case 'x': return "ks";
		case 'y': return "";
		case 'z': return "s";
		default:
			return ""+cur;
		}
	}

	private static boolean vowel(char ch) {
		return ch=='a' || ch=='e' || ch=='i' || ch=='o' || ch=='u' || ch=='y';
	}
}

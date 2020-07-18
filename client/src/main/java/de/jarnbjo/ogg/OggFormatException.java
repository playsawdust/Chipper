/*
 * This software is based on or using the J-Ogg library available from
 * http://www.j-ogg.de and copyrighted by Tor-Einar Jarnbjo.
 *
 * You are free to use, modify, redistribute or include this software in your own
 * free or commercial software. The only restriction is, that you make it obvious
 * that your software is based on J-Ogg by including this notice in the
 * documentation, about box or wherever you feel appropriate.
 */

package de.jarnbjo.ogg;

import java.io.IOException;

/**
 * Exception thrown when trying to read a corrupted Ogg stream.
 */
public class OggFormatException extends IOException {
	private static final long serialVersionUID = 1240486559620129278L;

	public OggFormatException() {
		super();
	}

	public OggFormatException(String message) {
		super(message);
	}
}
package com.fis.webserver.model;

import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;

/**
 * Represents a chunk of data that has been read from the socket represented by the containig SelectionKey
 * 
 * @author Florin Iordache
 *
 */

public class SocketReadPayload extends WorkPayload {
	private CharBuffer charBuffer;
	
	public SocketReadPayload(SelectionKey key, CharBuffer charBuffer) {
		super(key);
		this.charBuffer = charBuffer;
	}

	public CharBuffer getCharBuffer() {
		return charBuffer;
	}
}

package com.fis.webserver.model;

import java.nio.channels.SelectionKey;

import com.fis.webserver.model.http.HttpRequest;

/**
 * Models a valid request that has been received from the client identified by
 * the contained selection key
 * 
 * @author Florin Iordache
 * 
 */
public class HttpRequestPayload extends WorkPayload {
	
	private HttpRequest request;
	
	public HttpRequestPayload(SelectionKey key, HttpRequest request) {
		super(key);
		this.request = request;
	}

	public HttpRequest getRequest() {
		return request;
	}
}

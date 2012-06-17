package com.fis.webserver.model;

import java.nio.channels.SelectionKey;

import com.fis.webserver.model.http.HttpResponse;

/**
 * Models a response that needs to be sent to the client identified by the
 * contained selection key
 * 
 * @author Florin Iordache
 * 
 */

public class HttpResponsePayload extends WorkPayload {

	private HttpResponse response;
	
	public HttpResponsePayload(SelectionKey key, HttpResponse response) {
		super(key);
		
		this.response = response;
	}

	public HttpResponse getResponse() {
		return response;
	}
	
}

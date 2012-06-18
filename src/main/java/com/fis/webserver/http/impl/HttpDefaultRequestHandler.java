package com.fis.webserver.http.impl;

import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;

/**
 * Default handler of unsupported http methods
 * 
 * Will return a 501 unimplemented status
 * 
 * @author Florin Iordache
 *
 */

public class HttpDefaultRequestHandler implements HttpRequestHandler {

	@Override
	public HttpResponse handle(HttpRequest request) {
		//returning a 501 status message
		HttpResponse response = new HttpResponse(501);
		
		return response;
	}

}

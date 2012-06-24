package com.fis.webserver.http.impl;

import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;
import com.fis.webserver.model.http.HttpResponseCode;

/**
 * Default handler of unsupported http methods
 * 
 * Will return a unimplemented http status
 * 
 * @author Florin Iordache
 *
 */

public class HttpUnimplementedRequestHandler implements HttpRequestHandler {

	@Override
	public HttpResponse handle(HttpRequest request) {
		//returning a not implemented code
		HttpResponse response = new HttpResponse(HttpResponseCode.NOT_IMPLEMENTED);
		
		return response;
	}
}

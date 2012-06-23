package com.fis.webserver.http.impl;

import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;

/**
 * Request handler for the POST method
 * 
 * The web server will not support dynamic content generation.
 * 
 * The only difference from the GET handler is the resource cleanup that has to
 * be done after the response is sent
 * 
 * @author Florin Iordache
 * 
 */

public class HttpPostResponseHandler extends HttpBaseResponseHandler implements
		HttpRequestHandler {

	@Override
	public HttpResponse handle(HttpRequest request) {
		
		//use the base handler
		HttpResponse response = super.handle(request);
		
		// this is a POST request, make sure we clean up any temp file that was
		// created in the process
		response.setCleaner(request.getRequestBody().getCleaner());
		
		return response;
	}
	
}

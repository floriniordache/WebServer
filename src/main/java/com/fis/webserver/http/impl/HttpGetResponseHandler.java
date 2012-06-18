package com.fis.webserver.http.impl;

import java.io.InputStream;

import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;
import com.fis.webserver.util.URLTranslator;

/**
 * Request handler for the GET http method
 * 
 *
 * 
 * @author Florin Iordache
 *
 */

public class HttpGetResponseHandler implements HttpRequestHandler {

	@Override
	public HttpResponse handle(HttpRequest request) {
		//find the requested resource
		InputStream requestedResourceInputStream = URLTranslator.mapResource(request.getURL());
		
		int statusCode = 404;
		if( requestedResourceInputStream != null ) {
			//found the resource
			
			statusCode = 200;
		}
		
		HttpResponse response = new HttpResponse(statusCode);
		response.addHeader("Content-Type", "text/html");
		response.setContentStream(requestedResourceInputStream);
		
		return response;
	}

}

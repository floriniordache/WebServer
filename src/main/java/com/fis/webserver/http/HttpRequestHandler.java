package com.fis.webserver.http;

import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;

/**
 * Interface for a generic http handler
 * 
 * @author Florin Iordache
 *
 */

public interface HttpRequestHandler {
	
	/**
	 * Handles a http request
	 * 
	 * @param request HttpRequest object that needs to be handled
	 * @return a HttpResponse object
	 */
	public HttpResponse handle(HttpRequest request);
}

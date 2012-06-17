package com.fis.webserver.http;

import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;

/**
 * Describes the basic functions that are performed by a response builder
 * 
 * @author Florin Iordache
 * 
 */

public interface ResponseBuilder extends Runnable {

	/**
	 * Takes the new http request and performs the needed operations in order to
	 * build the response
	 * 
	 * @param request
	 */
	public HttpResponse respond(HttpRequest request);
}

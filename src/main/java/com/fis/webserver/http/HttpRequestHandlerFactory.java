package com.fis.webserver.http;

import com.fis.webserver.http.impl.HttpDefaultRequestHandler;

/**
 * Creates HttpRequestHandler objects based on the given http method that needs
 * to be handled
 * 
 * @author Florin Iordache
 * 
 */

public class HttpRequestHandlerFactory {
	
	/**
	 * Creates a HttpRequestHandler that is able to handle the given method
	 * 
	 * @param method Method thad needs to be handled
	 * @return a HttpRequestHandler object
	 */
	public static HttpRequestHandler createRequestHandler(String method) {
		return new HttpDefaultRequestHandler();
	}
}

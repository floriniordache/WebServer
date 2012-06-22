package com.fis.webserver.http;

import com.fis.webserver.http.impl.HttpDefaultRequestHandler;
import com.fis.webserver.http.impl.HttpGetResponseHandler;
import com.fis.webserver.http.impl.HttpPostResponseHandler;

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
		if("GET".equals(method)) {
			return new HttpGetResponseHandler();
		}
		else if("POST".equals(method)) {
			return new HttpPostResponseHandler();
		}
		return new HttpDefaultRequestHandler();
	}
}

package com.fis.webserver.http;

import java.util.HashMap;

import com.fis.webserver.http.impl.HttpHeadResponseHandler;
import com.fis.webserver.http.impl.HttpUnimplementedRequestHandler;
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
	
	//map with references to the implemented handlers
	private static HashMap<String, HttpRequestHandler> handlerMap;
	
	static {
		handlerMap = new HashMap<String, HttpRequestHandler>();
		handlerMap.put("GET",new HttpGetResponseHandler());
		handlerMap.put("POST",new HttpPostResponseHandler());
		handlerMap.put("HEAD",new HttpHeadResponseHandler());
	}
	
	/**
	 * Creates a HttpRequestHandler that is able to handle the given method
	 * 
	 * @param method Method thad needs to be handled
	 * @return a HttpRequestHandler object
	 */
	public static HttpRequestHandler createRequestHandler(String method) {
		HttpRequestHandler handler = handlerMap.get(method);
		if( handler == null ) {
			handler = new HttpUnimplementedRequestHandler();
		}
		
		return handler;
	}
}

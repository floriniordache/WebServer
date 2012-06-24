package com.fis.webserver.http;

import java.util.HashMap;

import com.fis.webserver.http.impl.HttpHeadResponseHandler;
import com.fis.webserver.http.impl.HttpUnimplementedRequestHandler;
import com.fis.webserver.http.impl.HttpGetResponseHandler;
import com.fis.webserver.http.impl.HttpPostResponseHandler;
import com.fis.webserver.model.http.HttpRequestMethod;

/**
 * Looks up a HttpRequestHandler that is capable of solving a certain request method
 * 
 * @author Florin Iordache
 * 
 */

public enum HttpRequestHandlerFinder {
	INSTANCE;
	
	//map with references to the implemented handlers
	private HashMap<String, HttpRequestHandler> handlerMap;
	
	private HttpRequestHandlerFinder() {
		handlerMap = new HashMap<String, HttpRequestHandler>();
		handlerMap.put(HttpRequestMethod.GET, new HttpGetResponseHandler());
		handlerMap.put(HttpRequestMethod.POST, new HttpPostResponseHandler());
		handlerMap.put(HttpRequestMethod.HEAD, new HttpHeadResponseHandler());		
	}
	
	/**
	 * Tries to find a HttpRequestHandler in the internal handler map.
	 * The handler that can deal with the needed method will be returned on a successful hit.
	 * The default handler will be returned if a handler is not implemented for the received method  
	 * 
	 * @param method Method thad needs to be handled
	 * @return a HttpRequestHandler object
	 */
	public static HttpRequestHandler lookupRequestHandler(String method) {

		HttpRequestHandler handler = INSTANCE.handlerMap.get(method);
		
		if ( handler == null ) {
			handler = new HttpUnimplementedRequestHandler();
		}
		
		return handler;
	}
}

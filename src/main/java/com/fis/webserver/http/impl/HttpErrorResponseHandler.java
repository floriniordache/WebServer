package com.fis.webserver.http.impl;

import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.http.exceptions.BadRequestException;
import com.fis.webserver.http.exceptions.EntityTooLargeException;
import com.fis.webserver.http.exceptions.HeaderTooLargeException;
import com.fis.webserver.http.exceptions.RequestException;
import com.fis.webserver.http.exceptions.URITooLongException;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;
import com.fis.webserver.model.http.HttpResponseCode;

/**
 * Handler for a erroneous http requests
 * 
 * Based on the exception received as a parameter will build an apropriate
 * response for the client
 * 
 * @author Florin Iordache
 * 
 */

public class HttpErrorResponseHandler implements HttpRequestHandler {
	//exception that ocurred during the request
	private RequestException requestException;
	
	public HttpErrorResponseHandler(RequestException requestException) {
		this.requestException = requestException;
	}
	
	@Override
	public HttpResponse handle(HttpRequest request) {
		HttpResponseCode responseCode = HttpResponseCode.INTERNAL_SERVER_ERROR;
		
		if( requestException instanceof URITooLongException ) {
			responseCode = HttpResponseCode.REQUEST_URI_TOO_LONG;
		}
		else if (requestException instanceof HeaderTooLargeException) {
			responseCode = HttpResponseCode.REQUEST_HEADER_TOO_LARGE;
		}
		else if (requestException instanceof EntityTooLargeException) {
			responseCode = HttpResponseCode.ENTITY_TOO_LARGE;
		}
		else if(requestException instanceof BadRequestException) {
			responseCode = HttpResponseCode.BAD_REQUEST;
		}
		
		HttpResponse response = new HttpResponse(responseCode);
		
		return response;
	}

}

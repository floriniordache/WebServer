package com.fis.webserver.http.impl;

import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.http.exceptions.RequestException;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;
import com.fis.webserver.model.http.HttpResponseCode;

/**
 * Handler for a erroneous http requests
 * 
 * Based on the exception received as a parameter will build an appropriate
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
		
		//map the error that occurred to the corresponding response
		switch( requestException.getErrorCause() ) {
		case RequestException.BAD_REQUEST:
			responseCode = HttpResponseCode.BAD_REQUEST;
			break;
		case RequestException.ENTITY_TOO_LARGE:
			responseCode = HttpResponseCode.ENTITY_TOO_LARGE;
			break;
		case RequestException.HEADER_TOO_LARGE:
			responseCode = HttpResponseCode.REQUEST_HEADER_TOO_LARGE;
			break;
		case RequestException.URI_TOO_LONG:
			responseCode = HttpResponseCode.REQUEST_URI_TOO_LONG;
			break;
		default:
			responseCode = HttpResponseCode.INTERNAL_SERVER_ERROR;
		}
		
		//build and return the response object
		HttpResponse response = new HttpResponse(responseCode);
		
		//make sure we clean the resources used on the server
		response.setCleaner(request.getRequestBody().getCleaner());
		
		return response;
	}

}

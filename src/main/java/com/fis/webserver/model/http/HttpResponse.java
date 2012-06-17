package com.fis.webserver.model.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Models the Http Response
 * 
 * @author Florin Iordache
 *
 */

public class HttpResponse {
	public static final String HTTP_VERSION = "HTTP/1.1";
	public static final String SERVER_HEADER_VALUE = "FISServer";
	
	private int statusCode;
	private HashMap<String, String> responseHeaders;
	private StringBuilder responseBody;
	
	public HttpResponse(int statusCode) {
		this.statusCode = statusCode;
		
		responseHeaders = new HashMap<String, String>();
		responseHeaders.put("Server", SERVER_HEADER_VALUE);
		responseHeaders.put("Connection", "Close");
		
		responseBody = new StringBuilder();
	}
	
	public int getStatusCode() {
		return statusCode;
	}

	public HashMap<String, String> getResponseHeaders() {
		return responseHeaders;
	}

	public void addHeader(String headerName, String headerValue) {
		responseHeaders.put(headerName, headerValue);
	}
	
	public StringBuilder getResponseBody() {
		return responseBody;
	}
	
	public StringBuilder getRawResponse() {
		StringBuilder rawResponse = new StringBuilder(HTTP_VERSION);
		rawResponse.append(" ");
		rawResponse.append(statusCode);
		rawResponse.append("\r\n");
		
		for( Map.Entry<String, String> headerEntry : responseHeaders.entrySet() ) {
			rawResponse.append(headerEntry.getKey() + ": " + headerEntry.getValue() + "\r\n");
		}
		rawResponse.append("\r\n");
		return rawResponse;
	}
}

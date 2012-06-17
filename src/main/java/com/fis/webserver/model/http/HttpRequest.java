package com.fis.webserver.model.http;

import java.util.HashMap;

/**
 * Object representing a parsed HTTP request
 * 
 * @author Florin Iordache
 *
 */

public class HttpRequest {
	//contains the http method (GET, POST, etc.)
	private String method;
	
	//the URI on which the method must be performed on
	private String URI;
	
	//http major and minor versions
	private int httpMajorVersion;
	private int httpMinorVersion;
	
	//request headers
	private HashMap<String, String> headers;
	
	//request body
	private StringBuilder requestBody;
	
	public HttpRequest() {
		this.headers = new HashMap<String, String>();
		
		requestBody = new StringBuilder();
	}

	public String getURI() {
		return URI;
	}

	public int getHttpMajorVersion() {
		return httpMajorVersion;
	}

	public int getHttpMinorVersion() {
		return httpMinorVersion;
	}

	public HashMap<String, String> getHeaders() {
		return headers;
	}

	public void addHeader(String headerName, String headerValue) {
		headers.put(headerName, headerValue);
	}
	
	public void setMethod(String method) {
		this.method = method;
	}

	public void setURI(String uRI) {
		URI = uRI;
	}

	public void setHttpMajorVersion(int httpMajorVersion) {
		this.httpMajorVersion = httpMajorVersion;
	}

	public void setHttpMinorVersion(int httpMinorVersion) {
		this.httpMinorVersion = httpMinorVersion;
	}

	public void addToRequestBody(StringBuilder requestBodyPart) {
		this.requestBody.append(requestBodyPart);
	}

	public StringBuilder getRequestBody() {
		return requestBody;
	}

	public String getMethod() {
		return method;
	}
}

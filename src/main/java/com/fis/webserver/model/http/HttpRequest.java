package com.fis.webserver.model.http;

import java.util.HashMap;

/**
 * Object representing a parsed HTTP request
 * 
 * @author Florin Iordache
 *
 */

public class HttpRequest {	
	public static final String HTTP_CONTENT_LENGTH_HEADER = "Content-Length";
	
	//contains the http method (GET, POST, etc.)
	private String method;
	
	//the URL on which the method must be performed on
	private String URL;
	
	//http major and minor versions
	private int httpMajorVersion;
	private int httpMinorVersion;
	
	//request headers
	private HashMap<String, String> headers;
	
	//container for the request body
	private RequestBody requestBody;

	private long contentLength;
	
	public HttpRequest() {
		this.headers = new HashMap<String, String>();
		
		requestBody = new RequestBody();
		
		contentLength = -1;
	}

	public String getURL() {
		return URL;
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
		
		// if the header being read is the content-length, store the value in a
		// separate variable
		if( HTTP_CONTENT_LENGTH_HEADER.equalsIgnoreCase(headerName) ) {
			contentLength = Long.parseLong(headerValue);
			
			//update content length of the request body
			requestBody.setMaxEntityBodyLength(contentLength);
		}
	}
	
	public void setMethod(String method) {
		this.method = method;
	}

	public void setURL(String URL) {
		this.URL = URL;
	}

	public void setHttpMajorVersion(int httpMajorVersion) {
		this.httpMajorVersion = httpMajorVersion;
	}

	public void setHttpMinorVersion(int httpMinorVersion) {
		this.httpMinorVersion = httpMinorVersion;
	}

	public RequestBody getRequestBody() {
		return this.requestBody;
	}
		
	public String getMethod() {
		return method;
	}
	
	/**
	 * Returns the value of the Content-Length header, if any or -1 if the
	 * header is not present
	 */
	public long getContentLength() {
		return contentLength;
	}
}

package com.fis.webserver.model.http;

/**
 * Enum of the supported response codes
 * 
 * @author Florin Iordache
 *
 */

public enum HttpResponseCode {

	OK(200),
	BAD_REQUEST(400),
	NOT_FOUND(404),
	ENTITY_TOO_LARGE(413),
	REQUEST_URI_TOO_LONG(414),
	REQUEST_HEADER_TOO_LARGE(431),
	INTERNAL_SERVER_ERROR(500),
	NOT_IMPLEMENTED(501);
	
	private int code;
	
	private HttpResponseCode(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return this.code;
	}
}

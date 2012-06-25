package com.fis.webserver.http.exceptions;


/**
 * Base Request exception
 * 
 * @author Florin Iordache
 *
 */
public class RequestException extends Exception {
	public static final int BAD_REQUEST = 0;
	public static final int ENTITY_TOO_LARGE = 1;
	public static final int HEADER_TOO_LARGE = 2;
	public static final int URI_TOO_LONG = 3;
	
	private static final long serialVersionUID = -4566362139997433898L;
	
	private int cause;
	
	public RequestException(int cause) {
		super();
		this.cause = cause;
	}
	
	public int getErrorCause() {
		return this.cause;
	}
}

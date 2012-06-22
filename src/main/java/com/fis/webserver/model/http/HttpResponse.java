package com.fis.webserver.model.http;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.fis.webserver.util.cleaner.Cleaner;

/**
 * Models the Http Response
 * 
 * @author Florin Iordache
 *
 */

public class HttpResponse {
	public static final String HTTP_VERSION = "HTTP/1.1";
	public static final String SERVER_HEADER_VALUE = "FISServer";
	
	//date format for the date and last-modified information of the response headers
	public static final DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");  
	
	private int statusCode;
	private HashMap<String, String> responseHeaders;
	
	private InputStream contentInputStream;
	
	//will perform the needed cleanup after the response is sent to the client
	private Cleaner resourceCleaner;
	
	static {
		//initialize the timezone of the dateformat object to be GMT
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public HttpResponse(int statusCode) {
		this.statusCode = statusCode;
		
		// create the generic headers: Server name, date of the response
		// generation and connection close(for the moment)
		responseHeaders = new HashMap<String, String>();
		responseHeaders.put("Server", SERVER_HEADER_VALUE);

		responseHeaders.put("Date", df.format(new Date()));
		responseHeaders.put("Connection", "Close");
		
		contentInputStream = null;
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
	
	public InputStream getContentStream() {
		return contentInputStream;
	}
	
	public void setContentStream(InputStream contentInputStream) {
		this.contentInputStream = contentInputStream;
	}
	
	public void setCleaner(Cleaner resourceCleaner) {
		this.resourceCleaner = resourceCleaner;
	}
	
	public Cleaner getResourceCleaner() {
		return resourceCleaner;
	}

	public void setResourceCleaner(Cleaner resourceCleaner) {
		this.resourceCleaner = resourceCleaner;
	}

	public StringBuilder getSerializedHeader() {
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

package com.fis.webserver.http.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.fis.webserver.config.MimeTypes;
import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.model.http.HttpHeader;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;
import com.fis.webserver.model.http.HttpResponseCode;
import com.fis.webserver.util.FileUtils;
import com.fis.webserver.util.URLTranslator;

/**
 * Base request handler for supported http requests
 *
 * @author Florin Iordache
 *
 */


public class HttpBaseResponseHandler implements HttpRequestHandler {

	public static final Logger logger = Logger.getLogger(HttpGetResponseHandler.class);
		
	@Override
	public HttpResponse handle(HttpRequest request) {
		//map the request to an actual file in the server's file system
		File requestedFile = URLTranslator.mapResource(request.getURL());
		
		HashMap<String, String> contentHeaders = new HashMap<String, String>();
		
		FileInputStream responseBody = null;
		
		HttpResponseCode statusCode = HttpResponseCode.NOT_FOUND;
		if( requestedFile != null ) {

			//found the resource
			statusCode = HttpResponseCode.OK;
			
			//build content headers
			contentHeaders.put(HttpHeader.CONTENT_TYPE, MimeTypes.getMimeType(FileUtils.getExtension(requestedFile)));
			contentHeaders.put(HttpHeader.CONTENT_LENGTH, String.valueOf(requestedFile.length()));
			
			//check if the response can contain a response body
			if( containsResponseBody() ) {
				responseBody = FileUtils.getFileInputStream(requestedFile);
			}
		}
		
		HttpResponse response = new HttpResponse(statusCode);
		
		//add content related headers to the response headers
		response.addAll(contentHeaders);
		
		// update the response with the file channel of the requested resource
		// if there is an associated InputStream
		if( responseBody != null ) {
			response.setContentChannel(responseBody.getChannel());
		}
		
		return response;
	}

	/**
	 * Returns a flag if the response should contain a response body or not
	 * 
	 * GET and POST can include a response body, while a HEAD should not
	 * 
	 * @return
	 */
	public boolean containsResponseBody() {
		return true;
	}

}

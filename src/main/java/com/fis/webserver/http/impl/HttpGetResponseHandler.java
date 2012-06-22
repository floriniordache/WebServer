package com.fis.webserver.http.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.fis.webserver.config.MimeTypes;
import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;
import com.fis.webserver.util.FileUtils;
import com.fis.webserver.util.URLTranslator;

/**
 * Request handler for the GET http method
 * 
 *
 * 
 * @author Florin Iordache
 *
 */

public class HttpGetResponseHandler implements HttpRequestHandler {
	public static final Logger logger = Logger.getLogger(HttpGetResponseHandler.class);
	
	@Override
	public HttpResponse handle(HttpRequest request) {

		//map the request to an actual file in the server's file system
		File requestedFile = URLTranslator.mapResource(request.getURL());
		
		HashMap<String, String> contentHeaders = new HashMap<String, String>();
		
		InputStream requestedResourceInputStream = null;
		
		int statusCode = 404;
		if( requestedFile != null ) {

			//found the resource
			statusCode = 200;
			
			//attempt to open the file
			try {
				requestedResourceInputStream = new FileInputStream(requestedFile);
			}
			catch(Exception e) {
				logger.error("Could not open " + requestedFile.getName(), e);
			}
			
			//build content headers
			contentHeaders.put("Content-Type", MimeTypes.getMimeType(FileUtils.getExtension(requestedFile)));
			contentHeaders.put("Content-Length", String.valueOf(requestedFile.length()));
		}
		
		HttpResponse response = new HttpResponse(statusCode);
		
		//add content related headers to the response headers
		response.addAll(contentHeaders);
		
		response.setContentStream(requestedResourceInputStream);
		
		return response;
	}

}

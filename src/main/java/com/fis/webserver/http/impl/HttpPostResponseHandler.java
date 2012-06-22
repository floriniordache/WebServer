package com.fis.webserver.http.impl;

import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;
import com.fis.webserver.util.cleaner.TempFileCleaner;

/**
 * Request handler for the POST method
 * 
 * Dynamic content generation is supported via CGI Handler will try to spawn a
 * new process that will run the executable on the server side.
 * 
 * The request body will be piped in the input stream of the process and the
 * output stream that the executable generates will be relayed back to the
 * client
 * 
 * If the request string does not refer to a CGI script, the handler will use
 * the GET method to handle the request
 * 
 * @author Florin Iordache
 * 
 */

public class HttpPostResponseHandler implements HttpRequestHandler {

	@Override
	public HttpResponse handle(HttpRequest request) {
		// TODO Spawn new process
		//TODO Pass request body input stream to the spawned process input stream
		//TODO Relay process output stream as the response output stream
		
		HttpResponse response = new HttpResponse(200);
		response.addHeader("Content-Type", "text/html");
		
		//passing a resource cleaner to the response object
		//that will close the entity body input stream and delete any temp file
		//it will be called after the response is completely sent back to the client
		response.setCleaner(new TempFileCleaner(request.getEntityBody(), request.getTempFile()));
		
		return response;
	}

}

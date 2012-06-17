package com.fis.webserver.http;

import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;

/**
 * Interface for a generic http handler
 * 
 * @author Florin Iordache
 *
 */

public interface HttpRequestHandler {
	public HttpResponse handle(HttpRequest request);
}

package com.fis.webserver.http.impl;

import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.http.HttpRequestHandlerFactory;
import com.fis.webserver.http.ResponseBuilder;
import com.fis.webserver.model.HttpRequestPayload;
import com.fis.webserver.model.HttpResponsePayload;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;
import com.fis.webserver.pool.WorkerManager;

/**
 * Implementation of the ResponseBuilder
 * 
 * This implementation will read parsed HttpRequests from the workQueue and
 * compile a response for that given request object
 * 
 * @author Florin Iordache
 * 
 */

public class HttpResponseBuilder implements ResponseBuilder {
	public static final Logger logger = Logger.getLogger(HttpResponseBuilder.class);
	
	//queue where the newly parsed incoming requests are placed
	private BlockingQueue<HttpRequestPayload> pendingRequests;
	
	//reference to the worker manager object
	private WorkerManager workManager;
	
	public HttpResponseBuilder(BlockingQueue<HttpRequestPayload> workQueue, WorkerManager manager) {
		this.pendingRequests = workQueue;
		
		this.workManager = manager;
	}
	
	@Override
	public void run() {
		while(true) {
			//wait on the queue for new work
			try {
				HttpRequestPayload requestData = pendingRequests.take();
				
				//respond to the request
				HttpResponse response = respond(requestData.getRequest());
				
				//send the response to the worker manager to send it back to the client
				workManager.sendResponse(new HttpResponsePayload(requestData.getKey(), response));
			} catch (InterruptedException e) {
				logger.error("Could not read new request object from the work queue!", e);
			}
		}
	}

	@Override
	public HttpResponse respond(HttpRequest request) {

		//get a handler capable to solve the request
		HttpRequestHandler handler = HttpRequestHandlerFactory.createRequestHandler(request.getMethod());
		
		//delegate the handling of the request to the handler
		HttpResponse response = handler.handle(request);
		
		return response;
	}

}

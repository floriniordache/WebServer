package com.fis.webserver.http.impl;

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.fis.webserver.http.HttpRequestParserWorkState;
import com.fis.webserver.http.RequestParser;
import com.fis.webserver.model.HttpRequestPayload;
import com.fis.webserver.model.SocketReadPayload;
import com.fis.webserver.model.http.HttpRequest;

/**
 * Implementation of the RequestParser
 * 
 * The worker uses two blocking queues: - waits for a new chunk of data on the
 * receivedDataQueue queue - after successfully parsing a http requests, it
 * pushes it into parsedRequestQueue queue
 * 
 * The internal workStates map is used to handle partial request processing as
 * new data is read from the incoming queue
 * 
 * @author Florin Iordache
 * 
 */

public class HttpRequestParser implements RequestParser {
	public static final Logger logger = Logger.getLogger(HttpRequestParser.class);

	//internal map with the work states of all currently parsed requests
	private HashMap<SelectionKey, HttpRequestParserWorkState> workStates;

	//New data queue
	private BlockingQueue<SocketReadPayload> receivedDataQueue;
	
	//parsed HttpRequests queue
	private BlockingQueue<HttpRequestPayload> parsedRequestQueue;
	
	public HttpRequestParser(BlockingQueue<SocketReadPayload> incomingWorkQueue,
			BlockingQueue<HttpRequestPayload> outgoingWorkQueue) {
		//creating the work state and the internal queue
		workStates = new HashMap<SelectionKey, HttpRequestParserWorkState>();
		
		this.receivedDataQueue = incomingWorkQueue;
		this.parsedRequestQueue = outgoingWorkQueue;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				
				//should change to receivedDataQueue.poll() to be able to also handle client timeouts
				SocketReadPayload data = receivedDataQueue.take();
				
				//check if we already have this key
				HttpRequestParserWorkState workState = workStates.get(data.getKey());
				
				if( workState == null ) {
					//key not present, add new work state
					workState = new HttpRequestParserWorkState();
					
					workStates.put(data.getKey(), workState);
				}

				//send the data for processing
				boolean parsingFinished = workState.newData(data.getBuffer());
				
				if( parsingFinished ) {
					//parsing is finished, deliver the data in the exit queue
					HttpRequest request = workState.getHttpRequest();
					parsedRequestQueue.put(new HttpRequestPayload(data.getKey(), request));
					
					//remove the work state related to this key
					workStates.remove(data.getKey());
				}
				
			} catch (InterruptedException e) {
				logger.error("Could not process new data!", e);
			}
		}
	}

	/**
	 * A new chunk of data is available, will append it to existing data if the
	 * key is already present in the map, or create a new entry
	 * 
	 * @param newData the data that was read
	 */
	@Override
	public void newData(SocketReadPayload newData) {
		try {
			receivedDataQueue.put(newData);
		} catch (InterruptedException e) {
			logger.error("Could not add new data to the internal queue!", e);
		}
	}
}

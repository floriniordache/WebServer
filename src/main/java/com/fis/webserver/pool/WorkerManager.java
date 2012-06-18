package com.fis.webserver.pool;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.fis.webserver.config.WebServerConfiguration;
import com.fis.webserver.core.WebWorker;
import com.fis.webserver.core.impl.HttpWebWorker;
import com.fis.webserver.http.ResponseBuilder;
import com.fis.webserver.http.impl.HttpRequestParser;
import com.fis.webserver.http.impl.HttpResponseBuilder;
import com.fis.webserver.model.HttpRequestPayload;
import com.fis.webserver.model.HttpResponsePayload;
import com.fis.webserver.model.SocketReadPayload;

/**
 * WebWorker singleton thread pool manager
 * 
 * @author Florin Iordache
 *
 */

public class WorkerManager {
	public static final Logger logger = Logger.getLogger(WorkerManager.class);
	
	private List<WebWorker> workerPool;
	
	private HttpRequestParser requestParserWorker;
	
	private ResponseBuilder responseBuilderWorker;
	
	// the ResponseBuilder thread waits on this queue for new valid parsed http
	// requests that need to be solved 
	private BlockingQueue<HttpRequestPayload> responderWorkQueue;
	
	// The RequestParser thread waits on this queue for new incoming data from
	// the connected clients. The workers will push newly read data into this
	// queue as it is available for reading from the sockets.
	// The data will need to be parsed into a valid
	// HttpRequest if possible
	private BlockingQueue<SocketReadPayload> requestParserWorkQueue;
	
	public WorkerManager() {
		
		//create the work queues
		requestParserWorkQueue = new ArrayBlockingQueue<SocketReadPayload>(
				3 * WebServerConfiguration.INSTANCE.getClientsPerWorker(), true);
		
		responderWorkQueue = new ArrayBlockingQueue<HttpRequestPayload>(
				3 * WebServerConfiguration.INSTANCE.getClientsPerWorker(), true);
		
		workerPool = new ArrayList<WebWorker>();
		// start the worker threads, based on the minimum workers setting of the
		// server
		logger.debug("Spawning " + WebServerConfiguration.INSTANCE.getMinWorkers() + " worker threads!");
		for( int workerIdx = 0 ; workerIdx < WebServerConfiguration.INSTANCE.getMinWorkers() ; workerIdx++ ) {
			
			//create new worker that can handle the max number of clients defined by the config
			WebWorker worker = new HttpWebWorker(
					WebServerConfiguration.INSTANCE.getClientsPerWorker(), requestParserWorkQueue);
			
			//create and start new thread that will run the worker
			Thread workerThread = new Thread(worker);
			workerThread.setName("Worker " + workerIdx);
			workerThread.start();
			
			//add the worker to the pool
			workerPool.add(worker);
		}
		
		//start the request parser thread
		logger.debug("Spawning RequestParser worker");
		requestParserWorker = new HttpRequestParser(requestParserWorkQueue,
				responderWorkQueue);
		Thread parserWorkerThread = new Thread(requestParserWorker);
		parserWorkerThread.setName("RequestParser");
		parserWorkerThread.start();
		
		//start the response builder thread
		logger.debug("Spawning ResponseBuilder worker");
		responseBuilderWorker = new HttpResponseBuilder(responderWorkQueue, this);
		Thread responseBuilderWorkerThread = new Thread(responseBuilderWorker);
		responseBuilderWorkerThread.setName("ResponseBuilder");
		responseBuilderWorkerThread.start();
		
		logger.debug("Worker threads spawned!");
	}

	/**
	 * On an incoming new connection the manager will select a thread from the
	 * pool to handle this client
	 * 
	 * @param key
	 */
	public void handleNewClient(SocketChannel socketChannel) {
		logger.debug("New incoming client, selecting worker thread from the pool...");
		
		//determine the lowest loaded worker
		WebWorker lowestLoaded = null;
		
		for(WebWorker worker : workerPool) {
			if (lowestLoaded == null
					|| lowestLoaded.getFreeSlots() < worker.getFreeSlots()) {
				lowestLoaded = worker;
			}
		}
		
		logger.debug("Registering the new socket channel with one of the workers...");
		
		//try to pass the socket to the lowest loaded worker
		if( !lowestLoaded.handle( socketChannel ) ) {
			logger.error("All existing workers are full, rejecting request!");
			
			//reject the request for the moment
			try {
				socketChannel.close();
			} catch (IOException e) {
				logger.error("Error while closing channel!", e);
			}
		}
	}
	
	/**
	 * 
	 * 
	 * @param response
	 */
	public void sendResponse(HttpResponsePayload response) {
		logger.debug("Sending response to the client...");
		SelectionKey key = response.getKey();
		
		for( WebWorker worker : workerPool ) {
			if( worker.isHandlingClient(key) ) {
				worker.sendResponse(key, response.getResponse());
			}
		}
	}
}

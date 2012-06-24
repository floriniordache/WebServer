package com.fis.webserver.pool;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

import com.fis.webserver.config.WebServerConfiguration;
import com.fis.webserver.core.WebWorker;
import com.fis.webserver.core.impl.HttpWebWorker;

/**
 * WebWorker thread pool manager
 * 
 * It dispatches the incoming connections to one of the WebWorker objects in the
 * internal pool, using a Round Robin balancing algorithm
 * 
 * The linked list is used as a queue: when dispatching an incoming connection
 * to one of the workers, the workers will be extracted from the beginning of
 * the list, and re-added at the end. If the pool is exhausted, the worker will try to
 * spawn a new worker thread that will handle the new connection
 * 
 * @author Florin Iordache
 * 
 */

public class WorkerManager {
	public static final Logger logger = Logger.getLogger(WorkerManager.class);
	
	private PriorityQueue<WebWorker> workerPool;
	
	public WorkerManager() {
		
		workerPool = new PriorityQueue<WebWorker>();
		// start the worker threads, based on the minimum workers setting of the
		// server
		logger.debug("Spawning " + WebServerConfiguration.INSTANCE.getMinWorkers() + " worker threads!");
		for( int workerIdx = 0 ; workerIdx < WebServerConfiguration.INSTANCE.getMinWorkers() ; workerIdx++ ) {
			
			//create new worker that can handle the max number of clients defined by the config
			spawnWorker();
		}
		
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
		
		//get the lowest loaded worker
		WebWorker handlingWorker = workerPool.poll();
		
		if( handlingWorker == null) {
			logger.fatal("Worker thread pool is empty!");
			
			return;
		}
		
		boolean clientHandled = handlingWorker.handle(socketChannel);
		
		
		//put the handling worker back in the queue
		workerPool.add(handlingWorker);
		
		if( !clientHandled ) {
			// if the lowest loaded worker can't handle the new client, we need
			// to spawn another one
			
			logger.debug("No worker available, must try to increase pool size!");
			
			//increase pool method will automatically insert new worker in the queue
			WebWorker newWorker = increasePool();
			if( newWorker != null ) {
				logger.debug("New worker created, passing the client socketChannel for handling!");
				newWorker.handle(socketChannel);
			}
			else {
				logger.error("All existing workers are full, rejecting request!");

				//reject the request, can't handle it
				try {
					socketChannel.close();
				} catch (IOException e) {
					logger.error("Error while closing channel!", e);
				}
			}
		}
	}
	
	/**
	 * Try to increase the pool , if the settings permit it
	 * 
	 * @return a new WebWorker
	 */
	private WebWorker increasePool() {
		// if the lowest loaded worker can't handle a new client, try to see
		// if we can spawn another web worker
		if(workerPool.size() < WebServerConfiguration.INSTANCE.getMaxWorkers()) {
			
			//we can spawn another worker
			WebWorker newWorker = spawnWorker();
			
			return newWorker;
		}
		else {
			logger.error("Maximum number of clients reached, consider tuning the server parameters to be able to support more!");
		}
		
		return null;
	}
	
	/**
	 * 
	 * Spawns new worker thread
	 * 
	 */
	private WebWorker spawnWorker() {
		logger.info("Spawning new WebWorker thread!");
		//create new worker that can handle the max number of clients defined by the config
		WebWorker worker = new HttpWebWorker(
				WebServerConfiguration.INSTANCE.getClientsPerWorker());
		
		//create and start new thread that will run the worker
		Thread workerThread = new Thread(worker);
		workerThread.setName("WebWorker " + workerThread.getId());
		workerThread.start();
		
		//add the worker to the pool
		workerPool.add(worker);
		
		return worker;
	}
}

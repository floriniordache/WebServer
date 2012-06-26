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
 * The pool size is based on the webserver.properties configuration information.
 * Minimum number of workers will be determined via
 * WebServerConfiguration.INSTANCE.getMinWorkers() If at some point more
 * requests hit the webserver, and all the workers are full, a new worker will
 * be spawned, as long as the pool size does not exceed
 * WebServerConfiguration.INSTANCE.getMaxWorkers(). Periodic attempts are made
 * to compact the worker pool. Workers are discarded from pool if they are not
 * handling any clients, and there are at least
 * WebServerConfiguration.INSTANCE.getMinWorkers() workers available in the pool
 * 
 * It uses an internal PriorityQueue to store the available WebWorkers. The
 * WebWorkers are sorted by the number of free client slots in descending order.
 * 
 * When polling the queue, the returned WebWorker is automatically one that has
 * the highest number of client slots free (meaning the worker with the lowest
 * load). After handing the new client to the worker, the workers are re-added
 * to the queue, to be kept in sorted order.
 * 
 * Also, after each operation of handing new connection to the workers, an
 * attempt is made to compact the pool to the minimum number of workers stated
 * in the config file
 * 
 * @author Florin Iordache
 * 
 */

public class WorkerManager {
	public static final Logger logger = Logger.getLogger(WorkerManager.class);
	
	//main pool of web workers
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
		
		//try to compact pool
		compactPool();
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
	
	
	/**
	 * 
	 * Attempts to compact pool by discarding the unused workers
	 * 
	 */
	private void compactPool() {
		logger.info("Compacting WebWorker pool...");
		
		WebWorker handlingWorker = null;
		while((workerPool.size() > WebServerConfiguration.INSTANCE.getMinWorkers()) && ((handlingWorker = workerPool.poll()) != null)) {
			//search for idle workers
			int workerFreeSlots = handlingWorker.getFreeSlots();
			
			if( workerFreeSlots < WebServerConfiguration.INSTANCE.getClientsPerWorker() ) {
				//this is a priority queue ordered descending by the number of free slots
				//if we reached a worker with a lower number of client slots, we should terminate processing, we can't discard any workers
				logger.trace("No more workers to discard!");
				workerPool.add(handlingWorker);
				break;
			}
			else {
				logger.trace("Found idle WebWorker, shutting down and discarding from pool!");
				
				//this is an idle worker
				handlingWorker.shutDown();
				
				//don't add back to pool
			}
		}
		
	}
}

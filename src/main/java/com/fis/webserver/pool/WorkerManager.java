package com.fis.webserver.pool;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fis.webserver.core.WebWorker;
import com.fis.webserver.core.impl.HttpWebWorker;

/**
 * WebWorker singleton thread pool manager
 * 
 * @author Florin Iordache
 *
 */

public enum WorkerManager {
	INSTANCE;
	
	public static final Logger logger = Logger.getLogger(WorkerManager.class);
	
	private List<WebWorker> workerPool;
	
	private WorkerManager() {
		workerPool = new ArrayList<WebWorker>();
		
		WebWorker worker = new HttpWebWorker(0);
		Thread workerThread = new Thread(worker);
		workerThread.setName("Worker 0");
		workerThread.start();
		
		workerPool.add(worker);
	}

	/**
	 * On an incoming new connection the manager will select a thread from the
	 * pool to handle this client
	 * 
	 * @param key
	 */
	public void handleNewClient(SocketChannel socketChannel) {
		logger.debug("New incoming client, selecting worker thread from the pool...");
		
		for(WebWorker worker : workerPool) {
			if( worker.handle( socketChannel ) ) {
				break;
			}
		}
	}
}

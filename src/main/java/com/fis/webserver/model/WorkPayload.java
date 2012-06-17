package com.fis.webserver.model;

import java.nio.channels.SelectionKey;

/**
 * Base class for all the objects being passed in work queues between the worker
 * threads
 * 
 * It contains the selection key identifying the originating client
 * 
 * @author Florin Iordache
 * 
 */

public abstract class WorkPayload {
	private SelectionKey key;
	
	public WorkPayload(SelectionKey key) {
		this.key = key;
	}

	public SelectionKey getKey() {
		return key;
	}
}

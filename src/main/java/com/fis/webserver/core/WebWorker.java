package com.fis.webserver.core;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Public interface describing the worker thread capabilities
 * 
 * @author Florin Iordache
 *
 */

public interface WebWorker extends Runnable, Comparable<WebWorker> {
	
	/**
	 * Handles new client connection to be handled by this worker
	 * 
	 * @param socketChannel
	 * @return true indicating whether this worker is able to handle the new client
	 */
	public boolean handle(SocketChannel socketChannel);
	
	
	/**
	 * Determines if the client represented by the key is handled by this worker
	 * 
	 * @param key SelectionKey representing a client connection
	 * @return true if the client is handled by this worker
	 */
	public boolean isHandlingClient(SelectionKey key);
	
	/**
	 * Closes the channel associates with this key and cancels the key handling
	 * 
	 * @param key
	 * 		Key for which handling will be canceled
	 */
	public void closeChannel(SelectionKey key);
	
	/**
	 * Should return the number unused client slots
	 * 
	 * @return integer representing the number of extra clients this worker can handle
	 */
	public int getFreeSlots();
}

package com.fis.webserver.core;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.fis.webserver.model.http.HttpResponse;

/**
 * Public interface describing the worker thread capabilities
 * 
 * @author Florin Iordache
 *
 */

public interface WebWorker extends Runnable {
	
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
	 * Sends a response back to the client
	 * 
	 * @param key SelectionKey representing the client connection
	 * @param response the response that needs to be sent
	 */
	public void sendResponse(SelectionKey key, HttpResponse response);
	
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

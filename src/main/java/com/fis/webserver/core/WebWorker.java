package com.fis.webserver.core;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

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
	 * Closes the channel associates with this key and cancels the key handling
	 * 
	 * @param key
	 * 		Key for which handling will be canceled
	 */
	public void closeChannel(SelectionKey key);
}

package com.fis.webserver.http;

import com.fis.webserver.model.SocketReadPayload;

/**
 * Interface of the worker that is in charge with parsing incoming http requests
 * 
 * @author Florin Iordache
 * 
 */

public interface RequestParser extends Runnable {

	/**
	 * Method called when a new chunk of request data is available for processing
	 * 
	 * @param newData the newly read data
	 */
	public void newData(SocketReadPayload newData);

}
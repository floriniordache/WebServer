package com.fis.webserver.core;

import com.fis.webserver.core.impl.IncomingConnectionListenerImpl;

/**
 * Factory for the ConnectionListener implementors
 * 
 * @author Florin Iordache
 *
 */

public class ConnectionListenerFactory {
	
	/**
	 * Creates a connection listener
	 * 
	 * @return connection listener implementation
	 */
	public static ConnectionListener getConnectionListner() {
		return new IncomingConnectionListenerImpl();
	}
}

package com.fis.webserver.core.impl;

import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.fis.webserver.core.ConnectionListener;
import com.fis.webserver.pool.WorkerManager;

/**
 * Incoming connection listener
 * 
 * Extends the abstract Connection listener.
 * 
 * Uses a WorkerManager to distribute incoming connections to a pool of
 * WebWorker threads
 * 
 * @author Florin Iordache
 * 
 */

public class IncomingConnectionListenerImpl extends ConnectionListener {

	public IncomingConnectionListenerImpl(int portNo) {
		super(portNo);
	}

	public static final Logger logger = Logger.getLogger(IncomingConnectionListenerImpl.class);

	@Override
	public void acceptConnection(SocketChannel channel) {
	    WorkerManager.INSTANCE.handleNewClient(channel);
	}
}

package com.fis.webserver.core;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

/**
 * Abstract connection listener class based on Java NIO framework
 * classes
 * 
 * Provides basic functionality for listening for new connections, while leaving
 * the handling of new connections up to the implementing classes
 * 
 * @author Florin Iordache
 * 
 */

public abstract class ConnectionListener extends Thread {
	public static final Logger logger = Logger.getLogger(ConnectionListener.class);
	
	//the port where the connections are expected
	private int port;
	
	//incoming channel for accepting connections
	private ServerSocketChannel serverSocketChannel;
		
	//flag indicating if the connection listener is initialized and listening for connections
	private boolean initialized;
	
	public ConnectionListener( int portNo ) {
		this.port = portNo;
		this.initialized = false;
	}
	
	public void run() {
		
		//initialize the selector
		this.initialize();
		
		//monitor the selector for incoming connections
		while(true) {
			try {
				logger.debug("Waiting for a new connection...");
				
				SocketChannel socketChannel = serverSocketChannel.accept();
				acceptConnection(socketChannel);
			}
			catch(Exception e) {
				logger.error("Error while waiting for new connection!", e);
			}
		}
	}
	
	public void initialize() {
		
		logger.debug("Initializing server socket channel on port " + port);
		
		try {
			//creating non blocking server socket channel
			serverSocketChannel = ServerSocketChannel.open();
			//serverSocketChannel.configureBlocking(false);
			
			//bind the server socket to local machine address and port
			InetSocketAddress inetAddr = new InetSocketAddress(port);
			serverSocketChannel.socket().bind(inetAddr);
			
			logger.debug("Successfully initialized server socket channel on port " + port);
			
			initialized = true;
		}
		catch(Exception e) {
			initialized = false;
			logger.error(
					"Error initializing the incoming connection listener on port "
							+ port, e);
		}
	}

	public boolean isInitialized() {
		return initialized;
	}
	
	/**
	 * Abstract accept connection method
	 * It will be invoked when a new connection is available
	 * 
	 * @param key
	 */
	public abstract void acceptConnection(SocketChannel channel);
}

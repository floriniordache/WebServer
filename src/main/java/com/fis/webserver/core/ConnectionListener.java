package com.fis.webserver.core;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.fis.webserver.config.WebServerConfiguration;

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
		
	//the selector this listener will be monitoring
	private Selector serverSelector;
	
	//flag indicating if the connection listener is initialized and listening for connections
	private boolean initialized;
	
	public ConnectionListener(  ) {
		this.port = WebServerConfiguration.INSTANCE.getPortNumber();
		
		this.initialized = false;
	}
	
	public void run() {
		
		//initialize the selector
		this.initialize();
		
		//monitor the selector for incoming connections
		while(true) {
			try {
				logger.debug("Waiting for a new connection...");
				
				//wait for an event on the selector
				serverSelector.select();
				
				//iterate over the selected keys
				logger.debug("Found incoming " + serverSelector.selectedKeys().size() + " new clients!");
				Iterator<SelectionKey> selectedKeysIterator = serverSelector.selectedKeys().iterator();
				while(selectedKeysIterator.hasNext()) {
					
					// get a selection key and remove it from the set(the
					// selector does not automatically do that)
					SelectionKey key = selectedKeysIterator.next();
					selectedKeysIterator.remove();
					
					if( key.isValid() && key.isAcceptable() ) {
						SocketChannel newClientSocketChannel = serverSocketChannel.accept();
						acceptConnection(newClientSocketChannel);
					}
				}
			}
			catch(Exception e) {
				logger.error("Error while waiting for new connection!", e);
			}
		}
	}
	
	public void initialize() {
		
		logger.debug("Initializing server socket channel on port " + port);
		
		try {
			//initialize the selector
			serverSelector = SelectorProvider.provider().openSelector();
			
			//creating non blocking server socket channel
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			
			//bind the server socket to local machine address and port
			InetSocketAddress inetAddr = new InetSocketAddress(port);
			serverSocketChannel.socket().bind(inetAddr);
			
			// register the channel with the server selector, indicating it's
			// interested in accept events
			serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
			
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

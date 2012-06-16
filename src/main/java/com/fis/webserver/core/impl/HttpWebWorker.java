package com.fis.webserver.core.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import com.fis.webserver.core.WebWorker;

/**
 * WebWorker implementation
 * 
 * @author Florin Iordache
 *
 */

public class HttpWebWorker implements WebWorker {
	public static final Logger logger = Logger.getLogger(HttpWebWorker.class);
	
	//maximum number of clients this worker can handle
	private int maxClients;
	
	//internal selector this worker is monitoring
	private Selector socketSelector;
	
	private ArrayBlockingQueue<SocketChannel> pendingChanges;
	
	public HttpWebWorker(int maxClients) {
		this.maxClients = maxClients;
		
		pendingChanges = new ArrayBlockingQueue<SocketChannel>(10);
		
		try {
			// Create a new selector
		    socketSelector = SelectorProvider.provider().openSelector();
		}
		catch(Exception e) {
			logger.error("Could not initialize socket selector!", e);
		}
	}
	
	@Override
	public void run() {
		
		//handle the reading requests and writing responses to the registered channels
		while(true) {
			try {
				logger.debug("Waiting to read data");
				
				//wait for at least one incoming connection
				socketSelector.select();
				
				//check for pending changes
				SocketChannel newChannel = pendingChanges.poll();
				if(newChannel != null) {
					//configure the channel for non-blocking mode
					newChannel.configureBlocking(false);
					//register this socket channel for the read operation
					newChannel.register(socketSelector, SelectionKey.OP_READ);
				}
				
				//iterate over the available selection keys
				Iterator<SelectionKey> selectedKeysIterator = socketSelector.selectedKeys().iterator(); 
				while(selectedKeysIterator.hasNext()) {
					SelectionKey selectionKey = selectedKeysIterator.next();
					
					//remove from set to prevent future processing
					selectedKeysIterator.remove();
					
					//check if key is valid
					if(!selectionKey.isValid()) {
						continue;
					}
					
					//read the data
					if(selectionKey.isReadable()) {
						logger.debug("Reading request from socket");
						
						readRequest(selectionKey);
						
					}
					else if( selectionKey.isWritable() ) {
						logger.debug("Writing response to socket");
						
						writeResponse(selectionKey);
					}
				}
				
			}
			catch(Exception e) {
				logger.error("Error while waiting for new connection!", e);
			}
		}
	}

	@Override
	public boolean handle(SocketChannel socketChannel) {
		pendingChanges.add(socketChannel);
		socketSelector.wakeup();
		
		return true;
	}

	/**
	 * Handles the data reading operation from the socket channel associated
	 * with this key
	 * 
	 * @param key
	 *            The key representing the socket channel that has data
	 *            available for reading
	 */
	private void readRequest(SelectionKey key) {
		//get the associated socket channel
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		//prepare the byte buffer
		ByteBuffer readBuffer = ByteBuffer.allocate(8196);
		
		//attempt to read from the socket
		int bytesRead = -1;
		try {
			bytesRead = socketChannel.read(readBuffer);
		}
		catch(IOException e) {
			//the remote host has closed the connection
			closeChannel(key);
		}

		/*
		 * check if we were able to read something; if not, the remote
		 * connection has been closed cleanly
		 */
		if( bytesRead < 0 ) {
			closeChannel(key);
		}
		
		Charset charset = Charset.forName("UTF-8");
		CharsetDecoder decoder = charset.newDecoder();
		decoder.reset();
		
		try {
			readBuffer.position(0);
			logger.debug("Read from socket: " + decoder.decode(readBuffer).toString());
		} catch (CharacterCodingException e) {
		}
		//key.interestOps(SelectionKey.OP_WRITE);
	}

	/**
	 * Sends the response to the socket channel associated with this key
	 * 
	 * @param key
	 *            Key representing the socket channel where the response will be
	 *            written
	 */
	private void writeResponse(SelectionKey key) {
		//write a dummy response and close the channel;
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		ByteBuffer writeBuffer = ByteBuffer.wrap("Thanks".getBytes());
		
		try {
			socketChannel.write(writeBuffer);
		} catch (IOException e) {
			logger.error("Error writing to socket!", e);
		}
		
		closeChannel(key);
	}

	@Override
	public void closeChannel(SelectionKey key) {
		try {
			key.channel().close();
		}
		catch(IOException e) {
			logger.warn("Exception while shutting down channel!", e);
		}
		key.cancel();
	}
}

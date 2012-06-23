package com.fis.webserver.core.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.fis.webserver.core.WebWorker;
import com.fis.webserver.http.IncrementalResponseWriter;
import com.fis.webserver.model.SocketReadPayload;
import com.fis.webserver.model.http.HttpResponse;

/**
 * WebWorker implementation
 * 
 * @author Florin Iordache
 *
 */

public class HttpWebWorker implements WebWorker {
	public static final Logger logger = Logger.getLogger(HttpWebWorker.class);
	
	//number of available client slots
	private int freeClientSlots;
	
	//internal selector this worker is monitoring
	private Selector socketSelector;
	
	// queue containing the new SocketChannels that need to be listened by this
	// worker
	private BlockingQueue<SocketChannel> newClientsQueue;
	
	// work queue for pushing the read data
	private BlockingQueue<SocketReadPayload> workQueue;
	
	//map with the pending responses that need to be sent back to the clients
	private HashMap<SelectionKey, IncrementalResponseWriter> pendingResponses;
	
	//buffer used to read and write data
	private ByteBuffer dataBuffer;
	
	public HttpWebWorker(int maxClients, BlockingQueue<SocketReadPayload> workQueue) {
		this.freeClientSlots = maxClients;
		
		this.workQueue = workQueue;
		
		newClientsQueue = new ArrayBlockingQueue<SocketChannel>(maxClients + 1);
		
		pendingResponses = new HashMap<SelectionKey, IncrementalResponseWriter>();
		
		try {
			// Create a new selector
		    socketSelector = SelectorProvider.provider().openSelector();
		}
		catch(Exception e) {
			logger.error("Could not initialize socket selector!", e);
		}
		
		//prepare the byte buffer
		dataBuffer = ByteBuffer.allocate(8190);
	}
	
	@Override
	public void run() {

		//handle the reading requests and writing responses to the registered channels
		while(true) {
			try {
				logger.trace("Waiting to read data");
				
				//wait for at least one incoming connection
				socketSelector.select();
				
				//check for pending changes
				SocketChannel newChannel = newClientsQueue.poll();
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
						logger.trace("Reading request from socket");
						
						readRequest(selectionKey);
						
					}
					else if( selectionKey.isWritable() ) {
						logger.trace("Writing response to socket");
						
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
		
		// accept the client for handling only if there are still more client
		// slots available
		if( freeClientSlots > 1 ) {
			
			//see if we can insert new client in the queue
			if(newClientsQueue.offer(socketChannel)) {
				socketSelector.wakeup();
				freeClientSlots --;
				return true;
			}
			else {
				//queue is full
				logger.error("WebWorker incoming client queue is full!");
			}
		}
		
		return false;
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
		
		//clear the buffer
		dataBuffer.clear();
		
		//attempt to read from the socket
		int bytesRead = -1;
		try {
			bytesRead = socketChannel.read(dataBuffer);
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
				
		try {
			dataBuffer.flip();
			
			//copy the buffer to send it to the parsing worker
			byte[] readData = new byte[dataBuffer.limit()];
			dataBuffer.get(readData);
			
			//push the new data in the work queue
			workQueue.put(new SocketReadPayload(key, readData));
		}
		catch (InterruptedException ie) {
			logger.error("Worker interrupted while pushing newly read data to the work queue!", ie);
		}
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
		
		//use the IncrementalResponseWriter to write data to the socket
		
		IncrementalResponseWriter responseWriter = pendingResponses.get(key);
		if( responseWriter == null ) {
			logger.error("No response available for sending!");
			
			//close the channel, we don't have any data
			closeChannel(key);
		}
		else {
			//clear the data buffer
			dataBuffer.clear();
			
			//write a chunk of data to the buffer
			boolean sendFinished = responseWriter.incrementalWriteResponse(dataBuffer);		
			
			//write the data to the socket
			try {
				dataBuffer.flip();
				socketChannel.write(dataBuffer);
			} catch (IOException e) {
				logger.error("Error writing to socket!", e);
				
				//close the channel
				closeChannel(key);
				
				//remove the helper response writer from the pending send data queue
				pendingResponses.remove(key);
			}
			
			//check if processing is finished
			if(sendFinished) {
				//key.interestOps(SelectionKey.OP_READ);
				
				//close the channel
				closeChannel(key);
				
				//remove the helper response writer from the pending send data queue
				pendingResponses.remove(key);
			}
		}
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
		freeClientSlots ++;
	}

	@Override
	public boolean isHandlingClient(SelectionKey key) {
		return socketSelector.keys().contains(key);
	}

	@Override
	public void sendResponse(SelectionKey key, HttpResponse response) {		
		//queue the response
		//use an IncrementalResponseWriter to help with the serialization process
		pendingResponses.put(key, new IncrementalResponseWriter(response));
		
		//register the socket channel for read operation
		key.interestOps(SelectionKey.OP_WRITE);
		
		socketSelector.wakeup();
	}

	@Override
	public int getFreeSlots() {
		return freeClientSlots;
	}
}

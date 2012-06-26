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
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.fis.webserver.config.WebServerConfiguration;
import com.fis.webserver.core.WebWorker;
import com.fis.webserver.http.HttpRequestHandler;
import com.fis.webserver.http.HttpRequestHandlerFinder;
import com.fis.webserver.http.HttpRequestParser;
import com.fis.webserver.http.IncrementalResponseWriter;
import com.fis.webserver.http.exceptions.RequestException;
import com.fis.webserver.model.http.HttpRequest;
import com.fis.webserver.model.http.HttpResponse;

/**
 * WebWorker implementation
 * 
 * The web worker will handle incoming connections via its handle(SocketChannel)
 * method. If there are still client slots free, the new SocketChannel will be
 * pushed in the internal new client queue (newClientsQueue). The socketSelector
 * will be waked up, so it will pick up the newly added clients if it is
 * blocking in the SocketSelector.select() method call.
 * 
 * After the new client is picked up from the queue, its SocketChannel will be
 * registered with the selector of this worker.
 * 
 * To take into account that data will be read into chunks from the client
 * SocketChannels, there will be a HttpRequestParser object for each handled
 * client connection that will take care of incrementally parsing the incoming
 * data, trying to identify a valid HttpRequest.
 * 
 * Also, after a valid HttpRequest has been parsed from a client connection and
 * a response compiled, the response will be send to the client in chunks. There
 * will be an IncrementalResponseWriter for each client, that will take care of
 * incrementally sending the response to the client.
 * 
 * Currently Keep-Alive is not supported. The connection is terminated after the
 * accept - read request - write response cycle.
 * 
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
		
	//map with the pending responses that need to be sent back to the clients
	private HashMap<SelectionKey, IncrementalResponseWriter> pendingResponses;
	
	//map with the request reads that are in progress
	private HashMap<SelectionKey, HttpRequestParser> pendingReads;
	
	//buffer used to read and write data
	private ByteBuffer dataBuffer;
	
	//flag indicating that this worker should shut down
	private boolean shutDown;
	
	public HttpWebWorker(int maxClients ) {
		this.freeClientSlots = maxClients;
		
		newClientsQueue = new ArrayBlockingQueue<SocketChannel>(maxClients);
		
		pendingResponses = new HashMap<SelectionKey, IncrementalResponseWriter>();
		
		pendingReads = new HashMap<SelectionKey, HttpRequestParser>();
		
		try {
			// Create a new selector
		    socketSelector = SelectorProvider.provider().openSelector();
		}
		catch(Exception e) {
			logger.error("Could not initialize socket selector!", e);
		}
		
		//prepare the byte buffer
		//allocating the max request line byte size
		dataBuffer = ByteBuffer.allocate(WebServerConfiguration.MAX_REQUEST_LINE_SIZE);
		
		shutDown = false;
	}
	
	@Override
	public void run() {

		//handle the reading requests and writing responses to the registered channels
		while(true) {
			try {
				logger.trace("Waiting to read or write data from the clients...");
				
				//wait for at least one incoming connection
				socketSelector.select();
				
				//check for the shutdown flag
				if(shutDown) {
					//terminate the worker
					logger.trace("Terminating WebWorker!");
					break;
				}
				
				//check for pending incoming clients
				SocketChannel newChannel = null;
				
				//check the queue for new clients, and register them all with the internal selector
				while ((newChannel = newClientsQueue.poll(1,TimeUnit.MILLISECONDS)) != null) {
					// configure the channel for non-blocking mode
					newChannel.configureBlocking(false);
					// register this socket channel for the read operation
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
		logger.trace("Trying to push new client in the internal worker queue...");
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
				logger.trace("WebWorker incoming client queue is full! queue size=" + newClientsQueue.size());
			}
		}
		
		logger.trace("WebWorker incoming client queue is full, can't handle client!");
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
		
		//try to read data from the channel
		int bytesRead = channelReadData(key);

		// if we were able to read somthing, try to continue with request
		// parsing
		if( bytesRead > 0 ) {

			//continue with the request parsing
			dataBuffer.flip();
			
			//see if we have a pending read
			HttpRequestParser parserWorkState = pendingReads.get(key);
			//if we don't, create a new one
			if( parserWorkState == null ) {
				parserWorkState = new HttpRequestParser();
				
				pendingReads.put(key, parserWorkState);
			}
			
			//copy the read buffer to the work state to process the new data			
			boolean parsingFinished = parserWorkState.newData(dataBuffer);
			
			if( parsingFinished ) {
				//parsing is finished, must respond to the request
				HttpRequest request = parserWorkState.getHttpRequest();
				
				//remove this work state from the map
				pendingReads.remove(key);
				
				//respond to this request
				respond(key, request, parserWorkState.getException());
			}
		}
	}

	/**
	 * Reads a chunk of data from the SocketChannel associated with a SelectionKey
	 * 
	 * Method will use the WebWorker's internal dataBuffer to read incoming bytes
	 * 
	 * @param key - SelectionKey of the channel we want to read from
	 * @return - number of bytes actually read, or -1 if the channel was closed
	 */
	private int channelReadData(SelectionKey key) {
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
			
			logger.debug("Unable to read data from the connection!", e);
		}

		/*
		 * check if we were able to read something; if not, the remote
		 * connection has been closed cleanly
		 */
		if( bytesRead < 0 ) {
			closeChannel(key);
			
			logger.debug("Remote host has closed connection!");
		}
		
		return bytesRead;
	}

	/**
	 * 
	 * Prepares the http response
	 * 
	 * Method will try to find a handler, based on the request method, if the
	 * request was successfully parsed, or based on the exception received as a
	 * parameter, if parsing has failed
	 * 
	 * @param key
	 *            SelectionKey indicating the incoming connection
	 * @param request
	 *            HttpRequest object
	 * @param exception
	 *            RequestException object, resulted from the parsing operation
	 * 
	 */
	private void respond(SelectionKey key, HttpRequest request, RequestException exception) {
		
		HttpRequestHandler handler = null;
		
		//get an apropriate handler
		if( exception != null ) {
			//there was an error parsing the request
			//get the error handler
			handler = HttpRequestHandlerFinder.getErrorHandler(exception);
		}
		else {
			//get a handler capable to solve the request
			handler = HttpRequestHandlerFinder.lookupRequestHandler(request.getMethod());			
		}

		//build the response		
		//delegate the handling of the request to the handler
		HttpResponse response = handler.handle(request);
		
		//queue the response to be sent back to client
		queueResponse(key, response);
	}
	
	/**
	 * Queues the HttpResponse in the pendingResponses map to be sent back to the client.
	 * The key's interestOps will be switched to OP_WRITE and the SelectionKey is removed from the pendingReads map
	 * 
	 * @param key SelectionKey object
	 * 
	 * @param response HttpResponse that has to be queued for sending back to the client
	 */
	private void queueResponse(SelectionKey key, HttpResponse response) {
		//queue the response
		//use an IncrementalResponseWriter to help with the serialization process
		pendingResponses.put(key, new IncrementalResponseWriter(response));
		
		//remove the client from the pending reads map
		pendingReads.remove(key);
		
		//register the socket channel for read operation
		key.interestOps(SelectionKey.OP_WRITE);
		
		socketSelector.wakeup();
	}
	
	/**
	 * Sends the response to the socket channel associated with this key
	 * 
	 * @param key
	 *            Key representing the socket channel where the response will be
	 *            written
	 */
	private void writeResponse(SelectionKey key) {		
		//use the IncrementalResponseWriter to write data to the socket	
		IncrementalResponseWriter responseWriter = pendingResponses.get(key);
		if( responseWriter == null ) {
			logger.error("No response available for sending!");
			
			//close the channel, we don't have any data
			closeChannel(key);
		}
		else {
			
			//send a chunk of data to the client
			boolean sendFinished = channelWriteResponse(responseWriter, key);		
						
			//check if processing is finished
			if(sendFinished) {
				//key.interestOps(SelectionKey.OP_READ);
				
				//close the channel
				closeChannel(key);
			}
		}
	}

	/**
	 * Writes a chunk of data back to the client
	 * 
	 * It uses the clients IncrementalWriteResponse to fill the data buffer
	 * 
	 * @param responseWriter IncrementalResponseWriter associated with the key
	 * @param key SelectionKey
	 * @return boolean indicating if the response has been completely sent to the client
	 */
	private boolean channelWriteResponse(
			IncrementalResponseWriter responseWriter, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
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
		}
		
		return sendFinished;
	}
	
	@Override
	public void closeChannel(SelectionKey key) {
		try {
			key.cancel();
			key.channel().close();
		}
		catch(IOException e) {
			logger.warn("Exception while shutting down channel!", e);
		}
		
		// since the channel is closing, we need to also remove the key from the
		// pendingReads and pendingResponses maps
		pendingReads.remove(key);
		pendingResponses.remove(key);
		
		//we have an extra client slot free
		freeClientSlots ++;
	}

	@Override
	public boolean isHandlingClient(SelectionKey key) {
		return socketSelector.keys().contains(key);
	}

	@Override
	public int getFreeSlots() {
		return freeClientSlots;
	}

	/**
	 * Implementing the comparable interface. A WebWorker with a higher number
	 * of free client slots will come before a WebWorker with a lower client of
	 * slots in an ordered list
	 */
	@Override
	public int compareTo(WebWorker o) {
		if( this.freeClientSlots > o.getFreeSlots() ) {
			return -1;
		}
		else if( this.freeClientSlots < o.getFreeSlots() ) {
			return 1;
		}
		
		return 0;
	}

	@Override
	public void shutDown() {
		//set the shutdown flag to true
		shutDown = true;
		
		//wake the selector
		socketSelector.wakeup();
		
	}
}

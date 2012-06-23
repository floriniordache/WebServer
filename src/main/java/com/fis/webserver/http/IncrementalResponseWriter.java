package com.fis.webserver.http;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.fis.webserver.model.http.HttpResponse;
import com.fis.webserver.util.cleaner.Cleaner;

/**
 * Prepares the HttpResponse to be sent back to the client.
 * 
 * It allows for the response to be sent back in chunks, by the use of repeated
 * invocations of incrementalWriteResponse(ByteBuffer)
 * 
 * 
 * @author Florin Iordache
 * 
 */

public class IncrementalResponseWriter {
	
	public static final Logger logger = Logger.getLogger(IncrementalResponseWriter.class);
	
	private HttpResponse response;
	
	//buffer containing the http response header
	ByteBuffer responseHeader;
	
	public IncrementalResponseWriter(HttpResponse response) {
		this.response = response;
		
		//prepare the response header
		responseHeader = response.getRawHeader();
	}

	/**
	 * Writes a portion of the response to the received byte buffer
	 * 
	 * @param destination
	 *         - byte buffer that will receive a part (or all) of the
	 *            serialized response
	 * @return - true if the response has been completely transferred , false
	 *         	  otherwise
	 */
	public boolean incrementalWriteResponse(ByteBuffer destination) {
		//check the remaining space in the destination buffer
		int remainingBufferCapacity = destination.remaining();
		
		boolean processingFinished = false;
		
		try {
			while(remainingBufferCapacity > 0) {
				//check if we finished writing the header
				if( responseHeader.remaining() > 0 ) {
					writeResponseChunk(responseHeader, destination);
				}
				else {
					//check if the response has an associated file channel
					FileChannel responseResourceChannel = response.getContentChannel();

					//read from response file channel directly to destination buffer
					if( responseResourceChannel != null ) {
						responseResourceChannel.read(destination);
						
						//check if we finished writing the response
						if(responseResourceChannel.position() >= responseResourceChannel.size() ) {
							processingFinished = true;
							break;
						}
					}
					else {
						//don't have any response body, signal the end of processing
						processingFinished = true;
						break;
					}
				}
				
				remainingBufferCapacity = destination.remaining();
			}
		}
		catch(Exception e) {
			logger.error("Error while writing response!", e);
		}
		
		if( processingFinished ) {			
			//call the response resource cleaner, if it exists
			Cleaner resourceCleaner = response.getResourceCleaner();
			if(resourceCleaner != null) {
				resourceCleaner.cleanUp();
			}
		}
		
		return processingFinished;
	}

	/**
	 * Attempts to write up to size bytes from the sourceBuffer to the outputBuffer
	 * 
	 * @param sourceBuffer - the source of the read data
	 * @param outputBuffer - destination of the read data
	 * @return number of bytes actually read/written
	 */
	private int writeResponseChunk(ByteBuffer sourceBuffer, ByteBuffer outputBuffer) {
		//write response header bytes
		//make sure we don't overflow the destination
		int numBytes = sourceBuffer.remaining() <= outputBuffer.remaining() ?
				sourceBuffer.remaining() : outputBuffer.remaining();
		for( int i = 0 ; i < numBytes ; i++ ) {
			outputBuffer.put(sourceBuffer.get());
		}
		
		return numBytes;
	}
}

package com.fis.webserver.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

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
	private InputStream headerInputStream;
	
	//indicates that the header has been written
	private boolean headerDone;
	
	// byte buffer used for reading the data from the response input streams
	private byte[] readBuf;
	
	public IncrementalResponseWriter(HttpResponse response) {
		this.response = response;
		
		//prepare the response header
		StringBuilder responseHeader = response.getSerializedHeader();
		
		//create an input stream for the header
		headerInputStream = new ByteArrayInputStream(responseHeader.toString().getBytes());
		
		headerDone = false;
		
		readBuf = new byte[8192];
	}

	/**
	 * Writes a portion of the response to the received byte buffer
	 * 
	 * @param destination
	 *            - byte buffer that will receive a part (or all) of the
	 *            serialized response
	 * @return - true if the response has been completely serialized, false
	 *         otherwise
	 */
	public boolean incrementalWriteResponse(ByteBuffer destination) {
		int numRead = 0;
		
		//check the remaining space in the destination buffer
		int remainingBufferCapacity = destination.remaining();
		
		boolean processingFinished = false;
		
		while(remainingBufferCapacity > 0) {
			//make sure we don't try to read too much
			int bytesToRead = remainingBufferCapacity > readBuf.length ? readBuf.length
					: remainingBufferCapacity;
			
			//first write the header
			if( !headerDone ) {
				numRead = readInputStreamChunk(headerInputStream, destination,
						bytesToRead);
				if (numRead < 0) {
					// no more bytes to read for the header
					// switch the flag indicating the header processing has
					// finished
					headerDone = true;
				}
			}
			else {
				InputStream responseContentStream = response.getContentStream();
				//read from the response's content input stream, if available
				if( responseContentStream == null ) {
					//no content, mark the processing as finished and break
					processingFinished = true;
					
					break;
				}
				else {
					
					numRead = readInputStreamChunk(responseContentStream, destination,
							bytesToRead);
					if(numRead < 0) {
						//finished with the content
						processingFinished = true;
						break;
					}
				}
			}
			
			remainingBufferCapacity = destination.remaining();
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
	 * Attempts to read up to size bytes from the inputStream and writes the
	 * read content into the output buffer
	 * 
	 * @param inputStream - the source of the read data
	 * @param outputBuffer - destination of the read data
	 * @param size - maximum number of bytes to be read
	 * @return number of bytes actually read or -1 in case of any error
	 */
	private int readInputStreamChunk(InputStream inputStream, ByteBuffer outputBuffer, int size ) {
		int numRead = 0;
		try {
			if((numRead = inputStream.read(readBuf, 0 , size)) >= 0) {
				outputBuffer.put(readBuf, 0, numRead);
			}
			else {
				//no more data to read from stream, closing
				inputStream.close();
			}
		} catch(IOException e) {
			logger.error("Error while serializing response content!", e);
			try {
				numRead = -1;
				
				//attempt to close the stream
				inputStream.close();
			} catch (IOException e1) {
				//ignored
			}
		}
		
		return numRead;
	}
	
	/**
	 * Cleans up the resources associated with this response
	 */
	public void cleanup() {
		
	}
}

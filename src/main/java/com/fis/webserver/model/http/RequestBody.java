package com.fis.webserver.model.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.fis.webserver.util.cleaner.Cleaner;
import com.fis.webserver.util.cleaner.TempFileCleaner;

/**
 * Http request body manager.
 * 
 * It offers support for caching small-sized request bodies. After the size
 * exceeds the limit threshold, a temporary file will be used to store the
 * entire request body
 * 
 * @author Florin Iordache
 * 
 */

public class RequestBody {
	public static final Logger logger = Logger.getLogger(RequestBody.class);
	
	//cache for the entityBody
	private ByteBuffer entityBody;
	
	//flag to indicate whether to cache the entity body or not
	private boolean cachedEntityBody;
	
	// output stream where the entity body will be written when its size exceeds
	// the maximum cached size
	private OutputStream entityBodyOutputStream;
	
	// temporary storage for the request body if it's larger than the cached size
	private File tempFile;
	
	// keeps track of the current length of the entity body , so we can check if
	// it is actually longer than the reported content-length header
	private long entityBodyLength;
	
	// max allowed length of the entity body, as per the Content-Length http header
	private long maxEntityBodyLength;

	//input stream of the request entity body(if any)
	private InputStream entityBodyInputStream;

	public RequestBody() {
		cachedEntityBody = true;
		
		entityBodyOutputStream = null;
		
		entityBody = null;
		
		tempFile = null;
		
		entityBodyLength = 0;
		
		maxEntityBodyLength = -1;
	}
	
	public File getTempFile() {
		return tempFile;
	}
	
	public InputStream getEntityBodyInputStream() {
		return entityBodyInputStream;
	}

	public void setEntityBodyInputStream(InputStream entityBodyInputStream) {
		this.entityBodyInputStream = entityBodyInputStream;
	}
	
	public long getMaxEntityBodyLength() {
		return maxEntityBodyLength;
	}

	public void setMaxEntityBodyLength(long maxEntityBodyLength) {
		this.maxEntityBodyLength = maxEntityBodyLength;
	}

	/**
	 * Determines if the request body has reached or exceeded
	 * maxEntityBodyLength bytes
	 * 
	 * Since maxEntityBodyLength is determined by the Content-Length header,
	 * this will indicate that no more bytes should be read for this request
	 * body
	 * 
	 * @return boolean indicating if we already have at least maxEntityBodyLength bytes
	 *         of request body content and should no longer read from the stream
	 */
	public boolean getShouldFinish() {
		return (maxEntityBodyLength > 0 && entityBodyLength >= maxEntityBodyLength);
	}
	
	/**
	 * Called when reading the request body is considered to be finished.
	 * 
	 * Closes the output stream if a temp file is used to store the request body.
	 * 
	 * Initializes an InputStream over the request body as follows:
	 * 	- FileInputStream if a temp file is used
	 * 	- ByeArrayInputStream if the request body is cached into memory
	 * 
	 */
	public void done() {
		if( tempFile != null ) {
			//need to close the output stream
			try {
				entityBodyOutputStream.close();
			} catch (Exception e) {
				logger.warn("Could not properly close the temporary file output stream!", e);
			}
		}
		
		//create an input stream of the body content
		
		//we have a temporary file, create a new FileInputStream
		if(tempFile != null) {
			try {
				entityBodyInputStream = new FileInputStream(tempFile);
			} catch (FileNotFoundException e) {
				logger.error("Could not open the temporary file for reading!", e);
			}
		}
		else {
		//content is small enough to cache to memory, create a new ByteArrayInputStream
			entityBody.flip();
			byte[] writeBuf = new byte[entityBody.limit()];
			entityBody.get(writeBuf);
			
			entityBodyInputStream = new ByteArrayInputStream(writeBuf);
		}
	}
	
	/**
	 * Adds content to this request body
	 * 
	 * @param buf Buffer containing new data to append
	 */
	public void append(ByteBuffer buf) {
		//increment the size of the entity body read so far
		entityBodyLength += buf.limit();
		
		//copy all remainig data to the body part
		
		//check if the body is to be cached
		if( cachedEntityBody ) {
			
			//we need to cache it, create the buffer
			if( entityBody == null ) {
				entityBody = ByteBuffer.allocate(16380);
			}
		
			//check if we have enough space remaining in buffer
			if( entityBody.remaining() < buf.limit() ) {
				//not enough space in memory buffer, write to file

				//create temporary file
				entityBodyOutputStream = createTempFile();
				
				//write to file
				entityBody.flip();
				writeToTempFile(entityBody);
				writeToTempFile(buf);
				
				//nullify the buffer so the space will be collected
				entityBody = null;
				
				//set the cached flag to false
				cachedEntityBody = false;
			}
			else {
				entityBody.put(buf);
			}
		}
		else {
			//write to temp file
			writeToTempFile(buf);
		}
	}
	
	/**
	 * Creates and opens a temporary file for writing
	 * 
	 * @return OutputStream of the temporary file or null in case of an error
	 */
	private OutputStream createTempFile() {
		FileOutputStream tempOS = null;
		try {
			//tempFile = new File(System.currentTimeMillis()+".tmp");
			tempFile = File.createTempFile("FISServer", ".tmp");
			tempOS = new FileOutputStream(tempFile);
		}
		catch(Exception e) {
			logger.error("Could not create temporary file to store request body!", e);
		}
		
		return tempOS;
	}
	
	/**
	 * Writes the content of buf to the temporary file
	 * 
	 * @param buf
	 */
	private void writeToTempFile(ByteBuffer buf) {
		try {
			byte[] writeBuf = new byte[buf.limit()];
			buf.get(writeBuf);
			
			entityBodyOutputStream.write(writeBuf);
			entityBodyOutputStream.flush();
		}
		catch(Exception e) {
			logger.error("Could not write request body to temporary file!", e);
		}
	}

	/**
	 * Builds a Cleaner to be used to cleanup the resources used by this request
	 * body
	 * 
	 * @return Cleaner implementation that will clean up the temp file and close
	 *         the streams
	 */
	public Cleaner getCleaner() {
		return new TempFileCleaner(entityBodyInputStream, tempFile);
	}
}

package com.fis.webserver.model.http;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.fis.webserver.config.WebServerConfiguration;
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
	
	// FileChannel of the file where the entity body will be written when its size exceeds
	// the maximum cached size
	private FileChannel entityBodyFileChannel;
	
	// temporary storage for the request body if it's larger than the cached size
	private File tempFile;
	
	// keeps track of the current length of the entity body , so we can check if
	// it is actually longer than the reported content-length header
	private long entityBodyLength;
	
	// max allowed length of the entity body, as per the Content-Length http header
	private long maxEntityBodyLength;

	public RequestBody() {
		cachedEntityBody = true;
		
		entityBodyFileChannel = null;
		
		entityBody = null;
		
		tempFile = null;
		
		entityBodyLength = 0;
		
		maxEntityBodyLength = -1;
	}
	
	public File getTempFile() {
		return tempFile;
	}
	
	public FileChannel getEntityBodyChannel() {
		return entityBodyFileChannel;
	}

	public void setEntityBodyChannel(FileChannel entityBodyFileChannel) {
		this.entityBodyFileChannel = entityBodyFileChannel;
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
	 * Determines if the request body has exceeded maxEntityBodyLength bytes.
	 * 
	 * It indicates that the client sent more bytes than stated in the Content-Length header
	 * 
	 * @return boolean indicating if entityBodyLength > maxEntityBodyLength
	 */
	public boolean getIsError() {
		return (maxEntityBodyLength > 0 && entityBodyLength > maxEntityBodyLength);
	}
	
	/**
	 * Called when reading the request body is considered to be finished.
	 * 
	 * Prepares the entity body for reading.
	 * 
	 * If the request body is cached, the ByteBuffer holding it will be flipped
	 * to prepare for reading calls.
	 * If the request body is stored in a
	 * temporary file, the associated file channel is prepared for reading by
	 * positioning the current position of the FileChannel to the beginning
	 * 
	 */
	public void done() {
		if( tempFile != null ) {
			//position the entity body channel to the beginning, to prepare for reading
			try {				
				//position to the beginning
				entityBodyFileChannel.position(0);
			} catch (Exception e) {
				logger.warn("Could not prepare the temporary file for reading!", e);
			}
		}
		else {
			//flip the entity body byte buffer to prepare for reading
			if( entityBody != null ) {
				entityBody.flip();
			}
		}
	}
	
	/**
	 * Adds content to this request body
	 * 
	 * @param buf Buffer containing new data to append
	 */
	public void append(ByteBuffer buf) {		
		//copy all remainig data to the body part
		
		//check if the body is to be cached
		if( cachedEntityBody ) {
			
			//we need to cache it, create the buffer
			if( entityBody == null ) {
				//twice the allowed size of a request line
				entityBody = ByteBuffer.allocate( 2 * WebServerConfiguration.MAX_REQUEST_LINE_SIZE );
			}
		
			//check if we have enough space remaining in buffer
			if( entityBody.remaining() < buf.limit() ) {
				//not enough space in memory buffer, write to file

				//create temporary file
				entityBodyFileChannel = createTempFile();
				
				//write to file
				entityBody.flip();
				
				//reset the entity body length
				entityBodyLength = 0;
				writeToTempFile(entityBody);
				writeToTempFile(buf);		
				
				//nullify the buffer so the space will be collected
				entityBody = null;
				
				//set the cached flag to false
				cachedEntityBody = false;
			}
			else {
				entityBody.put(buf);
				
				//update the entity body length so far
				entityBodyLength = entityBody.position();			
			}
		}
		else {
			writeToTempFile(buf);
		}
	}
	
	/**
	 * Creates and opens a temporary file for writing
	 * 
	 * @return FileChannel of the temporary file or null in case of an error
	 */
	private FileChannel createTempFile() {
		FileChannel tempFileChannel = null;
		try {
			//create a temporary file
			tempFile = File.createTempFile("FISServer", ".tmp", new File(WebServerConfiguration.INSTANCE.getTempFolder()));
			
			//open the file and return the associated file channel
			FileOutputStream tempOS = new FileOutputStream(tempFile);
			tempFileChannel = tempOS.getChannel();
		}
		catch(Exception e) {
			logger.error("Could not create temporary file to store request body!", e);
		}
		
		return tempFileChannel;
	}
	
	/**
	 * Writes the content of buf to the temporary file
	 * 
	 * @param buf
	 */
	private void writeToTempFile(ByteBuffer buf) {
		int numWritten = -1;
		try {
			numWritten = entityBodyFileChannel.write(buf);
			
			//update the entity body length to match the current size of the data
			entityBodyLength += numWritten;
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
		return new TempFileCleaner(entityBodyFileChannel, tempFile);
	}
}

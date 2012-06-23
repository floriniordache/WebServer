package com.fis.webserver.util.cleaner;

import java.io.File;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

/**
 * Cleaner implementation that is used to dispose of any temporary files that a
 * request has created on the web server's filesystem
 * 
 * The object will be created using an instance of a file and an open input stream on that file.
 * 
 * On cleanup, it will try to first close the input stream, and then delete the file
 * 
 * @author Florin Iordache
 * 
 */

public class TempFileCleaner implements Cleaner {

	public static final Logger logger = Logger.getLogger(TempFileCleaner.class);
	
	private File tempFile;
	private FileChannel fileChannel;
	
	public TempFileCleaner(FileChannel fileChannel, File tempFile) {
		this.tempFile = tempFile;
		this.fileChannel = fileChannel;
	}
	
	/**
	 * Closes the file channel
	 * deletes the file from the filesystem
	 */
	@Override
	public void cleanUp() {
		//first close the file channel
		closeFileChannel();
		
		//try to delete the file
		deleteTempFile();
	}
	
	/**
	 * Attempts to close the file channel resource
	 */
	private void closeFileChannel() {
		if( fileChannel != null ) {
			try {
				fileChannel.close();
			}
			catch(Exception e) {
				logger.warn("Could not close InputStream!", e);
			}
		}		
	}
	
	/**
	 * Attempts to delete the temporary file
	 */
	private void deleteTempFile() {
		if( tempFile != null && tempFile.exists() && !tempFile.isDirectory() ) {
			if(!tempFile.delete()) {
				logger.warn("Could not delete temporary file " + tempFile.getName() + " from the filesystem !");
			}
		}		
	}

}

package com.fis.webserver.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * Helper class for handling File operations
 * 
 * @author Florin Iordache
 *
 */

public class FileUtils {
	public static final Logger logger = Logger.getLogger(FileUtils.class);
	
	/**
	 * Determines the extension of a file
	 * 
	 * @param file File object
	 * @return String representing the extension of this file
	 */
	public static String getExtension(File file) {
		String extension = "";
		
		if( file != null && !file.isDirectory() ) {
			int dotPosition = file.getName().lastIndexOf(".");
			if(dotPosition >= 0) {
				return file.getName().substring(dotPosition);
			}
		}
		
		return extension;
	}

	/**
	 * Attempts to open a file for reading
	 * 
	 * @param file
	 *            File object
	 * @return a non null InputStream if the file exists and has been
	 *         successfully opened
	 */
	public static InputStream getFileInputStream(File file) {
		InputStream fileInputStream = null;
		
		//attempt to open the file
		try {
			fileInputStream = new FileInputStream(file);
		}
		catch(Exception e) {
			logger.error("Could not open " + file.getName(), e);
		}
		
		return fileInputStream;
	}
}

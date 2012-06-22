package com.fis.webserver.util;

import java.io.File;

/**
 * Helper class for handling File operations
 * 
 * @author Florin Iordache
 *
 */

public class FileUtils {
	
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
}

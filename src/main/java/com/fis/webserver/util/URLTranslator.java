package com.fis.webserver.util;

import java.io.File;

import org.apache.log4j.Logger;

import com.fis.webserver.config.WebServerConfiguration;

/**
 * Helper class to translate the http request path to a filesystem path
 * 
 * @author Florin Iordache
 *
 */

public class URLTranslator {

	public static final Logger logger = Logger.getLogger(URLTranslator.class);
	
	/**
	 * Attempts to find the resource identified by the received url on the file
	 * system . If successful the method will return a valid File object
	 * 
	 * @param url
	 *            - url of the resource that the method is trying to find
	 * @return File object of the identified file in the server's file system
	 */
	public static File mapResource(String url) {
		String serverDocumentRoot = WebServerConfiguration.INSTANCE.getDocRoot();
		
		//map the request to a file system path
		String fileSystemPath = serverDocumentRoot + File.separator + url.replace("/", File.separator);
		
		logger.debug("Attempting to find resource with url=" + url
				+ " file system path=" + fileSystemPath);
		
		//see if the file exists
		File resource = new File( fileSystemPath );
		if( resource.exists() ) {
			return resource;
		}
		
		logger.debug("Resource with url=" + url + " does not exist!");
		//file does not exist
		return null;
	}
}

package com.fis.webserver.config;

import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Singleton holding the mime-types supported by this web server
 * 
 * @author Florin Iordache
 * 
 */

public enum MimeTypes {
	INSTANCE;
	
	public static final String MIME_TYPES_CONFIG_FILE = "mimetypes.config";
	
	public static final String DEFAULT_MIMETYPE = "application/octet-stream";
	
	private ConfigLoader mimeTypesConfig;
	private Logger logger;
	
	private MimeTypes() {
		//initialize logger
		logger = Logger.getLogger(MimeTypes.class);
		
		//load the MIME_TYPES_CONFIG_FILE file containing the supported mime types
		logger.debug("Loading supported mime-types...");
		mimeTypesConfig = new ConfigLoader(MIME_TYPES_CONFIG_FILE);
	}

	/**
	 * Get the mime type of a given extension
	 * 
	 * @param extension
	 *            - extension of the resource
	 * @return String object representing the configured mime-type for that
	 *         extension if extension is not mapped to any mime type, the with
	 *         DEFAULT_MIMETYPE will be returned
	 */
	public static String getMimeType(String extension) {
		Set<String> extensions = INSTANCE.mimeTypesConfig.getPropertyNames();
		String mimeType = DEFAULT_MIMETYPE;

		if( extensions != null ) {
			mimeType = INSTANCE.mimeTypesConfig.getProperty(extension);
			
			if( mimeType == null ) {
				mimeType = DEFAULT_MIMETYPE;
			}
		}
		
		return mimeType;
	}
}

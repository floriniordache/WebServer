package com.fis.webserver.config;

import org.apache.log4j.Logger;

/**
 * Singleton holding the web server configuration information
 * 
 * @author Florin Iordache
 *
 */

public enum WebServerConfiguration {
	INSTANCE;
	
	private Logger logger;
	
	//maximum size of a line in the request header (request line or header)
	public static final int MAX_REQUEST_LINE_SIZE = 8192;
	
	//maximum entity body size -> 10MB
	public static final long MAX_ENTITY_BODY_SIZE = 1020 * 1024 * 10;
	
	//file that contains all server's configuration information
	public static final String WEB_SERVER_CONFIG_FILE = "webserver.properties";
	
	//listen port property
	public static final String LISTEN_PORT_PROPERTY = "listen.port";
	
	//minimum number of workers property
	public static final String WORKERS_MIN_PROPERTY = "server.workers.min"; 

	//maximum number of workers property
	public static final String WORKERS_MAX_PROPERTY = "server.workers.max";
	
	//clients per worker property
	public static final String MAX_CLIENTS_PER_WORKER_PROPERTY = "server.workers.clients.max";
	
	//document root of the server
	public static final String DOCUMENT_ROOT_PROPERTY = "server.document.root";
	
	//temporary folder
	public static final String TEMP_FOLDER_PROPERTY = "server.folder.temp";
	
	private Integer minWorkers;
	private Integer maxWorkers;
	private Integer portNumber;
	private Integer clientsPerWorker;
	private String docRoot;
	private String tempFolder;
	private Boolean configOk;
	
	private WebServerConfiguration() {
		logger = Logger.getLogger(WebServerConfiguration.class);
		
		logger.debug("Loading web server's configuration!");
		
		ConfigLoader serverConfigLoader = new ConfigLoader(WEB_SERVER_CONFIG_FILE);
		
		portNumber = serverConfigLoader.getIntProperty(LISTEN_PORT_PROPERTY);
		minWorkers = serverConfigLoader.getIntProperty(WORKERS_MIN_PROPERTY);
		maxWorkers = serverConfigLoader.getIntProperty(WORKERS_MAX_PROPERTY);
		clientsPerWorker = serverConfigLoader.getIntProperty(MAX_CLIENTS_PER_WORKER_PROPERTY);
		docRoot = serverConfigLoader.getProperty(DOCUMENT_ROOT_PROPERTY);
		tempFolder = serverConfigLoader.getProperty(TEMP_FOLDER_PROPERTY);
		
		if (portNumber == null || minWorkers == null || maxWorkers == null
				|| clientsPerWorker == null || docRoot == null || tempFolder == null) {
			logger.error("Error loading " + WEB_SERVER_CONFIG_FILE + "!");
			
			//mark the config as being incomplete
			configOk = false;
		}
		
		configOk = true;
	}
	
	public int getMinWorkers() {
		return minWorkers;
	}

	public int getMaxWorkers() {
		return maxWorkers;
	}

	public int getClientsPerWorker() {
		return clientsPerWorker;
	}
	
	public int getPortNumber() {
		return portNumber;
	}
	
	public String getDocRoot() {
		return docRoot;
	}
	
	public Boolean isConfigValid() {
		return configOk;
	}
	
	public String getTempFolder() {
		return tempFolder;
	}
}

package com.fis.webserver.config;

import org.apache.log4j.Logger;

import com.fis.webserver.model.WebServerConfiguration;

/**
 * Configuration loader for the web server configuration
 * 
 * @author Florin Iordache
 *
 */

public class WebServerConfigLoader extends ConfigLoader {
	public static final Logger logger = Logger.getLogger(WebServerConfigLoader.class);
	
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
	
	public WebServerConfigLoader() {
		super(WEB_SERVER_CONFIG_FILE);
	}

	/**
	 * Load the server configuration
	 * 
	 * @return a WebServerConfiguration object containing the server config
	 */
	public static WebServerConfiguration loadServerConfig() {
		logger.debug("Loading web server's configuration!");
		
		WebServerConfigLoader serverConfigLoader = new WebServerConfigLoader();
		
		Integer listenPort = serverConfigLoader.getIntProperty(LISTEN_PORT_PROPERTY);
		Integer minWorkers = serverConfigLoader.getIntProperty(WORKERS_MIN_PROPERTY);
		Integer maxWorkers = serverConfigLoader.getIntProperty(WORKERS_MAX_PROPERTY);
		Integer maxClients = serverConfigLoader.getIntProperty(MAX_CLIENTS_PER_WORKER_PROPERTY);
		
		WebServerConfiguration serverConfig = null;
		
		if( listenPort != null ) {
			serverConfig = new WebServerConfiguration(listenPort, minWorkers,
					maxWorkers, maxClients);
		}
		else {
			logger.error("Error loading " + WEB_SERVER_CONFIG_FILE + "!");
		}
		
		return serverConfig;
	}
}

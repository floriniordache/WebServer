package com.fis.webserver;

import org.apache.log4j.Logger;

import com.fis.webserver.config.WebServerConfigLoader;
import com.fis.webserver.core.Server;
import com.fis.webserver.core.impl.IncomingConnectionListenerImpl;
import com.fis.webserver.model.WebServerConfiguration;

/**
 * Web Server's main class
 * ss
 * @author Florin Iordache
 *
 */
public class FISServer implements Server {
	
	public static final Logger logger = Logger.getLogger(FISServer.class);

	private WebServerConfiguration serverConfig;
	
	public FISServer() {
		//load the server configuration		
		serverConfig = WebServerConfigLoader.loadServerConfig();
	}
	
	@Override
	public void startUp() {

		//check if the configuration for the server was successfully loaded
		if(serverConfig == null) {
			logger.error("Server configuration not loaded, server shutting down!");
			
			return;
		}
		
		//start the incoming listener thread
		IncomingConnectionListenerImpl connectionListener = new IncomingConnectionListenerImpl(serverConfig.getPortNumber());
		new Thread(connectionListener).run();
	}
	
	public static void main(String[] args) {
		
		//create a new server instance
		Server server = new FISServer();
		
		//start the server
		server.startUp();
	}
}

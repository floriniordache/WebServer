package com.fis.webserver;

import org.apache.log4j.Logger;

import com.fis.webserver.config.WebServerConfiguration;
import com.fis.webserver.core.ConnectionListener;
import com.fis.webserver.core.ConnectionListenerFactory;
import com.fis.webserver.core.Server;

/**
 * Web Server's main class
 * ss
 * @author Florin Iordache
 *
 */
public class FISServer implements Server {
	
	public static final Logger logger = Logger.getLogger(FISServer.class);
	
	public FISServer() {

	}
	
	@Override
	public void startUp() {

		//check if the configuration for the server was successfully loaded
		if(!WebServerConfiguration.INSTANCE.isConfigValid()) {
			logger.error("Server configuration not loaded, server shutting down!");
			
			return;
		}
		
		//get a connection listener implementation
		ConnectionListener mainConnectionListener = ConnectionListenerFactory.getConnectionListner();
		
		//start the connection listener
		new Thread(mainConnectionListener).run();
	}
	
	public static void main(String[] args) {
		
		//create a new server instance
		Server server = new FISServer();
		
		//start the server
		server.startUp();
	}
}

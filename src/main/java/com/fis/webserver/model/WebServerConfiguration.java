package com.fis.webserver.model;

/**
 * Models the web server configuration information
 * 
 * @author Florin Iordache
 *
 */

public class WebServerConfiguration {
	private int portNumber;
	private int minWorkers;
	private int maxWorkers;
	private int clientsPerWorker;
	
	public WebServerConfiguration(int portNumber, int minWorkers, int maxWorkers, int clientsPerWorker) {
		this.portNumber = portNumber;
		this.minWorkers = minWorkers;
		this.maxWorkers = maxWorkers;
		this.clientsPerWorker = clientsPerWorker;
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
}

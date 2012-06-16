package com.fis.webserver.model;

/**
 * Models the web server configuration information
 * 
 * @author Florin Iordache
 *
 */

public class WebServerConfiguration {
	private int portNumber;

	public WebServerConfiguration(int portNumber) {
		this.portNumber = portNumber;
	}
	
	public int getPortNumber() {
		return portNumber;
	}
}

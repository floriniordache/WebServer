package com.fis.webserver.config;

import java.net.URL;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Helper class to handle configuration files from the classpath
 * 
 * @author Florin Iordache
 * 
 */
public class ConfigLoader {

	public static final Logger logger = Logger.getLogger(ConfigLoader.class);

	// name of the resource that needs loading
	private String resourceName;

	// properties object resulted from reading the resource
	private Properties loadedProperties;

	public ConfigLoader(String resourceName) {
		this.resourceName = resourceName;
		
		loadResource();
	}

	/**
	 * loads the resource from the classpath
	 */
	private void loadResource() {
		if (loadedProperties == null) {
			logger.debug("Loading " + resourceName + " configuration file ");
			try {
				// create a new properties object and try to load it with the
				// contents read from the resource
				Properties props = new Properties();
				URL url = ClassLoader.getSystemResource(resourceName);
				props.load(url.openStream());

				loadedProperties = props;
				
				logger.debug("Configuration file " + resourceName
						+ " successfully loaded!");
			} catch (Exception e) {
				logger.error("Could not read " + resourceName
						+ " resource from classpath! ", e);
			}
		}
	}

	/**
	 * Reads a property from the underlying properties object
	 * 
	 * @param propName
	 *            property that needs to be read
	 * @return property value read from the underlying Properties object or null
	 *         if the Properties object was not properly initialized
	 */
	public String getProperty(String propName) {
		if( loadedProperties != null ) {
			return loadedProperties.getProperty(propName);
		}
		
		return null;
	}
	
	/**
	 * Reads a property from the underlying properties object, performing conversion to integer
	 * @param propName
	 * 		property that needs to be read
	 * @return
	 * 		- integer value of the property if conversion succeeded and property is present
	 * 		- null of conversion failed or property is not present
	 */
	public Integer getIntProperty(String propName) {
		try {
			return Integer.parseInt(getProperty(propName));
		}
		catch(NumberFormatException nfe) {
			logger.warn("Could not convert " + propName + " to integer value! ", nfe);
		}
		
		return null;
	}

	/**
	 * @return a set containing all the property names in this configuration
	 *         file
	 */
	public Set<String> getPropertyNames() {
		if( loadedProperties != null ) {
			return loadedProperties.stringPropertyNames();
		}
		
		return null;
	}
}

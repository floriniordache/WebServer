WebServer
=========

multi-threaded web server written in Java

Running the webserver:
The generated jar already contains a webserver.properties file, the one I used during the development.
It is possible to run the server with a different configuration by placing the folder where the new webserver.properties config file is in the
 java classpath, before the entry of the .jar file, making the JVM to search first in that location like this:
	java -cp <webserver_properties_folder_path>;webserver-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.fis.webserver.FISServer
	

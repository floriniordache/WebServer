WebServer
=========

Multi-threaded web server written in Java

Used tools:
Eclipse 3.6
Maven 2.2.1

Dependencies:
log4j 1.2.16

Summary

It is a limited implementation of the HTTP protocol. The supported HTTP methods are GET, HEAD and POST. The server does not support dynamic content
generation, so even though POST is supported, it behaves more like a GET while temporarily storing the posted data on the web server until the request
is handled. For the moment, the server does not support keep-alive connections.

The web server has a main accepting thread that waits for incoming connections. When a new connection is available, the incoming connection is passed
to a worker thread pool manager that chooses the lowest loaded worker to handle the newly incoming connection. After the connection is accepted, the worker
proceeds to reading the request. If the request is successfully parsed, the worker will try to find an appropriate handler for that request, compile a response
and send it back to the client. The connection is terminated after the response is sent to the client.

While parsing for the request, the workers do some limited request error checking (request header too long, request uri too long, content-length too long, invalid
content-length size).

The webserver needs a webserver.properties file in the classpath to function properly. I have included the default one that I have used during development.
To run the server with a different configuration (to the content root for example), the classpath entry containing the config file folder needs to be specified
before the entry of the .jar file, making the JVM search that location first when loading resource from the classpath:
	java -cp <webserver_properties_folder_path>;webserver-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.fis.webserver.FISServer
	

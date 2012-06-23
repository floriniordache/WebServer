package com.fis.webserver.http;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.fis.webserver.model.http.HttpRequest;

/**
 * Parses a http request taking into account that the request might be available
 * in chunks
 * 
 * @author Florin Iordache
 * 
 */

public class HttpRequestParserWorkState {
	public static final Logger logger = Logger.getLogger(HttpRequestParserWorkState.class);
	
	//regular expression used to parse the request line
	public static final Pattern methodPattern = Pattern.compile("(?is)([^ ]+) ([^ ]+) HTTP/([0-9]+)\\.([0-9]+)");
	
	//regular expressions used to parse http headers and folded headers
	public static final Pattern headerPattern = Pattern.compile("(?is)([^:]+):\\s([^\r]*)");
	public static final Pattern foldedHeaderPattern = Pattern.compile("(?is)[\\s\\t]{1}([^\r]*)");
	
	//possible parsing states
	
	//expecting to parse the first line of the request
	// METHOD URI HTTP/1.0
	public static final int STATE_REQUEST_LINE = 0;
	
	//parsing the request headers
	public static final int STATE_HEADERS = 1;
	
	//parsing the request body
	public static final int STATE_BODY = 2;
	
	private int currentState = STATE_REQUEST_LINE;
	
	//contains the data that was not parsed yet
	private ByteBuffer buf;
	
	//current http request
	private HttpRequest httpRequest;
	
	//last parsed header, so we can handle folded headers
	private String parsedHeaderName;
	private StringBuilder parsedHeaderValue;
	
	//flag indicating parsing has finished;
	private boolean finished;
	
	//default encoding for the http header
	public static final String defaultEncoding = "ISO-8859-1";
	
	//header decoder
	private CharsetDecoder decoder;
	
	public  HttpRequestParserWorkState() {
		buf = ByteBuffer.allocate(8190*2);
		buf.clear();
		
		httpRequest = new HttpRequest();
		
		currentState = STATE_REQUEST_LINE;
		
		decoder =  Charset.forName(defaultEncoding).newDecoder();
		
		finished = false;
	}

	public HttpRequest getHttpRequest() {
		if(finished) {
			return httpRequest;
		}
		return null;
	}
	
	/**
	 * Appends the new data to the internal buffer
	 * Tries to advance in the processing of the available data
	 * 
	 * @return boolean indicating if the request parsing has finished
	 * @param newData
	 */
	public boolean newData(ByteBuffer newData) {		
		try {
			//append the data to the internal buffer
			buf.put(newData);

			//process the available data			
			parseData();
			buf.compact();
		} catch (Exception e) {
			logger.error("Could not parse request!", e);
			finished = true;
		}
		
		//check if we already read the number of bytes reported in Content-Length header
		if (httpRequest.getRequestBody().getShouldFinish()) {
			//finish reading, prevent DoS
			finished = true;
		}
		
		//perform cleanup
		if(finished) {
			//done reading the request body
			httpRequest.getRequestBody().done();
		}
		
		return finished;
	}
	
	public HttpRequest getRequest() {
		return httpRequest;
	}
	
	private void parseData() throws Exception {
		
		buf.flip();

		/*
		 * Header parsing has finished, copy all remainig bytes on the request
		 * to the request body
		 */
		if( currentState == STATE_BODY ) {
			copyBufferToBody();
			
			//no line processing needed
			return;
		}
		
		try {
			//search for the line terminator
			int separatorPos = searchLineTerminator();
			
			while(separatorPos >= 0) {
				/*
				 * Found a line separator Decode the byte sequence using the
				 * static decoder
				 * Depending of the current state of the parser,
				 * try to extract Request method, headers or entity-body
				 */
				
				//copy the line
				byte[] line = new byte[separatorPos - buf.position()];
				buf.get(line);
				
				//decode the line
				String lineStr = decoder.decode(ByteBuffer.wrap(line)).toString();
				
				//process the line
				Matcher methodMatcher = methodPattern.matcher(lineStr);
				Matcher headerMatcher = headerPattern.matcher(lineStr);
				Matcher foldedHeaderMatcher = foldedHeaderPattern.matcher(lineStr);
				
				switch (currentState) {
				case STATE_REQUEST_LINE:
					//try to parse the header
					if( methodMatcher.matches() ) {
						//parsing success, extract the data
						String method = methodMatcher.group(1);
						String URL = methodMatcher.group(2);
						int majorVersion = Integer.parseInt(methodMatcher.group(3));
						int minorVersion = Integer.parseInt(methodMatcher.group(4));
						
						httpRequest.setMethod(method);
						httpRequest.setURL(URL);
						httpRequest.setHttpMajorVersion(majorVersion);
						httpRequest.setHttpMinorVersion(minorVersion);
						
						currentState = STATE_HEADERS;
					}
					else {
						//not a valid request
						throw new Exception("Not a valid http request");
					}
					break;
				case STATE_HEADERS:
					//determine if we have the body separator
					if( "".equals(lineStr) ) {
						//found body separator
						currentState = STATE_BODY;
						
						//save the previous header
						saveParsedHeader();
						
						//if this is a no-content method (GET, HEAD) signal the finish of processing
						if(httpRequest.getContentLength() <= 0) {
							finished = true;
						}
					}
					else if(headerMatcher.matches()) {
						//save the previous parsed header, if available
						saveParsedHeader();
					
						//found header
						parsedHeaderName = headerMatcher.group(1);
						parsedHeaderValue = new StringBuilder(headerMatcher.group(2));
					}
					else if( foldedHeaderMatcher.matches() ) {
						//found folded header
						if( parsedHeaderValue != null ) {
							parsedHeaderValue.append(foldedHeaderMatcher.group(1));
						}
						else {
							throw new Exception("Not a valid http request");
						}
					}
					break;
				default:
					break;
				}
				
				//"eat" the two CR LF bytes
				buf.get();
				buf.get();
				
				// copy the remainder to the request body if we have reached the
				// entity-body state
				if( currentState == STATE_BODY ) {
					copyBufferToBody();
					break;
				}
				else {
					separatorPos = searchLineTerminator();	
				}
			}
		}
		catch(Exception e) {
			logger.error("Error parsing the http request!", e);
		}
	}
	
	/**
	 * Used only when the parsing has reached the request body
	 * Will copy all remaining data in buffer to the request body
	 */
	private void copyBufferToBody() {
		//check if we're parsing the body
		if( currentState == STATE_BODY ) {
			//append buf to the request body of the http request
			httpRequest.getRequestBody().append(buf);
		}
	}

	/**
	 * Saves the values parsed in the parsedHeaderValue* variables in the
	 * request object
	 */
	private void saveParsedHeader() {
		if( parsedHeaderName != null && parsedHeaderValue != null ) {
			httpRequest.addHeader(parsedHeaderName, parsedHeaderValue.toString());
		}
	}

	/**
	 * TODO Change with a better search algorithm
	 * search for header line terminator ( CR LF )
	 * 
	 * @return position of CR byte or -1 if sequence CR LF was not found in this
	 *         buffer
	 */
	private int searchLineTerminator() {
		
		//start with current position of the buffer
		int currentPos = buf.position();
		
		boolean found = false;
		while(!found) {
			
			//terminate if we reached the buffer limit
			if( currentPos >= buf.limit() ) {
				break;
			}
			
			/*
			 * if current byte is CR, also try to match the next byte to LF
			 * break the loop on success
			 */
			if( buf.get(currentPos) == 13 ) {
				if( currentPos + 1 < buf.limit() && buf.get(currentPos + 1) == 10 ) {
					found = true;
					break;
				}
			}
			
			currentPos ++;
		}
		
		//if found flag is set, return the position where the CR LF was found
		if(found) {
			return currentPos;
		}
		
		return -1;
	}
}

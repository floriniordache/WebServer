package com.fis.webserver.http;

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
	
	public static final Pattern methodPattern = Pattern.compile("(?is)([^ ]+) ([^ ]+) HTTP/([0-9]+)\\.([0-9]+)\r\n?");
	public static final Pattern headerPattern = Pattern.compile("(?is)([^:]+):\\s([^\r]*)\r\n?");
	public static final Pattern foldedHeaderPattern = Pattern.compile("(?is)[\\s\\t]{1}([^\r]*)\r\n?");
	public static final Pattern lineSeparatorPattern = Pattern.compile("(?is)\r\n?");
	
	//possible parsing states
	
	//expecting to parse the first line of the request
	// METHOD URI HTTP/1.0
	public static final int STATE_METHOD = 0;
	
	//parsing the request headers
	public static final int STATE_HEADERS = 1;
	
	//parsing the request body
	public static final int STATE_BODY = 2;
	
	private int currentState = STATE_METHOD;
	
	//contains the data that was not parsed yet
	private StringBuilder buf;
	
	//current http request
	private HttpRequest httpRequest;
	
	//last parsed header, so we can handle folded headers
	private String parsedHeaderName;
	private StringBuilder parsedHeaderValue;
	
	//flag indicating parsing has finished;
	private boolean finished;
	
	public  HttpRequestParserWorkState() {
		buf = new StringBuilder();
		
		httpRequest = new HttpRequest();
		
		currentState = STATE_METHOD;
		
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
	public boolean newData(char[] newData) {
		buf.append(newData);
		
		//process the available data
		try {
			parseData();
		} catch (Exception e) {
			logger.error("Could not parse request!", e);
		}
		
		return finished;
	}
	
	public HttpRequest getRequest() {
		return httpRequest;
	}
	
	private void parseData() throws Exception {
		
		//copy the data to the request body
		if( currentState == STATE_BODY ) {
			copyBufferToBody();
			
			//no line processing needed
			return;
		}
				
		//parse the data line by line
		int lineIdx = buf.indexOf("\r\n");
		if( lineIdx < 0 ) {
			//also support \r as line separator
			lineIdx = buf.indexOf("\r");
		}
		
		while(lineIdx >= 0) {
			//get the line, with the \r\n terminator
			lineIdx += 2;
			String line = buf.substring(0, lineIdx);
	
			Matcher methodMatcher = methodPattern.matcher(line);
			Matcher bodyMatcher = lineSeparatorPattern.matcher(line);
			Matcher headerMatcher = headerPattern.matcher(line);
			Matcher foldedHeaderMatcher = foldedHeaderPattern.matcher(line);
			
			switch (currentState) {
			case STATE_METHOD:
				//try to parse the header
				if( methodMatcher.matches() ) {
					//parsing success, extract the data
					String method = methodMatcher.group(1);
					String URI = methodMatcher.group(2);
					int majorVersion = Integer.parseInt(methodMatcher.group(3));
					int minorVersion = Integer.parseInt(methodMatcher.group(4));
					
					httpRequest.setMethod(method);
					httpRequest.setURI(URI);
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
				if( bodyMatcher.matches() ) {
					//found body separator
					currentState = STATE_BODY;
					
					//save the previous header
					saveParsedHeader();
					
					finished = true;
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
			
			//remove processed data from the buffer
			buf.delete(0, lineIdx);
			
			//copy the remainder to the http body
			if( currentState == STATE_BODY ) {
				copyBufferToBody();
				break;
			}
			else {
				//continue line processing
				lineIdx = buf.indexOf("\r\n");
			}
		}
	}
	
	/**
	 * Used only when the parsing has reached the request body
	 * Will copy all remaining data in buffer to the request body
	 */
	private void copyBufferToBody() {
		//check if we're parsing the body
		if( currentState == STATE_BODY ) {
			//copy all remainig data to the body part
			httpRequest.addToRequestBody(buf);
			buf.delete(0, buf.length());
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
}

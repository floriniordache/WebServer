package com.fis.webserver.http;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.fis.webserver.config.WebServerConfiguration;
import com.fis.webserver.http.exceptions.RequestException;
import com.fis.webserver.model.http.HttpRequest;

/**
 * Parses a http request taking into account that the request might be available
 * in chunks
 * 
 * The HttpRequestParser uses three internal states to identify a valid http
 * request : STATE_REQUEST_LINE - the parser is waiting to identify the request
 * line STATE_HEADERS - the request line was successfully parsed previously, and
 * now the parser is identifying request headers STATE_BODY - request line and
 * headers were successfully parsed, parser is reading the entity body of the
 * request
 * 
 * The internal ByteBuffer is used to store the unparsed data. To identify the
 * request parts, a search algorithm is used to find the http line terminator
 * byte sequence CR LF (13 10). After a line is identified, it is decoded using
 * the ISO-8859-1 charset.
 * 
 * After decoding, a set of patterns is applied, depending on the internal
 * parser state. The patterns will try to parse the request method and any
 * headers the request contains
 * 
 * @author Florin Iordache
 * 
 */

public class HttpRequestParser {

	public static final Logger logger = Logger.getLogger(HttpRequestParser.class);
	
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
	
	// holds the a RequestException object, if a problem has been encountered
	// while parsing the request
	private RequestException reqParserException;
	
	public  HttpRequestParser() {
		
		//allocating twice the allowed size of a request line
		buf = ByteBuffer.allocate( 2 * WebServerConfiguration.MAX_REQUEST_LINE_SIZE );
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
	 * @throws URITooLongException if the request uri is too long
	 * 			EntityTooLargeException if the request body is too large for the server to handle
	 * 			HeaderTooLargeException if one of the headers exceeds the max header size
	 * 			BadRequestException if a problem is detected while reading the request, such 
	 * 			as a mismatch between the content-length and the actual size of the sent content
	 */
	public boolean newData(ByteBuffer newData)  {		
		try {
			//append the data to the internal buffer
			buf.put(newData);

			//process the available data
			parseData();
			buf.compact();
		}
		catch(RequestException reqEx) {
			this.reqParserException = reqEx;
			logger.trace("RequestException while processing request!", reqEx);
			finished = true;
		}
		catch (Exception e) {
			logger.error("Unknown error encountered while parsing request!", e);
			finished = true;
		}
		
		//check if we exceeded the number of bytes reported in the Content-Length header
		if (httpRequest.getRequestBody().getIsError()) {
			logger.trace("Request entity body is larger than the value in the Content-Length header, throwing BadRequestException!");
			//finish reading, prevent DoS
			this.reqParserException = new RequestException(RequestException.BAD_REQUEST);
			finished = true;
		}
		else if (httpRequest.getRequestBody().getShouldFinish()) {
			//finish reading request body
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
	
	/**
	 * 
	 * The method that does the actual data parsing.
	 * 
	 * Will incrementally parse the contents of buf ByteBuffer trying to identify a valid Http request.
	 * 
	 * @throws RequestException
	 * @throws Exception
	 */
	private void parseData() throws RequestException, Exception {
		
		buf.flip();

		/*
		 * Header parsing has finished, copy all remainig bytes from the request 
		 * to the request body
		 */
		if( currentState == STATE_BODY ) {
			copyBufferToBody();
			
			//no line processing needed
			return;
		}
		
		//search for the line terminator
		int separatorPos = searchLineTerminator();
		
		//check for errors
		checkForHeaderSizeExceeded(separatorPos);
		
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
					logger.trace("Could not parse Http request line!");
					throw new RequestException(RequestException.BAD_REQUEST);
				}
				break;
			case STATE_HEADERS:
				//determine if we have the body separator
				if( "".equals(lineStr) ) {
					//found body separator
					currentState = STATE_BODY;
					
					//save the previous header
					saveParsedHeader();
					
					// if the Content-Length header was received, check if it
					// does not exceed the max allowed entity size
					if( httpRequest.getContentLength() > WebServerConfiguration.MAX_ENTITY_BODY_SIZE ) {
						logger.trace("Content-Length too large!");
						throw new RequestException(RequestException.ENTITY_TOO_LARGE);
					}
					
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
						
						//throw HeaderTooLargeException if the header exceeds the maximum size
						if( parsedHeaderValue.length() > WebServerConfiguration.MAX_REQUEST_LINE_SIZE ) {
							logger.trace("Header value too large!");
							throw new RequestException(RequestException.HEADER_TOO_LARGE);
						}
					}
					else {
						logger.trace("Could not parse request!");
						throw new RequestException(RequestException.BAD_REQUEST);
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
				
				//check for errors
				checkForHeaderSizeExceeded(separatorPos);
			}
		}
	}
	
	/**
 	 * Checks current found separator position to signal sizing errors
	 * 
	 * An exception will be thrown if:
	 * - the separator was not found and the current buffer size is greater than the MAX_REQUEST_LINE_SIZE,
	 * 		thus indicating that the line will exceed max size
	 * - the separator was found, but the position indicates a line size larger than MAX_REQUEST_LINE_SIZE
	 * 
	 * @throws RequestException
	 */
	private void checkForHeaderSizeExceeded(int separatorPos) throws RequestException {
		if( (separatorPos == -1 && buf.remaining() > WebServerConfiguration.MAX_REQUEST_LINE_SIZE) ||
				separatorPos > WebServerConfiguration.MAX_REQUEST_LINE_SIZE) {
			switch( currentState ) {
			case STATE_HEADERS:
				logger.trace("Header line exceeds max allowed size!");
				throw new RequestException(RequestException.HEADER_TOO_LARGE);
			default:
				logger.trace("Request method line exceeds max allowed size!");
				throw new RequestException(RequestException.URI_TOO_LONG);
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

	public RequestException getException() {
		return reqParserException;
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

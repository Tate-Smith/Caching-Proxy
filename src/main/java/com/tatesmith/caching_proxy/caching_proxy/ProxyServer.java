/*
 * This is the proxy server class, it handles creating the class and taking input from
 * the client and sending it to the origin, and then taking the origins output and sending
 * it back to the client. If the request the client sent, was already in the cache then
 * this server just sends the result from the cache back to the client instead of 
 * querying the origin server.
 */

package com.tatesmith.caching_proxy.caching_proxy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ProxyServer {
	
	private int port;
	private String origin;
	private int originPort;
	private HashMap<String, byte[]> cache;
	// this is the file where the cache is stored
	private static final String CACHE_FILE = "proxyCache.dat";
	
	public ProxyServer() {
		/* constructor */
		cache = new HashMap<>();
		loadCache();
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	
	public void setOriginPort(boolean secure) {
		if (secure) this.originPort = 443;
		else this.originPort = 80;
	}
	
	@SuppressWarnings("unchecked")
	private void loadCache() {
		/*
		 * This private helper function loads all the data from the file storage back into the cache
		 */
		
	    File file = new File(CACHE_FILE);
	    if (!file.exists()) return;
	    
	    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
	        cache = (HashMap<String, byte[]>) ois.readObject();
	        System.out.println("Loaded cache with " + cache.size() + " entries");
	    } catch (Exception e) {
	        System.err.println("Could not load cache: " + e.getMessage());
	    }
	}

	private void saveCache() {
		/*
		 * This helper function saves the cache to the file
		 */
		
	    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CACHE_FILE))) {
	        oos.writeObject(cache);
	    } catch (Exception e) {
	        System.err.println("Could not save cache: " + e.getMessage());
	    }
	}
	
	private byte[] getClientInput(InputStream inputStream) throws IOException {
		/*
		 * This is a private helper function that takes in an input stream and reads every byte from it
		 * until its over and stores it into an array
		 */
		
		byte[] data = new byte[4096];
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int bytes;
		
		// get all the bytes from the input
		while((bytes = inputStream.read(data)) != -1) {
			buffer.write(data, 0, bytes);
		
			String cur = buffer.toString(StandardCharsets.UTF_8);
			// stop taking input after the header (ignore the body)
			if (cur.contains("\r\n\r\n")) break;
		}
		
		String str = buffer.toString(StandardCharsets.UTF_8);
		
		// set the host to the origin
		if (str.contains("Host: ")) str = str.replaceAll("Host:[^\r\n]*", "Host: " + origin);
		
		// set the connection to close, so it doesnt stay open forever
		str = str.replaceAll("Connection: [^\r\n]*", "");
		str = str.replace("\r\n\r\n", "\r\nConnection: close\r\n\r\n");
		
		byte[] input = str.getBytes(StandardCharsets.UTF_8);
		return input;
	}
	
	private byte[] getOriginInput(InputStream inputStream) throws IOException {
		/*
		 * This is a private helper function that takes in an input stream and reads every byte from it
		 * until its over and stores it into an array
		 */
		
		byte[] data = new byte[4096];
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int bytes;
		
		// get all the bytes from the input
		while((bytes = inputStream.read(data)) != -1) {
			buffer.write(data, 0, bytes);
		}
		
		byte[] input = buffer.toByteArray();
		return input;
	}
	
	private String getRequest(byte[] input) {
		/*
		 * This is a helper function that takes in a byte array and decodes it to get the full request the
		 * client is making
		 */
		
		// write function to get the decoded request
		String request = new String(input, StandardCharsets.UTF_8);
		// split the request into lines
		String[] lines = request.split("\r\n");
		String[] parts = lines[0].split(" ");
		// make sure its a valid request (at least 2 parts)
		if (parts.length < 2) return "";
		String line = parts[0] + ":" + parts[1];
		return line;
	}
	
	private byte[] addCacheInfo(boolean hit, byte[] data) {
		/*
		 * This helper function take in a boolean, that if true will add "X-Cache: HIT" to the
		 * data if false, itll add "X-Cache: MISS"
		 */
		
		String info = "X-Cache: MISS";
		if (hit) info = "X-Cache: HIT";
		
		// get the request in String format
		String request = new String(data, StandardCharsets.UTF_8);
		
		// split the string into header and body
		int index = request.indexOf("\r\n\r\n");
		String header = request.substring(0, index);
		String body = request.substring(index + 4);
		
		// add put the string backtogether with the cache info
		String newRequest = header + "\r\n" + info + "\r\n\r\n" + body;
		// return it as bytes
		byte[] newData = newRequest.getBytes(StandardCharsets.UTF_8);
		return newData;
	}
	
	private void origin(byte[] input, String request, boolean get, OutputStream proxyOutput) {
		/*
		 * This method takes in the input byte[], the String request, whether it is a get method or not, 
		 * and the proxy servers output stream, it then creates the socket to the origin server, and 
		 * sends the request to it and retireves the response, caches it then sends it through the proxy 
		 * servers output
		 */
		
		// open up a socket to the origin server
		Socket originServer = null;
		try {
			
			System.out.println("Connecting to: " + origin);
			
			originServer = new Socket(origin, originPort);
			
			// get the input and output streams
			OutputStream originOutput = originServer.getOutputStream();
			InputStream originInput = originServer.getInputStream();
			
			// request the origin for the info
			originOutput.write(input);
			originOutput.flush();
			
			// get the output from the origin server as a byte[]
			byte[] output = getOriginInput(originInput);
				
			// add it to the cache if a GET request then return it to the client
			if (get) {
				cache.put(request, output);
				saveCache();
			}
			proxyOutput.write(addCacheInfo(false, output));
			proxyOutput.flush();
			
			originOutput.close();
			originInput.close();
		} catch (IOException e) {
	        System.err.println("Error connecting to origin: " + e.getMessage());
	        
	        // Send error response to client
	        try {
	            String errorResponse = "HTTP/1.1 502 Bad Gateway\r\n" +
	                                   "Content-Type: text/plain\r\n" +
	                                   "Connection: close\r\n\r\n" +
	                                   "Could not connect to origin server";
	            proxyOutput.write(errorResponse.getBytes());
	            proxyOutput.flush();
	        } catch (IOException ex) {
	            System.err.println("Could not send error to client: " + ex.getMessage());
	        }
	    } finally {
	    	// if there is error close origin
	        if (originServer != null && !originServer.isClosed()) {
	            try {
	                originServer.close();
	            } catch (IOException e) {
	                System.err.println("Error closing origin connection: " + e.getMessage());
	            }
	        }
	    }
	}
	
	public void start() throws InterruptedException, IOException {
		// set up the actual server proxy and make requests to the server
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			// create the server Socket for the servers port
			System.out.println("Server listening on port: " + port);
			// loop through all the requests sent
			while (true) {
				// accept the client connection
				Socket clientSocket = null;
				
				try {
					clientSocket = serverSocket.accept();
					System.out.println("connected");
					
					// get the output and input streams from the client to the proxy server
					OutputStream proxyOutput = clientSocket.getOutputStream();
					InputStream proxyInput = clientSocket.getInputStream();
					
					// get input from the client as a byte[]
					byte[] input = getClientInput(proxyInput);
					
					// get the actual requests from the client, to store in the cache
					String request = getRequest(input);
					
					if (request.isEmpty()) {
						System.out.println("Recieved Empty Request");
						continue;
					}
					
					// check to see if its a GET request (only cache request)
					String method = request.split(":")[0];
					boolean get = method.equalsIgnoreCase("GET");
					
					// check to see if the request is already in the cache
					if (get && cache.containsKey(request)) {
						// if it does return it to the client
						byte[] response = cache.get(request);
						proxyOutput.write(addCacheInfo(true, response));
						proxyOutput.flush();
					}
					else {
						// if not send the rquest through to the origin
						origin(input, request, get, proxyOutput);
					}
					
					proxyOutput.close();
					proxyInput.close();
				
				} catch (IOException e) {
			        System.err.println("Error handling request: " + e.getMessage());
			        // Continue to next request instead of crashing
			    } finally {
			        // close the socket even if there's an error
			        if (clientSocket != null && !clientSocket.isClosed()) {
			            try {
			                clientSocket.close();
			            } catch (IOException e) {
			                System.err.println("Error closing client socket: " + e.getMessage());
			            }
			        }
			    }
			}
		} catch (IOException e) {
		    System.err.println("Server error: " + e.getMessage());
		    e.printStackTrace();
		}
	}
	
	public void clear() {
		/* clear cache function */
		cache.clear();
	    File file = new File(CACHE_FILE);
	    if (file.exists()) {
	        file.delete();
	    }
	    System.out.println("Cache Cleared");
	}
}

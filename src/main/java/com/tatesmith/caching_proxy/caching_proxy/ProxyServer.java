package com.tatesmith.caching_proxy.caching_proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ProxyServer {
	
	private int port;
	private String origin;
	private HashMap<String, byte[]> cache;
	
	public ProxyServer() {
		cache = new HashMap<>();
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setOrigin(String origin) {
		this.origin = origin;
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
			if (cur.contains("\r\n\r\n")) break;
		}
		
		String str = buffer.toString(StandardCharsets.UTF_8);
		
		// set the host to the origin
		if (str.contains("Host:")) str = str.replaceAll("Host:[^\r\n]*", "Host:" + origin);
		
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
		System.out.println(request);
		// split the request into lines
		String[] lines = request.split("\r\n");
		String[] parts = lines[0].split(" ");
		if (parts.length < 2) return "";
		String line = parts[0] + ":" + parts[1];
		// the first line is the actual request
		return line;
	}
	
	private byte[] addCacheInfo(boolean hitOrMiss, byte[] data) {
		/*
		 * This helper function take in a boolean, that if true will add "X-Cache: HIT" to the
		 * data if false, itll add "X-Cache: MISS"
		 */
		
		String info = "X-Cache: MISS";
		if (hitOrMiss) info = "X-Cache: HIT";
		
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
	
	private void origin(byte[] input, String request, String method, OutputStream proxyOutput) throws UnknownHostException, IOException {
		/*
		 * This method takes in the input byte[], the String request, the method, and the proxy servers
		 * output stream, it then creates the socket to the origin server, and sends the request to it
		 * and retireves the response, caches it then sends it through the proxy servers output
		 */
		
		System.out.println("Connecting to: " + origin);
		
		// if not open up a socket to the origin server
		Socket originServer = new Socket(origin, 80);
		
		// get its input and output streams
		OutputStream originOutput = originServer.getOutputStream();
		InputStream originInput = originServer.getInputStream();
		
		System.out.println("input: " + new String(input, StandardCharsets.UTF_8));
		
		// request the origin for the info
		originOutput.write(input);
		originOutput.flush();
		
		// get the output from the origin server as a byte[]
		byte[] output = getOriginInput(originInput);
		System.out.println("output: " + new String(output, StandardCharsets.UTF_8));
			
		// add it to the cache if a GET request then return it to the client
		if (method.equals("GET")) cache.put(request, output);
		proxyOutput.write(addCacheInfo(false, output));
		proxyOutput.flush();
		
		// close all the origin server lines
		originOutput.close();
		originInput.close();
		originServer.close();
	}
	
	public void start() throws InterruptedException {
		// set up the actual server proxy and make requests to the server
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			// create the server Socket for the servers port
			System.out.println("Server listening on port: " + port);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				
				System.out.println("connected");
				
				// get the output and input streams from the client to the proxy server
				OutputStream proxyOutput = clientSocket.getOutputStream();
				InputStream proxyInput = clientSocket.getInputStream();
				
				// get input from the client as a byte[]
				byte[] input = getClientInput(proxyInput);
				
				// get the actual requests from the client, to store in the cache
				String request = getRequest(input);
				
				System.out.println(request);
				
				if (request.isEmpty()) {
					System.out.println("Recieved Empty Request");
					continue;
				}
				
				// check to see if its a GET request (only cache request)
				String method = request.split(":")[0];
				
				// check to see if the request is already in the cache
				if (method.equals("GET") && cache.containsKey(request)) {
					// if it does return it to the client
					byte[] response = cache.get(request);
					proxyOutput.write(addCacheInfo(true, response));
					proxyOutput.flush();
				}
				else {
					origin(input, request, method, proxyOutput);
				}
				
				proxyOutput.close();
				proxyInput.close();
				clientSocket.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}
	
	public void clear() {
		cache.clear();
		System.out.println("Cache Cleared");
	}
}

package com.tatesmith.caching_proxy.caching_proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;

public class ProxyServer {
	
	private int port;
	private String origin;
	private HashMap<String, HttpResponse<String>> cache;
	
	public ProxyServer() {
		cache = new HashMap<>();
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	
	public void start() throws InterruptedException {
		// set up the actual server proxy and make requests to the server
		try {
			// create the server Socket for the servers port
			ServerSocket serverSocket = new ServerSocket(port);
			System.out.println("Server listening on port: " + port);
			Socket clientSocket = serverSocket.accept();
			
			// get the output and input streams from the client
			PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			
			// while still getting input from the client
			String line = input.readLine();
			while (line != null) {
				// first check if this request is already in the cache
				if (cache.containsKey(line)) output.println(line);
				else {
					// otherwise request origin for the data and add it to the cache
					String query = origin + "/" + line;
					
					HttpClient client = HttpClient.newHttpClient();
					
					HttpRequest request = HttpRequest.newBuilder()
							.uri(URI.create(query))
							.timeout(Duration.ofSeconds(10))
							.header("accept", "application./json")
							.build();
					
					// get the response
					HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
					
					// add the reponse to the cache then output it
					cache.put(line, response);
					output.println(line);
				}
				
				// get next input
				line = input.readLine();
			}
			
			// close all
			serverSocket.close();
			clientSocket.close();
			output.close();
			input.close();
			
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public void clear() {
		cache.clear();
		System.out.println("Cache Cleared");
	}
}

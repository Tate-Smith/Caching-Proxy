package com.tatesmith.caching_proxy.caching_proxy;

import java.util.HashMap;

public class ProxyServer {
	
	private int port;
	private String origin;
	private HashMap<String, String> cache;
	
	public ProxyServer() {
		cache = new HashMap<>();
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	
	public void start() {
		// TODO set up the actual server proxy and make requests to the server
		
		
	}
	
	public void clear() {
		cache.clear();
		System.out.println("Cache Cleared");
	}
}

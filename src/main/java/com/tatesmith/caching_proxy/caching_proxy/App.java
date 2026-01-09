package com.tatesmith.caching_proxy.caching_proxy;

public class App {
    public static void main(String[] args) {
    	// intalize a proxy server object
    	ProxyServer server = new ProxyServer();
    	// check if its clearing th eproxy or instantiating one
    	if (args.length == 2) {
    		// clearing the proxy, make sure the format is correct
    		if (!args[0].equals("caching-proxy") || !args[1].equals("--clear-cache")) {
    			System.out.println("Invalid Input, format: caching-proxy --clear-cache");
    			System.exit(1);
    		}
    		
    		// clear the cache
    		server.clear();
    	}
    	else if (args.length == 5) {
	        // get the port number and server from the user
	    	// format: caching-proxy --port <number> --origin <url>
    		if (!args[0].equals("caching-proxy") || !args[2].equals("--port") || !args[4].equals("--origin")) {
    			System.out.println("Invalid Input, format: caching-proxy --port <number> --origin <url>");
    			System.exit(1);
    		}
	    	int port = Integer.valueOf(args[2]);
	    	String origin = args[4];
	    	
	    	// set the port and origin of the server
	    	server.setPort(port);
	    	server.setOrigin(origin);
	    	
	    	//TODO run the actual server proxy
	    	
	    	
    	}
    	else {
    		// otherwise its invalid input
    		System.out.println("Invalid Input");
    		System.exit(1);
    	}
    	
    }
}

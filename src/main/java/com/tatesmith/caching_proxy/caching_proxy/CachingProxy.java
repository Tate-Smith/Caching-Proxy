/*
 * The main function that takes in the command line input and creates the server proxy
 */

package com.tatesmith.caching_proxy.caching_proxy;

import java.io.IOException;

public class CachingProxy {
    public static void main(String[] args) throws IOException {
    	// intalize a proxy server object
    	ProxyServer server = new ProxyServer();
    	// check if its clearing the proxy or instantiating one
    	if (args.length == 1) {
    		// clearing the proxy, make sure the format is correct
    		if (!args[0].equals("--clear-cache")) {
    			System.out.println("Invalid Input, format: caching-proxy --clear-cache");
    			System.exit(1);
    		}
    		
    		// clear the cache
    		server.clear();
    	}
    	else if (args.length == 4) {
	        // get the port number and server from the user
	    	// format: caching-proxy --port <number> --origin <url>
    		if (!args[0].equals("--port") || !args[2].equals("--origin")) {
    			System.out.println("Invalid Input, format: caching-proxy --port <number> --origin <url>");
    			System.exit(1);
    		}
	    	int port = Integer.valueOf(args[1]);
	    	String origin = args[3];
	    	
	    	// remove the http off the front of origin
	    	if (origin.startsWith("http://")) {
	    		origin = origin.substring(7);
	    		server.setOriginPort(false);
	    	}
	    	else if (origin.startsWith("https://")) {
	    		origin = origin.substring(8);
	    		server.setOriginPort(true);
	    	}
	    	
	    	// set the port and origin of the server
	    	server.setPort(port);
	    	server.setOrigin(origin);
	    	
	    	//run the actual server proxy
	    	try {
				server.start();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
	    	
    	}
    	else {
    		// otherwise its invalid input
    		System.out.println("Invalid Input");
    		System.exit(1);
    	}
    	
    }
}

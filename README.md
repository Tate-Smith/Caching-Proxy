# Caching-Proxy

## Project Overview
This project is a caching proxy server that improves performance by reducing redundant calls to origin servers. The user specifies a port and origin server via command line, and the proxy server forwards client requests to the origin server while intelligently caching responses. Subsequent identical requests are served directly from cache, significantly reducing load on the origin server and improving response times.

## Features
- **Smart Caching**: Automatically caches GET request responses to reduce origin server calls
- **HTTP & HTTPS Support**: Works with both HTTP and HTTPS origin servers
- **Persistent Cache**: Cache data persists to disk and survives server restarts
- **Cache Management**: Easy cache clearing via command-line interface
- **Cache Headers**: Adds `X-Cache: HIT` or `X-Cache: MISS` headers to track cache status
- **Error Handling**: Gracefully handles connection errors and invalid requests

## Requirements
- Java 11 or higher
- Maven 3.6 or higher

## Installation

Clone the repository and build the project:

```bash
git clone https://github.com/tatesmith/caching-proxy.git
cd caching-proxy
mvn clean package
```

This creates an executable JAR file in the `target/` directory.

## Usage

### Starting the Proxy Server

```bash
java -jar target/caching-proxy-0.0.1-SNAPSHOT.jar --port <number> --origin <url>
```

**Parameters:**
- `--port`: The port number on which the proxy server will listen
- `--origin`: The URL of the origin server (supports both `http://` and `https://`)

**Examples:**

```bash
# HTTP origin
java -jar target/caching-proxy-0.0.1-SNAPSHOT.jar --port 3000 --origin http://dummyjson.com

# HTTPS origin
java -jar target/caching-proxy-0.0.1-SNAPSHOT.jar --port 3000 --origin https://api.github.com
```

### Testing the Proxy

Once the server is running, make requests through the proxy in a new terminal:

```bash
# First request - fetches from origin (MISS)
curl -i http://localhost:3000/products/1

# Second identical request - serves from cache (HIT)
curl -i http://localhost:3000/products/1
```

Look for the `X-Cache` header in the response:
- `X-Cache: MISS` - Response fetched from origin server
- `X-Cache: HIT` - Response served from cache

### Clearing the Cache

To clear all cached responses:

```bash
java -jar target/caching-proxy-0.0.1-SNAPSHOT.jar --clear-cache
```

## How It Works

1. **Request Reception**: The proxy receives an HTTP request from a client
2. **Cache Check**: 
   - For GET requests, checks if the response is already cached
   - If cached, returns the stored response with `X-Cache: HIT`
   - If not cached, forwards the request to the origin server
3. **Response Caching**: Stores the origin server's response in memory and on disk
4. **Response Delivery**: Sends the response back to the client with appropriate cache header

## Caching Behavior

- **Cached Methods**: Only `GET` requests are cached (safe and idempotent operations)
- **Not Cached**: `POST`, `PUT`, `DELETE`, and other methods always forward to origin
- **Cache Key**: Based on HTTP method and request path
- **Persistence**: Cache is saved to `proxyCache.dat` file

## Project Structure

```
caching-proxy/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/tatesmith/caching_proxy/caching_proxy/
│   │           ├── CachingProxy.java      # Main entry point and CLI handler
│   │           └── ProxyServer.java       # Proxy server logic with caching
│   └── test/
│       └── java/
│           └── com/tatesmith/caching_proxy/caching_proxy/
│               └── Test                   # testcases
├── pom.xml                                # Maven build config
└── README.md                              # Project documentation
```

## Performance Example

```bash
# First request (cache MISS) - ~200ms
time curl -s http://localhost:3000/products/1 > /dev/null

# Second request (cache HIT) - ~5ms 
time curl -s http://localhost:3000/products/1 > /dev/null
```

## Examples

### Basic HTTP Usage

```bash
# Start proxy
java -jar target/caching-proxy-0.0.1-SNAPSHOT.jar --port 3000 --origin http://dummyjson.com

# Test different endpoints
curl http://localhost:3000/products/1
curl http://localhost:3000/products/2
curl http://localhost:3000/users/1
```

## Troubleshooting

### Port Already in Use

```
Error: Address already in use
```

**Solution**: Choose a different port or kill the process:

```bash
# Find the process using the port
lsof -i :3000

# Kill it
kill -9 <PID>
```

### Cannot Connect to Origin

```
Error connecting to origin: Connection refused
```

**Solution**: Verify the origin URL is correct and accessible:

```bash
# Test origin directly
curl http://dummyjson.com
```

### SSL/HTTPS Errors

```
Error connecting to origin: SSL handshake failed
```

**Solution**: Ensure you're using `https://` in the origin URL for secure servers:

```bash
# Correct
--origin https://api.github.com

# Incorrect (will fail for HTTPS servers)
--origin http://api.github.com
```

### Cache File Errors

```
Could not load cache: ...
```

**Solution**: Clear and recreate the cache:

```bash
rm proxyCache.dat
java -jar target/caching-proxy-0.0.1-SNAPSHOT.jar --clear-cache
```

## Technical Details

### Key Design Decisions
- **GET-only caching**: Only idempotent GET requests are cached for safety
- **In-memory + disk storage**: Fast lookups with persistence across restarts
- **Connection: close**: Simplifies socket management
- **Custom X-Cache header**: Transparent cache status for debugging

## Limitations

- Request bodies are ignored for caching
- No cache expiration or TTL mechanism implemented
- Cache size is unlimited (could grow large over time)
- Does not respect origin server cache-control headers

## Acknowledgments

- Built as part of the https://roadmap.sh/projects/caching-server project challenge
- Inspired by real-world reverse proxy implementations like  nginx

## Author

**Tate Smith**
- GitHub: https://github.com/Tate-Smith
- LinkedIn: www.linkedin.com/in/tate-smith-b1a973264

*Last updated: January 2026*

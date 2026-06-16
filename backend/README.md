# APT Search Engine - Backend API

Express.js REST API wrapper for the APT Search Engine Java backend.

## Overview

This backend server provides:
- REST API endpoint for searching
- Connection to Java QueryProcessor
- Result caching for performance
- CORS support for frontend integration
- Health check endpoint

## Prerequisites

- Node.js 14+
- npm
- Java 11+ (for the Java search engine)
- MongoDB (for the Java backend)

## Installation

```bash
cd /home/hosam/APT-project-/backend
npm install
```

## Starting the Server

```bash
npm start
```

The server will start on `http://localhost:8080` (or port specified by `PORT` environment variable).

## API Endpoints

### POST /api/search
Executes a search query.

**Request:**
```json
{
    "query": "your search query"
}
```

**Response (Success):**
```json
{
    "query": "search query",
    "results": [
        {
            "docId": "document-id",
            "title": "Page Title",
            "url": "https://example.com/page",
            "snippet": "Snippet text...",
            "score": 0.9234
        }
    ],
    "cached": false
}
```

**Response (Error):**
```json
{
    "error": "Error message",
    "results": []
}
```

### GET /api/health
Health check endpoint.

**Response:**
```json
{
    "status": "ok",
    "message": "APT Search Backend is running",
    "timestamp": "2026-06-15T12:00:00.000Z"
}
```

### GET /
Server info endpoint.

**Response:**
```json
{
    "message": "APT Search Engine Backend API",
    "endpoints": {
        "search": "POST /api/search",
        "health": "GET /api/health"
    },
    "version": "1.0.0"
}
```

## Configuration

### Environment Variables

```bash
PORT=8080                    # Server port (default: 8080)
JAVA_HOME=/path/to/java     # Java installation path
CLASSPATH=/path/to/jars     # Java classpath for MongoDB drivers
```

### Runtime Options

```bash
PORT=3000 npm start          # Run on port 3000
```

## Architecture

### Request Flow

1. Frontend sends POST request to `/api/search`
2. Backend receives query
3. Backend spawns Java process to execute search
4. Java process queries MongoDB and returns results
5. Backend caches results
6. Backend returns JSON response

### Caching

Results are cached in memory by query string:
- Repeated searches return instantly
- Cache persists for server lifetime
- For production, consider Redis or similar

### Error Handling

- Returns mock data if Java process fails
- Graceful degradation for UI testing
- Comprehensive error logging

## Development

### Debugging

Enable verbose logging:
```bash
DEBUG=* npm start
```

### Testing the API

Using curl:
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query": "search term"}'
```

Using curl (Windows):
```bash
curl -X POST http://localhost:8080/api/search ^
  -H "Content-Type: application/json" ^
  -d "{\"query\": \"search term\"}"
```

### Adding Endpoints

Edit `server.js` to add new endpoints:
```javascript
app.get('/api/suggestions', (req, res) => {
    // Your code here
});
```

## Deployment

### Heroku

```bash
heroku create apt-search-api
heroku config:set PORT=5000
git push heroku main
```

### Docker

Create `Dockerfile`:
```dockerfile
FROM node:14
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 8080
CMD ["npm", "start"]
```

Build and run:
```bash
docker build -t apt-search-api .
docker run -p 8080:8080 apt-search-api
```

## Troubleshooting

### Java Process Won't Start
- Verify Java is installed: `java -version`
- Check JAVA_HOME path
- Verify MongoDB is running
- Check JAR dependencies in classpath

### Connection Refused Errors
- Ensure server is running
- Check port 8080 is not in use: `lsof -i :8080`
- Use different port: `PORT=3000 npm start`

### Slow Responses
- Check Java process logs
- Monitor MongoDB performance
- Check network latency
- Consider caching at frontend level

## Performance Optimization

1. **Result Caching**: Implemented in-memory cache
2. **Process Pooling**: Consider child_process pool for high load
3. **Database Indexing**: Ensure MongoDB indexes are optimal
4. **Connection Pooling**: Use MongoDB connection pool
5. **Query Optimization**: Profile slow queries

## Security

- CORS enabled for all origins (configure for production)
- Input validation on query parameter
- SQL/NoSQL injection protection via parameterized queries
- Rate limiting recommended for production

## License

Same as parent APT project

## Support

Check `frontend/README.md` for overall project documentation.

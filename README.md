# APT Search Engine

A complete, production-ready search engine with a clean, minimal frontend and Java-based backend.

## Features

- **Clean Frontend**: Minimalist white background with dark text, no external JS dependencies
- **Fast Search**: TF-IDF and PageRank-based ranking algorithm
- **Web Crawler**: Multi-threaded crawler for indexing web pages
- **MongoDB Integration**: Stores indexed documents and search metadata
- **REST API**: Easy integration with custom clients
- **Pagination**: Browse through large result sets
- **Configuration UI**: Switch between different backend endpoints

## Quick Start

### Option 1: Using Quick Start Script

**macOS/Linux:**
```bash
chmod +x quickstart.sh
./quickstart.sh
```

**Windows:**
```bash
quickstart.bat
```

Then open: `http://localhost:3000`

### Option 2: Manual Setup

**Frontend:**
```bash
cd frontend
python3 -m http.server 3000
# Open http://localhost:3000
```

**Backend (optional, for live search):**
```bash
cd backend
npm install
npm start
# Backend runs on http://localhost:8080
```

## Documentation

- **[Complete Setup Guide](SETUP_GUIDE.md)** - Detailed setup and troubleshooting
- **[Frontend README](frontend/README.md)** - Frontend architecture and features
- **[Backend README](backend/README.md)** - API documentation and deployment

## Project Structure

```
APT-project-/
├── frontend/              React-free search UI
│   ├── index.html        Main page
│   ├── styles.css        Minimal styling
│   ├── app.js            Search logic
│   └── README.md         Frontend docs
├── backend/              Express API server
│   ├── server.js         REST API
│   ├── package.json      Dependencies
│   └── README.md         Backend docs
├── indexer/              Java search engine
│   ├── QueryProcessor.java
│   ├── InvertedIndex.java
│   └── Ranker.java
├── crawler/              Web crawler
├── Rankers/              Ranking algorithms
├── data/                 Indexed documents
└── SETUP_GUIDE.md        Complete setup guide
```

## Technology Stack

| Component | Technology |
|-----------|-----------|
| **Frontend** | HTML5, CSS3, Vanilla JavaScript |
| **Backend** | Node.js, Express.js |
| **Search** | Java 11+, MongoDB |
| **Ranking** | TF-IDF, PageRank |
| **Crawling** | Multi-threaded Java |

## Features Overview

### Frontend
- Responsive, mobile-friendly design
- Real-time search with loading states
- Pagination (10 results per page)
- Configurable API endpoint
- No external JavaScript frameworks

### Backend
- REST API wrapper around Java search engine
- Result caching for performance
- Health check endpoint
- CORS support
- Graceful error handling

### Search Engine
- Inverted index data structure
- Stop word filtering
- Porter stemming
- Phrase matching
- Page ranking algorithm

### Web Crawler
- Multi-threaded crawling
- robots.txt respect
- URL frontier management
- Duplicate detection

## API Endpoints

### Search
```
POST /api/search
{
    "query": "search terms"
}
```

Returns ranked results with score, title, URL, and snippet.

### Health Check
```
GET /api/health
```

Returns server status.

## Configuration

### Change API Endpoint
1. Open frontend in browser
2. Look for "API Endpoint:" field at top
3. Enter your backend URL
4. Click search

### Frontend Customization
- **Logo**: Edit `frontend/index.html`
- **Colors**: Edit `frontend/styles.css`
- **Results per page**: Edit `frontend/app.js`

## Usage

### Basic Search
Simply type a search query and press Enter or click Search.

### Advanced Search
- Multiple terms: `term1 term2` (AND logic)
- Exact phrase: `"exact phrase"`
- Browse results: Use pagination buttons

### Using with Custom Backend
Update the API endpoint to point to your backend service.

## Performance

- **Frontend Load**: ~100ms (static HTML/CSS/JS)
- **First Search**: 500ms - 2s (Java process startup)
- **Cached Search**: ~50ms
- **Page Navigation**: Instant

## Deployment

### Frontend
Deploy to any static hosting:
- GitHub Pages
- Netlify
- Vercel
- AWS S3

### Backend
Deploy Node.js server to:
- Heroku
- AWS EC2
- DigitalOcean
- Any Node.js host

See [SETUP_GUIDE.md](SETUP_GUIDE.md) for detailed deployment instructions.

## Development

### Prerequisites
- Node.js 14+ (for backend)
- Java 11+ (for search engine)
- MongoDB (for indexing)
- Python 3 (for frontend server)

### Development Workflow
```bash
# Terminal 1: Frontend
cd frontend && python3 -m http.server 3000

# Terminal 2: Backend
cd backend && npm start

# Terminal 3: Java backend (if needed)
cd indexer && java QueryProcessor
```

### Running Tests
```bash
# Backend tests
cd backend && npm test

# Java tests
cd Rankers && javac RankerTest.java && java RankerTest
```

## Troubleshooting

### Frontend shows "Unable to reach search backend"
1. Verify backend is running: `curl http://localhost:8080/api/health`
2. Check API endpoint URL is correct
3. Ensure MongoDB is running (for Java backend)
4. Check browser console (F12) for errors

### Backend won't start
1. Verify Node.js installed: `node --version`
2. Install dependencies: `cd backend && npm install`
3. Check port 8080 not in use: `lsof -i :8080`

### No search results
1. Verify documents are indexed in MongoDB
2. Check Java backend logs
3. Try different search terms
4. Verify MongoDB connection

See [SETUP_GUIDE.md](SETUP_GUIDE.md) for comprehensive troubleshooting.

## Performance Optimization

1. **Caching**: Backend caches repeated queries
2. **Indexing**: MongoDB indexes are crucial
3. **Pagination**: Only load 10 results per page
4. **Minimalism**: Frontend uses no external libraries

## Security

- CORS enabled (configure for production)
- Input validation on queries
- MongoDB connection security
- HTTPS recommended for production

## Roadmap

Future enhancements:
- [ ] Advanced search filters
- [ ] Search suggestions/autocomplete
- [ ] Search analytics
- [ ] Custom ranking weights
- [ ] Export results
- [ ] Dark mode

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Known Issues

- Java process startup adds 500ms-2s latency on first search
- MongoDB must be running for live search
- CORS needs configuration for cross-domain use

## FAQ

**Q: Do I need Java to use the frontend?**
A: No, frontend works standalone with mock results for testing.

**Q: Can I index my own documents?**
A: Yes, use the crawler and indexer in `crawler/` and `indexer/` directories.

**Q: How is search ranking determined?**
A: Combination of TF-IDF (term frequency), PageRank, and phrase matching.

**Q: Can I deploy this to production?**
A: Yes, see SETUP_GUIDE.md for deployment instructions.

**Q: Does it support filters and facets?**
A: Basic support available, can be extended.

## License

[License information if applicable]

## Support

For issues or questions:
1. Check [SETUP_GUIDE.md](SETUP_GUIDE.md)
2. Review specific component README files
3. Check browser console (F12) for errors
4. Check backend logs
5. Review error messages carefully

## Contact

[Contact information if applicable]

---

**Version**: 1.0.0  
**Last Updated**: June 2026  
**Status**: Stable
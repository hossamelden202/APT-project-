# APT Search Engine - Frontend

A clean, minimal search engine frontend with dark text on white background. Built with vanilla HTML, CSS, and JavaScript.

## Features

- Clean and minimal user interface
- Fast, responsive design
- White background with dark text for excellent readability
- Real-time search with pagination
- Error handling and loading states
- Configurable backend API endpoint
- No external dependencies in frontend (pure vanilla JavaScript)

## Files

```
frontend/
├── index.html          Main HTML structure
├── styles.css          Styling (white bg, dark text, minimal design)
├── app.js              Frontend logic and API client
└── README.md          This file
```

## Quick Start

### Option 1: Using a Simple HTTP Server (Recommended)

#### With Python 3:
```bash
cd /home/hosam/APT-project-/frontend
python3 -m http.server 3000
```

Then open `http://localhost:3000` in your browser.

#### With Python 2:
```bash
cd /home/hosam/APT-project-/frontend
python -m SimpleHTTPServer 3000
```

#### With Node.js (if installed):
```bash
cd /home/hosam/APT-project-/frontend
npx http-server -p 3000
```

### Option 2: Direct Browser Access
Simply open `frontend/index.html` directly in your browser (file:// protocol). Note that API calls may fail due to CORS restrictions unless the backend is running.

## Configuration

The frontend requires a backend API endpoint to work. By default, it points to:
```
http://localhost:8080/api/search
```

You can change this in the frontend:
1. Look for the "API Endpoint:" field at the top of the page
2. Enter your backend API URL
3. Click search to test

## Backend Setup

### Prerequisites
- Node.js 14+ (for the Express wrapper)
- Java 11+ (for the search engine)
- MongoDB running on localhost:27017 (for the Java backend)

### Setting up the Backend

1. Install Node dependencies:
```bash
cd /home/hosam/APT-project-/backend
npm install
```

2. Start the backend server:
```bash
npm start
```

The API will be available at `http://localhost:8080`

3. Test the health endpoint:
```bash
curl http://localhost:8080/api/health
```

## API Specification

### Search Endpoint

**Request:**
```
POST /api/search
Content-Type: application/json

{
    "query": "your search query"
}
```

**Response:**
```json
{
    "query": "your search query",
    "results": [
        {
            "docId": "document-id",
            "title": "Document Title",
            "url": "https://example.com/page",
            "snippet": "Preview text from the document...",
            "score": 0.9234
        }
    ],
    "cached": false
}
```

### Health Check Endpoint

**Request:**
```
GET /api/health
```

**Response:**
```json
{
    "status": "ok",
    "message": "APT Search Backend is running",
    "timestamp": "2026-06-15T12:00:00.000Z"
}
```

## Frontend Architecture

### State Management
The app uses a simple state object to manage:
- `query`: Current search query
- `results`: Array of search results
- `currentPage`: Current page number
- `resultsPerPage`: Results per page (default: 10)
- `totalResults`: Total number of results
- `isLoading`: Loading state flag

### Key Functions

- `performSearch()`: Main search handler
- `executeSearch(query)`: Calls the backend API
- `renderResults()`: Renders results for current page
- `updatePagination()`: Updates pagination controls
- `showLoadingState()`: Shows loading spinner
- `showError(message)`: Shows error message

### Styling Approach

The design follows a minimalist approach:
- **Colors**: White background (#ffffff), dark text (#212121)
- **Typography**: System fonts for fast loading
- **Spacing**: Generous padding and margins for readability
- **Interactions**: Smooth transitions and hover states
- **Responsive**: Mobile-friendly layout

## Customization

### Change Colors
Edit `styles.css` and search for color values:
```css
body {
    background-color: #ffffff;  /* Background */
    color: #212121;             /* Text */
}
```

### Change Results Per Page
Edit `app.js`:
```javascript
const state = {
    resultsPerPage: 10,  /* Change this value */
};
```

### Change Logo Text
Edit `index.html`:
```html
<h1 class="logo">Search</h1>  <!-- Change "Search" to your text -->
```

## Troubleshooting

### Issue: "Unable to reach the search backend"
**Solutions:**
1. Verify backend is running: `curl http://localhost:8080/api/health`
2. Check the API endpoint URL is correct
3. Ensure CORS is enabled on the backend
4. Check browser console for detailed errors (F12 → Console)

### Issue: No results displayed
**Solutions:**
1. Try a different search query
2. Check backend logs for errors
3. Verify MongoDB is running (for the Java backend)
4. Ensure the database contains indexed documents

### Issue: Slow search
**Solutions:**
1. The backend caches results - repeated searches are faster
2. For large datasets, consider implementing pagination on the backend
3. Check database performance and indexing

## Performance Tips

1. **Browser Caching**: Results are cached on the backend by query
2. **Pagination**: Only 10 results loaded per page reduces DOM overhead
3. **Lazy Loading**: Consider implementing lazy image loading for future versions
4. **CDN**: Serve CSS/JS from CDN for production

## Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- Mobile browsers (iOS Safari 14+, Chrome Mobile)

## Deployment

### Frontend Deployment
The frontend can be hosted on any static hosting service:
- GitHub Pages
- Netlify
- Vercel
- AWS S3
- Any web server (Apache, Nginx, etc.)

### Backend Deployment
Deploy the Node.js Express server to:
- Heroku
- AWS EC2
- DigitalOcean
- Any Node.js hosting

Update the API endpoint in the frontend configuration.

## Development

### Adding Features

1. **Search Filters**: Add input fields and pass parameters to API
2. **Advanced Search**: Implement query syntax parsing
3. **Recent Searches**: Use localStorage to store search history
4. **Search Analytics**: Send analytics events to backend
5. **Dark Mode**: Add theme toggle to CSS

### Code Structure

```
app.js
├── State management
├── DOM element references
├── Event listeners
├── Search execution
├── Result rendering
└── UI state management
```

## Security Notes

- The frontend validates input before sending to API
- Sanitize results if displaying user-generated content
- Use HTTPS in production
- Implement rate limiting on the backend

## License

Same as parent APT project

## Support

For issues or questions:
1. Check browser console (F12)
2. Check backend logs
3. Review troubleshooting section
4. Check API endpoint is accessible

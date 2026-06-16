#!/usr/bin/env python3
"""
Simple HTTP server for testing the frontend locally.
Serves files from the current directory on port 3000.
"""

import http.server
import socketserver
import os
import sys
from pathlib import Path

PORT = 3000
DIRECTORY = os.getcwd()

class MyHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)
    
    def end_headers(self):
        # Add cache control headers for development
        self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        super().end_headers()

def run_server():
    # Change to frontend directory if we're at project root
    if os.path.exists('frontend'):
        os.chdir('frontend')
        print(f"Changed to frontend directory: {os.getcwd()}")
    
    with socketserver.TCPServer(("", PORT), MyHTTPRequestHandler) as httpd:
        print(f"\n{'='*50}")
        print(f"Search Engine Frontend")
        print(f"{'='*50}\n")
        print(f"Server running at: http://localhost:{PORT}")
        print(f"Serving from: {os.getcwd()}")
        print(f"\nPress Ctrl+C to stop the server\n")
        print(f"{'='*50}\n")
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")
            sys.exit(0)

if __name__ == "__main__":
    run_server()

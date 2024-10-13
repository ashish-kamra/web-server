# Java HTTP Server Implementation

This project implements a basic HTTP server in Java, following the stages outlined in the CodeCrafters HTTP server course (https://app.codecrafters.io/courses/http-server).

## Features

- Handles GET and POST requests
- Supports file serving and creation
- Implements content encoding (currently gzip)
- Handles multiple concurrent connections using a thread pool
- Validates HTTP requests and headers
- Provides appropriate error responses for invalid requests

## Key Components

1. `WebServer`: The main class that initializes the server and manages incoming connections.
2. `ClientHandler`: Handles individual client connections and processes HTTP requests.
3. `RequestHandler`: Parses and validates HTTP requests, routing them to appropriate handlers.
4. `ResponseWriter`: Generates and sends HTTP responses.
5. `FileHandler`: Manages file-related operations (serving and creating files).
6. `ContentEncoder`: Handles content encoding (e.g., gzip compression) for responses.

## How to Run

1. Compile the Java files.
2. Run the `WebServer` class.
3. The server will start on port 8080 by default.
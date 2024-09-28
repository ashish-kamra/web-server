import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
             ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.printf("Listening on port: %d \n", serverSocket.getLocalPort());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.printf("Accepted connection from %s \n", clientSocket.getPort());
                executor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.out.printf("Error occurred while connecting to Port %d or while accepting the connection: %s \n", PORT, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, HTTPConstants.BAD_REQUEST_MESSAGE);
                return;
            }

            System.out.println("Received request: " + requestLine);
            // Validate the request line
            if (!validateRequestLine(requestLine)) {
                sendErrorResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, HTTPConstants.BAD_REQUEST_MESSAGE);
                return;
            }

            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String path = requestParts[1];
            String httpVersion = requestParts[2];

            // Validate path, method and HTTP version
            if (!isValidMethod(method)) {
                sendErrorResponse(writer, HTTPConstants.NOT_IMPLEMENTED_STATUS_CODE, HTTPConstants.NOT_IMPLEMENTED_MESSAGE);
                return;
            }

            if (!isValidHttpVersion(httpVersion)) {
                sendErrorResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Bad Request - Invalid HTTP Version");
                return;
            }

            if (!isValidPath(path)) {
                sendErrorResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, "Bad Request - Invalid Path");
                return;
            }

            // Extract and validate headers
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while (!(headerLine = reader.readLine()).isEmpty()) {
                if (!isValidHeader(headerLine)) {
                    sendErrorResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Bad Request - Invalid Header Format");
                    return;
                }
                String[] headerParts = headerLine.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            }

            System.out.println("Headers: " + headers);

            // Handle the request based on the path and method
            if ("GET".equalsIgnoreCase(method)) {
                handleGetRequest(writer, path);
            } else {
                sendErrorResponse(writer, HTTPConstants.NOT_IMPLEMENTED_STATUS_CODE, HTTPConstants.NOT_IMPLEMENTED_MESSAGE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private boolean validateRequestLine(String requestLine) {
        String[] parts = requestLine.split(" ");
        return parts.length == 3; // Request line must have 3 parts: Method, Path, HTTP Version
    }

    private boolean isValidMethod(String method) {
        return HTTPConstants.HTTP_GET.equalsIgnoreCase(method);
    }

    private boolean isValidHttpVersion(String version) {
        return HTTPConstants.HTTP_VERSION.equalsIgnoreCase(version);
    }

    private boolean isValidHeader(String headerLine) {
        return headerLine.contains(": ");
    }

    private boolean isValidPath(String path) {
        return path.equals("/") || path.startsWith("/echo/");
    }

    private void handleGetRequest(PrintWriter writer, String path) throws IOException {
        if (!path.startsWith("/")) {
            sendErrorResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Bad Request - Invalid URI");
            return;
        }

        if ("/".equals(path)) {
            sendResponse(writer, 200, "OK"); // Default route
        }
        if (path.startsWith("/echo/")) {
            sendResponse(writer, 200, "OK", HTTPConstants.CONTENT_TYPE_TEXT, path.substring(6));
        }

    }

    private void sendResponse(PrintWriter writer, int statusCode, String statusMessage) {
        writer.println(HTTPConstants.HTTP_VERSION + " " + statusCode + " " + statusMessage);
        writer.println("Connection: close");
        writer.println(); //Each header ends with CRLF and the entire header section also ends with CRLF.
    }


    private void sendResponse(PrintWriter writer, int statusCode, String statusMessage, String contentType, String responseBody) {
        writer.println(HTTPConstants.HTTP_VERSION + " " + statusCode + " " + statusMessage);
        writer.println(HTTPConstants.CONTENT_TYPE_HEADER + contentType);
        writer.println(HTTPConstants.CONTENT_LENGTH_HEADER + responseBody.length());
        writer.println("Connection: close");
        writer.println(); //First CRLF marks the end of status line, Second one marks the end of headers
        writer.println(responseBody);
    }

    private void sendErrorResponse(PrintWriter writer, int statusCode, String statusMessage) throws IOException {
        writer.println(HTTPConstants.HTTP_VERSION + " " + statusCode + " " + statusMessage);
        writer.println("Connection: close");
        writer.println();
    }
}
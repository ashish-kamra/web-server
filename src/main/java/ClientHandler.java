import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final RequestHandler requestHandler;
    private final ResponseWriter responseWriter;

    public ClientHandler(Socket clientSocket, RequestHandler requestHandler, ResponseWriter responseWriter) {
        this.clientSocket = clientSocket;
        this.requestHandler = requestHandler;
        this.responseWriter = responseWriter;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                responseWriter.sendResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, HTTPConstants.BAD_REQUEST_MESSAGE);
                return;
            }

            System.out.println("Received request: " + requestLine);
            // Validate the request line
            if (!requestHandler.validateRequestLine(requestLine)) {
                responseWriter.sendResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, HTTPConstants.BAD_REQUEST_MESSAGE);
                return;
            }

            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String path = requestParts[1];
            String httpVersion = requestParts[2];

            // Validate path, method and HTTP version
            if (!requestHandler.isValidMethod(method)) {
                responseWriter.sendResponse(writer, HTTPConstants.NOT_IMPLEMENTED_STATUS_CODE, HTTPConstants.NOT_IMPLEMENTED_MESSAGE);
                return;
            }

            if (!requestHandler.isValidHttpVersion(httpVersion)) {
                responseWriter.sendResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Bad Request - Invalid HTTP Version");
                return;
            }

            if (!requestHandler.isValidPath(path)) {
                responseWriter.sendResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, HTTPConstants.NOT_FOUND_MESSAGE);
                return;
            }

            // Extract and validate headers
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while (!(headerLine = reader.readLine()).isEmpty()) {
                if (!requestHandler.isValidHeader(headerLine)) {
                    responseWriter.sendResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Bad Request - Invalid Header Format");
                    return;
                }
                String[] headerParts = headerLine.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            }

            System.out.println("Headers: " + headers);

            // Handle the request based on the path and method
            if ("GET".equalsIgnoreCase(method)) {
                requestHandler.handleGetRequest(writer, path, headers, clientSocket);
            } else if ("POST".equalsIgnoreCase(method)) {
                requestHandler.handlePostRequest(reader, writer, path, headers);
            } else {
                responseWriter.sendResponse(writer, HTTPConstants.NOT_IMPLEMENTED_STATUS_CODE, HTTPConstants.NOT_IMPLEMENTED_MESSAGE);
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
}
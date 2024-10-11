import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WebServer {
    private static final int PORT = 8080;
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 100;

    private final ExecutorService executor;
    private volatile boolean isRunning = true;

    public WebServer() {
        this.executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy() //If the executor is saturated, this policy will make the main thread execute the task instead of rejecting it.
        );
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.printf("Server started on port %d", PORT);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.printf("Accepted connection from %s \n", clientSocket.getPort());
                    executor.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.out.printf("Error accepting client connection: %s", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.printf("Could not start server: %s", e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        isRunning = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        System.out.println("Server shut down");
    }

    public static void main(String[] args) {
        WebServer server = new WebServer();
        server.start();
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
                sendErrorResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, HTTPConstants.NOT_FOUND_MESSAGE);
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
                handleGetRequest(writer, path, headers);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePostRequest(reader, writer, path, headers);
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
        return HTTPConstants.HTTP_GET.equalsIgnoreCase(method) || HTTPConstants.HTTP_POST.equalsIgnoreCase(method);
    }

    private boolean isValidHttpVersion(String version) {
        return HTTPConstants.HTTP_VERSION.equalsIgnoreCase(version);
    }

    private boolean isValidHeader(String headerLine) {
        return headerLine.contains(": ");
    }

    private boolean isValidPath(String path) {
        return path.equals("/") || path.startsWith("/echo/") || path.equals("/user-agent") || path.startsWith("/files/");
    }

    private void handleGetRequest(PrintWriter writer, String path, Map<String, String> headers) throws IOException {
        if (!path.startsWith("/")) {
            sendErrorResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Bad Request - Invalid URI");
            return;
        }
        System.out.println("path: " + path);
        switch (path) {
            case "/":
                sendResponse(writer, HTTPConstants.OK_STATUS_CODE, HTTPConstants.OK_MESSAGE);
                break;
            case "/user-agent":
                handleUserAgentRequest(writer, headers);
                break;
            default:
                if (path.startsWith("/echo/")) {
                    String message = path.substring(6);
                    sendResponse(writer, HTTPConstants.OK_STATUS_CODE, HTTPConstants.OK_MESSAGE, HTTPConstants.CONTENT_TYPE_TEXT, message);
                } else if (path.startsWith("/files/")) {
                    handleFileRequest(writer, path.substring(7));
                } else {
                    sendErrorResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, HTTPConstants.NOT_FOUND_MESSAGE);
                }
                break;
        }
    }

    private void handlePostRequest(BufferedReader reader, PrintWriter writer, String path, Map<String, String> headers) throws IOException {
        if (path.startsWith("/files/")) {
            String filename = path.substring(7);
            handleFileCreation(reader, writer, filename, headers);
        } else {
            sendErrorResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, HTTPConstants.NOT_FOUND_MESSAGE);
        }
    }

    private void handleFileCreation(BufferedReader reader, PrintWriter writer, String filename, Map<String, String> headers) throws IOException {
        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        if (contentLength == 0) {
            sendErrorResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Missing Content-Length header");
            return;
        }

        char[] buffer = new char[contentLength];
        int bytesRead = reader.read(buffer, 0, contentLength);
        if (bytesRead != contentLength) {
            sendErrorResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Incomplete request body");
            return;
        }

        String content = new String(buffer);
        String filePath = System.getProperty("user.dir") + "/files/" + filename;

        try {
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));
            sendResponse(writer, HTTPConstants.CREATED_STATUS_CODE, HTTPConstants.CREATED_MESSAGE);
        } catch (IOException e) {
            sendErrorResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Failed to create file: " + e.getMessage());
        }
    }

    private void handleFileRequest(PrintWriter writer, String filename) throws IOException {
        File file = new File(System.getProperty("user.dir") + "/files/" + filename);
        if (!file.exists() || !file.isFile()) {
            sendErrorResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, HTTPConstants.NOT_FOUND_MESSAGE);
            return;
        }

        // Use PrintWriter to send headers
        writer.println(HTTPConstants.HTTP_VERSION + " " + HTTPConstants.OK_STATUS_CODE + " " + HTTPConstants.OK_MESSAGE);
        writer.println(HTTPConstants.CONTENT_TYPE_HEADER + HTTPConstants.CONTENT_TYPE_OCTET_STREAM);
        writer.println(HTTPConstants.CONTENT_LENGTH_HEADER + file.length());
        writer.println("Connection: close");
        writer.println();
        writer.flush();

        // Switch to OutputStream for binary data (file content)
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             OutputStream out = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    private void handleUserAgentRequest(PrintWriter writer, Map<String, String> headers) {
        String userAgent = headers.getOrDefault("User-Agent", "Not provided");
        sendResponse(writer, HTTPConstants.OK_STATUS_CODE, HTTPConstants.OK_MESSAGE, HTTPConstants.CONTENT_TYPE_TEXT, userAgent);
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
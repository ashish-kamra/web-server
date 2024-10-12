import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class RequestHandler {
    private final ResponseWriter responseWriter;
    private final FileHandler fileHandler;

    public RequestHandler(ResponseWriter responseWriter, FileHandler fileHandler) {
        this.responseWriter = responseWriter;
        this.fileHandler = fileHandler;
    }

    public boolean validateRequestLine(String requestLine) {
        String[] parts = requestLine.split(" ");
        return parts.length == 3; // Request line must have 3 parts: Method, Path, HTTP Version
    }

    public boolean isValidMethod(String method) {
        return HTTPConstants.HTTP_GET.equalsIgnoreCase(method) || HTTPConstants.HTTP_POST.equalsIgnoreCase(method);
    }

    public boolean isValidHttpVersion(String version) {
        return HTTPConstants.HTTP_VERSION.equalsIgnoreCase(version);
    }

    public boolean isValidHeader(String headerLine) {
        return headerLine.contains(": ");
    }

    public boolean isValidPath(String path) {
        return path.equals("/") || path.startsWith("/echo/") || path.equals("/user-agent") || path.startsWith("/files/");
    }

    public void handleGetRequest(PrintWriter writer, String path, Map<String, String> headers, Socket clientSocket) throws IOException {
        if (!path.startsWith("/")) {
            responseWriter.sendResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Bad Request - Invalid URI");
            return;
        }
        System.out.println("path: " + path);
        switch (path) {
            case "/":
                responseWriter.sendResponse(writer, HTTPConstants.OK_STATUS_CODE, HTTPConstants.OK_MESSAGE);
                break;
            case "/user-agent":
                handleUserAgentRequest(writer, headers);
                break;
            default:
                if (path.startsWith("/echo/")) {
                    String message = path.substring(6);
                    responseWriter.sendResponse(writer, HTTPConstants.OK_STATUS_CODE, HTTPConstants.OK_MESSAGE, HTTPConstants.CONTENT_TYPE_TEXT, message);
                } else if (path.startsWith("/files/")) {
                    fileHandler.handleFileRequest(writer, path.substring(7), clientSocket);
                } else {
                    responseWriter.sendResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, HTTPConstants.NOT_FOUND_MESSAGE);
                }
                break;
        }
    }

    public void handlePostRequest(BufferedReader reader, PrintWriter writer, String path, Map<String, String> headers) throws IOException {
        if (path.startsWith("/files/")) {
            String filename = path.substring(7);
            fileHandler.handleFileCreation(reader, writer, filename, headers);
        } else {
            responseWriter.sendResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, HTTPConstants.NOT_FOUND_MESSAGE);
        }
    }

    private void handleUserAgentRequest(PrintWriter writer, Map<String, String> headers) {
        String userAgent = headers.getOrDefault("User-Agent", "Not provided");
        responseWriter.sendResponse(writer, HTTPConstants.OK_STATUS_CODE, HTTPConstants.OK_MESSAGE, HTTPConstants.CONTENT_TYPE_TEXT, userAgent);
    }
}


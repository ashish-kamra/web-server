import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
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

    public void handleGetRequest(PrintWriter writer, OutputStream outputStream, String path, Map<String, String> headers, Socket clientSocket) throws IOException {
        if (!path.startsWith("/")) {
            responseWriter.sendResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Bad Request - Invalid URI");
            return;
        }

        String contentEncoding = getEncoding(headers.get("Accept-Encoding"));
        System.out.println("path: " + path);
        switch (path) {
            case "/" -> responseWriter.sendResponse(writer, HTTPConstants.OK_STATUS_CODE, HTTPConstants.OK_MESSAGE);
            case "/user-agent" -> handleUserAgentRequest(writer, outputStream, headers, contentEncoding);
            default -> {
                if (path.startsWith("/echo/")) {
                    String message = path.substring(6);
                    responseWriter.sendResponse(writer, outputStream, HTTPConstants.OK_STATUS_CODE, HTTPConstants.OK_MESSAGE, HTTPConstants.CONTENT_TYPE_TEXT, message, contentEncoding);
                } else if (path.startsWith("/files/")) {
                    fileHandler.handleFileRequest(writer, path.substring(7), clientSocket, contentEncoding);
                } else {
                    responseWriter.sendResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, HTTPConstants.NOT_FOUND_MESSAGE);
                }
            }
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

    private void handleUserAgentRequest(PrintWriter writer, OutputStream outputStream, Map<String, String> headers, String contentEncoding) throws IOException {
        String userAgent = headers.getOrDefault("User-Agent", "Not provided");
        responseWriter.sendResponse(writer, outputStream, HTTPConstants.OK_STATUS_CODE, HTTPConstants.OK_MESSAGE, HTTPConstants.CONTENT_TYPE_TEXT, userAgent, contentEncoding);
    }

    private String getEncoding(String acceptEncoding) {
        if (acceptEncoding == null) {
            return null;
        }
        String[] encodings = acceptEncoding.split(",");
        for (String encoding : encodings) {
            String cleanEncoding = encoding.trim().split(";")[0];
            if (ContentEncoder.supportsEncoding(cleanEncoding)) {
                return cleanEncoding;
            }
        }
        return null;
    }
}


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class FileHandler {
    private final ResponseWriter responseWriter;

    public FileHandler(ResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    public void handleFileCreation(BufferedReader reader, PrintWriter writer, String filename, Map<String, String> headers) throws IOException {
        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        if (contentLength == 0) {
            responseWriter.sendResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Missing Content-Length header");
            return;
        }

        char[] buffer = new char[contentLength];
        int bytesRead = reader.read(buffer, 0, contentLength);
        if (bytesRead != contentLength) {
            responseWriter.sendResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Incomplete request body");
            return;
        }

        String content = new String(buffer);
        String filePath = System.getProperty("user.dir") + "/files/" + filename;

        try {
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));
            responseWriter.sendResponse(writer, HTTPConstants.CREATED_STATUS_CODE, HTTPConstants.CREATED_MESSAGE);
        } catch (IOException e) {
            responseWriter.sendResponse(writer, HTTPConstants.BAD_REQUEST_STATUS_CODE, "Failed to create file: " + e.getMessage());
        }
    }

    public void handleFileRequest(PrintWriter writer, String filename, Socket clientSocket) throws IOException {
        File file = new File(System.getProperty("user.dir") + "/files/" + filename);
        if (!file.exists() || !file.isFile()) {
            responseWriter.sendResponse(writer, HTTPConstants.NOT_FOUND_STATUS_CODE, HTTPConstants.NOT_FOUND_MESSAGE);
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
}
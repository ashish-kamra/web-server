import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class ResponseWriter {

    public void sendResponse(PrintWriter writer, int statusCode, String statusMessage) {
        writer.println(HTTPConstants.HTTP_VERSION + " " + statusCode + " " + statusMessage);
        writer.println("Connection: close");
        writer.println(); //Each header ends with CRLF and the entire header section also ends with CRLF.
    }

    public void sendResponse(PrintWriter writer, OutputStream outputStream, int statusCode, String statusMessage, String contentType, String responseBody, String contentEncoding) throws IOException {
        byte[] responseBytes = responseBody.getBytes();

        if (ContentEncoder.supportsEncoding(contentEncoding)) {
            responseBytes = ContentEncoder.encode(responseBytes, contentEncoding);
        }
        writer.println(HTTPConstants.HTTP_VERSION + " " + statusCode + " " + statusMessage);
        writer.println(HTTPConstants.CONTENT_TYPE_HEADER + contentType);
        writer.println(HTTPConstants.CONTENT_LENGTH_HEADER + responseBytes.length);
        if (ContentEncoder.supportsEncoding(contentEncoding)) {
            writer.println("Content-Encoding: " + contentEncoding);
        }
        writer.println("Connection: close");
        writer.println(); //First CRLF marks the end of status line, Second one marks the end of headers
        writer.flush();

        outputStream.write(responseBytes);
        outputStream.flush();
    }
}
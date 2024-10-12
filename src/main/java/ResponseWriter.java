import java.io.PrintWriter;

public class ResponseWriter {

    public void sendResponse(PrintWriter writer, int statusCode, String statusMessage) {
        writer.println(HTTPConstants.HTTP_VERSION + " " + statusCode + " " + statusMessage);
        writer.println("Connection: close");
        writer.println(); //Each header ends with CRLF and the entire header section also ends with CRLF.
    }

    public void sendResponse(PrintWriter writer, int statusCode, String statusMessage, String contentType, String responseBody) {
        writer.println(HTTPConstants.HTTP_VERSION + " " + statusCode + " " + statusMessage);
        writer.println(HTTPConstants.CONTENT_TYPE_HEADER + contentType);
        writer.println(HTTPConstants.CONTENT_LENGTH_HEADER + responseBody.length());
        writer.println("Connection: close");
        writer.println(); //First CRLF marks the end of status line, Second one marks the end of headers
        writer.println(responseBody);
    }
}
public final class HTTPConstants {
    private HTTPConstants() {
    }

    public static final String CONTENT_TYPE_HEADER = "Content-Type: ";
    public static final String CONTENT_LENGTH_HEADER = "Content-Length: ";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String CRLF = "\r\n";
    public static final int BAD_REQUEST_STATUS_CODE = 400;
    public static final int NOT_FOUND_STATUS_CODE = 400;
    public static final String BAD_REQUEST_MESSAGE = "Bad Request";
    public static final int NOT_IMPLEMENTED_STATUS_CODE = 501;
    public static final String NOT_IMPLEMENTED_MESSAGE = "Not Implemented";
    public static final String HTTP_GET = "GET";
    public static final String HTTP_VERSION = "HTTP/1.1";
}

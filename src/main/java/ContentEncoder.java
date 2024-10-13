import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class ContentEncoder {
    public static boolean supportsEncoding(String encoding) {
        return "gzip".equalsIgnoreCase(encoding);
    }

    public static OutputStream getEncodedOutputStream(OutputStream out, String encoding) throws IOException {
        if ("gzip".equalsIgnoreCase(encoding)) {
            return new GZIPOutputStream(out);
        }
        return out;
    }

    public static byte[] encode(byte[] data, String encoding) throws IOException {
        if (!supportsEncoding(encoding)) {
            return data;
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (OutputStream encodedStream = getEncodedOutputStream(byteStream, encoding)) {
            encodedStream.write(data);
        }
        return byteStream.toByteArray();
    }
}
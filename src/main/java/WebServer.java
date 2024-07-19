import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.printf("Listening on port: %d \n", serverSocket.getLocalPort());
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.printf("Accepted connection from %s \n", clientSocket.getPort());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
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
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             PrintWriter writer = new PrintWriter(outputStream, true)) {

            String readLine = reader.readLine();
            if(readLine != null) {
                System.out.printf("Received: %s \n", readLine);
                String response = "HTTP/1.1 200 OK\r\n\r\nPort: " + clientSocket.getPort();
                writer.println(response);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}

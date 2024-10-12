import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WebServer {
    private static final int PORT = 8080;
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 100;

    private final ExecutorService executor;
    private volatile boolean isRunning = true;

    private final ResponseWriter responseWriter;
    private final RequestHandler requestHandler;
    private final FileHandler fileHandler;

    public WebServer() {
        this.executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy() //If the executor is saturated, this policy will make the main thread execute the task instead of rejecting it.
        );
        this.responseWriter = new ResponseWriter();
        this.fileHandler = new FileHandler(responseWriter);
        this.requestHandler = new RequestHandler(responseWriter, fileHandler);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.printf("Server started on port %d \n", PORT);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.printf("Accepted connection from %s \n", clientSocket.getPort());
                    executor.submit(new ClientHandler(clientSocket, requestHandler, responseWriter));
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
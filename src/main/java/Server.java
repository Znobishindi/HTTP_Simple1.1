import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    final ExecutorService threadPool = Executors.newFixedThreadPool(64);

    private final static Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public void listen(int port) {
        try (final ServerSocket server = new ServerSocket(port)) {
            while (true) {
                final Socket socket = server.accept();

                ClientHandler client = new ClientHandler(socket);

                threadPool.submit(client);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        if (handlers.containsKey(method)) {
            handlers.get(method).put(path, handler);
        } else {
            handlers.put(method, new ConcurrentHashMap<>(Map.of(path, handler)));
        }
    }

    public static Map<String, Map<String, Handler>> getHandlers() {
        return handlers;
    }
}
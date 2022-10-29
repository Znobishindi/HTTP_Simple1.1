import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public void listen(int port) {
        try (final ServerSocket server = new ServerSocket(port)) {
            while (true) {
                    final Socket socket = server.accept();
                    final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final var out = new BufferedOutputStream(socket.getOutputStream());


//                    clientHandler(socket, in, out);
                    Runnable task = () -> clientHandler(socket, in, out);
                    threadPool.execute(task);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }


    protected void clientHandler(Socket socket, BufferedReader in, BufferedOutputStream out) {
        final String requestLine;
        try {
            requestLine = in.readLine();

            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                socket.close();
                in.close();
                out.close();
            }

            final String path = parts[1];
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                socket.close();
                in.close();
                out.close();
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                socket.close();
                in.close();
                out.close();
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

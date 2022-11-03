import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;

    final static List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 final BufferedOutputStream out = new BufferedOutputStream((socket.getOutputStream()))
            ) {
                Request request = createRequest(in,out);
                Handler handler = Server.getHandlers().get(request.getMethod()).get(request.getPath());


                if (handler == null) {
                    Path parent = Path.of(request.getPath()).getParent();
                    handler = Server.getHandlers().get(request.getMethod()).get(parent.toString());
                    if (handler == null) {
                        veryBadResponse404(out);
                        return;
                    }
                }
                handler.handle(request, out);
                goodResponse200(request, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static void goodResponse200(Request request, BufferedOutputStream out) {
        try {
            final var filePath = Path.of(".", "public", request.getPath());
            final String mimeType = Files.probeContentType(filePath);
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

    private static void veryBadResponse404(BufferedOutputStream out) {
        try {
            out.write((
                    """
                            HTTP/1.1 404 Not Found\r
                            Content-Length: 0\r
                            Connection: close\r
                            \r
                            """
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     private Request createRequest(BufferedReader in, BufferedOutputStream out) throws IOException {
        final String requestLine = in.readLine();
        final var parts = requestLine.split(" ");
        if (parts.length != 3) {
            socket.close();
        }
        final String path = parts[1];

        if (!validPaths.contains(path)) {
            veryBadResponse404(out);
        }
        String line;
        Map<String, String> headers = new HashMap<>();

        while (!(line = in.readLine()).equals("")) {
            int indexSeparator = line.indexOf(":");
            String name = line.substring(0, indexSeparator);
            String value = line.substring(indexSeparator + 2);
            headers.put(name, value);
        }
        Request request = new Request(parts[0], parts[1], headers, socket.getInputStream());
        out.flush();
        return request;
    }
}


import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket socket;

    final static List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (final BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
             final BufferedOutputStream out = new BufferedOutputStream((socket.getOutputStream()))
        ) {
            Request request = createRequest(in, out);
            if (request == null) throw new AssertionError();
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
        } catch (URISyntaxException e) {
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

    private Request createRequest(BufferedInputStream in, BufferedOutputStream out) throws IOException, URISyntaxException {
        final int limit = 4096;

        in.mark(limit);
        final byte[] buffer = new byte[limit];

        final int read = in.read(buffer);

        final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};

        final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);

        if (requestLineEnd == -1) {
            veryBadResponse404(out);
            return null;
        }
        final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

        if (requestLine.length != 3) {
            veryBadResponse404(out);
            return null;
        }

        final String method = requestLine[0];

        final String path = requestLine[1];
        if (!requestLine[1].startsWith("/")) {
            veryBadResponse404(out);
            return null;
        }
        final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final int headersStart = requestLineEnd + requestLineDelimiter.length;
        final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            veryBadResponse404(out);
            return null;
        }

        in.reset();
        in.skip(headersStart);

        final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
        final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        String body = null;
        List<NameValuePair> postParams = new ArrayList<>();

        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            final Optional<String> contentLength = extractHeader(headers, "Content-Length");

            if (contentLength.isPresent()) {
                final int length = Integer.parseInt(contentLength.get());
                final byte[] bodyBytes = in.readNBytes(length);
                body = new String(bodyBytes);
                final Optional<String> contentType = extractHeader(headers, "Content-Type");
                //Задание со *
                if (contentType.isPresent()) {
                    final String type = contentType.get();
                    if (type.equals("application/x-www-form-urlencoded")) {
                        postParams = URLEncodedUtils.parse(new URI("?" + body), StandardCharsets.UTF_8);
                    }
                }
            }
        }

        Request request = new Request(method, path, headers, body, postParams);

        try {
            request.setQueryParams(URLEncodedUtils.parse(new URI(path), StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        System.out.println(request);
        System.out.println(request.getQueryParam("value"));
        out.flush();

        return request;
    }

    private int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}


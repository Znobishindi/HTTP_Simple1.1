

public class Main {
    final static int PORT = 9999;

    public static void main(String[] args) {
        final Server server = new Server();

        for (String validPath : ClientHandler.validPaths) {
            server.addHandler("GET", validPath, ClientHandler::goodResponse200
            );
        }

        server.addHandler("POST", "/resources.html", ClientHandler::goodResponse200
        );

        server.listen(PORT);
    }
}

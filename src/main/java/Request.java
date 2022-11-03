import java.io.InputStream;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String,String> headers;
    private final InputStream body;


    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Request(String method, String path, Map<String, String> headers, InputStream body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
    }
}

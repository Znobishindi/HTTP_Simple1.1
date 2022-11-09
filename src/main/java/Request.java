import org.apache.http.NameValuePair;

import java.util.List;
import java.util.stream.Collectors;

public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private String body;
    private List<NameValuePair> queryParams;
    private final List<NameValuePair> postParams;

    private String printHeaders() {
        String head = null;
        for (int i = 0; i < headers.size(); i++) {
            head += (headers.get(i) + "\n");
        }
        return head;
    }

    @Override
    public String toString() {
        String printRequest = null;
        if (postParams.isEmpty()) {
            printRequest = "Request:" + "\n" +
                    "method= " + method + "\n" +
                    "path= " + path + "\n" +
                    "headers= " + "\n" + printHeaders() + "\n" +
                    "body= " + "\n" + body + "\n" +
                    "queryParams= " + queryParams;
        } else {
            printRequest = "Request:" + "\n" +
                    "method= " + method + "\n" +
                    "path= " + path + "\n" +
                    "headers= " + "\n" + printHeaders() + "\n" +
                    "postParams= " + postParams + "\n" +
                    "queryParams= " + queryParams + "\n";

        }
        return printRequest;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(List<NameValuePair> queryParams) {
        this.queryParams = queryParams;
    }

    public String getQueryParam(String param) {
        return queryParams.stream()
                .filter(p -> p.getName().equals(param))
                .map(p -> p.getValue())
                .collect(Collectors.joining(", "));
    }


    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Request(String method, String path, List<String> headers, String body,List<NameValuePair> postParams) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.postParams = postParams;
    }

}

package dk.kofoed.proxy.exception;

public class ProxyClientException extends RuntimeException {

    private int statusCode;
    private String responseBody;
    
    public ProxyClientException(String message, int status) {
        super(message);
        this.statusCode = status;
        this.responseBody = "{}";
    }

    public ProxyClientException(String message, String body, int status) {
        super(message);
        this.statusCode = status;
        this.responseBody = body;
    }
    
    public int getStatusCode() {
        return this.statusCode;
    }

    public String getResponseBody() {
        return this.responseBody;
    }
    
}

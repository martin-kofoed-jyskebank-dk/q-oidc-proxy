package dk.kofoed.proxy.exception;

public class OidcProviderException extends RuntimeException {

    private int errorCode;

    public OidcProviderException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return this.errorCode;
    }
    
}

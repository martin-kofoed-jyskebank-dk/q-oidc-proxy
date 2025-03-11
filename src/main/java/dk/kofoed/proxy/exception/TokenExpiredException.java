package dk.kofoed.proxy.exception;

public class TokenExpiredException extends Exception {

    private String refreshToken;

    public TokenExpiredException(String message, String refreshToken) {
        super(message);
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
    
}

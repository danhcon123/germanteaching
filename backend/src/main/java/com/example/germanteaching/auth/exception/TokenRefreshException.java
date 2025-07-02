package com.example.germanteaching.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a refresh-token operation (lookup, rotation, revocation) fails.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TokenRefreshException extends RuntimeException{
    // Avoid getting compatibility warnings, cause RuntimeException is Serializable
    private static final long serialVersionUID = 1L;
    
    private final String token;

    /**
     * @param token   the refresh token value that triggered the error
     * @param message details on why the operation failed
     */
    public TokenRefreshException(String token, String message) {
        super(String.format("Failed for [%s]: %s", token, message));
        this.token = token;
    }

    /**
     * @param token   the refresh token value that triggered the error
     * @param message details on why the operation failed
     * @param cause   the underlying cause
     */
    public TokenRefreshException(String token, String message, Throwable cause) {
        super(String.format("Failed for [%s]: %s", token, message), cause);
        this.token = token;
    }

    public String getToken(){
        return token;
    }
}

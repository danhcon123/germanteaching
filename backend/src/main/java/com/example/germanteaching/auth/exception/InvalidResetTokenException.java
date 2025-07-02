package com.example.germanteaching.auth.exception;

/**
 * Thrown when password reset token is invalid or expired
 */
public class InvalidResetTokenException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public InvalidResetTokenException(String message) {
        super(message);
    }
    
    public InvalidResetTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.example.germanteaching.auth.exception;

/**
 * Thrown when attempting to register a user that already exists
 */
public class UserAlreadyExistsException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public UserAlreadyExistsException(String message) {
        super(message);
    }

    public UserAlreadyExistsException(String message, Throwable cause){
        super(message, cause);
    }
}

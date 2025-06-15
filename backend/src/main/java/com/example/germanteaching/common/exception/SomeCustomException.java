package com.example.germanteaching.common.exception;   // pick any package

/**
 * Thrown when a business-rule violation occurs (rename & document as needed).
 */
public class SomeCustomException extends RuntimeException {

    public SomeCustomException(String message) {
        super(message);
    }

    public SomeCustomException(String message, Throwable cause) {
        super(message, cause);
    }
}

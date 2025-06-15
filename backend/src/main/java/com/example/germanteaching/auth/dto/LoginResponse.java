package com.example.germanteaching.auth.dto;

/**
 * DTO for user login responses.
 */

public record LoginResponse (
    String accessToken,
    long expiresInSeconds, // remaining lifetime
    String tokenType // typically "Bearer"
) {
    public LoginResponse(String accessToken, long expiresInSeconds) {
        this(accessToken, expiresInSeconds, "Bearer");
    }
}
    

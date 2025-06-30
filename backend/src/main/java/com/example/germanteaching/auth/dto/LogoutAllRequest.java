package com.example.germanteaching.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LogoutAllRequest {
    @NotBlank(message = "Username is required")
    private String username;

    // Constructors
    public LogoutAllRequest() {}

    public LogoutAllRequest(String username) {
        this.username = username;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
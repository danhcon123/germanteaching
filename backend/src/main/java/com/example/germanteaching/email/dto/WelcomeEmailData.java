package com.example.germanteaching.email.dto;

public class WelcomeEmailData {
    private String username;
    private String loginUrl;
    private String appName;

    public WelcomeEmailData(String username, String loginUrl, String appName) {
        this.username = username;
        this.loginUrl=loginUrl;
        this.appName=appName;
    }
    
    // Getters and Setters
        // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getLoginUrl() { return loginUrl; }
    public void setLoginUrl(String loginUrl) { this.loginUrl = loginUrl; }
    
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
}

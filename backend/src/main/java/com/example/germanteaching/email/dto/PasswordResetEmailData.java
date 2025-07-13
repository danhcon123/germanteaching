package com.example.germanteaching.email.dto;

public class PasswordResetEmailData {
    private String username;
    private String resetUrl;
    private int expirationHours;
    private String appName;

    public PasswordResetEmailData(String username, String resetUrl, int expirationHours, String appName) {
        this.username=username;
        this.resetUrl=resetUrl;
        this.expirationHours=expirationHours;
        this.appName=appName;
    }
    
    // Getters and Setters
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getResetUrl() { return resetUrl; }
    public void setResetUrl(String resetUrl) { this.resetUrl = resetUrl; }
    
    public int getExpirationHours() { return expirationHours; }
    public void setExpirationHours(int expirationHours) { this.expirationHours = expirationHours; }
    
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
}

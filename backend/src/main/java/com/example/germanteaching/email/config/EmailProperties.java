package com.example.germanteaching.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {
    private String fromAddress;
    private String fromName;
    private String baseUrl;
    private int resetTokenExpirationHours;
    private boolean enabled;
    private String appName = "German Teaching App"; // Default app name

    // Getters & Setters
        // Getters and setters
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public int getResetTokenExpirationHours() { return resetTokenExpirationHours; }
    public void setResetTokenExpirationHours(int resetTokenExpirationHours) { 
        this.resetTokenExpirationHours = resetTokenExpirationHours; 
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) {
        this.appName = appName;
    }

}

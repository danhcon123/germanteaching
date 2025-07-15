package com.example.germanteaching.email.service;

import com.example.germanteaching.email.dto.PasswordResetEmailData;
import com.example.germanteaching.email.dto.WelcomeEmailData;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.entity.PasswordResetToken;

public interface EmailService {
    /**
     * Send password reset email using email string
     */
    void sendPasswordResetEmail(String toEmail, PasswordResetEmailData data);

    /**
     * Send password reset email using User entity and token
     * This method will create the reset URL and email data automatically
     */
    void sendPasswordResetEmail(User user, PasswordResetToken token);

    /**
     * Send welcome email using email string
     */
    void sendWelcomeEmail(String toEmail, WelcomeEmailData data);

    /**
     * Send Welcome Email using User entity
     * This method will create the login URL and email data automatically
     */
    void sendWelcomeEmail(User user);

    /**
     * Send plain text email
     */
    void sendPlainTextEmail(String toEmail, String subject, String body);

    /**
     * Check if email service is enabled
     */
    boolean isEmailEnabled();
    
    /**
     * Build reset URL for a given token
     */
    String buildPasswordResetUrl(String token);

    /**
     * Build login URL
     */
    String buildLoginUrl();
}

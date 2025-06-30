package com.example.germanteaching.auth.service;

import com.example.germanteaching.auth.entity.PasswordResetToken;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.repository.UserRepository;
import com.example.germanteaching.auth.repository.PasswordResetTokenRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

public class PasswordResetTokenService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenService.class);

    private static final SecureRandom secureRandom =  new SecureRandom();
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    // Token expiration time in hours
    @Value("${app.password-reset.tokenn-expiration-hours: 24}")
    private int tokenExpirationHours;
    
    /**
     * Create a new password reset token for given user
     * If user already has an active token, it will be replaced
     */
    @Transactional
    public PasswordResetToken createResetToken(User user){
        logger.info("Creating password reset token for user: {}", user.getUsername());
        
        // Delete any existing tokens for this user
    }
}

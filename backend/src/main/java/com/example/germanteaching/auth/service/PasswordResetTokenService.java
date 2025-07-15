package com.example.germanteaching.auth.service;

import com.example.germanteaching.auth.entity.PasswordResetToken;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.repository.UserRepository;

import jakarta.persistence.LockModeType;

import com.example.germanteaching.auth.repository.PasswordResetTokenRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

public class PasswordResetTokenService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenService.class);

    private static final SecureRandom secureRandom =  new SecureRandom();
    
    private static final Duration REUSE_WINDOW = Duration.ofMinutes(15);

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
        if (passwordResetTokenRepository.existsByUserAndUsedFalseAndExpiryDateAfter(user, Instant.now())) {
            Optional<PasswordResetToken> recent = 
                passwordResetTokenRepository.findFirstByUserAndUsedFalseAndExpiryDateAfterOrderByCreatedAtDesc(user, Instant.now());
            if (recent.isPresent() && recent.get().getCreateDate().isAfter(Instant.now().minus(REUSE_WINDOW))) {
                return recent.get();
            }
        }
        logger.info("Creating password reset token for user: {}", user.getUsername());
        
        // Delete any existing tokens for this user (enforces one active token per user)
        passwordResetTokenRepository.deleteByUser(user);

        // Generating secure random token
        String token = generateSecureToken();
        
        // Calculate expiration time
        Instant expiryDate = Instant.now().plus(tokenExpirationHours, ChronoUnit.HOURS);
        
        // Create and save new token
        PasswordResetToken resetToken = new PasswordResetToken(user, token, expiryDate);
        PasswordResetToken savedToken =  passwordResetTokenRepository.save(resetToken);
        
        logger.info("Password reset token created for user: {} (expired: {})", user.getUsername(), expiryDate);
        return savedToken;
    }
    
    /**
     * Validate a reset token and return the associated user if valid
     * Return empty Optional<User> if token is invalid, expired or already used
     */
    @Transactional(readOnly = true) // Spring-managed transaction that’s optimized for reads, no dirty checks or flush
    public Optional<User> validateResetToken(String token) {
        logger.debug("Validating password reset token");
        
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Password reset validation failed: empty token");
            return Optional.empty();
        }

        return passwordResetTokenRepository.findByToken(token)
                .filter(resetToken -> {
                    if (!resetToken.isValid()) {
                        logger.warn("Password reset validation failed: token is expired or already used");
                        return false;
                    }
                    return true;
                })
                // Transforms the surviving PasswordResetToken into its associated User object.
                // Final result is Optional<User>: present only if the token existed and was valid.
                .map(PasswordResetToken::getUser);
    }

    /**
     * Validate and immediately mark used in one transaction+lock.
     */
    @Transactional
    public Optional<User> validateAndInvalidate(String token) {
        return passwordResetTokenRepository.findByToken(token)
        .filter(PasswordResetToken::isValid)
        .map(t -> {
            t.setUsed(true);
            return t.getUser();
        });
    }

    /**
     * Mark a reset token as used (invalidate it)
     * This should be called after successful password reset.
     */
    @Transactional
    public void invalidateToken(String token) {
        logger.debug("Invalidating password reset token");

        passwordResetTokenRepository.findByToken(token)
            .ifPresent(resetToken -> {
                resetToken.setUsed(true);
                passwordResetTokenRepository.save(resetToken);
                logger.info("Password reset token invalidated for user: {}",
                    resetToken.getUser().getUsername());
            });
    }

    /**
     * Check if a user has any active (unused and not expired) reset tokens.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveToken(User user){
        return passwordResetTokenRepository.existsByUserAndUsedFalseAndExpiryDateAfter(
            user, Instant.now());
    }
    
    /**
     * Clean up expired tokens from the database
     * This should be called periodically (e.g., via scheduled task)
     */
    @Transactional
    public void deleteExpiredTokens(){
        logger.info("Cleaning up expired password reset tokens");
        passwordResetTokenRepository.deleteExpiredTokens();
    }

    /**
     * Generate a cryptographically secure random token
     * Uses 32 bytes of random data encoded as Base64 URL-safe string
     */
    private String generateSecureToken(){
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Get token expiration time in hours (for information purpose)
     */
    public int getTokenExpirationHours(){
        return tokenExpirationHours;
    }
}

package com.example.germanteaching.auth.service;

import com.example.germanteaching.auth.entity.RefreshToken;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.exception.TokenRefreshException;
import com.example.germanteaching.auth.repository.RefreshTokenRepository;
import com.example.germanteaching.auth.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing refresh tokens.
 * Handles creation, validation, rotation and clean up of refresh tokens.  
*/
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshTokenDurationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                            UserRepository userRepository,
                            @Value("${app.jwtRefreshExpirationMs}") long refreshTokenDurationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenDurationMs = refreshTokenDurationMs;
    }
        /**
     * Create a new refresh token for a user
     * Optionally revokes existing tokens for single-device login
     */
    @Transactional
    public RefreshToken createRefreshToken(Integer userId, boolean revokeExisting) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Optionally revoke existing tokens (for single-device login)
        if (revokeExisting) {
            revokeAllUserTokens(user);
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken = refreshTokenRepository.save(refreshToken);
        logger.info("Created new refresh token for user: {}", user.getUsername());

        return refreshToken;
    }

    /**
     * Create refresh token with default settings (allows multiple devices)
     */
    public RefreshToken createRefreshToken(Integer userId) {
        return createRefreshToken(userId, false);
    }

    /**
     * Find refresh token by token string
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verify if refresh token is valid and not expired, then give token back
     * if one of both or both yes, then clean from the store, logs why and throws Exception
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (!token.isValid()){
            String message = token.isExpired() ? "Refresh token is expired" : "Refresh token is revoked";
            logger.warn("{} : {}", message, token.getToken());
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), message);
        }
        return token;
    }

    /**
     * Refresh token rotation: create new token and revoke the old one (make it unusable).
     * This is a security best practice
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String oldToken){
        RefreshToken oldRefreshToken = findByToken(oldToken)
                .map(this::verifyExpiration)
                .orElseThrow(() -> new TokenRefreshException(oldToken, "Refresh token not found"));
        
        // Create new token
        RefreshToken newRefreshToken = createRefreshToken(oldRefreshToken.getUser().getUserId(), false);

        // Revoke the old token
        oldRefreshToken.revoke();
        refreshTokenRepository.save(oldRefreshToken);

        logger.info("Rotated refresh token for user: {}", oldRefreshToken.getUser().getUsername());
        return newRefreshToken;
    }

    /**
     * Revoke a specific refresh token (mark as unusable)
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = findByToken(token)
                .orElseThrow(() -> new TokenRefreshException(token, "Refresh token not found"));

        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        logger.info("Revoked refresh token: {}", token);
    }

   /**
     * Revoke all refresh tokens for a user (logout from all devices).
     * SQL statement updates all tokens for the user to revoked state.
     * UPDATE refresh_tokens
        SET revoked = TRUE,
            revoked_at = :now
        WHERE user_id = :user_id
        AND revoked = FALSE;
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(user, Instant.now());
        logger.info("Revoked {} refresh tokens for user: {}", revokedCount, user.getUsername());
    }

    /**
     * Revoke all refresh tokens for a user by username.
     */
    @Transactional
    public void revokeAllUserTokens(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        revokeAllUserTokens(user);
    }

    /**
     * Get count of valid refresh tokens for a user.
     */
    public long getValidTokenCount(User user) {
        return refreshTokenRepository.countValidTokensByUser(user, Instant.now());
    }

    /**
     * Check validity of a token string without loading the entity.
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        return refreshTokenRepository.existsByTokenAndIsValid(token, Instant.now());
    }

    /**
     * Scheduled cleanup of expired refresh tokens
     * Runs daily at 2 AM by default (configurable via cron expression) 
     */
    @Scheduled(cron = "${app.refreshTokenCleanupCron:0 0 2 * * ?}")
    @Transactional
    public void cleanupExpiredTokens() {
        // Delete all expired tokens; repository method does not return count
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
        logger.info("Expired refresh tokens cleaned up");
    }
}


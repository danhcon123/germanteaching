package com.example.germanteaching.auth.repository;

import com.example.germanteaching.auth.entity.PasswordResetToken;
import com.example.germanteaching.auth.entity.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.time.Instant;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken>findByToken(String token);

    @Modifying(clearAutomatically = true)
    void deleteByUser(User user);
    
    /**
     * Delete expired tokens - useful for cleanup jobs
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiryDate < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();

    /**
     * Find valid (unused and not expired) token
     */
    // @Query("SELECT p FROM PasswordResetToken p where p.token = :token AND p.used = false AND NOT p.isExpired() > CURRENT_TIMESTAMP")
    // Optional<PasswordResetToken> findValidToken(@Param("token") String token);

    Optional<PasswordResetToken> findByTokenAndExpiryDateAfter(String token, Instant now);

    /**
     * Check if user has any active tokens
     */
    boolean existsByUserAndUsedFalseAndExpiryDateAfter(User user, Instant currentTime);

    /**
     * Get the most recently created valid token 
     */
    Optional<PasswordResetToken>
        findFirstByUserAndUsedFalseAndExpiryDateAfterOrderByCreateDateDesc(User user, Instant now);
    
    
}

package com.example.germanteaching.auth.repository;

import com.example.germanteaching.auth.entity.RefreshToken;
import com.example.germanteaching.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefreshToken entity operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find a refresh token by its token string.
     * 
     * @param token the token string
     * @return an Optional containing the RefreshToken if found, or empty if not
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all valid (non-revoked, non-expired) refresh tokens for a user.
     * @param user owner of the tokens
     * @param now  the cutoff Instant; only tokens expiring after this are returned
     * @return
     */
    List<RefreshToken> findValidTokensByUser(@Param("user") User user, @Param("now") Instant now);

    /**
     * Find all refresh tokens associated with a specific user.
     * 
     * @param user the user whose tokens to find
     * @return a list of RefreshTokens for the user
     */
    List<RefreshToken> findByUser(User user);

    /**
     * Delete all refresh tokens for a user (used for logout all devices).
     * @param user the user whose tokens to delete
     */
    void deleteByUser(@Param("user") User user);

    /**
     * Delete all expired refresh tokens.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
    
    /**
     * Revoke all refresh tokens for a user.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllUserTokens(@Param("user") User user, @Param("now") Instant now);
    
    /**
     * Count valid refresh tokens for a user.
     * rt.expiryDate > :now
     * only include those RefreshToken rows whose expiryDate timestamp is after the moment represented by the :now parameter
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiryDate > :now")
    long countValidTokensByUser(@Param("user") User user, @Param("now") Instant now);

    /**
     * Check if token exists and is valid
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false AND rt.expiryDate > :now")
    boolean existsByTokenAndIsValid(@Param("token") String token, @Param("now") Instant now);

}

package com.example.germanteaching.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Uitility class for handling JWT operations such as token generation, validation, and extraction of user information.
 * Uses HS256 algorithm for token signing and verification.
 * 
 * Access Token: Short-lived (15 minutes) - used for API requests
 * Refresh Token: Long-lived (7 days) - used to get new access tokens
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private long jwtExpirationMs;

    @Value("${app.jwtRefreshExpirationMs:604800000}") // Default 7 days in milliseconds
    private long jwtRefreshExpirationMs;

    private SecretKey key;

    /**
     * Initialize the signing key after dependency injection.
     * Caretes a proper SecretKey from the configured secret string
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        logger.info("JWT utilities initialized successfully - Access token: {} ms, Refresh token: {} ms",
            jwtExpirationMs, jwtRefreshExpirationMs);
    }

    /**
     * Generate a short-lived access token for API requests.
     */
    public String generateAccessToken(String username){
        return generateToken(username, jwtExpirationMs, "ACCESS");
    }
    
    /**
     * Generate a long-lived refresh token for obataining new access tokens.
     */
    public String generateRefreshToken(String username) {
        return generateToken (username, jwtRefreshExpirationMs, "REFRESH");
    }

    /**
     * Generates a JWT token for the given username.
     * The token includes the username as the subject, issued at time, and expiration time.
     *
     * @param username the username to include in the token
     * @return a signed JWT token as a String
     */
    public String generateToken(String username, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .claim("type", tokenType)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }


/**
 * Validates a JWT token by checking its signature and expiration.
 * @param token the JWT token to validate
 * @return true, if the token is valid, false otherwise
 */
    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired (different from validation - this tells WHY it failed)
     * @param token
     * @return true/false
     */
    public boolean isTokenExpired(String token) {
        try {
            Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false; // Invalid for other reasons, not expired
        }
    }


    /**
     * Extrac the username from a valid JWT token.
     * @param token the JWT token token to extract the username from
     * @return the username contained in the token's subject claim
     * Extract username from token (works for both access and refresh tokens).
     */
    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
    }

    /**
     * Get token type (ACCESS or REFRESH)
     */
    public String getTokenType(String token){
        Claims claims = Jwts.parser()
                .setSigningKey(key)          // ← use the same key you initialized in @PostConstruct
                .build()
                .parseClaimsJws(token)       // ← throws if signature is invalid or token malformed
                .getBody();                  // ← the payload, as a Claims object
        return claims.get("type", String.class);
    }

    /**
     * Get remaining time until token expires (in milliseconds).
     * @return time in milliseconds until 0
     */
    public long getRemainingTime(String token) {
        try{
            Claims claims = Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            Date expiration = claims.getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (JwtException e) { 
            return 0;
        }
    }

    // Getters
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public long getJwtRefreshExpirationMs() {
        return jwtRefreshExpirationMs;
    }
}

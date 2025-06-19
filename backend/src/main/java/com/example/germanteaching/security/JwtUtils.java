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
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private long jwtExpirationMs;

    private SecretKey key;

    /**
     * Initialize the signing key after dependency injection.
     * Caretes a proper SecretKey from the configured secret string
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        logger.info("JWT utilities initialized successfully");
    }

    /**
     * Generates a JWT token for the given username.
     * The token includes the username as the subject, issued at time, and expiration time.
     *
     * @param username the username to include in the token
     * @return a signed JWT token as a String
     */
    public String generateJwtToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiry)
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
     * Extrac the username from a valid JWT token.
     * @param token the JWT token token to extract the username from
     * @return the username contained in the token's subject claim
     * @throws JwtException if the token is invalid or expired
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
     * Get the expiration date from the configuration.
     * @return JWT expiration time in milliseconds
     */
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}

package com.example.germanteaching.auth.service;

import com.example.germanteaching.auth.dto.LoginRequest;
import com.example.germanteaching.auth.dto.LoginResponse;
import com.example.germanteaching.auth.dto.TokenRefreshRequest;
import com.example.germanteaching.auth.dto.TokenRefreshResponse;
import com.example.germanteaching.common.exception.TokenRefreshException;
import com.example.germanteaching.auth.entity.RefreshToken;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.repository.RefreshTokenRepository;
import com.example.germanteaching.auth.repository.UserRepository;
import com.example.germanteaching.security.JwtUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Enhanced authentication service with refresh token support.
 * Handles login, token  refresh, and logout operations
 */
@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;
    
    /**
     * Authenticate user and generate both access and refresh tokens
     * Throws 401 Unauthorized on bad credentials
     */
    @Transactional
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        logger.info("Attempting to authenticate user: {}", loginRequest.getUsername());

        try {
            // Build an Authentication token from the username & password
            // Delegate to Spring Security’s AuthenticationManager to verify credentials
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            // If successful, store the Authentication object in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Extract the authenticated principal (user details)
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            //Load application’s User entity (e.g. for additional fields)
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            // Generate a JWT access token for subsequent requests
            String accessToken = jwtUtils.generateAccessToken(userDetails.getUsername());
            // Create (and persist) a new refresh token tied to this user
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserId());
            // Calculate how many seconds until the JWT expires
            long expiresInSeconds = jwtUtils.getJwtExpirationMs() / 1000L;

            logger.info("User {} authenticated successfully", loginRequest.getUsername());
            // Package everything into a LoginResponse DTO and return it
            return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                expiresInSeconds,
                user.getUserId(),
                user.getUsername(),
                user.getXp(),
                user.getLernCoins(),
                user.getCurrentStreakDays()
            );
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password",
                e
            );
        }
    }

    /**
     * Refresh access token using a valid refresh token
     * Throws 403 Forbidden on invalid or expired refresh token.
     */
    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request){
        String reqToken = request.getRefreshToken();
        return refreshTokenService.findByToken(reqToken)
            // Load and validate the stored RefreshToken object
            .map(refreshTokenService::verifyExpiration)
            // Extract the User from it
            .map(RefreshToken::getUser)
            // Generate new tokens and wrap into DTO
            .map(user -> {
                String newAccess = jwtUtils.generateAccessToken(user.getUsername());
                RefreshToken newRefresh = refreshTokenService.rotateRefreshToken(reqToken);
                long expiresInSeconds = jwtUtils.getJwtExpirationMs() / 1000L;
                logger.info("Token refreshed for user: {}", user.getUsername());
                return new TokenRefreshResponse(
                    newAccess,
                    newRefresh.getToken(),
                    "Bearer",
                    expiresInSeconds
                );
            }) 
            // If at any point Optional was empty (invalid token), throw 403
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Refresh token is invalid or expired"
            ));
    }

    /**
     * Logout user from current session (revoke specific refresh token).
     */
    @Transactional
    public void logoutUser(String refreshToken){
        if (refreshToken != null) {
            refreshTokenService.revokeRefreshToken(refreshToken);
            logger.info("Refresh token revoked");
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * Logout user from all devices (revoke all of user's refresh token)
     */
    @Transactional
    public void logoutFromAllDevices(String username){
        refreshTokenService.revokeAllUserTokens(username);
        logger.info("User {} logged out from all devices", username);
        SecurityContextHolder.clearContext();
    }

    /**
     * Check if a username corresponds to an active user.
     * This will be called for example forgot-password or authenticateUser
     */
    @Transactional(readOnly = true)
    public boolean isUserValid(String username) {
        return userRepository.findByUsername(username)
                .map(User::isActive)
                .orElse(false);
    }
}

package com.example.germanteaching.auth.service;

import com.example.germanteaching.auth.dto.ForgotPasswordRequest;
import com.example.germanteaching.auth.dto.LoginRequest;
import com.example.germanteaching.auth.dto.LoginResponse;
import com.example.germanteaching.auth.dto.TokenRefreshRequest;
import com.example.germanteaching.auth.dto.TokenRefreshResponse;
import com.example.germanteaching.auth.dto.RegisterRequest;
import com.example.germanteaching.auth.dto.ResetPasswordRequest;
import com.example.germanteaching.common.exception.TokenRefreshException;
import com.example.germanteaching.auth.entity.PasswordResetToken;
import com.example.germanteaching.auth.entity.RefreshToken;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.repository.RefreshTokenRepository;
import com.example.germanteaching.auth.repository.UserRepository;
import com.example.germanteaching.security.JwtUtils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
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
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;

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
     * Login user and return JWT token (for backward compativility with your controller)
     * Return null if authentication fails
     */
    @Transactional
    public String loginAndGetToken(LoginRequest loginRequest){
        try {
            LoginResponse response = authenticateUser(loginRequest);
            return response.getAccessToken();
        } catch (Exception e){
            logger.error("Login failed for user: {}", loginRequest.getUsername());
            return null;
        }
    }

    /**
     * Register a new user account
     * @param registerRequest
     * @return true, if successful, false if username/email already exists
     */
    @Transactional
    public boolean register (RegisterRequest registerRequest){
        logger.info("Attempting to register user: {}", registerRequest.getUsername());
        
        // Check if username already exists
        if(userRepository.findByUsername(registerRequest.getUsername()).isPresent()){
            logger.warn("Registration failed: username {} already exist", registerRequest.getUsername());
            return false;
        }
        
        // Check if email already exists
        if(userRepository.findByEmail(registerRequest.getEmail()).isPresent()){
            logger.warn("Registration failed: username {} already exist", registerRequest.getUsername());
            return false;
        }
        
        try {
            // Create new user entity
            User newUser = new User();
            newUser.setUsername(registerRequest.getUsername());
            newUser.setEmail(registerRequest.getEmail());
            newUser.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            newUser.setActive(true);
            
            // Set default values for gamification
            newUser.setXp(null);
            newUser.setLernCoins(null);
            newUser.setCurrentStreakDays(null);
            
            // Save
            userRepository.save(newUser);
            logger.info("User {} registered successfully", registerRequest.getUsername());
            return true;
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", registerRequest.getUsername());
            return false;
        }
    }

    /**
     * Forgot password - generate reset token and send email
     * @param request
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        logger.info("Password reset requested for email: {}", request.getEmail());
        
        // Find user by email (silently fail if not found for security)
        userRepository.findByEmail(request.getEmail())
            .ifPresent(user -> {
                try{
                    if (passwordResetTokenService.hasActiveToken(user)) {
                        logger.info("User {} already has an active reset token", user.getUsername());
                        // here can either creating a new token (current implementation) or
                        // replace old token by continuing createResetToken
                        return;
                    }
                
                // Create reset token
                PasswordResetToken resetToken = passwordResetTokenService.createResetToken(user);
                
                // Send email (implemented this based on email service) (EMAIL SERVICE SHOULD BE IMPLEMENTED)
                //sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken.getToken());
                
                // PLACEHOLDER
                logger.info("Password reset email sent to email: {}", request.getEmail());

                } catch (Exception e) {
                    logger.error("Failed to process passwrod reset for user: {}", user.getUsername());
                }
            });

        // Always log success for security (don't reveal if email exists or not)
        logger.info("Password reset request processed for email: {}", request.getEmail());
    }
    
    /**
     * Reset password using reset token
     */
    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {
        try {
            // Validate reset token and get associated user
            Optional<User> userOpt = passwordResetTokenService.validateResetToken(request.getToken());
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Update user's password
                user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);
                
                // Invalidate the reset token
                passwordResetTokenService.invalidateToken(request.getToken());

                // Revoke all refresh tokens to force re-login
                refreshTokenService.revokeAllUserTokens(user.getUsername());
                
                logger.info("Password reset successful for user: {}", user.getUsername());
                return true;
            }
        } catch (Exception e) {
            logger.error("Password reset failed", e);
        }

        logger.warn("Password reset failed: invalid or expired token");
        return false; 
    }

    /**
     * TODO:
     * Send password reset email (placeholder implementation)
     * Replace with ACTUAL EMAIL SERVICE IMPLEMENTATION
     */
    private void sendPasswordResetEmail(String email, String username, String token) {
        // Example implementation - replace with your email service
        logger.info("Sending password reset email to: {}", email);
        
        // Build reset URL (adjust based on your frontend)
        String resetUrl = String.format("https://yourdomain.com/reset-password?token=%s", token);
        
        // Email content
        String subject = "Password Reset Request";
        String body = String.format(
            "Hello %s,\n\n" +
            "You have requested to reset your password. Please click the link below to reset it:\n\n" +
            "%s\n\n" +
            "This link will expire in %d hours.\n\n" +
            "If you did not request this reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Your App Team",
            username, resetUrl, passwordResetTokenService.getTokenExpirationHours()
        );
        
        // TODO: Replace with actual email sending
        // emailService.sendEmail(email, subject, body);
        
        // For now, just log the email content (remove in production)
        logger.debug("Email content - Subject: {}, Body: {}", subject, body);
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

    /** */
}

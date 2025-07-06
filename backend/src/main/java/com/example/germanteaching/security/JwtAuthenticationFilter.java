package com.example.germanteaching.security;

import com.example.germanteaching.auth.dto.LoginRequest;
import com.example.germanteaching.auth.dto.LoginResponse;
import com.example.germanteaching.auth.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Filter that handles login requests.
 * 
 * This filter intercepts POST requests to /api/auth/login, validates credentials,
 * and if successful, generates JWT access and refresh tokens.
 */
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter{
    
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for JWT Authentication Filter.
     * @param authenticationManager the authentication manager to validate credentials
     * @param jwtUtils utility class for JWT tokens operations
     * @param refreshTokenService service for managing refresh tokens
     */
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, 
                                     JwtUtils jwtUtils, 
                                     RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.objectMapper = new ObjectMapper();
        //Set the login endpoint
        setFilterProcessesUrl("/api/auth/login");
    }

    /**
     * Attempts to authenticate the user from the login request.
     * Extracts username and password from JSON request body
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @return the authentication object if successful
     * @throws AuthenticationException if authentication fails
     */
    @Override
    public Authentication attemptAuthentication(
        HttpServletRequest request,
        HttpServletResponse response) throws AuthenticationException {
        
        try{
            // Parse the login credentials from JSON request body
            LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);

            // Create authentication token
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

            // Attempt authentication using the authentication manager
            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse login request", e);
        }
    }

    /**
     * Called when authentication is successful.
     * Generates JWT access and refresh tokens and sends them in the response.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param chain the filter chain#
     * @param authResult the successful authentication result
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    protected void successfulAuthentication(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain,
        Authentication authResult) throws IOException, ServletException {
        
        // Get the authenticated user details
        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();
        Integer userId = userDetails.getUserId();
        String username = userDetails.getUsername();
        Integer xp = userDetails.getXp();
        Integer lernCoins = userDetails.getLernCoins();
        Integer currentStreakDays = userDetails.getCurrentStreakDays();

        // Generate JWT access token
        String accessToken = jwtUtils.generateAccessToken(username);
        
        // Generate refresh token and save it in the database
        String refreshToken = refreshTokenService.createRefreshToken(userId, true).getToken();
        
        // Create response object
        LoginResponse resp = new LoginResponse(
            accessToken,
            refreshToken,
            "Bearer",
            jwtUtils.getJwtExpirationMs()/1_000, // Convert milliseconds to seconds
            userId,
            username,
            xp,
            lernCoins,
            currentStreakDays
            );
        
        // Set response content type and write the response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getOutputStream(), resp);
    }

    /**
     * Called when authentication fails.
     * Sends an error response with the failure reason.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param failed the authentication exception
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */

    @Override
    protected void unsuccessfulAuthentication(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException failed) throws IOException, ServletException {
        
        // Create error response
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Authentication failed");
        errorResponse.put("message", failed.getMessage());
        errorResponse.put("timestamp", Instant.now().toString());

        // Set response content type and status
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // Write the error response
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }  
}

package com.example.germanteaching.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties.Jwt;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;

import java.io.IOException;

/**
 * JWT Authorization Filter that validates JWT tokens on incoming requests.
 * 
 * This filter runs on every request and checks for a valid JWT token
 * in the Authorization header. If found and valid, it sets the authentication
 * in the SecurityContext.
 */
public class JwtAuthorizationFilter extends OncePerRequestFilter{
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_NAME = "Authorization";
    
    /**
     * Constructor for JWT Authorization Filter.
     * @param jwtUtils utility class for JWT operations
     * @param userDetailsService service to load user details
     */
    public JwtAuthorizationFilter(JwtUtils jwtUtils, CustomUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }
    
    /**
     * Main filter logic that runs on each request
     * Extracts and validates JWT token, then sets the authentication if valid
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if an error occurs during processing
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        
            // Extract JWT token from request
        String jwt = parseJwt(request);

            // If token exists, is valid, and no authentication is set yet
        if (jwt != null) {
            try {
                if (jwtUtils.validateJwtToken(jwt) 
                  && SecurityContextHolder.getContext().getAuthentication() == null) {
            
                    // Extract username from token
                    String username = jwtUtils.getUsernameFromJwtToken(jwt);
                    
                    // Load user details
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                // Set additional details
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in the security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                logger.warn("Invalid JWT token: " + ex.getMessage(), ex);            
            }
        filterChain.doFilter(request, response);
        }
    }

    /**
     * Extracts JWT token from the Authorization header
     * 
     * @param request the HTTP request
     * @return the JWT token if present, null if not found/invalid format
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(HEADER_NAME);
        // ensures the header isn’t null, empty, or just whitespace.
        // and verifies it begins with the literal "Bearer " (including the space).
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(TOKEN_PREFIX)) {
            // If both checks pass, 
            // it returns everything after "Bearer "—that is, the raw JWT string (eyJhbGciOiJI…).
            return headerAuth.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * Determines if this filter should be applied to the current request.
     * Skip filtering for public endpoints that don't require authentication.
     * 
     * @param request the HTTP request
     * @return true if the filter should be skipped, false if it should be applied
     * @throws ServletException if an error occurs during processing
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip filtering for public endpoints
        return path.startsWith("/api/public/") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/actuator/health") ||
               path.equals("/actuator/info");
    }
}

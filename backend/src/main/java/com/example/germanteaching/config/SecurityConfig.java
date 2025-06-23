package com.example.germanteaching.config;

import com.example.germanteaching.security.CustomUserDetailsService;
import com.example.germanteaching.security.JwtUtils;
import com.example.germanteaching.security.JwtAuthenticationFilter;
import com.example.germanteaching.security.JwtAuthorizationFilter;
import com.example.germanteaching.auth.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

/**
 * Central security configuration for the application.
 *
 * <ul>
 *   <li>Disables CSRF (stateless JWT usage)</li>
 *   <li>Configures session management as stateless</li>
 *   <li>Enables CORS for allowed origins</li>
 *   <li>Applies comprehensive security headers</li>
 *   <li>Registers JWT-based authentication and authorization filters</li>
 *   <li>Handles unauthorized access with HTTP status responses</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Utility for generating and validating JWT tokens
    private final JwtUtils jwtUtils;
    // Loads user-specific data for authentication
    private final CustomUserDetailsService userSvc;
    // Manages persistent refresh tokens in the database
    private final RefreshTokenService refreshTokenSvc;

    // List of allowed CORS origins, injected from application properties
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * Constructor-based injection of required security components.
     *
     * @param jwtUtils helper for JWT operations
     * @param userSvc service to load UserDetails
     * @param refreshTokenSvc service managing refresh tokens
     */
    public SecurityConfig(
            JwtUtils jwtUtils,
            CustomUserDetailsService userSvc,
            RefreshTokenService refreshTokenSvc) {
        this.jwtUtils = jwtUtils;
        this.userSvc = userSvc;
        this.refreshTokenSvc = refreshTokenSvc;
    }

    /**
     * Configures the HTTP security filter chain.
     *
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Get authentication manager lazily to avoid circular dependency
        AuthenticationManager authManager = authenticationManager(http);
        
        // Filter for handling login requests and issuing tokens
        JwtAuthenticationFilter authFilter = 
            new JwtAuthenticationFilter(authManager, jwtUtils, refreshTokenSvc);
        
        // Filter for validating JWTs on incoming requests
        JwtAuthorizationFilter authorizationFilter = 
            new JwtAuthorizationFilter(jwtUtils, userSvc);

        http
            // Disable CSRF since we're using stateless tokens
            .csrf(csrf -> csrf.disable())

            // Never create an HTTP session
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Apply custom CORS settings
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Apply comprehensive security headers
            .headers(headers -> headers
                // Content Security Policy - restrictive but functional
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self'; " +
                    "connect-src 'self'; " +
                    "frame-ancestors 'none'"
                ))
                // Clickjacking protection - deny all framing
                .frameOptions(frame -> frame.deny())
                // HTTP Strict Transport Security
                .httpStrictTransportSecurity(hsts -> 
                    hsts.includeSubDomains(true)
                        .maxAgeInSeconds(31536000)
                        .preload(true))
                // Additional security headers
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )

            // Define route authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public authentication endpoints
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/forgot-password",      // <— add this
                    "/api/auth/reset-password"        // <— and this if you have one
                ).permitAll()
                .requestMatchers( "/api/public/**").permitAll()
                // Health check endpoints (for monitoring)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            .addFilterBefore(authorizationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAt(authFilter, UsernamePasswordAuthenticationFilter.class)

            // Handle authentication failures with HTTP 401 status
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }

    /**
     * Exposes the AuthenticationManager used for user/password authentication.
     * Uses @Lazy to prevent circular dependency issues.
     *
     * @param http the HttpSecurity to pull context from
     * @return the configured AuthenticationManager
     * @throws Exception if building fails
     */
    @Bean
    @Lazy
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http
            .getSharedObject(AuthenticationManagerBuilder.class)
            .userDetailsService(userSvc)
            .passwordEncoder(passwordEncoder())
            .and()
            .build();
    }

    /**
     * Password encoder bean using BCrypt with strength 12.
     * Higher strength provides better security at the cost of performance.
     *
     * @return the BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configures CORS to allow specified origins, methods, and headers.
     * Credentials support is enabled for refresh token cookies.
     * 
     * Consider using environment-specific configurations for production.
     *
     * @return the CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        
        // Set allowed origins from properties
        cfg.setAllowedOrigins(Arrays.asList(allowedOrigins));
        
        // Allow common HTTP methods
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow necessary headers
        cfg.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Allow credentials for refresh token cookies
        cfg.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
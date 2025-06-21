package com.example.germanteaching.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.web.cors.*;

import com.example.germanteaching.security.JwtUtils;;


@Configuration
@EnableWebSecurity // enables Spring Security's web security support
public class SecurityConfig {

    private final JwtUtils jwtUtils; // token helper
    private final CustomUserDetailsService userSvc; // loads UserDetail by username
    private final RefreshTokenService refreshTokenSvc; // manages refresh token in DB

    public Security( JwtUtils jwtUtils,
                    CustomUserDetailsService userSvc,
                    RefreshTokenService refreshTokenSvc) {
        this.jwtUtils = jwtUtils;
        this.userSvc = userSvc;
        this.refreshTokenSvc = refreshTokenSvc;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 1) Create auth filter (handles /api/auth/login)
        JwtAuthenticationFilter authFilter =
            new JwtAuthenticationFilter(authenticationManager(http), jwtUtils, refreshTokenSvc);

        // 2) Create authorization filter (validates token on every request)
        JwtAuthorizationFilter authorizationFilter = 
            new JwtAuthorizationFilter(jwtUtils, userSvc);

        http
            .csrf(csrf -> csrf.disable()) // tookens only, no CSRF
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // no HTTP session
                .cors(Customizer.withDefaults()) //corsConfigurationSource() below
                .authorizeHttpRequests(auth -> auth 
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll() // open
                .anyRequest().authenticated()                                  // secure all others
                )
                .addFilter(authFilter)                                         // login filter
                .addFilterBefore(authorizationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                    .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        // wires UserDetailsService + password encoder into AuthManager
        return http.getsharedObject(AuthenticationManagerBuilder.class)
                   .userDetailsService(userSvc)
                   .passwordEncoder(passwordEncoder())
                   .and()
                   .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // Allow calls from Angular app (adjust origin for production)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:4200"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(true); // needed for refresh-token console
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
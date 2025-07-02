package com.example.germanteaching.auth.controller;

import com.example.germanteaching.auth.dto.*;
import com.example.germanteaching.auth.service.AuthService;
import com.example.germanteaching.auth.exception.TokenRefreshException;
import com.example.germanteaching.common.dto.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest req) { 
        // Service will throw UserAlreadyExistsException if user exists
        // Global handler will convert to 409 CONFLICT
        authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login (@Valid @RequestBody LoginRequest req) {
        // Service will throw AuthenticationException on invalid credentials
        // Global handler will convert to 401 UNAUTHORIZED
        LoginResponse resp = authService.authenticateUser(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken( @Valid @RequestBody TokenRefreshRequest req) {
        // Service will throw TokenRefreshException on invalid/expired token
        // Global handler will convert to 401 UNAUTHORIZED
        TokenRefreshResponse resp = authService.refreshToken(req);
        return ResponseEntity.ok(resp);
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req); // service handles silent success
        return ResponseEntity.ok(ApiResponse.success("IF the email is registered, a reset link has been sent to it."));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        boolean success = authService.resetPassword(req);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Password reset successful"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(ApiResponse.badRequest("Invalid or expired reset token"));
        }
    }
}

    

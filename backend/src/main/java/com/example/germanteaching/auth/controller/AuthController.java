package com.example.germanteaching.auth.controller;

import com.example.germanteaching.auth.dto.*;
import com.example.germanteaching.auth.service.AuthService;
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
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) { 
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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest req) {
        authService.logoutUser(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout/all")
    public ResponseEntity<Void> logoutAll(@RequestBody LogoutAllRequest req) {
        authService.logoutFromAllDevices(req.getUsername());
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        // Always return 202 regardless of whether email exists (security best practice)
        authService.forgotPassword(req); // service handles silent success
        return ResponseEntity.accepted().build();
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        // Service will throw InvalidResetTokenException on invalid/expired token
        // Global handler will convert to 400 BAD_REQUEST
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }
}

    

package com.example.germanteaching.auth.controller;

import com.example.germanteaching.auth.dto.*;
import com.example.germanteaching.auth.service.AuthService;
import com.example.germanteaching.common.dto.ApiResponse;

import io.micrometer.core.ipc.http.HttpSender.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest req) { 
        // Bean-Validation runs; if username/email/password violate the annotations you added,
        // Spring immediately returns 400 Bad Request with the validation errors and the method is never entered.
        /**
         * Suppose authService.register returns void or throws on failure, or returns a message.
         * Example: authService.register throws exception on error, or returns boolean
         */
        boolean ok = authService.register(req); //
        if (ok) {
            ApiResponse<Void> body = ApiResponse.success("Registration successful");
            return ResponseEntity.ok(body);
        } else {
            ApiResponse<Void> body = ApiResponse.badRequest("Username or email already exists");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login (@Valid @RequestBody LoginRequest req) {
        /**
        * Example if returning only ApiResponse:
        * ApiResponse resp = authService.login(req);
        * return new ResponseEntity<>(resp, resp.isSuccess() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED);
        */
        // Example if returning JWT:
        String token = authService.loginAndGetToken(req);
        if (token != null) {
            return ResponseEntity.ok(ApiResponse.success("Login successful", token));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(ApiResponse.unauthorized("Invalid username or password"));
        }
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

    

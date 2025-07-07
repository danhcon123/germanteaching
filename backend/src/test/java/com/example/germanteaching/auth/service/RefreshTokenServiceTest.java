package com.example.germanteaching.auth.service;

import com.example.germanteaching.auth.entity.RefreshToken;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.exception.TokenRefreshException;
import com.example.germanteaching.auth.repository.RefreshTokenRepository;
import com.example.germanteaching.auth.repository.UserRepository;
import com.example.germanteaching.auth.service.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
@Transactional // Roll back after each test for isolation
@TestPropertySource(properties = {
    "app.jwtRefreshExpirationMs=3600000", // 1 hour for testing
    "app.refreshTokenCleanupCron=0 0 2 * * ?",
    "spring.jpa.hibernate.ddl-auto=create-drop" // Use create-drop for tests
})
class RefreshTokenServiceTest {

    @TestConfiguration
    static class Clockconfig{
        @Bean
        public Clock clock() {
            // Fixed time for deterministic tests
            return Clock.fixed(Instant.parse("2025-07-07T00:00:00Z"), ZoneOffset.UTC);
        }    
    }

    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private Clock clock;

    private User testUser;
    private User testUser2;

    @BeforeEach
    void Setup(){
        // Create test users - no cleanup needed, rollback handles it
        testUser  = userRepository.save(newUser("testuser",  "test@example.com"));
        testUser2 = userRepository.save(newUser("testuser2", "test2@example.com"));
    }

    private User newUser(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash("hashedPassword");
        u.activate();
        return u;
    }

    private RefreshToken createToken(User u) {
        return refreshTokenService.createRefreshToken( u.getUserId());
    }

    private RefreshToken createToken(User u, boolean revokeExisting) {
        return refreshTokenService.createRefreshToken(u.getUserId(), revokeExisting);
    }

        @Nested
        @DisplayName("Token Creation")
        class CreationTests {
            @Test @DisplayName("creates a new refresh token")
            void createTokenSuccessfully() {
                RefreshToken token = createToken(testUser);

                assertThat(token).isNotNull();
                assertThat(token.getToken()).isNotBlank();
                assertThat(token.getUser().getUserId()).isEqualTo(testUser.getUserId());
                assertThat(token.getExpiryDate().isAfter(Instant.now(clock)));
                assertThat(token.isExpired()).isFalse();
                assertThat(token.isRevoked()).isFalse();

                assertThat(refreshTokenRepository.findByToken(token.getToken()))
                .isPresent()
                .get().extracting(RefreshToken::getToken)
                .isEqualTo(token.getToken());
            }
        }

        @Test @DisplayName("revokes old tokens when requested")
        void revokeExistingOnNew(){
            RefreshToken first = createToken(testUser);
            assertThat(first.isRevoked()).isFalse();

            RefreshToken second = createToken(testUser, true);
            assertThat(second.getToken()).isNotEqualTo(first.getToken());

            assertThat(refreshTokenRepository.findByToken(first.getToken()))
                .isPresent().get().extracting(RefreshToken::isRevoked)
                .isEqualTo(true);


            assertThat(second.isRevoked()).isFalse();
            }

        @Test @DisplayName("throws if user not found")
        void errorOnUnknownUser() {
            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(99999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 99999");
        }
    

    @Nested
    @DisplayName("Token Validation")
    class ValidationTests {
        @Test @DisplayName("accept valid tokens")
        void validToken() {
            RefreshToken token = createToken(testUser);
            RefreshToken verified = refreshTokenService.verifyExpiration(token);
            assertThat(verified.getToken()).isEqualTo(token.getToken());
        }
    }

    @Test @DisplayName("rejects expired tokens and removes them")
    void expiredToken() {
        RefreshToken token = createToken(testUser);
        token.setExpiryDate(Instant.now(clock).minusSeconds(3600));
        refreshTokenRepository.save(token);

        // when & then
        TokenRefreshException ex = assertThrows(
            TokenRefreshException.class,
            () -> refreshTokenService.verifyExpiration(token),
            "Expected verifyExpiration(...) to throw, but it didn't"
        );

        // assert on the exception
        assertThat(ex.getMessage()).contains("Refresh token is expired");
        assertThat(ex.getToken()).isEqualTo(token.getToken());

        // and verify the token was deleted
        assertThat(refreshTokenRepository.findByToken(token.getToken())).isEmpty();
    }
}


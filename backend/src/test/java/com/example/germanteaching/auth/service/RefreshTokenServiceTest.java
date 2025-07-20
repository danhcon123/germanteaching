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
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.time.Clock;
import java.time.ZoneOffset;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ RefreshTokenService.class, RefreshTokenServiceTest.Clockconfig.class}) //Import for the DataJpa to have access to services
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
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
    @Autowired private JdbcTemplate jdbc;
    private User testUser;
    private User testUser2;

    @BeforeEach
    void Setup(){
        jdbc.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

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
    
  
    @Test @DisplayName("accept valid tokens")
    void validToken() {
        RefreshToken token = createToken(testUser);
        RefreshToken verified = refreshTokenService.verifyExpiration(token);
        assertThat(verified.getToken()).isEqualTo(token.getToken());
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

    @Test @DisplayName ("rejects revoked tokens")
    void revokedToken(){
        RefreshToken token = createToken(testUser);
        token.revoke();
        refreshTokenRepository.save(token);
        
        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
            .isInstanceOf(TokenRefreshException.class)
            .hasMessageContaining("Refresh token is revoked");
    }
    

    @Test @DisplayName("isTokenValid works without loading entity")
    void isTokenValidChecks() {
        String valid = createToken(testUser).getToken();
        assertThat(refreshTokenService.isTokenValid(valid)).isTrue();
        assertThat(refreshTokenService.isTokenValid("bad")).isFalse();

        RefreshToken t2 =  createToken(testUser2);
        t2.revoke();
        refreshTokenRepository.save(t2);
        assertThat(refreshTokenService.isTokenValid(t2.getToken())).isFalse();
    }

    @Test @DisplayName("rotate tokens and revokes old")
    void rotateSuccess(){
        String old = createToken(testUser).getToken();
        RefreshToken next = refreshTokenService.rotateRefreshToken(old);

        assertThat(next.getToken()).isNotEqualTo(old);
        assertThat(next.getUser().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(next.isRevoked()).isFalse();
        
        assertThat(refreshTokenRepository.findByToken(old))
            .isPresent().get().extracting(RefreshToken::isRevoked)
            .isEqualTo(true);
    }

    @Test @DisplayName("error on missing token")
    void rotateMissing(){
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("nope"))
            .isInstanceOf(TokenRefreshException.class)
            .hasMessageContaining("Refresh token not found");
    }
    
    @Test @DisplayName("error on expired token rotate")
    void rotateExpired() {
        RefreshToken token = createToken(testUser);
        token.setExpiryDate(Instant.now(clock).minusSeconds(3600));
        refreshTokenRepository.save(token);
        
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(token.getToken()))
            .isInstanceOf(TokenRefreshException.class)
            .hasMessageContaining("Refresh token is expired");
    }

    @Test @DisplayName("Token Revocation")
    void revokeSingle(){
        String tok = createToken(testUser).getToken();
        refreshTokenService.revokeRefreshToken(tok);
        
        assertThat(refreshTokenRepository.findByToken(tok))
            .isPresent().get().extracting(RefreshToken::isRevoked)
            .isEqualTo(true);
    }
    
    @Test @DisplayName("revokes all for user")
    void revokeAllForUser(){
        String t1 = createToken(testUser).getToken();
        String t2 = createToken(testUser).getToken();
        createToken(testUser2);

        refreshTokenService.revokeAllUserTokens(testUser);
        assertThat(refreshTokenRepository.findByToken(t1))
            .isPresent()
            .get().extracting(RefreshToken::isRevoked)
            .isEqualTo(true);
        
        assertThat(refreshTokenRepository.findByToken(t2))
            .isPresent().get().extracting(RefreshToken::isRevoked)
            .isEqualTo(true);
    }

    @Test
    @DisplayName("handles concurrent creations")
    @DirtiesContext // Ensure spring context is reset after this test
    void tokenUniquenessConstraint() {
        List<RefreshToken> tokens = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tokens.add(createToken(testUser));
        }    
        
        // Verify all tokens have unique values
        Set<String> tokenValues = tokens.stream()
            .map(RefreshToken::getToken)
            .collect(Collectors.toSet());

        assertThat(tokenValues).hasSize(5);
        
        // Verify they're all persisted correctly
        tokens.forEach(token -> {
            assertThat(refreshTokenRepository.findByToken(token.getToken()))
                .isPresent()
                .get().extracting(RefreshToken::getToken)
                .isEqualTo(token.getToken());
        });
    }

    @Test
    @DisplayName("creates multiple unique tokens for same user")
    void multipleTokenCreations() {
        // Create multiple tokens sequentially (simulates what concurrent calls would do)
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tokens.add(createToken(testUser).getToken());
        }

        // Verify all tokens are unique
        Set<String> unique = new HashSet<>(tokens);
        assertThat(unique).hasSize(3);
        
        // Verify all tokens are valid
        tokens.forEach(token -> 
            assertThat(refreshTokenService.isTokenValid(token)).isTrue()
        );
        
        // Verify all tokens belong to the same user
        tokens.forEach(token -> {
            RefreshToken rt = refreshTokenRepository.findByToken(token).orElseThrow();
            assertThat(rt.getUser().getUserId()).isEqualTo(testUser.getUserId());
        });
    }

    @Test @DisplayName("null or empty inputs return false/empty")
    void nullEmptyInputs(){
        assertThat(refreshTokenService.findByToken(null)).isEmpty();
        assertThat(refreshTokenService.findByToken("")).isEmpty();
        assertThat(refreshTokenService.isTokenValid(null)).isFalse();
        assertThat(refreshTokenService.isTokenValid("")).isFalse();
    }

    @Test @DisplayName("user without tokens is handled gracefully")
    void userNoTokens(){
        assertThat(refreshTokenService.getValidTokenCount(testUser2)).isZero();
        assertThatCode(() -> refreshTokenService.revokeAllUserTokens(testUser2)).doesNotThrowAnyException();
    }
}


package com.example.germanteaching.auth.repository;

import com.example.germanteaching.auth.entity.RefreshToken;
import com.example.germanteaching.auth.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class RefreshTokenRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private JdbcTemplate jdbc;
    
    private User testUser;
    private User anotherUser;
    private Instant now;
    private Instant futureTime;
    private Instant pastTime;
    
    @BeforeEach
    void setUp(){
          // Truncate all users *and* reset the sequence back to 1
        jdbc.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
    
        now = Instant.now();
        futureTime = now.plus(1, ChronoUnit.HOURS);
        pastTime = now.minus(1, ChronoUnit.HOURS);

        // Create test users - no cleanup needed, rollback handles it
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("password");
        entityManager.persistAndFlush(testUser);
        
        anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPasswordHash("password");
        anotherUser.setUsername("anotheruser");
        entityManager.persistAndFlush(anotherUser);
    }

    @Test
    @DisplayName("Should find refresh token by token string")
    void shouldFindByToken() {
        // Given
        RefreshToken token = createRefreshToken("test-token-123", testUser, futureTime, false);
        entityManager.persistAndFlush(token);

        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("test-token-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("test-token-123");
        assertThat(found.get().getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should return empty when token not found")
    void shouldReturnEmptyWhenTokenNotFound() {
        // When
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("non-existent-token");

        // Then
        assertThat(found).isEmpty();
    }

     @Test
    @DisplayName("Should find valid tokens by user")
    void shouldFindValidTokensByUser() {
        // Given
        RefreshToken validToken1 = createRefreshToken("valid-token-1", testUser, futureTime, false);
        RefreshToken validToken2 = createRefreshToken("valid-token-2", testUser, futureTime, false);
        RefreshToken expiredToken = createRefreshToken("expired-token", testUser, pastTime, false);
        RefreshToken revokedToken = createRefreshToken("revoked-token", testUser, futureTime, true);
        RefreshToken anotherUserToken = createRefreshToken("another-user-token", anotherUser, futureTime, false);

        entityManager.persistAndFlush(validToken1);
        entityManager.persistAndFlush(validToken2);
        entityManager.persistAndFlush(expiredToken);
        entityManager.persistAndFlush(revokedToken);
        entityManager.persistAndFlush(anotherUserToken);

        // When
        List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUser(testUser, now);

        // Then
        assertThat(validTokens).hasSize(2);
        assertThat(validTokens).extracting(RefreshToken::getToken)
                .containsExactlyInAnyOrder("valid-token-1", "valid-token-2");
    }
    
    @Test
    @DisplayName("Should return empty list when no valid tokens exist")
    void shouldReturnEmptyListWhenNoValidTokensExist() {
        // Given
        RefreshToken expiredToken = createRefreshToken("expired-token", testUser, pastTime, false);
        RefreshToken revokedToken = createRefreshToken("revoked-token", testUser, futureTime, true);
        entityManager.persistAndFlush(expiredToken);
        entityManager.persistAndFlush(revokedToken);

        // When
        List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUser(testUser, now);

        // Then
        assertThat(validTokens).isEmpty();
    }

    @Test
    @DisplayName("Should find all tokens by user")
    void shouldFindByUser() {
        // Given
        RefreshToken token1 = createRefreshToken("token-1", testUser, futureTime, false);
        RefreshToken token2 = createRefreshToken("token-2", testUser, pastTime, true);
        RefreshToken anotherUserToken = createRefreshToken("another-token", anotherUser, futureTime, false);

        entityManager.persistAndFlush(token1);
        entityManager.persistAndFlush(token2);
        entityManager.persistAndFlush(anotherUserToken);

        // When
        List<RefreshToken> userTokens = refreshTokenRepository.findByUser(testUser);

        // Then
        assertThat(userTokens).hasSize(2);
        assertThat(userTokens).extracting(RefreshToken::getToken)
                .containsExactlyInAnyOrder("token-1", "token-2");
    }

    @Test
    @DisplayName("Should delete all tokens by user")
    void shouldDeleteByUser() {
        // Given
        RefreshToken token1 = createRefreshToken("token-1", testUser, futureTime, false);
        RefreshToken token2 = createRefreshToken("token-2", testUser, pastTime, true);
        RefreshToken anotherUserToken = createRefreshToken("another-token", anotherUser, futureTime, false);

        entityManager.persistAndFlush(token1);
        entityManager.persistAndFlush(token2);
        entityManager.persistAndFlush(anotherUserToken);

        // When
        refreshTokenRepository.deleteByUser(testUser);
        entityManager.flush();

        // Then
        List<RefreshToken> remainingTokens = refreshTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getUser()).isEqualTo(anotherUser);
    }

    @Test
    @DisplayName("Should delete expired tokens")
    void shouldDeleteExpiredTokens() {
        // Given
        RefreshToken validToken = createRefreshToken("valid-token", testUser, futureTime, false);
        RefreshToken expiredToken1 = createRefreshToken("expired-token-1", testUser, pastTime, false);
        RefreshToken expiredToken2 = createRefreshToken("expired-token-2", anotherUser, pastTime, false);

        entityManager.persistAndFlush(validToken);
        entityManager.persistAndFlush(expiredToken1);
        entityManager.persistAndFlush(expiredToken2);

        // When
        refreshTokenRepository.deleteExpiredTokens(now);
        entityManager.flush();

        // Then
        List<RefreshToken> remainingTokens = refreshTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getToken()).isEqualTo("valid-token");
    }

    @Test
    @DisplayName("Should revoke all user tokens")
    void shouldRevokeAllUserTokens() {
        // Given
        RefreshToken token1 = createRefreshToken("token-1", testUser, futureTime, false);
        RefreshToken token2 = createRefreshToken("token-2", testUser, futureTime, false);
        RefreshToken alreadyRevokedToken = createRefreshToken("revoked-token", testUser, futureTime, true);
        RefreshToken anotherUserToken = createRefreshToken("another-token", anotherUser, futureTime, false);

        entityManager.persistAndFlush(token1);
        entityManager.persistAndFlush(token2);
        entityManager.persistAndFlush(alreadyRevokedToken);
        entityManager.persistAndFlush(anotherUserToken);

        // When
        int revokedCount = refreshTokenRepository.revokeAllUserTokens(testUser, now);
        entityManager.flush();

        // Then
        assertThat(revokedCount).isEqualTo(2); // Only non-revoked tokens should be affected

        List<RefreshToken> testUserTokens = refreshTokenRepository.findByUser(testUser);
        assertThat(testUserTokens).allMatch(RefreshToken::isRevoked);
        assertThat(testUserTokens).allMatch(token -> token.getRevokedAt() != null);

        // Another user's tokens should be unaffected
        RefreshToken anotherUserRefreshedToken = refreshTokenRepository.findByUser(anotherUser).get(0);
        assertThat(anotherUserRefreshedToken.isRevoked()).isFalse();
    }
    
    @Test
    @DisplayName("Should count valid tokens by user")
    void shouldCountValidTokensByUser() {
        // Given
        RefreshToken validToken1 = createRefreshToken("valid-token-1", testUser, futureTime, false);
        RefreshToken validToken2 = createRefreshToken("valid-token-2", testUser, futureTime, false);
        RefreshToken expiredToken = createRefreshToken("expired-token", testUser, pastTime, false);
        RefreshToken revokedToken = createRefreshToken("revoked-token", testUser, futureTime, true);

        entityManager.persistAndFlush(validToken1);
        entityManager.persistAndFlush(validToken2);
        entityManager.persistAndFlush(expiredToken);
        entityManager.persistAndFlush(revokedToken);

        // When
        long count = refreshTokenRepository.countValidTokensByUser(testUser, now);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return zero when no valid tokens exist for user")
    void shouldReturnZeroWhenNoValidTokensExist() {
        // Given
        RefreshToken expiredToken = createRefreshToken("expired-token", testUser, pastTime, false);
        RefreshToken revokedToken = createRefreshToken("revoked-token", testUser, futureTime, true);

        entityManager.persistAndFlush(expiredToken);
        entityManager.persistAndFlush(revokedToken);

        // When
        long count = refreshTokenRepository.countValidTokensByUser(testUser, now);

        // Then
        assertThat(count).isEqualTo(0);
    }

    @DisplayName("Should check if token exists and is valid")
    void shouldCheckIfTokenExistsAndIsValid() {
        // Given
        RefreshToken validToken = createRefreshToken("valid-token", testUser, futureTime, false);
        RefreshToken expiredToken = createRefreshToken("expired-token", testUser, pastTime, false);
        RefreshToken revokedToken = createRefreshToken("revoked-token", testUser, futureTime, true);

        entityManager.persistAndFlush(validToken);
        entityManager.persistAndFlush(expiredToken);
        entityManager.persistAndFlush(revokedToken);

        // When & Then
        assertThat(refreshTokenRepository.existsByTokenAndIsValid("valid-token", now)).isTrue();
        assertThat(refreshTokenRepository.existsByTokenAndIsValid("expired-token", now)).isFalse();
        assertThat(refreshTokenRepository.existsByTokenAndIsValid("revoked-token", now)).isFalse();
        assertThat(refreshTokenRepository.existsByTokenAndIsValid("non-existent-token", now)).isFalse();
    }

    @Test
    @DisplayName("Should handle edge case with exact expiry time")
    void shouldHandleEdgeCaseWithExactExpiryTime() {
        // Given
        RefreshToken tokenExpiringNow = createRefreshToken("expiring-now", testUser, now, false);
        entityManager.persistAndFlush(tokenExpiringNow);

        // When & Then
        assertThat(refreshTokenRepository.existsByTokenAndIsValid("expiring-now", now)).isFalse();
        assertThat(refreshTokenRepository.countValidTokensByUser(testUser, now)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle user with no tokens")
    void shouldHandleUserWithNoTokens() {
        // When & Then
        assertThat(refreshTokenRepository.findByUser(testUser)).isEmpty();
        assertThat(refreshTokenRepository.findValidTokensByUser(testUser, now)).isEmpty();
        assertThat(refreshTokenRepository.countValidTokensByUser(testUser, now)).isEqualTo(0);
        assertThat(refreshTokenRepository.revokeAllUserTokens(testUser, now)).isEqualTo(0);
    }
    private RefreshToken createRefreshToken(String token, User user, Instant expiryDate, boolean revoked) {
        RefreshToken refreshToken = new RefreshToken(user, token, expiryDate);
        if (revoked) {
            refreshToken.revoke(); // Uses the entity's revoke() method
        }
        return refreshToken;
    }
}
package com.example.germanteaching.auth.repository;

import com.example.germanteaching.auth.entity.PasswordResetToken;
import com.example.germanteaching.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class PasswordResetTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private User testUser;
    private User anotherUser;
    private Instant now;
    private Instant futureTime;
    private Instant pastTime;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        futureTime = now.plus(23, ChronoUnit.HOURS);
        pastTime = now.minus(1, ChronoUnit.HOURS);

        // 1) Make sure the users_user_id_seq is at the current MAX(user_id)
        entityManager.getEntityManager()
            .createNativeQuery(
            "SELECT setval('users_user_id_seq', (SELECT COALESCE(MAX(user_id), 0) FROM users))"
            )
            .getSingleResult();
        // Create test users
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("password");
        testUser.setUsername("testuser");
        entityManager.persistAndFlush(testUser);

        anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPasswordHash("password");
        anotherUser.setUsername("anotheruser");
        entityManager.persistAndFlush(anotherUser);
    }

    @Test
    @DisplayName("Should find password reset token by token string")
    void shouldFindByToken() {
        // Given
        PasswordResetToken token = createPasswordResetToken("reset-token-123", testUser, pastTime, futureTime, false);
        entityManager.persistAndFlush(token);

        // When
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken("reset-token-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("reset-token-123");
        assertThat(found.get().getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should return empty when token not found")
    void shouldReturnEmptyWhenTokenNotFound() {
        // When
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken("non-existent-token");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should delete all tokens by user")
    void shouldDeleteByUser() {
        // Given
        PasswordResetToken token1 = createPasswordResetToken("token-1", testUser, pastTime, futureTime, false);
        PasswordResetToken token2 = createPasswordResetToken("token-2", testUser, pastTime, futureTime, true);
        PasswordResetToken anotherUserToken = createPasswordResetToken("another-token", anotherUser, Instant.now(), futureTime, false);

        entityManager.persistAndFlush(token1);
        entityManager.persistAndFlush(token2);
        entityManager.persistAndFlush(anotherUserToken);

        // When
        passwordResetTokenRepository.deleteByUser(testUser);
        entityManager.flush();

        // Then
        List<PasswordResetToken> remainingTokens = passwordResetTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getUser()).isEqualTo(anotherUser);
    }

    @Test
    @DisplayName("Should delete expired tokens")
    void shouldDeleteExpiredTokens() {
        // Given
        PasswordResetToken validToken = createPasswordResetToken("valid-token", testUser, Instant.now(), futureTime, false);
        PasswordResetToken expiredToken1 = createPasswordResetToken("expired-token-1", testUser,  Instant.now(),pastTime, false);
        PasswordResetToken expiredToken2 = createPasswordResetToken("expired-token-2", anotherUser, Instant.now(), pastTime, true);

        entityManager.persistAndFlush(validToken);
        entityManager.persistAndFlush(expiredToken1);
        entityManager.persistAndFlush(expiredToken2);

        // When
        passwordResetTokenRepository.deleteExpiredTokens();
        entityManager.flush();

        // Then
        List<PasswordResetToken> remainingTokens = passwordResetTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getToken()).isEqualTo("valid-token");
    }

    @Test
    @DisplayName("Should find valid token by token and expiry date")
    void shouldFindByTokenAndExpiryDateAfter() {
        // Given
        PasswordResetToken validToken = createPasswordResetToken("valid-token", testUser, Instant.now(), futureTime, false);
        PasswordResetToken expiredToken = createPasswordResetToken("expired-token", testUser, Instant.now(), pastTime, false);

        entityManager.persistAndFlush(validToken);
        entityManager.persistAndFlush(expiredToken);

        // When
        Optional<PasswordResetToken> validFound = passwordResetTokenRepository.findByTokenAndExpiryDateAfter("valid-token", now);
        Optional<PasswordResetToken> expiredFound = passwordResetTokenRepository.findByTokenAndExpiryDateAfter("expired-token", now);

        // Then
        assertThat(validFound).isPresent();
        assertThat(validFound.get().getToken()).isEqualTo("valid-token");
        assertThat(expiredFound).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when token not found in findByTokenAndExpiryDateAfter")
    void shouldReturnEmptyWhenTokenNotFoundInFindByTokenAndExpiryDateAfter() {
        // When
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByTokenAndExpiryDateAfter("non-existent-token", now);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if user has active tokens")
    void shouldCheckIfUserHasActiveTokens() {
        // Given
        PasswordResetToken activeToken = createPasswordResetToken("active-token", testUser, Instant.now(),futureTime, false);
        PasswordResetToken expiredToken = createPasswordResetToken("expired-token", testUser, Instant.now(),pastTime, false);
        PasswordResetToken usedToken = createPasswordResetToken("used-token", testUser, Instant.now(),futureTime, true);

        entityManager.persistAndFlush(activeToken);
        entityManager.persistAndFlush(expiredToken);
        entityManager.persistAndFlush(usedToken);

        // When
        boolean hasActiveTokens = passwordResetTokenRepository.existsByUserAndUsedFalseAndExpiryDateAfter(testUser, now);
        boolean anotherUserHasActiveTokens = passwordResetTokenRepository.existsByUserAndUsedFalseAndExpiryDateAfter(anotherUser, now);

        // Then
        assertThat(hasActiveTokens).isTrue();
        assertThat(anotherUserHasActiveTokens).isFalse();
    }

    @Test
    @DisplayName("Should return false when user has no active tokens")
    void shouldReturnFalseWhenUserHasNoActiveTokens() {
        // Given
        PasswordResetToken expiredToken = createPasswordResetToken("expired-token", testUser, Instant.now(),pastTime, false);
        PasswordResetToken usedToken = createPasswordResetToken("used-token", testUser, Instant.now(),futureTime, true);

        entityManager.persistAndFlush(expiredToken);
        entityManager.persistAndFlush(usedToken);

        // When
        boolean hasActiveTokens = passwordResetTokenRepository.existsByUserAndUsedFalseAndExpiryDateAfter(testUser, now);

        // Then
        assertThat(hasActiveTokens).isFalse();
    }

    @Test
    @DisplayName("Should return false when user has no tokens at all")
    void shouldReturnFalseWhenUserHasNoTokens() {
        // When
        boolean hasActiveTokens = passwordResetTokenRepository.existsByUserAndUsedFalseAndExpiryDateAfter(testUser, now);

        // Then
        assertThat(hasActiveTokens).isFalse();
    }

    @Test
    @DisplayName("Should handle edge case with exact expiry time")
    void shouldHandleEdgeCaseWithExactExpiryTime() {
        // Given
        PasswordResetToken tokenExpiringNow = createPasswordResetToken("expiring-now", testUser, Instant.now(),now, false);
        entityManager.persistAndFlush(tokenExpiringNow);

        // When
        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByTokenAndExpiryDateAfter("expiring-now", now);
        boolean hasActiveTokens = passwordResetTokenRepository.existsByUserAndUsedFalseAndExpiryDateAfter(testUser, now);

        // Then
        assertThat(found).isEmpty();
        assertThat(hasActiveTokens).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple tokens for same user")
    void shouldHandleMultipleTokensForSameUser() {
        // Given
        PasswordResetToken token1 = createPasswordResetToken("token-1", testUser, Instant.now(),futureTime, false);
        PasswordResetToken token2 = createPasswordResetToken("token-2", testUser, Instant.now(),futureTime, false);
        PasswordResetToken token3 = createPasswordResetToken("token-3", testUser,Instant.now(), futureTime, true);

        entityManager.persistAndFlush(token1);
        entityManager.persistAndFlush(token2);
        entityManager.persistAndFlush(token3);

        // When
        boolean hasActiveTokens = passwordResetTokenRepository.existsByUserAndUsedFalseAndExpiryDateAfter(testUser, now);

        // Then
        assertThat(hasActiveTokens).isTrue();
    }

    @Test
    @DisplayName("Should not delete tokens when deleting by different user")
    void shouldNotDeleteTokensWhenDeletingByDifferentUser() {
        // Given
        PasswordResetToken testUserToken = createPasswordResetToken("test-token", testUser,Instant.now(), futureTime, false);
        PasswordResetToken anotherUserToken = createPasswordResetToken("another-token", anotherUser, Instant.now(),futureTime, false);

        entityManager.persistAndFlush(testUserToken);
        entityManager.persistAndFlush(anotherUserToken);

        // When
        passwordResetTokenRepository.deleteByUser(testUser);
        entityManager.flush();

        // Then
        List<PasswordResetToken> remainingTokens = passwordResetTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getUser()).isEqualTo(anotherUser);
    }

    @Test
    @DisplayName("Should handle deletion when user has no tokens")
    void shouldHandleDeletionWhenUserHasNoTokens() {
        // Given
        PasswordResetToken anotherUserToken = createPasswordResetToken("another-token", anotherUser,Instant.now(), futureTime, false);
        entityManager.persistAndFlush(anotherUserToken);

        // When
        passwordResetTokenRepository.deleteByUser(testUser);
        entityManager.flush();

        // Then
        List<PasswordResetToken> remainingTokens = passwordResetTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getUser()).isEqualTo(anotherUser);
    }

    @Test
    @DisplayName("Should handle expired tokens cleanup when no expired tokens exist")
    void shouldHandleExpiredTokensCleanupWhenNoExpiredTokensExist() {
        // Given
        PasswordResetToken validToken = createPasswordResetToken("valid-token", testUser,Instant.now(), futureTime, false);
        entityManager.persistAndFlush(validToken);

        // When
        passwordResetTokenRepository.deleteExpiredTokens();
        entityManager.flush();

        // Then
        List<PasswordResetToken> remainingTokens = passwordResetTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getToken()).isEqualTo("valid-token");
    }

    @Test
    @DisplayName("Should find token regardless of used status in findByToken")
    void shouldFindTokenRegardlessOfUsedStatusInFindByToken() {
        // Given
        PasswordResetToken unusedToken = createPasswordResetToken("unused-token", testUser,Instant.now(), futureTime, false);
        PasswordResetToken usedToken = createPasswordResetToken("used-token", testUser, Instant.now(),futureTime, true);

        entityManager.persistAndFlush(unusedToken);
        entityManager.persistAndFlush(usedToken);

        // When
        Optional<PasswordResetToken> unusedFound = passwordResetTokenRepository.findByToken("unused-token");
        Optional<PasswordResetToken> usedFound = passwordResetTokenRepository.findByToken("used-token");

        // Then
        assertThat(unusedFound).isPresent();
        assertThat(usedFound).isPresent();
        assertThat(unusedFound.get().isUsed()).isFalse();
        assertThat(usedFound.get().isUsed()).isTrue();
    }
    
    private PasswordResetToken createPasswordResetToken(String token, User user, Instant createdDate,Instant expiryDate, boolean used) {
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setUser(user);
        passwordResetToken.setExpiryDate(expiryDate);
        passwordResetToken.setCreateDate(createdDate);
        passwordResetToken.setUsed(used);
        return passwordResetToken;
    }

}

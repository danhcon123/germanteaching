package com.example.germanteaching.auth.service;

import com.example.germanteaching.auth.entity.PasswordResetToken;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.repository.PasswordResetTokenRepository;
import com.example.germanteaching.auth.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(
  properties = { "app.password-reset.token-expiration-hours=24",
                 "app.password-reset.reuse-window-minutes=15" }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  com.example.germanteaching.GermanteachingApplication.class,
  PasswordResetTokenService.class,
  PasswordResetTokenServiceTest.ClockConfig.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PasswordResetTokenServiceTest {
    
    @TestConfiguration
    static class ClockConfig {
        @Bean
        public Clock clock() {
        return Clock.fixed(Instant.parse("2025-07-20T00:00:00Z"), ZoneOffset.UTC);        }
    }

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;
    
    private User testUser;
    private User secondUser;


    @BeforeEach
    void Setup(){
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();
        
        // Create test users
        testUser = createTestUser("testuser", "test@example.com");
        secondUser = createTestUser("seconduser", "second@example.com");
    }
    
        private User createTestUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("encoded_password");
        user.activate();
        return userRepository.save(user);
    }

    @Test
    void createResetToken_ShouldCreateNewToken_WhenNoActiveTokenExists() {
        // When
        PasswordResetToken result = passwordResetTokenService.createResetToken(testUser);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(testUser.getUserId(), result.getUser().getUserId());
        assertFalse(result.isUsed());
        assertTrue(result.getExpiryDate().isAfter(Instant.now().plus(23, ChronoUnit.HOURS)));

        // Verify token is persisted in database
        Optional<PasswordResetToken> foundToken = passwordResetTokenRepository.findByToken(result.getToken());
        assertTrue(foundToken.isPresent());
        assertEquals(result.getId(), foundToken.get().getId());
    }

    @Test
    void createResetToken_ShouldReuseRecentToken_WhenWithinReuseWindow(){
        // Given - Create an initial token
        PasswordResetToken initialToken = passwordResetTokenService.createResetToken(testUser);
        entityManager.flush();
        
        // When - Try to create another token immediatly (< 15 min)
        PasswordResetToken result = passwordResetTokenService.createResetToken(testUser);
        
        // Then - Should reuse the same token
        assertEquals(initialToken.getId(), result.getId());
        assertEquals(initialToken.getToken(), result.getToken());

        // Verify only one token exists in database
        long tokenCount = passwordResetTokenRepository.count();
        assertEquals(1, tokenCount);
    }

    @Test
    void createResetToken_ShouldDeleteOldToken_WhenCreatingNewOne(){
        // Given - Create initial token for user
        PasswordResetToken initialToken = passwordResetTokenService.createResetToken(testUser);
        String initialTokenValue = initialToken.getToken();

        // Simulate token being older than reuse window by updating created date
        initialToken.setCreateDate(Instant.now().minus(20, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(initialToken);
        entityManager.flush();

        // When - Create new token
        PasswordResetToken newToken = passwordResetTokenService.createResetToken(testUser);

        
        // Then - Should have new token and old one should be deleted
        assertNotEquals(initialTokenValue, newToken.getToken());

        // Verify old token is deleted
        Optional<PasswordResetToken> oldToken = passwordResetTokenRepository.findByToken(initialTokenValue);
        assertFalse(oldToken.isPresent());

        // Verify new token exists
        Optional<PasswordResetToken> savedNewToken = passwordResetTokenRepository.findByToken(newToken.getToken());
        assertTrue(savedNewToken.isPresent());
    }

    @Test
    void createResetToken_ShouldCreateSeparateTokens_ForDifferentUsers() {
        // When
        PasswordResetToken token1 = passwordResetTokenService.createResetToken(testUser);
        PasswordResetToken token2 = passwordResetTokenService.createResetToken(secondUser);

        // Then
        assertNotEquals(token1.getToken(), token2.getToken());
        assertEquals(testUser.getUserId(), token1.getUser().getUserId());
        assertEquals(secondUser.getUserId(), token2.getUser().getUserId());
        
        // Verify both tokens exist in database
        assertEquals(2, passwordResetTokenRepository.count());
    }

    @Test
    void validateResetToken_ShouldReturnUser_WhenTokenIsValid() {
        // Given - Create a valid token
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUser);
        
        // When - Validate the token
        Optional<User> result = passwordResetTokenService.validateResetToken(token.getToken());
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getUserId(), result.get().getUserId());
        assertEquals(testUser.getUsername(), result.get().getUsername());
    }

    @Test
    void validateResetToken_ShouldReturnEmpty_WhenTokenIsNull(){
        // When
        Optional<User> result = passwordResetTokenService.validateResetToken(null);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void validateResetToken_ShouldReturnEmpty_WhenTokenIsInvalid() {
        // When - Validate an invalid token
        Optional<User> result = passwordResetTokenService.validateResetToken("invalid-token");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void validateResetToken_ShouldReturnEmpty_WhenTokenIsWhitespace(){
        // When
        Optional<User> result = passwordResetTokenService.validateResetToken("   ");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void validateResetToken_ShouldReturnEmpty_WhenTokenIsExpired() {
        // Given
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUser);
        token.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(token);
        entityManager.flush();
        
        // When
        Optional<User> result = passwordResetTokenService.validateResetToken(token.getToken());
        
        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void validateResetToken_ShouldReturnEmpty_WhenTokenIsAlreadyUsed() {
        // Given - Create a valid token and mark it as used
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUser);
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        entityManager.flush();
        
        // When - Validate the used token
        Optional<User> result = passwordResetTokenService.validateResetToken(token.getToken());
        
        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void validateAndInvalidate_ShouldReturnUserAndMarkTokenUsed_WhenTokenIsValid() {
        // Given
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUser);
        String tokenValue = token.getToken();

        // When
        Optional<User> result = passwordResetTokenService.validateAndInvalidate(tokenValue);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getUserId(), result.get().getUserId());
        
        // Verify token is marked as used in database
        Optional<PasswordResetToken> updatedToken = passwordResetTokenRepository.findByToken(tokenValue);
        assertTrue(updatedToken.isPresent());
        assertTrue(updatedToken.get().isUsed());
    }

    @Test
    void validateAndInvalidate_ShouldReturnEmpty_WhenTokenIsInvalid() {

        // Given - Created expired token
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUser);
        token.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(token);
        entityManager.flush();

        // When - Validate and invalidate an invalid token
        Optional<User> result = passwordResetTokenService.validateAndInvalidate(token.getToken());
        
        // Then
        assertFalse(result.isPresent());
        
        // Verify token is not marked as used
        Optional<PasswordResetToken> updatedToken = passwordResetTokenRepository.findByToken(token.getToken());
        assertTrue(updatedToken.isPresent());
        assertFalse(updatedToken.get().isUsed());
    }

    // Given
    @Test
    void invalidateToken_ShouldMarkTokenAsUsed_WhenTokenExists() {
        // Given
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUser);
        String tokenValue = token.getToken();
        
        // When
        passwordResetTokenService.invalidateToken(tokenValue);

        // Then
        Optional<PasswordResetToken> updatedToken = passwordResetTokenRepository.findByToken(tokenValue);
        assertTrue(updatedToken.isPresent());
        assertTrue(updatedToken.get().isUsed());
    }

    @Test
    void invalidateToken_ShouldDoNothing_WhenTokenNotFound() {
        // Given
        long initialCount = passwordResetTokenRepository.count();
        
        // When
        passwordResetTokenService.invalidateToken("non-existent-token");

        // Then
        assertEquals(initialCount, passwordResetTokenRepository.count());
    }

    @Test
    void hasActiveToken_ShouldReturnFalse_WhenUserHasNoActiveToken() {
        // When
        boolean result = passwordResetTokenService.hasActiveToken(testUser);

        // Then
        assertFalse(result);
    }

    @Test
    void hasActiveToken_ShouldReturnFalse_WhenUserHasOnlyExpiredTokens(){
        // Given - Create token and expire it
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUser);
        token.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(token);
        entityManager.flush();

        // When
        boolean result = passwordResetTokenService.hasActiveToken(testUser);

        // Then
        assertFalse(result);
    }

    @Test
    void deleteExpiredTokens_ShouldRemoveExpiredTokens_AndKeepValidOnes() {
        // Given - Create one valid and one expired token
        PasswordResetToken validToken = passwordResetTokenService.createResetToken(testUser);
        
        PasswordResetToken expiredToken = passwordResetTokenService.createResetToken(secondUser);
        expiredToken.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(expiredToken);
        entityManager.flush();

        assertEquals(2, passwordResetTokenRepository.count());

        // When
        passwordResetTokenService.deleteExpiredTokens();
        entityManager.flush();

        // Then
        assertEquals(1, passwordResetTokenRepository.count());
        
        // Verify correct token remains
        Optional<PasswordResetToken> remainingToken = passwordResetTokenRepository.findByToken(validToken.getToken());
        assertTrue(remainingToken.isPresent());
        
        Optional<PasswordResetToken> deletedToken = passwordResetTokenRepository.findByToken(expiredToken.getToken());
        assertFalse(deletedToken.isPresent());
    }

    @Test
    void getTokenExpirationHours_ShouldReturnConfiguredValue() {
        // When
        int expirationHours = passwordResetTokenService.getTokenExpirationHours();

        // Then
        assertEquals(24, expirationHours);
    }

    @Test
    void createResetToken_ShouldGenerateUniqueTokens_WhenCalledMultipleTimes() {
        // When - Create multiple tokens
        PasswordResetToken token1 = passwordResetTokenService.createResetToken(testUser);

        token1.setCreateDate(Instant.now().minus(20, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(token1);
        entityManager.flush();
        
       
        PasswordResetToken token2 = passwordResetTokenService.createResetToken(testUser);
        
        // Then - Tokens should be unique
        assertNotEquals(token1.getToken(), token2.getToken());
        // Verify both tokens exist in database
        assertEquals(1, passwordResetTokenRepository.count());
        // Verify both tokens have proper format (Base64 URL-safe)
        assertTrue(token1.getToken().matches("^[A-Za-z0-9_-]+$"));
        assertTrue(token2.getToken().matches("^[A-Za-z0-9_-]+$"));
    }

    @Test
    void createResetToken_ShouldSetCorrectExpirationTime() {
               // Given
        Instant beforeCreation = Instant.now();
        
        // When
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUser);
        
        // Then
        Instant afterCreation = Instant.now();
        Instant expectedMinExpiry = beforeCreation.plus(24, ChronoUnit.HOURS);
        Instant expectedMaxExpiry = afterCreation.plus(24, ChronoUnit.HOURS);
        
        assertTrue(token.getExpiryDate().isAfter(expectedMinExpiry.minus(1, ChronoUnit.MINUTES)));
        assertTrue(token.getExpiryDate().isBefore(expectedMaxExpiry.plus(1, ChronoUnit.MINUTES)));
    }
    
        @Test
    void tokenShouldPersistThroughSessionBoundaries() {
        // Given
        PasswordResetToken token = passwordResetTokenService.createResetToken(testUser);
        String tokenValue = token.getToken();
        
        // When - Clear the persistence context to simulate new session
        entityManager.flush();
        entityManager.clear();

        // Then - Token should still be retrievable
        Optional<PasswordResetToken> retrievedToken = passwordResetTokenRepository.findByToken(tokenValue);
        assertTrue(retrievedToken.isPresent());
        assertEquals(tokenValue, retrievedToken.get().getToken());
        
        // User should be lazily loadable
        User retrievedUser = retrievedToken.get().getUser();
        assertEquals(testUser.getUsername(), retrievedUser.getUsername());
    }
}

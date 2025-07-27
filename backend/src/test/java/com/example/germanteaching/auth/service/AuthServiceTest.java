package com.example.germanteaching.auth.service;

import com.example.germanteaching.auth.dto.ForgotPasswordRequest;
import com.example.germanteaching.auth.dto.LoginRequest;
import com.example.germanteaching.auth.dto.LoginResponse;
import com.example.germanteaching.auth.dto.LogoutRequest;
import com.example.germanteaching.auth.dto.ResetPasswordRequest;
import com.example.germanteaching.auth.dto.TokenRefreshRequest;
import com.example.germanteaching.auth.dto.TokenRefreshResponse;
import com.example.germanteaching.auth.entity.PasswordResetToken;
import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.auth.entity.RefreshToken;
import com.example.germanteaching.auth.repository.PasswordResetTokenRepository;
import com.example.germanteaching.auth.repository.RefreshTokenRepository;
import com.example.germanteaching.auth.repository.UserRepository;
import com.example.germanteaching.email.config.EmailProperties;
import com.example.germanteaching.email.service.EmailService;
import com.example.germanteaching.security.JwtUtils;

import io.jsonwebtoken.security.Password;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Lettuce.Cluster.Refresh;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties.Jwt;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@EnableAutoConfiguration
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    AuthService.class,
    RefreshTokenService.class,
    PasswordResetTokenService.class,
    AuthServiceTest.TestConfig.class
})
@EnableJpaRepositories(basePackageClasses = {
    UserRepository.class,
    RefreshTokenRepository.class,
    PasswordResetTokenRepository.class
})
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(
    properties = {
        "app.jwt.secret=testSecret",
        "app.jwt.expiration-ms=86400000",
        "app.refresh-token.expiration-days=7",
        "app.password-reset.token-expiration-hours=24"
})
@EntityScan(basePackageClasses = User.class)
public class AuthServiceTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private PasswordResetTokenService passwordResetTokenService;

    @MockBean
    private AuthenticationManager authenticationManager;
    
    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private EmailService emailService;

    @MockBean
    private EmailProperties emailProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User inactiveUser;

    private final String TEST_PASSWORD = "password123";
    private final String ENCODED_PASSWORD = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"; // BCrypt for "password123"

    @Configuration
    static class TestConfig {
        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(); // Default level = 10
        }
        @Bean
        public Clock clock() {
            return Clock.systemDefaultZone(); // Use system clock for tests
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Clear all data
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();

        // Create test user
        testUser = createTestUser("testuser", "test@example.com", ENCODED_PASSWORD, true);
        inactiveUser = createTestUser("inactiveuser", "inactive@example.com", ENCODED_PASSWORD, false);

        // Mock JWT utils
        when(jwtUtils.generateAccessToken(anyString())).thenReturn("mock-jwt-token");
        when(jwtUtils.getJwtExpirationMs()).thenReturn(86400000L); // 24 hours

        // Mock email service
        when(emailService.isEmailEnabled()).thenReturn(true);

    }

    private User createTestUser(String username, String email, String passwordHash, boolean active) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setActive(active);
        user.setXp(100);
        user.setLernCoins(50);
        user.setCurrentStreakDays(5);
        return userRepository.save(user);
    }

    private UserDetails createMockUserDetails (String username){
        return new org.springframework.security.core.userdetails.User(
            username,
            ENCODED_PASSWORD,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void authenticateUser_ShouldReturnLoginResponse_WhenCredentialAreValid() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword(TEST_PASSWORD);

        //UserDetails userDetails = createMockUserDetails("testuser");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal())
            .thenReturn(createMockUserDetails("testuser"));
        when(authentication.getName()).thenReturn("testuser");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("mock-refresh-token");
        when(refreshTokenService.createRefreshToken(testUser.getUserId()))
        .thenReturn(mockRefreshToken);

        // Stub JWT for *any* username
        when(jwtUtils.generateAccessToken("testuser"))
        .thenReturn("mock-jwt-token");
        when(jwtUtils.getJwtExpirationMs())
        .thenReturn(86400000L); // 24 hours in milliseconds

        // When
        LoginResponse response = authService.authenticateUser(request);

        // Then
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresIn()); // 24 hours in seconds
        assertEquals(testUser.getUserId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals(testUser.getXp(), response.getXp());
        assertEquals(testUser.getLernCoins(), response.getLernCoins());
        assertEquals(testUser.getCurrentStreakDays(), response.getCurrentStreakDays());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateAccessToken(testUser.getUsername());
        verify(refreshTokenService).createRefreshToken(testUser.getUserId());
    }
}

package com.agent2026.interview.identity.application;

import com.agent2026.interview.identity.domain.PasswordPolicy;
import com.agent2026.interview.identity.domain.RefreshSession;
import com.agent2026.interview.identity.domain.UserAccount;
import com.agent2026.interview.identity.domain.UserStatus;
import com.agent2026.interview.identity.domain.UsernameNormalizer;
import com.agent2026.interview.identity.domain.port.RefreshSessionRepository;
import com.agent2026.interview.identity.domain.port.UserAccountRepository;
import com.agent2026.interview.identity.infrastructure.jwt.IssuedJwt;
import com.agent2026.interview.identity.infrastructure.jwt.JwtProperties;
import com.agent2026.interview.identity.infrastructure.jwt.JwtTokenService;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthApplicationServiceTest {
    private UserAccountRepository users;
    private RefreshSessionRepository sessions;
    private JwtTokenService tokens;
    private AuthApplicationService service;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        users = mock(UserAccountRepository.class);
        sessions = mock(RefreshSessionRepository.class);
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-that-is-at-least-32-bytes-long");
        properties.setIssuer("test-issuer");
        properties.setAudience("test-audience");
        Clock clock = Clock.systemUTC();
        tokens = new JwtTokenService(properties, clock);
        service = new AuthApplicationService(users, sessions, new UsernameNormalizer(), new PasswordPolicy(),
                new BCryptPasswordEncoder(4), tokens, clock);
        user = new UserAccount(7L, "TestUser", "testuser", new BCryptPasswordEncoder(4).encode("password1"),
                UserStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void loginCreatesRefreshSessionAndReturnsAccessToken() {
        when(users.findByNormalizedUsername("testuser")).thenReturn(Optional.of(user));

        AuthenticationResult result = service.login(" TESTUSER ", "password1");

        assertEquals(7L, result.user().id());
        assertEquals(7L, tokens.verifyAccess(result.accessToken()).userId());
        verify(sessions).create(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void wrongPasswordUsesGenericCredentialsError() {
        when(users.findByNormalizedUsername("testuser")).thenReturn(Optional.of(user));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login("testuser", "wrong-password"));

        assertEquals(ErrorCode.AUTH_CREDENTIALS_INVALID, error.getErrorCode());
        verify(sessions, never()).create(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void replayedRefreshTokenRevokesEntireFamily() {
        IssuedJwt refresh = tokens.issueRefresh(user.id(), "family-1");
        RefreshSession stored = new RefreshSession(10L, user.id(), refresh.jti(), "family-1",
                tokens.hash(refresh.token()), LocalDateTime.ofInstant(refresh.expiresAt(), ZoneOffset.UTC),
                LocalDateTime.now(), "successor-jti", LocalDateTime.now(), LocalDateTime.now());
        when(sessions.findByJtiForUpdate(refresh.jti())).thenReturn(Optional.of(stored));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.refresh(refresh.token()));

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_REPLAYED, error.getErrorCode());
        verify(sessions).revokeFamily(org.mockito.ArgumentMatchers.eq("family-1"), any());
    }
}

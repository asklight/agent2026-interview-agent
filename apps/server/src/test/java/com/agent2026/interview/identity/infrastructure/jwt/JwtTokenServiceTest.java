package com.agent2026.interview.identity.infrastructure.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {
    private JwtTokenService service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-that-is-at-least-32-bytes-long");
        properties.setIssuer("test-issuer");
        properties.setAudience("test-audience");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setRefreshTokenTtl(Duration.ofDays(30));
        service = new JwtTokenService(properties, Clock.systemUTC());
    }

    @Test
    void issuesTypedTokensThatCannotBeUsedInterchangeably() {
        IssuedJwt access = service.issueAccess(42L);
        IssuedJwt refresh = service.issueRefresh(42L, "family-1");

        assertEquals(42L, service.verifyAccess(access.token()).userId());
        assertEquals("family-1", service.verifyRefresh(refresh.token()).tokenFamilyId());
        assertThrows(JWTVerificationException.class, () -> service.verifyAccess(refresh.token()));
        assertThrows(JWTVerificationException.class, () -> service.verifyRefresh(access.token()));
    }

    @Test
    void hashesRefreshTokensWithoutPersistingRawValue() {
        String first = service.hash("refresh-token");
        String second = service.hash("another-token");

        assertEquals(64, first.length());
        assertNotEquals(first, second);
        assertTrue(service.hashMatches("refresh-token", first));
    }

    @Test
    void rejectsShortSigningSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");
        properties.setIssuer("issuer");
        properties.setAudience("audience");
        assertThrows(IllegalStateException.class,
                () -> new JwtTokenService(properties, Clock.systemUTC()));
    }
}

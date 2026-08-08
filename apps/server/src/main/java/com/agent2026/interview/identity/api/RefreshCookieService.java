package com.agent2026.interview.identity.api;

import com.agent2026.interview.identity.infrastructure.jwt.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;

@Component
public class RefreshCookieService {
    public static final String COOKIE_NAME = "agent2026_refresh";
    private final JwtProperties properties;
    private final Clock clock;

    public RefreshCookieService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public ResponseCookie issue(String token, Instant expiresAt) {
        Duration maxAge = Duration.between(clock.instant(), expiresAt);
        return base().value(token).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
    }

    public ResponseCookie clear() {
        return base().value("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base() {
        return ResponseCookie.from(COOKIE_NAME)
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .sameSite("Strict")
                .path("/api/auth");
    }
}

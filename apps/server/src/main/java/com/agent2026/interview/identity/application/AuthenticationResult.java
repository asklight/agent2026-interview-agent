package com.agent2026.interview.identity.application;

import com.agent2026.interview.identity.domain.UserAccount;

import java.time.Instant;

public record AuthenticationResult(
        UserAccount user,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}

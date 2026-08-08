package com.agent2026.interview.identity.api;

import com.agent2026.interview.identity.application.AuthenticationResult;

import java.time.Instant;

public record AuthResponse(String accessToken, Instant accessTokenExpiresAt, UserResponse user) {
    public static AuthResponse from(AuthenticationResult result) {
        return new AuthResponse(result.accessToken(), result.accessTokenExpiresAt(), UserResponse.from(result.user()));
    }
}

package com.agent2026.interview.identity.infrastructure.jwt;

import java.time.Instant;

public record IssuedJwt(String token, String jti, Instant expiresAt, String tokenFamilyId) {
}

package com.agent2026.interview.identity.infrastructure.jwt;

import java.time.Instant;

public record VerifiedJwt(Long userId, String jti, String tokenFamilyId, Instant expiresAt) {
}

package com.agent2026.interview.identity.domain;

import java.time.LocalDateTime;

public record RefreshSession(
        Long id,
        Long userId,
        String jti,
        String tokenFamilyId,
        String refreshTokenHash,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        String replacedByJti,
        LocalDateTime createTime,
        LocalDateTime lastUsedAt
) {
    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}

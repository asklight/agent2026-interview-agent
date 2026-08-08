package com.agent2026.interview.identity.domain.port;

import com.agent2026.interview.identity.domain.RefreshSession;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshSessionRepository {
    RefreshSession create(Long userId, String jti, String tokenFamilyId, String tokenHash,
                          LocalDateTime expiresAt);
    Optional<RefreshSession> findByJtiForUpdate(String jti);
    void markRotated(Long id, String successorJti, LocalDateTime usedAt);
    void revoke(Long id, LocalDateTime revokedAt);
    void revokeFamily(String tokenFamilyId, LocalDateTime revokedAt);
    void revokeAllForUser(Long userId, LocalDateTime revokedAt);
}

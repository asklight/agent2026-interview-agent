package com.agent2026.interview.identity.infrastructure.persistence;

import com.agent2026.interview.identity.domain.RefreshSession;
import com.agent2026.interview.identity.domain.port.RefreshSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class MybatisRefreshSessionRepository implements RefreshSessionRepository {
    private final RefreshSessionMapper mapper;

    public MybatisRefreshSessionRepository(RefreshSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public RefreshSession create(Long userId, String jti, String tokenFamilyId, String tokenHash,
                                 LocalDateTime expiresAt) {
        RefreshSessionEntity entity = new RefreshSessionEntity();
        entity.setUserId(userId);
        entity.setJti(jti);
        entity.setTokenFamilyId(tokenFamilyId);
        entity.setRefreshTokenHash(tokenHash);
        entity.setExpiresAt(expiresAt);
        mapper.insert(entity);
        return toDomain(mapper.selectById(entity.getId()));
    }

    @Override
    public Optional<RefreshSession> findByJtiForUpdate(String jti) {
        return Optional.ofNullable(mapper.selectByJtiForUpdate(jti)).map(this::toDomain);
    }

    @Override
    public void markRotated(Long id, String successorJti, LocalDateTime usedAt) {
        if (mapper.markRotated(id, successorJti, usedAt) != 1) {
            throw new IllegalStateException("Refresh session was concurrently rotated");
        }
    }

    @Override public void revoke(Long id, LocalDateTime revokedAt) { mapper.revoke(id, revokedAt); }
    @Override public void revokeFamily(String tokenFamilyId, LocalDateTime revokedAt) { mapper.revokeFamily(tokenFamilyId, revokedAt); }
    @Override public void revokeAllForUser(Long userId, LocalDateTime revokedAt) { mapper.revokeAllForUser(userId, revokedAt); }

    private RefreshSession toDomain(RefreshSessionEntity entity) {
        return new RefreshSession(entity.getId(), entity.getUserId(), entity.getJti(),
                entity.getTokenFamilyId(), entity.getRefreshTokenHash(), entity.getExpiresAt(),
                entity.getRevokedAt(), entity.getReplacedByJti(), entity.getCreateTime(), entity.getLastUsedAt());
    }
}

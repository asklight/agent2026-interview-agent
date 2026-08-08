package com.agent2026.interview.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("auth_session")
public class RefreshSessionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String jti;
    private String tokenFamilyId;
    private String refreshTokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String replacedByJti;
    private LocalDateTime createTime;
    private LocalDateTime lastUsedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
    public String getTokenFamilyId() { return tokenFamilyId; }
    public void setTokenFamilyId(String tokenFamilyId) { this.tokenFamilyId = tokenFamilyId; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String refreshTokenHash) { this.refreshTokenHash = refreshTokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public String getReplacedByJti() { return replacedByJti; }
    public void setReplacedByJti(String replacedByJti) { this.replacedByJti = replacedByJti; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}

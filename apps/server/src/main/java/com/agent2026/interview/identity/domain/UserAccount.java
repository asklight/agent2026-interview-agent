package com.agent2026.interview.identity.domain;

import java.time.LocalDateTime;

public record UserAccount(
        Long id,
        String username,
        String normalizedUsername,
        String passwordHash,
        UserStatus status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}

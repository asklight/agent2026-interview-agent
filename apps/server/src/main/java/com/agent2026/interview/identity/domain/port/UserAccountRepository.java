package com.agent2026.interview.identity.domain.port;

import com.agent2026.interview.identity.domain.UserAccount;

import java.util.Optional;

public interface UserAccountRepository {
    Optional<UserAccount> findById(Long id);
    Optional<UserAccount> findByNormalizedUsername(String normalizedUsername);
    UserAccount create(String username, String normalizedUsername, String passwordHash);
}

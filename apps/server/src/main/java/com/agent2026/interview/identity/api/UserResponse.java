package com.agent2026.interview.identity.api;

import com.agent2026.interview.identity.domain.UserAccount;

public record UserResponse(Long id, String username) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.id(), user.username());
    }
}

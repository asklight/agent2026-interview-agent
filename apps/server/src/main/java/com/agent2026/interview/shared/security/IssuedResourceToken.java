package com.agent2026.interview.shared.security;

/**
 * A newly issued token. The access token is returned once to the caller while only
 * the token hash should be persisted.
 */
public record IssuedResourceToken(String rawToken, String tokenHash) {
}

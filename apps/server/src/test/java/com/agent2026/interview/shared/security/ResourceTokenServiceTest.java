package com.agent2026.interview.shared.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceTokenServiceTest {

    private static final String SECRET = "unit-test-secret-at-least-32-bytes";

    @Test
    void issueCreatesRandom256BitTokenAndMatchingHash() {
        ResourceTokenService service = createService(SECRET);

        IssuedResourceToken first = service.issue();
        IssuedResourceToken second = service.issue();

        assertEquals(32, Base64.getUrlDecoder().decode(first.rawToken()).length);
        assertTrue(first.tokenHash().matches("[0-9a-f]{64}"));
        assertTrue(service.matches(first.rawToken(), first.tokenHash()));
        assertNotEquals(first.rawToken(), second.rawToken());
        assertNotEquals(first.tokenHash(), second.tokenHash());
    }

    @Test
    void hashUsesHmacSha256WithConfiguredSecret() {
        ResourceTokenService service = createService(SECRET);

        assertEquals(
                "c2a77b8b6a2f7378d2ee518051af2e5bd1ea078b606e006bd7358ab6616e6fa1",
                service.hash("known-token")
        );
    }

    @Test
    void matchesRejectsWrongTokenAndMalformedHash() {
        ResourceTokenService service = createService(SECRET);
        IssuedResourceToken issued = service.issue();

        assertFalse(service.matches("wrong-token", issued.tokenHash()));
        assertFalse(service.matches(issued.rawToken(), "not-a-valid-hash"));
        assertFalse(service.matches(issued.rawToken(), null));
        assertFalse(service.matches(null, issued.tokenHash()));
    }

    @Test
    void differentSecretsProduceDifferentHashes() {
        ResourceTokenService first = createService(SECRET);
        ResourceTokenService second = createService("another-unit-test-secret-at-least-32-bytes");

        assertNotEquals(first.hash("same-token"), second.hash("same-token"));
    }

    @Test
    void blankSecretIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> createService(" "));
    }

    @Test
    void secretShorterThan32Utf8BytesIsRejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createService("1234567890123456789012345678901")
        );

        assertTrue(exception.getMessage().contains("at least 32 UTF-8 bytes"));
    }

    @Test
    void multibyteSecretIsMeasuredByUtf8Bytes() {
        ResourceTokenService service = createService("资源令牌密钥必须足够长");

        assertTrue(service.matches("token", service.hash("token")));
    }

    private ResourceTokenService createService(String secret) {
        ResourceTokenProperties properties = new ResourceTokenProperties();
        properties.setSecret(secret);
        return new ResourceTokenService(properties);
    }
}

package com.agent2026.interview.shared.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ResourceTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int HASH_BYTE_LENGTH = 32;
    private static final int HASH_HEX_LENGTH = HASH_BYTE_LENGTH * 2;
    private static final int MIN_SECRET_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom;
    private final SecretKeySpec secretKey;

    @Autowired
    public ResourceTokenService(ResourceTokenProperties properties) {
        this(properties, new SecureRandom());
    }

    ResourceTokenService(ResourceTokenProperties properties, SecureRandom secureRandom) {
        if (properties == null || properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalArgumentException("security.resource-token.secret must not be blank");
        }
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTE_LENGTH) {
            throw new IllegalArgumentException("security.resource-token.secret must contain at least 32 UTF-8 bytes");
        }
        this.secureRandom = secureRandom;
        this.secretKey = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
    }

    public IssuedResourceToken issue() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String accessToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        return new IssuedResourceToken(accessToken, hash(accessToken));
    }

    public String hash(String accessToken) {
        return toLowerHex(hmac(accessToken));
    }

    public boolean matches(String accessToken, String expectedHash) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        byte[] expectedBytes = parseHash(expectedHash);
        if (expectedBytes == null) {
            return false;
        }
        return MessageDigest.isEqual(hmac(accessToken), expectedBytes);
    }

    private byte[] hmac(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            return mac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA-256 is unavailable", ex);
        }
    }

    private byte[] parseHash(String hash) {
        if (hash == null || hash.length() != HASH_HEX_LENGTH) {
            return null;
        }
        byte[] bytes = new byte[HASH_BYTE_LENGTH];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(hash.charAt(i * 2), 16);
            int low = Character.digit(hash.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            bytes[i] = (byte) ((high << 4) | low);
        }
        return bytes;
    }

    private String toLowerHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        final char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            chars[i * 2] = alphabet[value >>> 4];
            chars[i * 2 + 1] = alphabet[value & 0x0f];
        }
        return new String(chars);
    }
}

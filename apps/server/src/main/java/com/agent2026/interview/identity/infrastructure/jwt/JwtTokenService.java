package com.agent2026.interview.identity.infrastructure.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class JwtTokenService {
    private static final String TOKEN_TYPE = "token_type";
    private static final String TOKEN_FAMILY = "token_family";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final JwtProperties properties;
    private final Clock clock;
    private final Algorithm algorithm;
    private final JWTVerifier accessVerifier;
    private final JWTVerifier refreshVerifier;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        byte[] secretBytes = requireSecret(properties.getSecret());
        this.algorithm = Algorithm.HMAC256(secretBytes);
        this.accessVerifier = verifier(ACCESS);
        this.refreshVerifier = verifier(REFRESH);
    }

    public IssuedJwt issueAccess(Long userId) {
        return issue(userId, ACCESS, null, properties.getAccessTokenTtl().toSeconds());
    }

    public IssuedJwt issueRefresh(Long userId, String tokenFamilyId) {
        if (tokenFamilyId == null || tokenFamilyId.isBlank()) {
            throw new IllegalArgumentException("tokenFamilyId must not be blank");
        }
        return issue(userId, REFRESH, tokenFamilyId, properties.getRefreshTokenTtl().toSeconds());
    }

    public VerifiedJwt verifyAccess(String rawToken) {
        return verified(accessVerifier.verify(rawToken));
    }

    public VerifiedJwt verifyRefresh(String rawToken) {
        return verified(refreshVerifier.verify(rawToken));
    }

    public String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public boolean hashMatches(String rawToken, String expectedHash) {
        if (rawToken == null || expectedHash == null) return false;
        return MessageDigest.isEqual(hash(rawToken).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private IssuedJwt issue(Long userId, String type, String familyId, long ttlSeconds) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
        String jti = UUID.randomUUID().toString();
        var builder = JWT.create()
                .withIssuer(properties.getIssuer())
                .withAudience(properties.getAudience())
                .withSubject(userId.toString())
                .withJWTId(jti)
                .withClaim(TOKEN_TYPE, type)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt);
        if (familyId != null) builder.withClaim(TOKEN_FAMILY, familyId);
        return new IssuedJwt(builder.sign(algorithm), jti, expiresAt, familyId);
    }

    private JWTVerifier verifier(String type) {
        return JWT.require(algorithm)
                .withIssuer(properties.getIssuer())
                .withAudience(properties.getAudience())
                .withClaim(TOKEN_TYPE, type)
                .acceptLeeway(2)
                .build();
    }

    private VerifiedJwt verified(DecodedJWT jwt) {
        try {
            return new VerifiedJwt(Long.valueOf(jwt.getSubject()), jwt.getId(),
                    jwt.getClaim(TOKEN_FAMILY).asString(), jwt.getExpiresAtAsInstant());
        } catch (RuntimeException ex) {
            throw new JWTVerificationException("JWT claims are invalid", ex);
        }
    }

    private byte[] requireSecret(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}

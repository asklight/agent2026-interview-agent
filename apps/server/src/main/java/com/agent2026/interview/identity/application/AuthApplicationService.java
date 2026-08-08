package com.agent2026.interview.identity.application;

import com.agent2026.interview.identity.domain.PasswordPolicy;
import com.agent2026.interview.identity.domain.RefreshSession;
import com.agent2026.interview.identity.domain.UserAccount;
import com.agent2026.interview.identity.domain.UsernameNormalizer;
import com.agent2026.interview.identity.domain.port.RefreshSessionRepository;
import com.agent2026.interview.identity.domain.port.UserAccountRepository;
import com.agent2026.interview.identity.infrastructure.jwt.IssuedJwt;
import com.agent2026.interview.identity.infrastructure.jwt.JwtTokenService;
import com.agent2026.interview.identity.infrastructure.jwt.VerifiedJwt;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class AuthApplicationService {
    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final UsernameNormalizer usernameNormalizer;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final Clock clock;

    public AuthApplicationService(UserAccountRepository users, RefreshSessionRepository sessions,
                                  UsernameNormalizer usernameNormalizer, PasswordPolicy passwordPolicy,
                                  PasswordEncoder passwordEncoder, JwtTokenService tokens, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.usernameNormalizer = usernameNormalizer;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Transactional
    public AuthenticationResult register(String rawUsername, String password) {
        String username = usernameNormalizer.display(rawUsername);
        String normalized = usernameNormalizer.normalize(username);
        passwordPolicy.validate(password);
        if (users.findByNormalizedUsername(normalized).isPresent()) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        UserAccount user;
        try {
            user = users.create(username, normalized, passwordEncoder.encode(password));
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        return createLoginSession(user);
    }

    @Transactional
    public AuthenticationResult login(String rawUsername, String password) {
        String normalized;
        try {
            normalized = usernameNormalizer.normalize(usernameNormalizer.display(rawUsername));
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.AUTH_CREDENTIALS_INVALID);
        }
        UserAccount user = users.findByNormalizedUsername(normalized)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_CREDENTIALS_INVALID));
        if (!passwordEncoder.matches(password == null ? "" : password, user.passwordHash())) {
            throw new BusinessException(ErrorCode.AUTH_CREDENTIALS_INVALID);
        }
        requireActive(user);
        return createLoginSession(user);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthenticationResult refresh(String rawRefreshToken) {
        VerifiedJwt verified = verifyRefresh(rawRefreshToken);
        RefreshSession current = sessions.findByJtiForUpdate(verified.jti())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        if (!tokens.hashMatches(rawRefreshToken, current.refreshTokenHash())
                || !current.userId().equals(verified.userId())
                || !current.tokenFamilyId().equals(verified.tokenFamilyId())) {
            sessions.revokeFamily(current.tokenFamilyId(), now);
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REPLAYED);
        }
        if (current.isRevoked()) {
            if (current.replacedByJti() != null) {
                sessions.revokeFamily(current.tokenFamilyId(), now);
                throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REPLAYED);
            }
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        if (current.isExpired(now)) {
            sessions.revoke(current.id(), now);
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        UserAccount user = users.findById(current.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        requireActive(user);

        IssuedJwt access = tokens.issueAccess(user.id());
        IssuedJwt refresh = tokens.issueRefresh(user.id(), current.tokenFamilyId());
        sessions.create(user.id(), refresh.jti(), current.tokenFamilyId(), tokens.hash(refresh.token()),
                LocalDateTime.ofInstant(refresh.expiresAt(), ZoneOffset.UTC));
        sessions.markRotated(current.id(), refresh.jti(), now);
        return result(user, access, refresh);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        VerifiedJwt verified;
        try {
            verified = tokens.verifyRefresh(rawRefreshToken);
        } catch (JWTVerificationException ex) {
            return;
        }
        sessions.findByJtiForUpdate(verified.jti()).ifPresent(session -> {
            if (tokens.hashMatches(rawRefreshToken, session.refreshTokenHash())) {
                sessions.revoke(session.id(), LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
            }
        });
    }

    public UserAccount currentUser(Long userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));
        requireActive(user);
        return user;
    }

    private AuthenticationResult createLoginSession(UserAccount user) {
        String familyId = UUID.randomUUID().toString();
        IssuedJwt access = tokens.issueAccess(user.id());
        IssuedJwt refresh = tokens.issueRefresh(user.id(), familyId);
        sessions.create(user.id(), refresh.jti(), familyId, tokens.hash(refresh.token()),
                LocalDateTime.ofInstant(refresh.expiresAt(), ZoneOffset.UTC));
        return result(user, access, refresh);
    }

    private AuthenticationResult result(UserAccount user, IssuedJwt access, IssuedJwt refresh) {
        return new AuthenticationResult(user, access.token(), access.expiresAt(),
                refresh.token(), refresh.expiresAt());
    }

    private VerifiedJwt verifyRefresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        try {
            return tokens.verifyRefresh(rawRefreshToken);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
    }

    private void requireActive(UserAccount user) {
        if (!user.isActive()) throw new BusinessException(ErrorCode.USER_DISABLED);
    }
}

CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    username VARCHAR(64) NOT NULL COMMENT 'display username',
    normalized_username VARCHAR(64) NOT NULL COMMENT 'case-normalized login username',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt password hash',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_normalized_username (normalized_username),
    KEY idx_app_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='application users';

CREATE TABLE auth_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    user_id BIGINT NOT NULL COMMENT 'authenticated user id',
    jti CHAR(36) NOT NULL COMMENT 'refresh JWT id',
    token_family_id CHAR(36) NOT NULL COMMENT 'refresh rotation family id',
    refresh_token_hash CHAR(64) NOT NULL COMMENT 'SHA-256 digest of refresh JWT',
    expires_at DATETIME(3) NOT NULL COMMENT 'refresh token expiration',
    revoked_at DATETIME(3) NULL COMMENT 'revocation time',
    replaced_by_jti CHAR(36) NULL COMMENT 'successor refresh JWT id',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_used_at DATETIME(3) NULL COMMENT 'last successful refresh time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_session_jti (jti),
    UNIQUE KEY uk_auth_session_refresh_hash (refresh_token_hash),
    KEY idx_auth_session_user (user_id),
    KEY idx_auth_session_family (token_family_id),
    KEY idx_auth_session_expiry (expires_at),
    CONSTRAINT fk_auth_session_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rotating refresh token sessions';

ALTER TABLE interview_session
    ADD COLUMN user_id BIGINT NULL COMMENT 'owning application user' AFTER id,
    ADD KEY idx_interview_session_user_time (user_id, create_time),
    ADD CONSTRAINT fk_interview_session_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT;

ALTER TABLE project_profile
    ADD COLUMN user_id BIGINT NULL COMMENT 'owning application user; null for legacy token-owned data' AFTER id,
    ADD KEY idx_project_profile_user_time (user_id, create_time),
    ADD CONSTRAINT fk_project_profile_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT;

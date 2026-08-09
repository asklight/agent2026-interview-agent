CREATE TABLE training_ability_evidence (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_session_id BIGINT NOT NULL,
    source_report_id BIGINT NULL,
    source_report_version INT NOT NULL DEFAULT 1,
    evidence_key VARCHAR(128) NOT NULL,
    dimension_code VARCHAR(64) NOT NULL,
    polarity VARCHAR(16) NOT NULL COMMENT 'STRENGTH/GAP/RISK',
    severity TINYINT NOT NULL,
    confidence DECIMAL(4,3) NOT NULL,
    evidence_text VARCHAR(1000) NOT NULL,
    source_turn_id BIGINT NULL,
    source_evaluation_id BIGINT NULL,
    metadata_json JSON NULL,
    observed_at DATETIME(3) NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_agent_evidence_source
        (source_type, source_session_id, source_report_version, evidence_key),
    KEY idx_training_agent_evidence_user_dimension (user_id, dimension_code, observed_at),
    KEY idx_training_agent_evidence_user_source (user_id, source_type, source_session_id),
    CONSTRAINT fk_training_agent_evidence_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='immutable evidence projection for personalized training';

CREATE TABLE user_ability_snapshot (
    user_id BIGINT NOT NULL,
    dimension_code VARCHAR(64) NOT NULL,
    ability_state VARCHAR(16) NOT NULL COMMENT 'UNKNOWN/NEEDS_WORK/DEVELOPING/STABLE/STRONG',
    internal_value DECIMAL(8,3) NOT NULL DEFAULT 0,
    confidence DECIMAL(4,3) NOT NULL DEFAULT 0,
    strength_count INT NOT NULL DEFAULT 0,
    gap_count INT NOT NULL DEFAULT 0,
    risk_count INT NOT NULL DEFAULT 0,
    distinct_session_count INT NOT NULL DEFAULT 0,
    last_observed_at DATETIME(3) NULL,
    aggregation_policy_version VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, dimension_code),
    CONSTRAINT fk_training_agent_snapshot_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='derived user ability snapshot';

CREATE TABLE training_recommendation (
    user_id BIGINT NOT NULL,
    recommendation_revision BIGINT NOT NULL DEFAULT 1,
    dashboard_state VARCHAR(16) NOT NULL COMMENT 'COLD_START/READY',
    primary_training_type VARCHAR(32) NULL,
    primary_dimension_code VARCHAR(64) NULL,
    primary_title VARCHAR(255) NULL,
    primary_reason VARCHAR(1000) NULL,
    primary_action_json JSON NULL,
    alternative_items_json JSON NULL,
    evidence_ids_json JSON NULL,
    recommendation_policy_version VARCHAR(32) NOT NULL,
    generated_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_training_agent_recommendation_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='current personalized training recommendation';

CREATE TABLE training_evidence_sync_state (
    source_type VARCHAR(32) NOT NULL,
    source_session_id BIGINT NOT NULL,
    source_report_version INT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL COMMENT 'PENDING/COMPLETED/FAILED/REJECTED',
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NULL,
    last_error_code VARCHAR(64) NULL,
    last_attempt_at DATETIME(3) NULL,
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (source_type, source_session_id, source_report_version),
    KEY idx_training_agent_sync_user_status (user_id, status, next_retry_at),
    CONSTRAINT fk_training_agent_sync_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='rebuildable evidence synchronization state';

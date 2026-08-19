ALTER TABLE training_evidence_sync_state
    ADD COLUMN source_completed_at DATETIME(3) NULL AFTER user_id;

CREATE TABLE training_source_scan_cursor (
    source_type VARCHAR(32) NOT NULL,
    cursor_completed_at DATETIME(3) NULL,
    cursor_session_id BIGINT NULL,
    scan_cycle BIGINT NOT NULL DEFAULT 0,
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='durable keyset cursor for completed training report discovery';

CREATE TABLE training_agent_user_lock (
    user_id BIGINT NOT NULL,
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_training_agent_lock_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='per-user serialization lock owned by the training agent';

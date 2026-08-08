CREATE TABLE training_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    training_type VARCHAR(32) NOT NULL COMMENT 'KNOWLEDGE/PROJECT_DEEP_DIVE/ALGORITHM',
    source_session_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(1000) NULL,
    hidden TINYINT(1) NOT NULL DEFAULT 0,
    started_at DATETIME(3) NOT NULL,
    finished_at DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_history_source (training_type, source_session_id),
    KEY idx_training_history_user_time (user_id, hidden, started_at),
    CONSTRAINT fk_training_history_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rebuildable unified training history index';

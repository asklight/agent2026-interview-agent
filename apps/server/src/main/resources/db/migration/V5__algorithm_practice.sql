CREATE TABLE algorithm_problem (
    id BIGINT NOT NULL AUTO_INCREMENT,
    problem_code VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    statement TEXT NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    tags VARCHAR(255) NOT NULL,
    constraints_json JSON NOT NULL,
    evaluation_rubric_json JSON NOT NULL,
    follow_up_templates_json JSON NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_algorithm_problem_code (problem_code),
    KEY idx_algorithm_problem_filter (enabled, difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='structured algorithm oral interview problems';

CREATE TABLE algorithm_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/FINISHED/ABANDONED',
    current_stage VARCHAR(32) NOT NULL DEFAULT 'CLARIFY',
    version BIGINT NOT NULL DEFAULT 0,
    started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    finished_at DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_algorithm_session_user_time (user_id, create_time),
    KEY idx_algorithm_session_status (status),
    CONSTRAINT fk_algorithm_session_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_algorithm_session_problem FOREIGN KEY (problem_id) REFERENCES algorithm_problem (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='algorithm oral interview sessions';

CREATE TABLE algorithm_turn (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    role VARCHAR(16) NOT NULL COMMENT 'INTERVIEWER/CANDIDATE',
    stage VARCHAR(32) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    input_modality VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    parent_turn_id BIGINT NULL,
    client_turn_id VARCHAR(64) NULL,
    processing_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED' COMMENT 'PROCESSING/COMPLETED/RETRYABLE_FAILED',
    processing_started_at DATETIME(3) NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_algorithm_turn_sequence (session_id, sequence_no),
    UNIQUE KEY uk_algorithm_turn_client (session_id, client_turn_id),
    KEY idx_algorithm_turn_processing (processing_status, processing_started_at),
    CONSTRAINT fk_algorithm_turn_session FOREIGN KEY (session_id) REFERENCES algorithm_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_algorithm_turn_parent FOREIGN KEY (parent_turn_id) REFERENCES algorithm_turn (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='public algorithm interview conversation turns';

CREATE TABLE algorithm_turn_evaluation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    candidate_turn_id BIGINT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    evaluation_json JSON NOT NULL,
    model_response_hash CHAR(64) NULL,
    degraded TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_algorithm_evaluation_turn (candidate_turn_id),
    KEY idx_algorithm_evaluation_session (session_id),
    CONSTRAINT fk_algorithm_evaluation_session FOREIGN KEY (session_id) REFERENCES algorithm_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_algorithm_evaluation_turn FOREIGN KEY (candidate_turn_id) REFERENCES algorithm_turn (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='private structured algorithm turn evaluations';

CREATE TABLE algorithm_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    report_json JSON NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    generated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_algorithm_report_session (session_id),
    CONSTRAINT fk_algorithm_report_session FOREIGN KEY (session_id) REFERENCES algorithm_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='evidence-backed algorithm interview reports';

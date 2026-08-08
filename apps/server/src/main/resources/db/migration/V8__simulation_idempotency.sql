ALTER TABLE simulation_session
    ADD COLUMN client_request_id VARCHAR(64) NULL AFTER user_id,
    ADD UNIQUE KEY uk_simulation_user_request (user_id, client_request_id);

CREATE TABLE simulation_answer_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    simulation_session_id BIGINT NOT NULL,
    client_turn_id VARCHAR(64) NOT NULL,
    stage_type VARCHAR(24) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_simulation_answer_receipt (simulation_session_id, client_turn_id),
    CONSTRAINT fk_simulation_answer_receipt_session
        FOREIGN KEY (simulation_session_id) REFERENCES simulation_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

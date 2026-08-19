ALTER TABLE training_evidence_sync_state
    MODIFY COLUMN status VARCHAR(16) NOT NULL
        COMMENT 'PENDING/PROCESSING/COMPLETED/FAILED/REJECTED';

ALTER TABLE training_recommendation
    ADD COLUMN primary_estimated_minutes INT NOT NULL DEFAULT 10 AFTER primary_reason;

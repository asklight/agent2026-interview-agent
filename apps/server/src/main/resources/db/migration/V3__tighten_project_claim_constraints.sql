UPDATE project_claim
SET source_fragment = ''
WHERE source_fragment IS NULL;

UPDATE project_claim
SET expected_evidence_json = JSON_ARRAY()
WHERE expected_evidence_json IS NULL;

UPDATE project_claim
SET risk_level = 'MEDIUM'
WHERE risk_level IS NULL;

ALTER TABLE project_claim
    MODIFY COLUMN source_fragment TEXT NOT NULL COMMENT 'sanitized source fragment supporting the claim',
    MODIFY COLUMN expected_evidence_json JSON NOT NULL COMMENT 'expected evidence JSON array',
    MODIFY COLUMN risk_level VARCHAR(16) NOT NULL COMMENT 'LOW/MEDIUM/HIGH';

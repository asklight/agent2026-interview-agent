CREATE TABLE project_profile (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    access_token_hash CHAR(64) NOT NULL COMMENT 'HMAC-SHA-256 resource token digest',
    sanitized_description MEDIUMTEXT NOT NULL COMMENT 'sanitized project description',
    project_name VARCHAR(255) NULL COMMENT 'project name extracted or confirmed by user',
    summary TEXT NULL COMMENT 'structured project summary',
    tech_stack_json JSON NULL COMMENT 'technology stack JSON array',
    responsibilities_json JSON NULL COMMENT 'personal responsibilities JSON array',
    metrics_json JSON NULL COMMENT 'project metrics JSON array',
    architecture_json JSON NULL COMMENT 'architecture facts JSON value',
    uncertainties_json JSON NULL COMMENT 'uncertain extraction items JSON array',
    analysis_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ANALYZING/REVIEW_REQUIRED/READY/FAILED',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'optimistic locking version',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_profile_access_token_hash (access_token_hash),
    KEY idx_project_profile_analysis_status (analysis_status),
    KEY idx_project_profile_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='sanitized project profiles for deep-dive interviews';

CREATE TABLE project_claim (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    project_profile_id BIGINT NOT NULL COMMENT 'owning project profile id',
    claim_type VARCHAR(32) NOT NULL COMMENT 'claim category',
    statement TEXT NOT NULL COMMENT 'claim to validate during interview',
    source_fragment TEXT NULL COMMENT 'sanitized source fragment supporting the claim',
    related_technologies_json JSON NULL COMMENT 'related technologies JSON array',
    expected_evidence_json JSON NULL COMMENT 'expected evidence JSON array',
    risk_level VARCHAR(16) NULL COMMENT 'LOW/MEDIUM/HIGH',
    confirmed TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'whether user confirmed this claim',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_project_claim_profile (project_profile_id),
    KEY idx_project_claim_profile_confirmed (project_profile_id, confirmed),
    KEY idx_project_claim_type (claim_type),
    CONSTRAINT fk_project_claim_profile
        FOREIGN KEY (project_profile_id) REFERENCES project_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='verifiable claims extracted from project profiles';

CREATE TABLE project_probe_template (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    template_code VARCHAR(128) NOT NULL COMMENT 'stable template code',
    dimension VARCHAR(32) NOT NULL COMMENT 'deep-dive evaluation dimension',
    applicable_tags_json JSON NOT NULL COMMENT 'applicable technology or scenario tags',
    objective TEXT NOT NULL COMMENT 'stable interview objective',
    required_evidence_json JSON NOT NULL COMMENT 'evidence requirements JSON array',
    scoring_rubric_json JSON NOT NULL COMMENT 'dimension scoring rubric JSON value',
    follow_up_rules_json JSON NOT NULL COMMENT 'follow-up boundaries JSON value',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'whether template is active',
    version INT NOT NULL DEFAULT 1 COMMENT 'template content version',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_probe_template_code (template_code),
    KEY idx_project_probe_template_dimension (dimension),
    KEY idx_project_probe_template_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='structured first-layer project deep-dive templates';

ALTER TABLE interview_session
    MODIFY COLUMN module VARCHAR(32) NULL COMMENT 'selected module; nullable for project deep dive',
    MODIFY COLUMN current_question_type VARCHAR(16) NULL COMMENT 'legacy MAIN/FOLLOW_UP/EVALUATED state',
    ADD COLUMN feedback_timing VARCHAR(32) NOT NULL DEFAULT 'IMMEDIATE' COMMENT 'IMMEDIATE/AFTER_SESSION' AFTER mode,
    ADD COLUMN conversation_phase VARCHAR(32) NULL COMMENT 'project interview conversation phase' AFTER status,
    ADD COLUMN project_profile_id BIGINT NULL COMMENT 'project profile for project deep dive' AFTER conversation_phase,
    ADD COLUMN current_claim_id BIGINT NULL COMMENT 'claim currently being explored' AFTER project_profile_id,
    ADD COLUMN current_probe_dimension VARCHAR(32) NULL COMMENT 'current deep-dive dimension' AFTER current_claim_id,
    ADD COLUMN follow_up_count INT NOT NULL DEFAULT 0 COMMENT 'follow-ups for current claim chain' AFTER current_probe_dimension,
    ADD COLUMN max_follow_up_count INT NOT NULL DEFAULT 1 COMMENT 'maximum follow-ups for current claim chain' AFTER follow_up_count,
    ADD COLUMN input_modality VARCHAR(32) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/VOICE_TRANSCRIPT' AFTER max_follow_up_count,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT 'optimistic locking version' AFTER input_modality,
    ADD KEY idx_interview_session_mode (mode),
    ADD KEY idx_interview_session_project_profile (project_profile_id),
    ADD KEY idx_interview_session_current_claim (current_claim_id),
    ADD KEY idx_interview_session_conversation_phase (conversation_phase),
    ADD CONSTRAINT fk_interview_session_project_profile
        FOREIGN KEY (project_profile_id) REFERENCES project_profile (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_interview_session_current_claim
        FOREIGN KEY (current_claim_id) REFERENCES project_claim (id) ON DELETE SET NULL;

CREATE TABLE interview_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    session_id BIGINT NOT NULL COMMENT 'interview session id',
    project_profile_snapshot_json JSON NOT NULL COMMENT 'immutable project profile snapshot',
    planned_probes_json JSON NOT NULL COMMENT 'ordered probe plan with stable probe ids',
    template_version INT NOT NULL COMMENT 'probe template set version',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/COMPLETED/CANCELLED',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_plan_session (session_id),
    KEY idx_interview_plan_status (status),
    CONSTRAINT fk_interview_plan_session
        FOREIGN KEY (session_id) REFERENCES interview_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='immutable project interview plan snapshots';

CREATE TABLE interview_turn (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    session_id BIGINT NOT NULL COMMENT 'interview session id',
    sequence_no INT NOT NULL COMMENT 'stable message ordering within a session',
    role VARCHAR(32) NOT NULL COMMENT 'INTERVIEWER/CANDIDATE/SYSTEM',
    turn_type VARCHAR(32) NOT NULL COMMENT 'OPENING/MAIN/FOLLOW_UP/ANSWER/TRANSITION/CLOSING',
    content MEDIUMTEXT NOT NULL COMMENT 'sanitized message content',
    input_modality VARCHAR(32) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/VOICE_TRANSCRIPT',
    parent_turn_id BIGINT NULL COMMENT 'question or answer turn that this turn follows',
    claim_id BIGINT NULL COMMENT 'related project claim id',
    probe_id VARCHAR(64) NULL COMMENT 'stable probe id from interview plan snapshot',
    probe_dimension VARCHAR(32) NULL COMMENT 'deep-dive dimension covered by this turn',
    processing_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED' COMMENT 'PROCESSING/COMPLETED/RETRYABLE_FAILED',
    processing_started_at DATETIME(3) NULL COMMENT 'processing lease start time',
    client_turn_id VARCHAR(64) NULL COMMENT 'client-generated idempotency key for candidate turns',
    started_at DATETIME(3) NULL COMMENT 'future voice-compatible turn start time',
    ended_at DATETIME(3) NULL COMMENT 'future voice-compatible turn end time',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_turn_sequence (session_id, sequence_no),
    UNIQUE KEY uk_interview_turn_client_turn (session_id, client_turn_id),
    KEY idx_interview_turn_session_role (session_id, role),
    KEY idx_interview_turn_claim (claim_id),
    KEY idx_interview_turn_processing (processing_status, processing_started_at),
    CONSTRAINT fk_interview_turn_session
        FOREIGN KEY (session_id) REFERENCES interview_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_interview_turn_parent
        FOREIGN KEY (parent_turn_id) REFERENCES interview_turn (id) ON DELETE SET NULL,
    CONSTRAINT fk_interview_turn_claim
        FOREIGN KEY (claim_id) REFERENCES project_claim (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='unified interviewer, candidate, and system conversation turns';

CREATE TABLE turn_evaluation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    session_id BIGINT NOT NULL COMMENT 'interview session id',
    candidate_turn_id BIGINT NOT NULL COMMENT 'evaluated candidate turn id',
    probe_id VARCHAR(64) NULL COMMENT 'stable probe id from interview plan snapshot',
    score_json JSON NOT NULL COMMENT 'versioned dimension score JSON',
    hit_points_json JSON NOT NULL COMMENT 'evidence-backed hit points JSON array',
    missing_points_json JSON NOT NULL COMMENT 'missing evidence JSON array',
    weaknesses_json JSON NOT NULL COMMENT 'weaknesses JSON array',
    evidence_json JSON NOT NULL COMMENT 'evidence references JSON array',
    risk_flags_json JSON NOT NULL COMMENT 'non-accusatory risk flags JSON array',
    decision VARCHAR(32) NOT NULL COMMENT 'FOLLOW_UP/SWITCH_DIMENSION/SWITCH_CLAIM/WRAP_UP/FINISH',
    suggested_follow_up TEXT NULL COMMENT 'LLM suggested follow-up before policy validation',
    retrieval_trace_json JSON NULL COMMENT 'non-sensitive knowledge retrieval trace',
    model_response_hash CHAR(64) NULL COMMENT 'SHA-256 hash of raw model response',
    model_schema_version INT NOT NULL DEFAULT 1 COMMENT 'validated model output schema version',
    degraded TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'whether a degraded evaluation path was used',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_turn_evaluation_candidate_turn (candidate_turn_id),
    KEY idx_turn_evaluation_session (session_id),
    KEY idx_turn_evaluation_decision (decision),
    CONSTRAINT fk_turn_evaluation_session
        FOREIGN KEY (session_id) REFERENCES interview_session (id) ON DELETE CASCADE,
    CONSTRAINT fk_turn_evaluation_candidate_turn
        FOREIGN KEY (candidate_turn_id) REFERENCES interview_turn (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='private structured evaluation facts for candidate turns';

ALTER TABLE interview_report
    MODIFY COLUMN score_level VARCHAR(32) NULL COMMENT 'legacy score band',
    MODIFY COLUMN strengths TEXT NULL COMMENT 'legacy newline-separated strengths',
    MODIFY COLUMN weaknesses TEXT NULL COMMENT 'legacy newline-separated weaknesses',
    MODIFY COLUMN recommendations TEXT NULL COMMENT 'legacy newline-separated recommendations',
    ADD COLUMN mode VARCHAR(32) NOT NULL DEFAULT 'JAVA_CORE' COMMENT 'JAVA_CORE/PROJECT_DEEP_DIVE' AFTER session_id,
    ADD COLUMN generation_status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED' COMMENT 'PENDING/GENERATING/COMPLETED/FAILED' AFTER mode,
    ADD COLUMN report_json JSON NULL COMMENT 'versioned structured report facts' AFTER recommendations,
    ADD COLUMN schema_version INT NULL COMMENT 'report JSON schema version' AFTER report_json,
    ADD COLUMN generated_at DATETIME(3) NULL COMMENT 'structured report generation time' AFTER schema_version,
    ADD COLUMN error_code VARCHAR(64) NULL COMMENT 'stable report generation error code' AFTER generated_at,
    ADD KEY idx_interview_report_mode (mode),
    ADD KEY idx_interview_report_generation_status (generation_status);

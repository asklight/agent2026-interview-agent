CREATE TABLE IF NOT EXISTS question_card (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    card_code VARCHAR(128) NOT NULL COMMENT 'stable card code',
    module VARCHAR(32) NOT NULL COMMENT 'knowledge module',
    difficulty VARCHAR(16) NOT NULL COMMENT 'easy/medium/hard',
    main_question TEXT NOT NULL COMMENT 'main interview question',
    key_points TEXT NOT NULL COMMENT 'JSON array text of expected key points',
    common_mistakes TEXT COMMENT 'JSON array text of common mistakes',
    followups TEXT COMMENT 'JSON array text of follow-up questions',
    scenario_followups TEXT COMMENT 'JSON array text of scenario follow-ups',
    scoring_rubric TEXT COMMENT 'JSON array text of scoring rubric',
    tags VARCHAR(256) COMMENT 'comma-separated tags',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT 'whether this card is enabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_card_code (card_code),
    KEY idx_question_card_module (module),
    KEY idx_question_card_difficulty (difficulty),
    KEY idx_question_card_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='structured interview question cards';

CREATE TABLE IF NOT EXISTS interview_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    mode VARCHAR(32) NOT NULL COMMENT 'interview mode',
    module VARCHAR(32) NOT NULL COMMENT 'selected module',
    difficulty VARCHAR(16) COMMENT 'selected difficulty',
    question_count INT NOT NULL DEFAULT 5 COMMENT 'planned question count',
    completed_question_count INT NOT NULL DEFAULT 0 COMMENT 'completed main question count',
    current_question_id BIGINT COMMENT 'current question card id',
    current_question_type VARCHAR(16) NOT NULL DEFAULT 'MAIN' COMMENT 'MAIN/FOLLOW_UP/EVALUATED',
    current_follow_up_question TEXT COMMENT 'current follow-up question text',
    asked_question_ids TEXT COMMENT 'comma-separated asked question card ids',
    status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/FINISHED',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time DATETIME COMMENT 'finish time',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_interview_session_status (status),
    KEY idx_interview_session_module (module),
    KEY idx_interview_session_current_question (current_question_id),
    KEY idx_interview_session_question_type (current_question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='interview training sessions';

SET @add_completed_question_count = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE interview_session ADD COLUMN completed_question_count INT NOT NULL DEFAULT 0 COMMENT ''completed main question count'' AFTER question_count',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'interview_session'
      AND column_name = 'completed_question_count'
);
PREPARE add_completed_question_count_stmt FROM @add_completed_question_count;
EXECUTE add_completed_question_count_stmt;
DEALLOCATE PREPARE add_completed_question_count_stmt;

SET @add_current_question_type = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE interview_session ADD COLUMN current_question_type VARCHAR(16) NOT NULL DEFAULT ''MAIN'' COMMENT ''MAIN/FOLLOW_UP/EVALUATED'' AFTER current_question_id',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'interview_session'
      AND column_name = 'current_question_type'
);
PREPARE add_current_question_type_stmt FROM @add_current_question_type;
EXECUTE add_current_question_type_stmt;
DEALLOCATE PREPARE add_current_question_type_stmt;

SET @add_current_follow_up_question = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE interview_session ADD COLUMN current_follow_up_question TEXT COMMENT ''current follow-up question text'' AFTER current_question_type',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'interview_session'
      AND column_name = 'current_follow_up_question'
);
PREPARE add_current_follow_up_question_stmt FROM @add_current_follow_up_question;
EXECUTE add_current_follow_up_question_stmt;
DEALLOCATE PREPARE add_current_follow_up_question_stmt;

CREATE TABLE IF NOT EXISTS interview_answer (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    session_id BIGINT NOT NULL COMMENT 'interview session id',
    question_card_id BIGINT NOT NULL COMMENT 'question card id',
    question_text TEXT NOT NULL COMMENT 'question text at answer time',
    answer_text TEXT NOT NULL COMMENT 'user answer',
    evaluation_text TEXT COMMENT 'llm evaluation text',
    score DECIMAL(5,2) COMMENT 'optional score',
    turn_index INT NOT NULL DEFAULT 1 COMMENT 'answer turn index in session',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_interview_answer_session (session_id),
    KEY idx_interview_answer_question (question_card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='interview answer records';

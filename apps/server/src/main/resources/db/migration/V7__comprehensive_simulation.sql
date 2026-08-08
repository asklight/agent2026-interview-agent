CREATE TABLE simulation_session (
 id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, project_profile_id BIGINT NOT NULL,
 algorithm_problem_id BIGINT NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
 current_stage VARCHAR(24) NOT NULL DEFAULT 'PROJECT', version BIGINT NOT NULL DEFAULT 0,
 started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), finished_at DATETIME(3) NULL,
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), KEY idx_simulation_user_time(user_id, create_time),
 CONSTRAINT fk_simulation_user FOREIGN KEY(user_id) REFERENCES app_user(id),
 CONSTRAINT fk_simulation_profile FOREIGN KEY(project_profile_id) REFERENCES project_profile(id),
 CONSTRAINT fk_simulation_problem FOREIGN KEY(algorithm_problem_id) REFERENCES algorithm_problem(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE simulation_stage (
 id BIGINT NOT NULL AUTO_INCREMENT, simulation_session_id BIGINT NOT NULL, stage_type VARCHAR(24) NOT NULL,
 sequence_no INT NOT NULL, business_session_id BIGINT NULL, status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 started_at DATETIME(3) NULL, finished_at DATETIME(3) NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_simulation_stage(simulation_session_id, stage_type),
 CONSTRAINT fk_simulation_stage_session FOREIGN KEY(simulation_session_id) REFERENCES simulation_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE simulation_report (
 id BIGINT NOT NULL AUTO_INCREMENT, simulation_session_id BIGINT NOT NULL, report_json JSON NOT NULL,
 schema_version INT NOT NULL DEFAULT 1, generated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), UNIQUE KEY uk_simulation_report(simulation_session_id),
 CONSTRAINT fk_simulation_report_session FOREIGN KEY(simulation_session_id) REFERENCES simulation_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE interview_session ADD COLUMN simulation_id BIGINT NULL,
 ADD KEY idx_interview_session_simulation(simulation_id),
 ADD CONSTRAINT fk_interview_session_simulation FOREIGN KEY(simulation_id) REFERENCES simulation_session(id) ON DELETE SET NULL;
ALTER TABLE algorithm_session ADD COLUMN simulation_id BIGINT NULL,
 ADD KEY idx_algorithm_session_simulation(simulation_id),
 ADD CONSTRAINT fk_algorithm_session_simulation FOREIGN KEY(simulation_id) REFERENCES simulation_session(id) ON DELETE SET NULL;

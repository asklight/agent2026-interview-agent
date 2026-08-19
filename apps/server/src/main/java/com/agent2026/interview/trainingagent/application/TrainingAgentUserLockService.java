package com.agent2026.interview.trainingagent.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingAgentUserLockService {
    private final JdbcTemplate jdbc;

    public TrainingAgentUserLockService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensure(Long userId) {
        jdbc.update("INSERT IGNORE INTO training_agent_user_lock(user_id) VALUES (?)", userId);
    }
}

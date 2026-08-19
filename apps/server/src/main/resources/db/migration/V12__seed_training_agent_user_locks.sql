INSERT IGNORE INTO training_agent_user_lock (user_id)
SELECT id FROM app_user;

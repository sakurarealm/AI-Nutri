ALTER TABLE system_message_meta_records MODIFY COLUMN system_message_type VARCHAR(64) NOT NULL;
ALTER TABLE system_message_meta_records MODIFY COLUMN explanation_code VARCHAR(64) NULL;
ALTER TABLE daily_summary_records MODIFY COLUMN explanation_code VARCHAR(64) NOT NULL;

UPDATE user_capability_profiles
SET proactive_summary_enabled = 'ENABLED'
WHERE proactive_summary_enabled IS NULL OR proactive_summary_enabled = 'OFF';

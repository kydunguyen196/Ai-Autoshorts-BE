ALTER TABLE topic_ideas
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE topic_ideas
    ALTER COLUMN status SET DEFAULT 'PENDING';

UPDATE topic_ideas
SET status = 'PENDING'
WHERE status IS NULL;

CREATE INDEX IF NOT EXISTS idx_topic_ideas_status_scheduled_priority
    ON topic_ideas(status, scheduled_for, priority DESC, created_at ASC);
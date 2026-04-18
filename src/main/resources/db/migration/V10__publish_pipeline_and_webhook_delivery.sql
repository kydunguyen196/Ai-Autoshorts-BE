ALTER TABLE video_jobs
    ADD COLUMN IF NOT EXISTS audio_generation_mode VARCHAR(32),
    ADD COLUMN IF NOT EXISTS audio_provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS publish_ready_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS publish_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS publish_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS publish_attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS publish_provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS publish_external_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS publish_failure_reason TEXT,
    ADD COLUMN IF NOT EXISTS publish_failure_details TEXT,
    ADD COLUMN IF NOT EXISTS publish_last_error_at TIMESTAMPTZ;

UPDATE video_jobs
SET publish_status = CASE publish_status
    WHEN 'DRAFT' THEN 'NOT_PUBLISHED'
    WHEN 'SCHEDULED' THEN 'READY_TO_PUBLISH'
    WHEN 'QUEUED_FOR_PUBLISH' THEN 'PUBLISHING'
    WHEN 'PUBLISHED' THEN 'PUBLISHED'
    WHEN 'PUBLISH_FAILED' THEN 'PUBLISH_FAILED'
    ELSE 'NOT_PUBLISHED'
END;

UPDATE video_jobs
SET publish_status = 'NOT_PUBLISHED'
WHERE publish_status IS NULL;

UPDATE video_jobs
SET publish_status = 'READY_TO_PUBLISH'
WHERE status = 'COMPLETED'
  AND publish_status = 'NOT_PUBLISHED';

UPDATE video_jobs
SET publish_ready_at = COALESCE(publish_ready_at, completed_at)
WHERE publish_status IN ('READY_TO_PUBLISH', 'PUBLISHED', 'PUBLISHING', 'PUBLISH_FAILED');

ALTER TABLE video_jobs
    ALTER COLUMN publish_status SET DEFAULT 'NOT_PUBLISHED',
    ALTER COLUMN publish_attempt_count SET DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_video_jobs_publish_status
    ON video_jobs (user_id, publish_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_video_jobs_published_at
    ON video_jobs (published_at DESC);

CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    endpoint_url TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_attempt_at TIMESTAMPTZ,
    last_response_status INTEGER,
    last_error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_webhook_deliveries_job'
    ) THEN
        ALTER TABLE webhook_deliveries
            ADD CONSTRAINT fk_webhook_deliveries_job
            FOREIGN KEY (job_id) REFERENCES video_jobs(id)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_status_next_attempt
    ON webhook_deliveries (status, next_attempt_at ASC, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_job_created
    ON webhook_deliveries (job_id, created_at DESC);

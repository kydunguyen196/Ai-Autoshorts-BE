CREATE TABLE IF NOT EXISTS topic_ideas (
    id UUID PRIMARY KEY,
    topic VARCHAR(500) NOT NULL,
    content_style VARCHAR(100),
    priority INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(100),
    tags TEXT,
    scheduled_for TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_topic_ideas_status ON topic_ideas(status);
CREATE INDEX IF NOT EXISTS idx_topic_ideas_scheduled_for ON topic_ideas(scheduled_for);
CREATE INDEX IF NOT EXISTS idx_topic_ideas_priority_created_at ON topic_ideas(priority DESC, created_at ASC);

ALTER TABLE video_jobs
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_error_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_video_jobs_started_at ON video_jobs(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_video_jobs_completed_at ON video_jobs(completed_at DESC);
CREATE INDEX IF NOT EXISTS idx_video_jobs_last_error_at ON video_jobs(last_error_at DESC);

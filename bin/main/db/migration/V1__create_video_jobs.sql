CREATE TABLE IF NOT EXISTS video_jobs (
    id UUID PRIMARY KEY,
    topic VARCHAR(500) NOT NULL,
    style VARCHAR(100) NOT NULL,
    voice_id VARCHAR(200),
    duration_seconds INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    script_text TEXT,
    audio_url TEXT,
    subtitle_url TEXT,
    final_video_url TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_video_jobs_status ON video_jobs(status);
CREATE INDEX IF NOT EXISTS idx_video_jobs_created_at ON video_jobs(created_at DESC);

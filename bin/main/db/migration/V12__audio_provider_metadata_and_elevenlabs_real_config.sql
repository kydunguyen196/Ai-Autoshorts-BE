ALTER TABLE video_jobs
    ADD COLUMN IF NOT EXISTS audio_voice_id VARCHAR(200),
    ADD COLUMN IF NOT EXISTS audio_model_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS audio_output_format VARCHAR(80),
    ADD COLUMN IF NOT EXISTS audio_provider_request_duration_ms BIGINT,
    ADD COLUMN IF NOT EXISTS audio_failure_reason TEXT,
    ADD COLUMN IF NOT EXISTS audio_failure_details TEXT;

CREATE INDEX IF NOT EXISTS idx_video_jobs_audio_mode_created
    ON video_jobs (audio_generation_mode, created_at DESC);

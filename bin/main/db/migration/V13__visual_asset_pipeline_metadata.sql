ALTER TABLE video_jobs
    ADD COLUMN IF NOT EXISTS scene_assets_json TEXT,
    ADD COLUMN IF NOT EXISTS visual_generation_mode VARCHAR(32),
    ADD COLUMN IF NOT EXISTS visual_provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS visual_model_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS visual_failure_reason TEXT,
    ADD COLUMN IF NOT EXISTS visual_failure_details TEXT;

CREATE INDEX IF NOT EXISTS idx_video_jobs_visual_mode_created
    ON video_jobs (visual_generation_mode, created_at DESC);

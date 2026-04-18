ALTER TABLE video_jobs
    ADD COLUMN IF NOT EXISTS generation_batch_id UUID,
    ADD COLUMN IF NOT EXISTS generation_group_id UUID,
    ADD COLUMN IF NOT EXISTS variant_index INTEGER,
    ADD COLUMN IF NOT EXISTS variant_count INTEGER,
    ADD COLUMN IF NOT EXISTS ranking_score INTEGER,
    ADD COLUMN IF NOT EXISTS is_top_candidate BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS top_candidate_rank INTEGER,
    ADD COLUMN IF NOT EXISTS publish_status VARCHAR(32) DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS scheduled_publish_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS publish_platform VARCHAR(50);

UPDATE video_jobs
SET
    is_top_candidate = COALESCE(is_top_candidate, FALSE),
    publish_status = COALESCE(publish_status, 'DRAFT')
WHERE is_top_candidate IS NULL
   OR publish_status IS NULL;

ALTER TABLE video_jobs
    ALTER COLUMN is_top_candidate SET DEFAULT FALSE,
    ALTER COLUMN publish_status SET DEFAULT 'DRAFT';

CREATE INDEX IF NOT EXISTS idx_video_jobs_user_generation_group_created
    ON video_jobs (user_id, generation_group_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_video_jobs_user_generation_batch_created
    ON video_jobs (user_id, generation_batch_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_video_jobs_group_top_candidate_rank
    ON video_jobs (user_id, generation_group_id, is_top_candidate, top_candidate_rank, ranking_score DESC);

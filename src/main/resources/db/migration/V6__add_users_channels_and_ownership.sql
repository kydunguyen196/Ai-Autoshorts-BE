CREATE TABLE IF NOT EXISTS app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_users_email_lower ON app_users (LOWER(email));

CREATE TABLE IF NOT EXISTS channels (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_channels_user'
    ) THEN
        ALTER TABLE channels
            ADD CONSTRAINT fk_channels_user
            FOREIGN KEY (user_id) REFERENCES app_users(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_channels_user_created_at ON channels(user_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_channels_user_name_lower ON channels(user_id, LOWER(name));
CREATE UNIQUE INDEX IF NOT EXISTS idx_channels_one_default_per_user ON channels(user_id) WHERE is_default = TRUE;

INSERT INTO app_users (id, email, password_hash, display_name, role, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'system@autoshorts.local',
    'SYSTEM_ACCOUNT_NO_LOGIN',
    'System',
    'ADMIN',
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO channels (id, user_id, name, description, is_default, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000001',
    'System',
    'System channel for ownership backfill',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE video_jobs ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE video_jobs ADD COLUMN IF NOT EXISTS channel_id UUID;

UPDATE video_jobs
SET user_id = '00000000-0000-0000-0000-000000000001'
WHERE user_id IS NULL;

UPDATE video_jobs
SET channel_id = '00000000-0000-0000-0000-000000000010'
WHERE channel_id IS NULL;

ALTER TABLE video_jobs ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE video_jobs ALTER COLUMN channel_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_video_jobs_user'
    ) THEN
        ALTER TABLE video_jobs
            ADD CONSTRAINT fk_video_jobs_user
            FOREIGN KEY (user_id) REFERENCES app_users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_video_jobs_channel'
    ) THEN
        ALTER TABLE video_jobs
            ADD CONSTRAINT fk_video_jobs_channel
            FOREIGN KEY (channel_id) REFERENCES channels(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_video_jobs_user_created_at ON video_jobs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_video_jobs_user_channel_created_at ON video_jobs(user_id, channel_id, created_at DESC);

ALTER TABLE topic_ideas ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE topic_ideas ADD COLUMN IF NOT EXISTS channel_id UUID;

UPDATE topic_ideas
SET user_id = '00000000-0000-0000-0000-000000000001'
WHERE user_id IS NULL;

UPDATE topic_ideas
SET channel_id = '00000000-0000-0000-0000-000000000010'
WHERE channel_id IS NULL;

ALTER TABLE topic_ideas ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE topic_ideas ALTER COLUMN channel_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_topic_ideas_user'
    ) THEN
        ALTER TABLE topic_ideas
            ADD CONSTRAINT fk_topic_ideas_user
            FOREIGN KEY (user_id) REFERENCES app_users(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_topic_ideas_channel'
    ) THEN
        ALTER TABLE topic_ideas
            ADD CONSTRAINT fk_topic_ideas_channel
            FOREIGN KEY (channel_id) REFERENCES channels(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_topic_ideas_user_created_at ON topic_ideas(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_topic_ideas_user_channel_created_at ON topic_ideas(user_id, channel_id, created_at DESC);

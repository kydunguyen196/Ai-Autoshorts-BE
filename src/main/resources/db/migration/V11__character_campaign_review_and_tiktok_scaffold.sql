CREATE TABLE IF NOT EXISTS character_profiles (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    archetype VARCHAR(120),
    personality TEXT,
    tone_of_voice VARCHAR(160),
    speaking_style VARCHAR(160),
    catchphrases TEXT,
    visual_style TEXT,
    language VARCHAR(32),
    target_audience VARCHAR(255),
    allowed_topics TEXT,
    forbidden_topics TEXT,
    default_voice_provider VARCHAR(80),
    default_voice_id VARCHAR(200),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_profiles_owner_user'
    ) THEN
        ALTER TABLE character_profiles
            ADD CONSTRAINT fk_character_profiles_owner_user
            FOREIGN KEY (owner_user_id) REFERENCES app_users(id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_profiles_channel'
    ) THEN
        ALTER TABLE character_profiles
            ADD CONSTRAINT fk_character_profiles_channel
            FOREIGN KEY (channel_id) REFERENCES channels(id)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_character_profiles_owner_created
    ON character_profiles(owner_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_character_profiles_owner_channel_created
    ON character_profiles(owner_user_id, channel_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_character_profiles_owner_channel_name
    ON character_profiles(owner_user_id, channel_id, LOWER(name));

CREATE TABLE IF NOT EXISTS character_campaigns (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    character_profile_id UUID,
    product_name VARCHAR(180) NOT NULL,
    product_type VARCHAR(120),
    product_description TEXT,
    product_url TEXT,
    target_platform VARCHAR(80),
    campaign_objective VARCHAR(200),
    call_to_action TEXT,
    target_audience VARCHAR(255),
    offer_summary TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_campaigns_owner_user'
    ) THEN
        ALTER TABLE character_campaigns
            ADD CONSTRAINT fk_character_campaigns_owner_user
            FOREIGN KEY (owner_user_id) REFERENCES app_users(id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_campaigns_channel'
    ) THEN
        ALTER TABLE character_campaigns
            ADD CONSTRAINT fk_character_campaigns_channel
            FOREIGN KEY (channel_id) REFERENCES channels(id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_campaigns_profile'
    ) THEN
        ALTER TABLE character_campaigns
            ADD CONSTRAINT fk_character_campaigns_profile
            FOREIGN KEY (character_profile_id) REFERENCES character_profiles(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_character_campaigns_owner_created
    ON character_campaigns(owner_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_character_campaigns_owner_channel_created
    ON character_campaigns(owner_user_id, channel_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_character_campaigns_profile
    ON character_campaigns(character_profile_id);

CREATE TABLE IF NOT EXISTS character_assets (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    character_profile_id UUID NOT NULL,
    asset_type VARCHAR(50),
    reference_prompt TEXT,
    style_metadata_json TEXT,
    source VARCHAR(80),
    storage_url TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_assets_owner_user'
    ) THEN
        ALTER TABLE character_assets
            ADD CONSTRAINT fk_character_assets_owner_user
            FOREIGN KEY (owner_user_id) REFERENCES app_users(id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_assets_channel'
    ) THEN
        ALTER TABLE character_assets
            ADD CONSTRAINT fk_character_assets_channel
            FOREIGN KEY (channel_id) REFERENCES channels(id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_character_assets_profile'
    ) THEN
        ALTER TABLE character_assets
            ADD CONSTRAINT fk_character_assets_profile
            FOREIGN KEY (character_profile_id) REFERENCES character_profiles(id)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_character_assets_profile_active
    ON character_assets(character_profile_id, active, created_at DESC);

CREATE TABLE IF NOT EXISTS tiktok_account_connections (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    platform_account_id VARCHAR(255),
    platform_username VARCHAR(255),
    access_token_encrypted TEXT,
    refresh_token_encrypted TEXT,
    token_expires_at TIMESTAMPTZ,
    scopes TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    last_sync_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tiktok_connections_owner_user'
    ) THEN
        ALTER TABLE tiktok_account_connections
            ADD CONSTRAINT fk_tiktok_connections_owner_user
            FOREIGN KEY (owner_user_id) REFERENCES app_users(id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tiktok_connections_channel'
    ) THEN
        ALTER TABLE tiktok_account_connections
            ADD CONSTRAINT fk_tiktok_connections_channel
            FOREIGN KEY (channel_id) REFERENCES channels(id)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_tiktok_connections_owner_channel
    ON tiktok_account_connections(owner_user_id, channel_id);

CREATE INDEX IF NOT EXISTS idx_tiktok_connections_owner_status
    ON tiktok_account_connections(owner_user_id, status, updated_at DESC);

ALTER TABLE video_jobs
    ADD COLUMN IF NOT EXISTS character_profile_id UUID,
    ADD COLUMN IF NOT EXISTS character_campaign_id UUID,
    ADD COLUMN IF NOT EXISTS story_angle VARCHAR(200),
    ADD COLUMN IF NOT EXISTS product_placement_mode VARCHAR(64),
    ADD COLUMN IF NOT EXISTS ad_disclosure_mode VARCHAR(64),
    ADD COLUMN IF NOT EXISTS scene_count_target INTEGER,
    ADD COLUMN IF NOT EXISTS character_consistency_mode VARCHAR(64),
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reviewed_by UUID,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS selected_for_publish BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS publish_request_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS publish_response_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS publish_target_account_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS publish_last_status_check_at TIMESTAMPTZ;

UPDATE video_jobs
SET review_status = CASE
    WHEN status = 'COMPLETED' THEN 'GENERATED'
    ELSE 'DRAFT'
END
WHERE review_status IS NULL;

ALTER TABLE video_jobs
    ALTER COLUMN review_status SET DEFAULT 'DRAFT';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_video_jobs_character_profile'
    ) THEN
        ALTER TABLE video_jobs
            ADD CONSTRAINT fk_video_jobs_character_profile
            FOREIGN KEY (character_profile_id) REFERENCES character_profiles(id)
            ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_video_jobs_character_campaign'
    ) THEN
        ALTER TABLE video_jobs
            ADD CONSTRAINT fk_video_jobs_character_campaign
            FOREIGN KEY (character_campaign_id) REFERENCES character_campaigns(id)
            ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_video_jobs_reviewed_by'
    ) THEN
        ALTER TABLE video_jobs
            ADD CONSTRAINT fk_video_jobs_reviewed_by
            FOREIGN KEY (reviewed_by) REFERENCES app_users(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_video_jobs_character_profile
    ON video_jobs(user_id, character_profile_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_video_jobs_character_campaign
    ON video_jobs(user_id, character_campaign_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_video_jobs_review_status
    ON video_jobs(user_id, review_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_video_jobs_group_selected_for_publish
    ON video_jobs(user_id, generation_group_id, selected_for_publish);

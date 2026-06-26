-- Phase 3: multi-platform publishing (YouTube Shorts + Instagram Reels).
-- A generic social account connection table, modelled on tiktok_account_connections (V11) but
-- discriminated by a `platform` column so new networks slot in without new tables.

CREATE TABLE social_account_connections (
    id                       UUID PRIMARY KEY,
    owner_user_id            UUID NOT NULL,
    channel_id               UUID NOT NULL,
    platform                 VARCHAR(32) NOT NULL,
    platform_account_id      VARCHAR(255),
    platform_username        VARCHAR(255),
    access_token_encrypted   TEXT,
    refresh_token_encrypted  TEXT,
    token_expires_at         TIMESTAMPTZ,
    scopes                   TEXT,
    status                   VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    last_sync_at             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_social_connection_owner_channel_platform
        UNIQUE (owner_user_id, channel_id, platform)
);

CREATE INDEX idx_social_connection_owner_platform
    ON social_account_connections (owner_user_id, platform);

CREATE INDEX idx_social_connection_status
    ON social_account_connections (status);

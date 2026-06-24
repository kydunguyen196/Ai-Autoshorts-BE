-- =====================================================================
-- V16: Automated news ingestion, runtime settings, notifications, admin
-- =====================================================================

-- --- Account lifecycle + auto-publish flag --------------------------------
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE video_jobs ADD COLUMN IF NOT EXISTS auto_publish BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE topic_ideas ADD COLUMN IF NOT EXISTS auto_publish BOOLEAN NOT NULL DEFAULT FALSE;

-- --- News sources (RSS feeds configured per user/channel) -----------------
CREATE TABLE IF NOT EXISTS news_sources (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    feed_url TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auto_publish BOOLEAN NOT NULL DEFAULT FALSE,
    fetch_interval_minutes INTEGER NOT NULL DEFAULT 60,
    max_items_per_fetch INTEGER NOT NULL DEFAULT 5,
    content_style VARCHAR(100),
    last_fetched_at TIMESTAMPTZ,
    last_status VARCHAR(32),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_news_sources_user') THEN
        ALTER TABLE news_sources ADD CONSTRAINT fk_news_sources_user
            FOREIGN KEY (owner_user_id) REFERENCES app_users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_news_sources_channel') THEN
        ALTER TABLE news_sources ADD CONSTRAINT fk_news_sources_channel
            FOREIGN KEY (channel_id) REFERENCES channels(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_news_sources_owner ON news_sources(owner_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_news_sources_enabled ON news_sources(enabled, last_fetched_at);

-- --- News items (fetched feed entries, deduped per source) ----------------
CREATE TABLE IF NOT EXISTS news_items (
    id UUID PRIMARY KEY,
    news_source_id UUID NOT NULL,
    external_guid VARCHAR(512) NOT NULL,
    title VARCHAR(1000) NOT NULL,
    link TEXT,
    summary TEXT,
    published_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    topic_idea_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_news_items_source') THEN
        ALTER TABLE news_items ADD CONSTRAINT fk_news_items_source
            FOREIGN KEY (news_source_id) REFERENCES news_sources(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_news_items_source_guid
    ON news_items(news_source_id, external_guid);
CREATE INDEX IF NOT EXISTS idx_news_items_source_created
    ON news_items(news_source_id, created_at DESC);

-- --- Runtime settings (admin-editable config overrides) -------------------
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(150) PRIMARY KEY,
    setting_value TEXT,
    value_type VARCHAR(32) NOT NULL DEFAULT 'STRING',
    category VARCHAR(80) NOT NULL DEFAULT 'general',
    description TEXT,
    updated_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_settings_category ON app_settings(category);

-- --- Notifications --------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(48) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    data_json TEXT,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notifications_user') THEN
        ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user
            FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications(user_id, read_at, created_at DESC);

-- --- Admin audit log ------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_audit_log (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL,
    actor_email VARCHAR(320),
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(80),
    target_id VARCHAR(120),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_created ON admin_audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_actor ON admin_audit_log(actor_user_id, created_at DESC);

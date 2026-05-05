CREATE TABLE billing_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    plan_key VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL,
    credits_balance INTEGER NOT NULL,
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE credit_ledger_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    reason VARCHAR(80) NOT NULL,
    reference_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_credit_ledger_user_created ON credit_ledger_entries(user_id, created_at DESC);

ALTER TABLE video_jobs
    ADD COLUMN niche VARCHAR(80),
    ADD COLUMN platform VARCHAR(50),
    ADD COLUMN subtitle_style VARCHAR(80),
    ADD COLUMN visual_mode VARCHAR(80),
    ADD COLUMN voice_provider VARCHAR(80),
    ADD COLUMN voice_persona VARCHAR(120),
    ADD COLUMN quality_preset VARCHAR(80),
    ADD COLUMN export_status VARCHAR(32) NOT NULL DEFAULT 'NOT_READY',
    ADD COLUMN download_url TEXT,
    ADD COLUMN estimated_cost_credits INTEGER;

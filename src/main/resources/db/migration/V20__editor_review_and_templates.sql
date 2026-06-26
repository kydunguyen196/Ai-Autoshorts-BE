-- Phase 5: editor/preview (review-before-render) + template & brand kit.

-- 1) Review-before-render gate on the job. Opt-in; existing jobs render straight through.
ALTER TABLE video_jobs ADD COLUMN review_before_render BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE video_jobs ADD COLUMN render_approved BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE video_jobs ADD COLUMN template_id UUID;

-- 2) Brand kit on the channel (logo overlay + colors + intro/outro clips).
ALTER TABLE channels ADD COLUMN brand_logo_url TEXT;
ALTER TABLE channels ADD COLUMN brand_primary_color VARCHAR(16);
ALTER TABLE channels ADD COLUMN brand_accent_color VARCHAR(16);
ALTER TABLE channels ADD COLUMN brand_intro_url TEXT;
ALTER TABLE channels ADD COLUMN brand_outro_url TEXT;

-- 3) Reusable video templates (layout / caption styling) owned by a user.
CREATE TABLE video_templates (
    id                UUID PRIMARY KEY,
    owner_user_id     UUID NOT NULL,
    name              VARCHAR(150) NOT NULL,
    description       VARCHAR(500),
    caption_position  VARCHAR(32) NOT NULL DEFAULT 'BOTTOM',
    font_family       VARCHAR(120),
    primary_color     VARCHAR(16),
    accent_color      VARCHAR(16),
    is_default        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_video_templates_owner ON video_templates (owner_user_id);

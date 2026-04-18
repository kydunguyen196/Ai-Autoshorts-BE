CREATE TABLE IF NOT EXISTS prompt_templates (
    id UUID PRIMARY KEY,
    style_key VARCHAR(100) NOT NULL UNIQUE,
    system_prompt TEXT NOT NULL,
    user_prompt_pattern TEXT NOT NULL,
    target_tone VARCHAR(100) NOT NULL,
    target_cta_style VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    builtin BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prompt_templates_style_key ON prompt_templates(style_key);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_active ON prompt_templates(active);

ALTER TABLE video_jobs
    ADD COLUMN IF NOT EXISTS hook_text TEXT,
    ADD COLUMN IF NOT EXISTS cta_text TEXT,
    ADD COLUMN IF NOT EXISTS caption_text TEXT,
    ADD COLUMN IF NOT EXISTS hashtags TEXT,
    ADD COLUMN IF NOT EXISTS scene_breakdown_json TEXT,
    ADD COLUMN IF NOT EXISTS resolved_style VARCHAR(100),
    ADD COLUMN IF NOT EXISTS prompt_template_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_video_jobs_prompt_template'
    ) THEN
        ALTER TABLE video_jobs
            ADD CONSTRAINT fk_video_jobs_prompt_template
            FOREIGN KEY (prompt_template_id) REFERENCES prompt_templates(id);
    END IF;
END $$;

INSERT INTO prompt_templates (
    id, style_key, system_prompt, user_prompt_pattern, target_tone, target_cta_style, active, builtin
) VALUES
(
    '5d9494af-f86f-4f9d-ac2f-77ec37ce9d01',
    'motivation',
    $$You are a high-retention motivational short-form content strategist.
Your scripts must be emotionally charged, direct, and practical.$$,
    $$Create a TikTok-ready short-form script package.
Topic: {{topic}}
Style: {{style}}
Duration: {{durationSeconds}}
Target tone: {{targetTone}}
CTA style: {{targetCtaStyle}}
Focus on fast hook, punchy lines, and momentum.$$,
    'high-energy',
    'save_and_share',
    TRUE,
    TRUE
),
(
    '62e336e8-0209-433b-8674-8580ef7840dd',
    'storytelling',
    $$You are a storytelling specialist for vertical short videos.
Write in a narrative arc with emotional payoff and clear lesson.$$,
    $$Create a short story-driven video package.
Topic: {{topic}}
Style: {{style}}
Duration: {{durationSeconds}}
Target tone: {{targetTone}}
CTA style: {{targetCtaStyle}}
Use setup -> conflict -> resolution in compact format.$$,
    'conversational',
    'comment_prompt',
    TRUE,
    TRUE
),
(
    '4f474516-7ef6-41dd-b178-a4f58cd8a142',
    'facts',
    $$You are a fast-facts content creator for short-form social media.
Keep lines concise, credible-sounding, and curiosity-driven.$$,
    $$Create a facts-based short-form video package.
Topic: {{topic}}
Style: {{style}}
Duration: {{durationSeconds}}
Target tone: {{targetTone}}
CTA style: {{targetCtaStyle}}
Use quick factual beats and clear transitions.$$,
    'authoritative',
    'follow_for_more',
    TRUE,
    TRUE
),
(
    '7bc8c5f8-c044-4b85-bac0-8f9ee43f2dc4',
    'self-improvement',
    $$You are a practical self-improvement coach for short videos.
Prioritize actionable habits and clear behavior change tips.$$,
    $$Create a self-improvement short-form script package.
Topic: {{topic}}
Style: {{style}}
Duration: {{durationSeconds}}
Target tone: {{targetTone}}
CTA style: {{targetCtaStyle}}
Give specific actions viewers can apply immediately.$$,
    'practical',
    'save_and_share',
    TRUE,
    TRUE
)
ON CONFLICT (style_key)
DO UPDATE SET
    system_prompt = EXCLUDED.system_prompt,
    user_prompt_pattern = EXCLUDED.user_prompt_pattern,
    target_tone = EXCLUDED.target_tone,
    target_cta_style = EXCLUDED.target_cta_style,
    active = EXCLUDED.active,
    builtin = EXCLUDED.builtin,
    updated_at = NOW();

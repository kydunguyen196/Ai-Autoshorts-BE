ALTER TABLE video_jobs
    ADD COLUMN IF NOT EXISTS hook_strategy VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cta_strategy VARCHAR(100),
    ADD COLUMN IF NOT EXISTS structure_strategy VARCHAR(100),
    ADD COLUMN IF NOT EXISTS hook_strength_score INTEGER,
    ADD COLUMN IF NOT EXISTS engagement_score INTEGER,
    ADD COLUMN IF NOT EXISTS engagement_tags_json TEXT;

UPDATE prompt_templates
SET
    system_prompt = $$You are a viral short-form strategist for creator-led videos.
Write content that feels human, vivid, and platform-native.
Avoid generic templates and avoid repetitive opener phrases.$$,
    user_prompt_pattern = $$Create a viral-ready short-form video content package.
Topic: {{topic}}
Style: {{style}}
Duration: {{durationSeconds}}
Target tone: {{targetTone}}
CTA style preference: {{targetCtaStyle}}
Selected hook strategy: {{hookPattern}}
Selected CTA strategy: {{ctaPattern}}
Selected structure strategy: {{structurePattern}}

Global constraints:
- Keep language natural and spoken, not robotic.
- Vary sentence length and pacing.
- Avoid repeating stock openers like "Nobody tells you".
- Prioritize story tension, emotional stakes, and practical specificity.$$,
    hook_patterns_json = '["curiosity-gap","controversial-statement","emotional-hook","story-hook","problem-hook"]',
    cta_patterns_json = '["follow-cta","save-cta","comment-cta","reflection-cta"]',
    structure_patterns_json = '["mini-story","problem-solution-insight","narrative-lesson","list-with-story-transitions"]',
    updated_at = NOW()
WHERE style_key IN ('motivation', 'storytelling', 'facts', 'self-improvement');

UPDATE prompt_templates
SET
    target_tone = 'bold-practical',
    target_cta_style = 'follow_or_comment',
    updated_at = NOW()
WHERE style_key = 'motivation';

UPDATE prompt_templates
SET
    target_tone = 'narrative-emotional',
    target_cta_style = 'reflection_or_comment',
    updated_at = NOW()
WHERE style_key = 'storytelling';

UPDATE prompt_templates
SET
    target_tone = 'precise-curious',
    target_cta_style = 'save_or_follow',
    updated_at = NOW()
WHERE style_key = 'facts';

UPDATE prompt_templates
SET
    target_tone = 'practical-grounded',
    target_cta_style = 'save_or_reflection',
    updated_at = NOW()
WHERE style_key = 'self-improvement';

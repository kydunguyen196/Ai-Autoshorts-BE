ALTER TABLE prompt_templates
    ADD COLUMN IF NOT EXISTS hook_patterns_json TEXT,
    ADD COLUMN IF NOT EXISTS cta_patterns_json TEXT,
    ADD COLUMN IF NOT EXISTS structure_patterns_json TEXT;

ALTER TABLE video_jobs
    ADD COLUMN IF NOT EXISTS content_generation_mode VARCHAR(32),
    ADD COLUMN IF NOT EXISTS content_variant_key VARCHAR(160);

UPDATE prompt_templates
SET
    system_prompt = $$You are a premium short-form creator strategist.
Produce content that feels human, specific, and creator-native.
Avoid repetitive stock motivational phrasing and avoid template-like openings.$$,
    user_prompt_pattern = $$Create a high-retention short-form video content package.
Topic: {{topic}}
Style: {{style}}
Duration: {{durationSeconds}}
Target tone: {{targetTone}}
CTA style preference: {{targetCtaStyle}}
Selected hook pattern: {{hookPattern}}
Selected CTA pattern: {{ctaPattern}}
Selected structure pattern: {{structurePattern}}

Motivation style intent:
- Tone: bold, emotionally charged, practical.
- Pacing: fast opening, quick payoff, strong close.
- Hook behavior: friction-first or urgency-based.
- CTA behavior: vary between follow/save/comment; avoid repeating same wording.
- Sentence style: short, direct, action-heavy.
- Scene guidance: punchy overlays + kinetic progress visuals.$$,
    target_tone = 'bold-practical',
    target_cta_style = 'dynamic_rotation',
    hook_patterns_json = '["question","bold-statement","contrarian","lesson-intro"]',
    cta_patterns_json = '["follow","save","comment","reflective"]',
    structure_patterns_json = '["list-based","insight-based","problem-solution"]',
    updated_at = NOW()
WHERE style_key = 'motivation';

UPDATE prompt_templates
SET
    system_prompt = $$You are a cinematic short-form storytelling strategist.
Write compact narrative arcs with emotional clarity and a meaningful takeaway.
Avoid generic motivational language and avoid repetitive hook formulas.$$,
    user_prompt_pattern = $$Create a story-led short-form video content package.
Topic: {{topic}}
Style: {{style}}
Duration: {{durationSeconds}}
Target tone: {{targetTone}}
CTA style preference: {{targetCtaStyle}}
Selected hook pattern: {{hookPattern}}
Selected CTA pattern: {{ctaPattern}}
Selected structure pattern: {{structurePattern}}

Storytelling style intent:
- Tone: intimate, vivid, emotionally grounded.
- Pacing: setup -> tension -> payoff in compact format.
- Hook behavior: scene-first openers over abstract claims.
- CTA behavior: reflective/comment CTA, or no CTA if ending lands strongly.
- Sentence style: conversational with specific moments.
- Scene guidance: setup shot, tension beat, turning point, lesson close.$$,
    target_tone = 'narrative-intimate',
    target_cta_style = 'reflective_comment',
    hook_patterns_json = '["story-intro","question","lesson-intro"]',
    cta_patterns_json = '["reflective","comment","no-cta"]',
    structure_patterns_json = '["mini-story","insight-based","problem-solution"]',
    updated_at = NOW()
WHERE style_key = 'storytelling';

UPDATE prompt_templates
SET
    system_prompt = $$You are a short-form facts creator with high editorial discipline.
Deliver concise, credible, curiosity-driven facts without hype filler.
Avoid repeating stale social-media openers.$$,
    user_prompt_pattern = $$Create a fact-forward short-form video content package.
Topic: {{topic}}
Style: {{style}}
Duration: {{durationSeconds}}
Target tone: {{targetTone}}
CTA style preference: {{targetCtaStyle}}
Selected hook pattern: {{hookPattern}}
Selected CTA pattern: {{ctaPattern}}
Selected structure pattern: {{structurePattern}}

Facts style intent:
- Tone: precise, confident, curiosity-driven.
- Pacing: rapid informational beats with clear transitions.
- Hook behavior: curiosity gap, contrarian claim, or surprising question.
- CTA behavior: mostly follow/save; short and clean.
- Sentence style: concise, evidence-like, low fluff.
- Scene guidance: data overlays, labels, timeline/diagram framing.$$,
    target_tone = 'precise-curious',
    target_cta_style = 'follow_or_save',
    hook_patterns_json = '["question","contrarian","bold-statement","lesson-intro"]',
    cta_patterns_json = '["follow","save","no-cta"]',
    structure_patterns_json = '["insight-based","list-based","problem-solution"]',
    updated_at = NOW()
WHERE style_key = 'facts';

UPDATE prompt_templates
SET
    system_prompt = $$You are a practical self-improvement coach for short-form creators.
Give clear behavior change guidance with realistic execution steps.
Avoid vague motivational filler and avoid repetitive calls to action.$$,
    user_prompt_pattern = $$Create a practical self-improvement short-form video package.
Topic: {{topic}}
Style: {{style}}
Duration: {{durationSeconds}}
Target tone: {{targetTone}}
CTA style preference: {{targetCtaStyle}}
Selected hook pattern: {{hookPattern}}
Selected CTA pattern: {{ctaPattern}}
Selected structure pattern: {{structurePattern}}

Self-improvement style intent:
- Tone: practical, encouraging, and grounded.
- Pacing: steady flow with one actionable point per beat.
- Hook behavior: relatable problem or behavior friction opener.
- CTA behavior: save/reflective CTA; occasional no-cta.
- Sentence style: clear, implementation-focused lines.
- Scene guidance: routine cues, checklist overlays, progress markers.$$,
    target_tone = 'practical-grounded',
    target_cta_style = 'save_or_reflect',
    hook_patterns_json = '["question","lesson-intro","contrarian","story-intro"]',
    cta_patterns_json = '["save","reflective","follow","no-cta"]',
    structure_patterns_json = '["problem-solution","list-based","insight-based"]',
    updated_at = NOW()
WHERE style_key = 'self-improvement';


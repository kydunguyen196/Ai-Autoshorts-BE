# AutoShorts AI - Backend MVP (Queue + SaaS Foundation)

AutoShorts AI is a backend-first Spring Boot service that turns topics into short-form 9:16 videos.

This phase adds the first SaaS foundation layer:
- JWT authentication
- user ownership for jobs/topics
- channel grouping per user
- ownership-safe APIs on top of the existing queue-based generation engine
- frontend-prep API additions (CORS config, paged feeds, bootstrap metadata)
- content quality upgrade with style variants, viral strategy selection, and generation-mode observability
- multi-variant generation groups with ranking and top-candidate selection hooks

## Stack

- Java 17
- Spring Boot 3.x
- Gradle (Wrapper included)
- PostgreSQL + Flyway
- Redis (optional cache)
- RabbitMQ (durable queue + DLQ)
- FFmpeg
- OpenAI + ElevenLabs (real/mock/fallback aware)
- S3-compatible storage (or local mock storage)

## Core Architecture

1. API receives request and creates DB records in `PENDING`.
2. Producer publishes generation message to RabbitMQ.
3. Worker consumes queue messages and runs orchestration pipeline.
4. Pipeline updates guarded job lifecycle + step tracking.
5. Assets are produced with FFmpeg and uploaded to storage.

Ownership model:
- `app_users` own `channels`.
- `video_jobs` and `topic_ideas` are linked to both `user_id` and `channel_id`.
- Protected endpoints always scope reads/writes by authenticated user.

## API Surface

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

### Channels
- `GET /api/channels`
- `POST /api/channels`

### Videos
- `POST /api/videos/generate`
- `POST /api/videos/batch-generate`
- `GET /api/videos/{jobId}`
- `GET /api/videos`
- `GET /api/videos/feed`
- `GET /api/videos/group/{groupId}`
- `GET /api/videos/group/{groupId}/top-candidates`
- `GET /api/videos/batch/{batchId}`
- `POST /api/videos/{jobId}/retry`

### Topics
- `GET /api/topics`
- `GET /api/topics/feed`
- `POST /api/topics`
- `POST /api/topics/import`

### Frontend Metadata
- `GET /api/frontend/bootstrap`

### Health
- `GET /api/health`
- `GET /actuator/health`

## Flyway Migrations

- `V1__create_video_jobs.sql`
- `V2__add_job_step_tracking.sql`
- `V3__add_prompt_templates_and_content_metadata.sql`
- `V4__add_topic_ideas_and_job_execution_metadata.sql`
- `V5__align_topic_idea_version_and_scheduler_index.sql`
- `V6__add_users_channels_and_ownership.sql`
- `V7__upgrade_content_quality_and_generation_metadata.sql`
- `V8__viral_content_enhancement_metadata.sql`
- `V9__content_selection_engine_and_grouping.sql`

## Environment Setup (Windows PowerShell)

1. Copy env file:

```powershell
Copy-Item .env.example .env
```

2. Ensure Java 17 in current shell:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
.\gradlew.bat --version
```

3. Run preflight:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\preflight.ps1
```

4. Start local dependencies:

```powershell
docker compose up -d postgres redis rabbitmq
```

Optional MinIO if `S3_MOCK=false`:

```powershell
docker compose up -d minio minio-init
```

5. Ensure background asset exists:

```powershell
.\scripts\generate-default-background.ps1
```

6. Build and run:

```powershell
.\gradlew.bat clean build
powershell -ExecutionPolicy Bypass -File .\scripts\bootrun-local.ps1
```

## Required/Important Env Vars

- `APP_JWT_SECRET` (>= 32 chars)
- `APP_JWT_TOKEN_TTL_SECONDS`
- `APP_FFMPEG_BINARY` (or `FFMPEG_PATH`)
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`
- `APP_QUEUE_*` queue config vars
- `APP_SCHEDULER_ENABLED` (default `false`)
- `APP_WEB_CORS_*` browser CORS config vars
- `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_MOCK`

## Auth Quickstart (PowerShell)

Register:

```powershell
./samples/powershell/auth-register.ps1
```

Login and capture token:

```powershell
$login = ./samples/powershell/auth-login.ps1
$token = $login.accessToken
$token
```

Read current user:

```powershell
./samples/powershell/auth-me.ps1 -Token $token
```

Create/list channels:

```powershell
./samples/powershell/create-channel.ps1 -Token $token
./samples/powershell/list-channels.ps1 -Token $token
```

Generate/list/get/retry video jobs:

```powershell
$job = ./samples/powershell/generate-video.ps1 -Token $token
./samples/powershell/list-videos.ps1 -Token $token -Limit 20
./samples/powershell/list-videos-feed.ps1 -Token $token -Page 0 -Limit 20
./samples/powershell/get-video-job.ps1 -Token $token -JobId $job.jobId
./samples/powershell/retry-job.ps1 -Token $token -JobId $job.jobId
# Group/batch selection views (replace IDs from job payloads):
./samples/powershell/list-videos-by-group.ps1 -Token $token -GroupId <generationGroupId>
./samples/powershell/list-top-candidates.ps1 -Token $token -GroupId <generationGroupId>
./samples/powershell/list-videos-by-batch.ps1 -Token $token -BatchId <generationBatchId>
```

Topic APIs:

```powershell
./samples/powershell/create-topic.ps1 -Token $token
./samples/powershell/import-topics.ps1 -Token $token
./samples/powershell/list-topics.ps1 -Token $token -Limit 20
./samples/powershell/list-topics-feed.ps1 -Token $token -Page 0 -Limit 20
./samples/powershell/frontend-bootstrap.ps1 -Token $token
```

Batch and scheduler smoke:

```powershell
./samples/powershell/batch-generate.ps1 -Token $token
./samples/powershell/scheduler-smoke.ps1 -Token $token -WaitSeconds 12
```

## Auth Quickstart (curl)

Register:

```bash
bash samples/curl/auth-register.sh
```

Login (set token):

```bash
export AUTH_TOKEN="$(bash samples/curl/auth-login.sh | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
```

Call protected endpoint:

```bash
bash samples/curl/list-videos.sh
```

Protected curl samples read `AUTH_TOKEN`:
- `samples/curl/generate-video.sh`
- `samples/curl/batch-generate.sh`
- `samples/curl/list-videos.sh`
- `samples/curl/list-videos-feed.sh`
- `samples/curl/list-videos-by-group.sh` (requires `GROUP_ID`)
- `samples/curl/list-top-candidates.sh` (requires `GROUP_ID`)
- `samples/curl/list-videos-by-batch.sh` (requires `BATCH_ID`)
- `samples/curl/get-video-job.sh <jobId>`
- `samples/curl/retry-job.sh <jobId>`
- `samples/curl/create-topic.sh`
- `samples/curl/import-topics.sh`
- `samples/curl/list-topics.sh`
- `samples/curl/list-topics-feed.sh`
- `samples/curl/frontend-bootstrap.sh`
- `samples/curl/create-channel.sh`
- `samples/curl/list-channels.sh`
- `samples/curl/auth-me.sh`

Public curl samples:
- `samples/curl/health.sh`
- `samples/curl/auth-register.sh`
- `samples/curl/auth-login.sh`

## Ownership Behavior

- `GET /api/videos` returns only current user's jobs.
- `GET /api/topics` returns only current user's topics.
- `GET /api/videos/{jobId}` returns 404 for jobs not owned by caller.
- Retry, batch generation, and topic imports are all scoped to caller ownership.
- Scheduler keeps dispatching topics across users, but each dispatched job preserves original topic owner/channel.

## Queue + Retry Semantics

- Broker retry/DLQ handles transient consumer failures.
- Business retry is explicit via `POST /api/videos/{jobId}/retry` and still requires job ownership.
- Job status transitions and step tracking behavior are preserved from previous phase.

## OpenAI Mode and Verification

Mode selection:
- `OPENAI_MOCK=true`: force mock OpenAI generation mode.
- `OPENAI_MOCK=false` with `OPENAI_API_KEY` set: real OpenAI generation mode.
- `OPENAI_MOCK=false` with missing `OPENAI_API_KEY`: content layer fallback mode (safe fallback content, no real OpenAI call).

How to verify real vs mock vs fallback:
- Startup logs include: `openAiMockConfigured`, `openAiApiKeyPresent`, and `openAiEffectiveMode`.
- Content logs include:
  - `event=content_generation_source mode=REAL|MOCK ...`
  - `event=content_fallback_applied ...` when fallback content is used.
- Job detail response (`GET /api/videos/{jobId}`) now includes:
  - `contentGenerationMode` (`REAL`, `MOCK`, or `FALLBACK`)
  - `contentVariantKey` (selected style/hook/cta/structure variant)
  - `hookStrategy`, `ctaStrategy`, `structureStrategy`
  - `hookStrengthScore`, `engagementScore`, `engagementTagsJson`.

## Viral Content Enhancement (Phase 7B)

Phase 7B adds content effectiveness upgrades while preserving queue/job lifecycle/contracts:
- Hook strategy rotation: `curiosity-gap`, `controversial-statement`, `emotional-hook`, `story-hook`, `problem-hook`
- Structure strategy rotation:
  - `mini-story`
  - `problem-solution-insight`
  - `narrative-lesson`
  - `list-with-story-transitions`
- CTA strategy rotation: `follow-cta`, `save-cta`, `comment-cta`, `reflection-cta`
- Humanization pass:
  - reduces rigid `First/Second/Third` flow
  - varies sentence length and transitions
  - strips stock opener phrasing
- Heuristic quality metadata:
  - `hookStrengthScore` (0-100)
  - `engagementScore` (0-100)
  - `engagementTagsJson`

Variation-quality test checklist:
1. Submit 5-10 jobs with the same topic/style and compare:
   - `hookStrategy`
   - `structureStrategy`
   - `ctaStrategy`
   - `contentVariantKey`
2. Confirm outputs avoid stock openers and are not all list-first scripts.
3. Verify `hookStrengthScore` and `engagementScore` are populated and vary across jobs.
4. Check logs for:
   - `event=content_template_resolved ... hookType=... ctaType=... structureType=...`
   - `event=content_generation_ready ... hookScore=... engagementScore=...`

## Content Selection Engine (Phase 8A)

Phase 8A adds automation-ready multi-variant selection without changing queue execution:
- Optional `variantCount` per generation request (`1-10`).
- Batch requests support `defaultVariantCount` and per-item `variantCount`.
- Variants are grouped by:
  - `generationBatchId` (request-level batch)
  - `generationGroupId` (same topic variant group)
- Ranking formula:
  - `rankingScore = round(0.70 * engagementScore + 0.30 * hookStrengthScore)`
- Top-candidate selection:
  - rank jobs in each `generationGroupId`
  - mark `isTopCandidate=true` for top N (dynamic N from variant count, capped at 3)
  - store `topCandidateRank`

New `video_jobs` metadata fields exposed in API:
- `generationBatchId`, `generationGroupId`
- `variantIndex`, `variantCount`
- `rankingScore`, `isTopCandidate`, `topCandidateRank`
- `publishStatus`, `scheduledPublishAt`, `publishPlatform`

New query endpoints:
- `GET /api/videos/group/{groupId}`
- `GET /api/videos/group/{groupId}/top-candidates`
- `GET /api/videos/batch/{batchId}`

Multi-variant test checklist:
1. Submit `POST /api/videos/generate` with `variantCount=5`.
2. Poll the first returned job and read its `generationGroupId`.
3. Query `GET /api/videos/group/{groupId}` and verify 5 variant jobs exist.
4. Query `GET /api/videos/group/{groupId}/top-candidates` and verify ranked top candidates.
5. Check logs for `event=ranking_decision ... selected=...`.

Local mock stack still supported:
- ElevenLabs without key -> mock audio
- Storage with `S3_MOCK=true` -> local filesystem storage

## Sample Request Files

- `samples/requests/auth-register.json`
- `samples/requests/auth-login.json`
- `samples/requests/channel-create.json`
- `samples/requests/generate-video.json`
- `samples/requests/generate-video-variants.json`
- `samples/requests/generate-video-self-improvement.json`
- `samples/requests/generate-video-storytelling-viral.json`
- `samples/requests/batch-generate.json`
- `samples/requests/batch-generate-variants.json`
- `samples/requests/topic-create.json`
- `samples/requests/topic-import.json`

## Integration Test Skeletons

- `src/test/java/com/autoshorts/ai/integration/AuthOwnershipIT.java`
- `src/test/java/com/autoshorts/ai/integration/VideoGenerationFlowIT.java`
- `src/test/java/com/autoshorts/ai/integration/ContentGenerationLayerIT.java`
- `src/test/java/com/autoshorts/ai/integration/BatchGenerationIT.java`
- `src/test/java/com/autoshorts/ai/integration/SchedulerAutomationIT.java`
- `src/test/java/com/autoshorts/ai/integration/TopicImportFlowIT.java`
- `src/test/java/com/autoshorts/ai/integration/QueueExecutionIT.java`

## Out of Scope (Not Added Yet)

- Billing / credits
- TikTok publishing
- Full frontend implementation (backend frontend-preparation support only)

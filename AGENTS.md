# AGENTS.md

## Repo shape
- Single Gradle project (not a monorepo): `settings.gradle` defines root `autoshorts-ai`.
- Main code is under `src/main/**`; tests are under `src/test/**`.
- Do not edit `bin/**` (generated/copied build outputs checked into workspace, not source of truth).

## Commands that matter
- Preferred local bootstrap (Windows): `powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1`
  - Creates `.env` if missing, runs preflight, starts Docker deps, generates default background.
- Preferred app start after bootstrap: `powershell -ExecutionPolicy Bypass -File .\scripts\bootrun-local.ps1`
  - Loads `.env`, enforces Java 17, then runs `./gradlew.bat bootRun`.
- Build: `./gradlew.bat clean build`
- All tests: `./gradlew.bat test`
- Single test class: `./gradlew.bat test --tests com.autoshorts.ai.integration.QueueExecutionIT`
- Single test method: `./gradlew.bat test --tests com.autoshorts.ai.integration.QueueExecutionIT.someTestMethod`

## Runtime/infra gotchas
- Java 17 is mandatory for both shell Java and Gradle launcher JVM (validated by `scripts/preflight.ps1` and `scripts/bootrun-local.ps1`).
- Queue is enabled by default (`app.queue.enabled=true`): generating/retrying jobs expects RabbitMQ reachable.
- FFmpeg must be executable via `APP_FFMPEG_BINARY` or `FFMPEG_PATH`; helper: `scripts/generate-default-background.ps1`.
- Default background file path is `assets/backgrounds/default.mp4` (fallback generation exists, but local scripts expect this asset flow).
- `S3_MOCK=true` stores assets locally and serves them via `/api/media/**` (`LocalMediaController` only loads in mock storage mode).

## Testing quirks
- Integration tests are `*IT` with `@SpringBootTest` + `@ActiveProfiles("test")`.
- Integration tests do not use real RabbitMQ/FFmpeg: they inject mock Rabbit connection and deterministic fake FFmpeg composer.
- Integration tests need PostgreSQL backend: they try Testcontainers first, then embedded postgres fallback; suite fails fast if neither starts.

## Architecture entrypoints
- App bootstrap: `src/main/java/com/autoshorts/ai/AutoShortsAiApplication.java`
- API entry for generation/retry/review/publish: `src/main/java/com/autoshorts/ai/controller/VideoGenerationController.java`
- Queue boundary: `src/main/java/com/autoshorts/ai/queue/VideoJobQueuePublisher.java`, `src/main/java/com/autoshorts/ai/queue/VideoJobQueueConsumer.java`
- Core job lifecycle/ranking: `src/main/java/com/autoshorts/ai/service/VideoJobService.java`
- Startup mode diagnostics (REAL/MOCK/FALLBACK signals): `src/main/java/com/autoshorts/ai/config/StartupDiagnosticsLogger.java`

## Migrations and artifacts
- Flyway migrations are under `src/main/resources/db/migration` and currently include versions through `V13__...`.
- If docs conflict on migration list or feature phase, trust files in `src/main/resources/db/migration` and current code/config.

## Ignore in commits
- Never commit `.env`.
- Treat `work/` and `build/` as runtime/build artifacts.

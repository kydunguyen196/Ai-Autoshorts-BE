# AGENTS.md — Backend (`Saas/`)

> Tài liệu đầy đủ: xem `README.md` ở root workspace (`d:\project\Ai-Autoshorts\README.md`).

## Repo shape

- Single Gradle project: `settings.gradle` → root `autoshorts-ai`
- Source: `src/main/**` | Tests: `src/test/**`
- Không edit `bin/**` — generated output, không phải source

## Lệnh quan trọng

```powershell
# Bootstrap (lần đầu hoặc sau khi reset)
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
# Thêm -WithMinio nếu S3_MOCK=false

# Chạy app
powershell -ExecutionPolicy Bypass -File .\scripts\bootrun-local.ps1

# Build
.\gradlew.bat clean build

# Tests
.\gradlew.bat test
.\gradlew.bat test --tests "com.autoshorts.ai.integration.QueueExecutionIT"
.\gradlew.bat test --tests "com.autoshorts.ai.integration.QueueExecutionIT.someMethod"
```

## Runtime gotchas

- **Java 17 bắt buộc** — cả shell `java` và Gradle JVM (`preflight.ps1` validate)
- **Queue enabled mặc định** — `app.queue.enabled=true` → cần RabbitMQ
- **FFmpeg** — phải resolve qua `APP_FFMPEG_BINARY` hoặc `FFMPEG_PATH`
- **Background asset** — `assets/backgrounds/default.mp4` (tạo bằng `generate-default-background.ps1`)
- **S3_MOCK=true** (mặc định) — media serve qua `/api/media/**` bởi `LocalMediaController`

## Architecture entrypoints

| File | Vai trò |
|---|---|
| `AutoShortsAiApplication.java` | App bootstrap |
| `controller/VideoGenerationController.java` | Generate, batch, retry, review, publish API |
| `queue/VideoJobQueuePublisher.java` | Publish job lên RabbitMQ |
| `queue/VideoJobQueueConsumer.java` | Consume job từ RabbitMQ |
| `orchestration/VideoGenerationOrchestrator.java` | Pipeline chính |
| `service/VideoJobService.java` | Job lifecycle, ranking, ownership |
| `config/StartupDiagnosticsLogger.java` | Log effective mode khi khởi động |

## Testing quirks

- Integration tests: `*IT` + `@SpringBootTest` + `@ActiveProfiles("test")`
- Mock RabbitMQ: `rabbitmq-mock` (không cần Docker RabbitMQ)
- Mock FFmpeg: `DeterministicFakeVideoComposer` từ `IntegrationTestConfiguration`
- PostgreSQL: Testcontainers → embedded-postgres fallback (fail fast nếu cả hai không khả dụng)
- **Trạng thái hiện tại**: 26/28 tests `@Disabled` — cần implement

## Migrations

- Flyway: `src/main/resources/db/migration/` — **V1 đến V15**
- Khi có conflict giữa tài liệu và code → tin vào file migration thực tế
- `ddl-auto: validate` — entity thay đổi cần migration mới

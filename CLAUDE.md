# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android app project that will evolve into **3D Match Pro** (3D大赛助手), a commodity popularity management tool. The current codebase is a fresh Android Studio template — the design doc and prototype live in `maomi/`.

The `maomi/main.js` AutoJS script is the prototype to replace; `maomi/1.md` is the full feature design document.

## Build & Test

```bash
# Build the project
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests (JVM, fast)
./gradlew test

# Run a single unit test
./gradlew test --tests "com.example.demo.ExampleUnitTest"

# Run instrumented tests (require device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Architecture (Planned)

The design doc (`maomi/1.md`) specifies:

- **Language**: Kotlin
- **UI**: Jetpack Compose (not XML views)
- **Networking**: Retrofit 2 + OkHttp 4 for HTTP communication
- **Database**: Room (SQLite) for task persistence, history, and checkpoint/restore
- **Concurrency**: Kotlin coroutines for parallel task execution
- **Background**: WorkManager / Foreground Service for long-running tasks when app is backgrounded

## Key Source Locations

| Path | Purpose |
|------|---------|
| `app/src/main/java/com/example/demo/` | App source (currently just `MainActivity.kt`) |
| `app/src/main/res/` | Android resources (strings, themes, layouts) |
| `gradle/libs.versions.toml` | Version catalog — all dependency versions |
| `maomi/1.md` | Full feature design doc (UI, architecture, tech stack) |
| `maomi/main.js` | AutoJS prototype — reference for HTTP request format and logic |

## Design Decisions From Spec

- **Log rendering**: Do NOT use `TextView.append()` for live logs — the design doc explicitly warns this causes severe UI lag at high volume. Use `RecyclerView` + `DiffUtil` (or a `LazyColumn` if using Compose).
- **Network security**: Target site is HTTPS; configure `network_security_config.xml` for certificate handling.
- **Anti-detection**: Dynamic User-Agent rotation from a pool of mobile device UAs; random jitter (±20%) added to fixed request intervals.
- **Task persistence**: Room DB stores item IDs, target counts, and execution records. App must support checkpoint/restore across restarts.

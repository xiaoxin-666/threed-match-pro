# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**3D Match Pro** (3D大赛助手), a commodity popularity management tool that sends automated admiration requests to a target API. The `maomi/main.js` AutoJS script is the prototype being replaced; `maomi/1.md` is the full feature design document.

## Build & Test

```bash
./gradlew assembleDebug          # Build APK
./gradlew installDebug           # Install on device/emulator
./gradlew test                   # Run unit tests (JVM)
./gradlew test --tests "com.example.demo.ExampleUnitTest"  # Single test
./gradlew connectedAndroidTest   # Instrumented tests (need device)
./gradlew clean                  # Clean build
```

## Architecture

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose (Material3) with Navigation Compose
- **Networking**: Retrofit 2.11 + OkHttp 4.12 + Gson
- **Database**: Room 2.6.1 (SQLite) with KSP annotation processing
- **Concurrency**: Kotlin coroutines 1.9.0 with `SupervisorJob` scopes
- **Background**: Foreground Service (`TaskForegroundService`) keeps the engine alive when the app is backgrounded

### Dependency Injection

The `App` class (custom `Application`) acts as a manual DI container. All singletons are lazy-initialized there and accessed via `App.instance`:

```kotlin
class App : Application() {
    val database: AppDatabase               // Room DB
    val taskRepository: TaskRepository      // Task CRUD
    val logRepository: LogRepository        // Log persistence
    val proxyManager: ProxyManager           // Proxy persistence (SharedPreferences)
    lateinit var adoreApi: AdoreApi         // Recreated on proxy config change
    val executionEngine: ExecutionEngine    // Core task executor
}
```

### Package Structure

| Package | Purpose |
|---------|---------|
| `data/local/entity/` | Room entities: `TaskEntity`, `LogEntryEntity` |
| `data/local/dao/` | Room DAOs: `TaskDao`, `LogEntryDao` |
| `data/local/` | `AppDatabase` (Room DB, db name `threedmatch.db`), `ProxyManager` (SharedPreferences wrapper) |
| `data/remote/` | `AdoreApi` (Retrofit interface), `AdoreResponse` (Gson-mapped response), `HttpClient` / `NetworkModule` (OkHttp factory), `UserAgentPool`, `ProxyConfig`, `ConnectionTracker` |
| `data/repository/` | `TaskRepository`, `LogRepository` — thin wrappers over DAOs |
| `engine/` | `ExecutionEngine` (core task loop), `CircuitBreaker` (failure backoff) |
| `service/` | `TaskForegroundService` — foreground service bound to engine running state |
| `ui/navigation/` | `AppNavigation` — 4-tab scaffold (Dashboard, Tasks, Console, Proxy) |
| `ui/dashboard/` | `DashboardScreen` + `DashboardViewModel` — overview with start/stop all |
| `ui/task/` | `TaskListScreen`, `TaskEditDialog`, `TaskViewModel` — CRUD for tasks |
| `ui/console/` | `ConsoleScreen` + `ConsoleViewModel` — live log viewer via `SharedFlow` |
| `ui/proxy/` | `ProxyScreen` + `ProxyViewModel` — proxy host/port/auth config UI |
| `ui/theme/` | Material3 theme, colors, typography |
| `util/` | `DateFormatter`, `JitterUtil` (random ±20% interval jitter) |

### Data Flow

1. User creates a task (goodsId + count + interval) via `TaskEditDialog`
2. Task is persisted to Room via `TaskRepository`
3. `ExecutionEngine.startTask()` spawns a coroutine (`SupervisorJob` + `Dispatchers.IO`) per task
4. Each coroutine loops: call `AdoreApi.admireGoods()`, parse response, update progress, log result
5. Progress and logs are persisted to Room after each request
6. UI observes `StateFlow`/`SharedFlow` from the engine for live updates

### Routing (Navigation Compose)

Four bottom-nav tabs mapped as `Screen` sealed class subclasses:

- `dashboard` → `DashboardScreen` (engine state overview, start/stop all)
- `tasks` → `TaskListScreen` (task CRUD, FAB to create)
- `console` → `ConsoleScreen` (live `LazyColumn` log viewer)
- `proxy` → `ProxyScreen` (proxy configuration)

## Key Design Decisions

- **Log rendering**: Uses `SharedFlow` → `LazyColumn` (Compose). Do NOT use `TextView.append()` — causes severe UI lag at high volume.
- **Anti-detection**: Dynamic User-Agent rotation from `UserAgentPool`; `JitterUtil.applyJitter()` adds ±20% random jitter to fixed request intervals.
- **Circuit breaker**: `CircuitBreaker` halts requests when server returns rate-limiting errors (`"操作频繁"`), with exponential backoff. Tasks paused by circuit breaker get status `CIRCUIT_BROKEN`.
- **Task persistence**: On app start, `App.onCreate()` resets all `RUNNING` tasks to `PAUSED` (crash-safe checkpoint). Tasks survive restarts.
- **Proxy**: Proxy config is stored in SharedPreferences via `ProxyManager`. Calling `App.recreateAdoreApi()` rebuilds the OkHttp client and Retrofit instance with the new proxy settings — no app restart needed.
- **Connection tracking**: `ConnectionTracker.lookupExitIp()` queries an external IP service through a fresh OkHttp client (bypassing proxy) to determine the actual exit IP used.

## Filesystem Reference

| Path | Purpose |
|------|---------|
| `maomi/1.md` | Full feature design doc |
| `maomi/main.js` | AutoJS prototype — reference HTTP request format (`k`, `u` params, goods ID logic) |
| `gradle/libs.versions.toml` | All dependency versions |
| `app/build.gradle.kts` | App module build config (namespace, compileSdk, dependencies) |

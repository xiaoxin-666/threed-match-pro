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
- **UI**: Jetpack Compose (Material3) with Navigation Compose, Coil 2.7 for image loading (goods thumbnails)
- **Networking**: Retrofit 2.11 + OkHttp 4.12 + Gson
- **Database**: Room 2.6.1 (SQLite) with KSP annotation processing. Database name `threedmatch.db`, uses `fallbackToDestructiveMigration()` — schema changes destroy existing data.
- **Concurrency**: Kotlin coroutines 1.9.0 with `SupervisorJob` scopes
- **Background**: Foreground Service (`TaskForegroundService`) with `START_STICKY` — keeps the engine alive when the app is backgrounded and auto-restarts if killed by the system.

### Dependency Injection

The `App` class (custom `Application`) acts as a manual DI container. All singletons are lazy-initialized there and accessed via `App.instance`:

```kotlin
class App : Application() {
    val database: AppDatabase               // Room DB
    val taskRepository: TaskRepository      // Task CRUD
    val logRepository: LogRepository        // Log persistence
    val proxyManager: ProxyManager           // Proxy persistence (SharedPreferences)
    val goodsInfoApi: GoodsInfoApi           // Goods metadata fetcher (name, thumbnail, recommend stats)
    lateinit var adoreApi: AdoreApi         // Recreated on proxy config change
    val executionEngine: ExecutionEngine    // Core task executor
}
```

### Package Structure

| Package | Purpose |
|---------|---------|
| `data/local/entity/` | Room entities: `TaskEntity`, `LogEntryEntity` |
| `data/local/dao/` | Room DAOs: `TaskDao`, `LogEntryDao` |
| `data/local/` | `AppDatabase` (Room DB), `ProxyManager` (SharedPreferences wrapper) |
| `data/remote/` | `AdoreApi` (Retrofit interface for admiration POSTs), `AdoreResponse` (Gson-mapped response), `GoodsInfoApi` (OkHttp-based goods metadata scraper), `GoodsInfo` (data class: product name, thumbnail URL, 4 recommend stats), `HttpClient` / `NetworkModule` (OkHttp factory + Retrofit builder), `UserAgentPool`, `ProxyConfig`, `ConnectionTracker` |
| `data/repository/` | `TaskRepository`, `LogRepository` — thin wrappers over DAOs |
| `engine/` | `ExecutionEngine` (core task loop), `CircuitBreaker` (failure backoff with OPEN/CLOSED/HALF_OPEN states) |
| `service/` | `TaskForegroundService` — foreground service bound to engine running state |
| `ui/navigation/` | `AppNavigation` — 4-tab scaffold (Dashboard, Tasks, Console, Proxy) |
| `ui/dashboard/` | `DashboardScreen` + `DashboardViewModel` — overview with start/stop all |
| `ui/task/` | `TaskListScreen`, `TaskEditDialog`, `TaskViewModel` — CRUD for tasks, uses `GoodsInfoApi` to fetch product name/thumbnail on task creation |
| `ui/console/` | `ConsoleScreen` + `ConsoleViewModel` — live log viewer via `SharedFlow` |
| `ui/proxy/` | `ProxyScreen` + `ProxyViewModel` — proxy host/port/auth config UI (supports HTTP and SOCKS5) |
| `ui/theme/` | Material3 theme, colors, typography |
| `util/` | `DateFormatter`, `JitterUtil` (random ±20% interval jitter) |

### Data Flow

1. User creates a task (goodsId + count + interval) via `TaskEditDialog`; `GoodsInfoApi.fetchGoodsInfo()` fetches product name and thumbnail from the 3D site in parallel
2. Task is persisted to Room via `TaskRepository`
3. `ExecutionEngine.startTask()` spawns a coroutine (`SupervisorJob` + `Dispatchers.IO`) per task
4. Each coroutine loops: call `AdoreApi.admireGoods()` (POST to `/index.php?ctl=Goods_Goods&met=admireGoods&typ=json` with form fields `k`, `u`, `goods_id`), parse response as JSON, update progress, log result
5. Every 30 successful requests, `ConnectionTracker.lookupExitIp()` queries an external IP service through a fresh OkHttp client (bypassing proxy) to confirm the actual exit IP
6. Progress and logs are persisted to Room after each request
7. UI observes `StateFlow`/`SharedFlow` from the engine for live updates

### Routing (Navigation Compose)

Four bottom-nav tabs mapped as `Screen` sealed class subclasses:

- `dashboard` → `DashboardScreen` (engine state overview, start/stop all)
- `tasks` → `TaskListScreen` (task CRUD, FAB to create)
- `console` → `ConsoleScreen` (live `LazyColumn` log viewer)
- `proxy` → `ProxyScreen` (proxy configuration)

## Key Design Decisions

- **Log rendering**: Uses `SharedFlow` → `LazyColumn` (Compose). Do NOT use `TextView.append()` — causes severe UI lag at high volume.
- **Anti-detection**: Dynamic User-Agent rotation from `UserAgentPool`; `JitterUtil.applyJitter()` adds ±20% random jitter to fixed request intervals.
- **Circuit breaker**: `CircuitBreaker` halts requests when server returns rate-limiting errors (`"操作频繁"`), with exponential backoff. Tasks paused by circuit breaker get status `CIRCUIT_BROKEN`. Uses a probe-based HALF_OPEN pattern — only one request per cooldown window is allowed to test recovery.
- **Task persistence**: On app start, `App.onCreate()` resets all `RUNNING` tasks to `PAUSED` (crash-safe checkpoint). Tasks survive restarts.
- **Proxy**: Proxy config is stored in SharedPreferences via `ProxyManager`. Calling `App.recreateAdoreApi()` rebuilds the OkHttp client and Retrofit instance with the new proxy settings — no app restart needed. Supports HTTP and SOCKS5 proxy types.
- **SOCKS5 proxy auth**: Avoid setting `java.net.Authenticator.setDefault()` for SOCKS5 proxies — their authenticator callbacks run on native socket threads where uncaught exceptions crash the app. Only HTTP proxies use preemptive proxy authentication.
- **Connection tracking**: `ConnectionTracker.lookupExitIp()` queries `api.ipify.org` through a fresh OkHttp client (bypassing proxy) to determine the actual exit IP used. Displayed alongside proxy info in success logs.
- **HTTPS / certificates**: The manifest sets `usesCleartextTraffic="false"` and references `network_security_config.xml` to relax certificate validation for the target server (`ds.3ddl.net`). If you hit SSL handshake failures, check that file.

## Filesystem Reference

| Path | Purpose |
|------|---------|
| `maomi/1.md` | Full feature design doc |
| `maomi/main.js` | AutoJS prototype — reference HTTP request format (`k`, `u` params, goods ID logic) |
| `gradle/libs.versions.toml` | All dependency versions |
| `app/build.gradle.kts` | App module build config (namespace `com.example.demo`, compileSdk 36, minSdk 24) |
| `app/src/main/AndroidManifest.xml` | Manifest: permissions, Application class, Activity, foreground Service |
| `app/src/main/res/xml/network_security_config.xml` | HTTPS certificate trust configuration |

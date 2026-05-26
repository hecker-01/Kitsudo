# Plan: Wear OS companion app for Kitsudo

## Context

Kitsudo is currently a phone-only Android app for managing tasks with cascade-completing subtasks. The goal is a Wear OS companion app that lets users view their phone's tasks on the watch and tick them complete (with the same cascade semantics). No creating or editing from the watch in v1.

The watch is a *paired* companion, not standalone: the phone owns the Room database and stays the source of truth. The watch maintains a local Room cache populated via the Wearable Data Layer, and sends completion actions back via the MessageClient API.

To avoid duplicating the domain and data layers, this plan first extracts shared code into a new `:core` Android library module. Both `:app` (phone) and `:wear` (watch) depend on it.

---

## 1. Extract `:core` module

Create `core/build.gradle.kts` as a `com.android.library` + Kotlin + KSP module reusing the same versions as `:app` (AGP 9.1.0, Kotlin 2.2.10, compileSdk 36, minSdk 33). Dependencies: Room, Hilt, DataStore, Coroutines (moved from `:app`).

Add `include(":core")` to `settings.gradle.kts` and replace those dependencies in `app/build.gradle.kts` with `implementation(project(":core"))`.

**Move to `:core`** (keep package `dev.heckr.kitsudo.*`):

| From | What |
|---|---|
| `domain/` | Everything — models, repository interfaces, all use cases (pure Kotlin, already clean) |
| `data/local/` | `KitsudoDatabase`, `TaskDao`, `TaskEntity`, `TaskWithSubtasksEntity`, all migrations |
| `data/mapper/` | `TaskMapper` |
| `data/repository/` | `TaskRepositoryImpl`, `ThemeRepositoryImpl` |
| `data/preferences/` | DataStore wiring |
| `di/DatabaseModule.kt` | Provides `KitsudoDatabase`, `TaskDao` |
| `di/PreferencesModule.kt` | Provides `DataStore<Preferences>` |
| New `CoreRepositoryModule` | Binds `TaskRepository` and `ThemeRepository` (split from existing `RepositoryModule`) |
| `ui/theme/Color.kt` | Catppuccin palette `Color` constants and `accentColor()` |

**Stay in `:app`** (phone-only): `data/notification/`, `data/update/`, `presentation/`, `ui/theme/Theme.kt` (Material3 builder), `WorkManagerModule`, notification preferences binding, `KitsudoApplication`, `MainActivity`. Update remaining imports.

Verify by running `./gradlew :app:assembleDebug` — should produce a byte-identical phone APK.

---

## 2. Phone-side sync additions (in `:app`)

Add `play-services-wearable` and `kotlinx-serialization-json` to the version catalog and `:app`.

Add `TaskDto` to `:core` — a `@Serializable` data class mirroring `Task`'s persisted fields, with `TaskDto.fromDomain()` / `toDomain()` extensions.

**New package `dev.heckr.kitsudo.data.wearsync` in `:app`:**

- **`PhoneTaskPublisher`** (`@Singleton`) — collects `GetTasksUseCase().invoke()`, flattens to `List<TaskDto>`, and on every emission writes a `PutDataRequest` at path `/tasks/snapshot` via `Wearable.getDataClient(context)`. Started from `KitsudoApplication.onCreate()` in a `CoroutineScope(SupervisorJob() + Dispatchers.IO)`.
- **`PhoneSyncService : WearableListenerService`** annotated `@AndroidEntryPoint`. Injects `CascadeCompleteUseCase`. Overrides `onMessageReceived(MessageEvent)`:
  - Path `/task/toggleComplete` → payload = UTF-8 task id → run `cascadeCompleteUseCase(taskId, newCompletedState)` on a coroutine scope.
  - Path `/tasks/requestSnapshot` → triggers `PhoneTaskPublisher` to re-publish immediately.

Register the service in `app/src/main/AndroidManifest.xml` with standard wearable intent filters.

---

## 3. New `:wear` module

Create `wear/` as a `com.android.application` module:

- `applicationId = "dev.heckr.kitsudo"` (must match phone for capability discovery; same `.dev` debug suffix)
- `minSdk = 33`, `compileSdk = 36`, same Kotlin/Compose BOM versions
- Manifest declares `<uses-feature android:name="android.hardware.type.watch"/>` and `<meta-data android:name="com.google.android.wearable.standalone" android:value="false"/>`
- Reuses signing config from `:app`

**Dependencies:** `:core`, Hilt, Room, Coroutines, `androidx.wear.compose:compose-material3`, `compose-navigation`, `compose-foundation`, `androidx.wear:wear-tooling-preview`, `play-services-wearable`, `kotlinx-serialization-json`.

**Structure** (`dev.heckr.kitsudo.wear.*`):

```
wear/
├── KitsudoWearApplication.kt           // @HiltAndroidApp
├── MainActivity.kt                      // @AndroidEntryPoint; hosts WearNavHost
├── data/
│   ├── sync/
│   │   ├── WearTaskListenerService.kt  // @AndroidEntryPoint : WearableListenerService
│   │   └── TaskActionSender.kt         // @Singleton; Wearable.getMessageClient + NodeClient
│   └── di/
│       └── WearDatabaseModule.kt       // @Provides KitsudoDatabase at "kitsudo-wear.db"
├── presentation/
│   ├── WearNavHost.kt                  // SwipeDismissableNavHost: task_list / task_detail routes
│   ├── tasks/
│   │   ├── WearTaskListScreen.kt       // ScalingLazyColumn; toggle chip + tap to open detail
│   │   ├── WearTaskListViewModel.kt    // observes GetTasksUseCase; routes toggle → TaskActionSender
│   │   ├── WearTaskDetailScreen.kt     // title, description, subtask list, complete button
│   │   └── WearTaskDetailViewModel.kt  // observes GetTaskWithSubtasksUseCase
│   └── theme/
│       └── KitsudoWearTheme.kt         // Wear Material3 ColorScheme from Catppuccin Mocha colors in :core
```

The watch has **its own Room DB instance** (different file name), populated only from phone sync. ViewModels use `GetTasksUseCase` / `GetTaskWithSubtasksUseCase` from `:core` as-is. Completing a task on the watch sends a `/task/toggleComplete` message via `TaskActionSender` (optimistic local DB update first; phone snapshot reconciles within ~1 s).

---

## 4. Sync protocol

| Direction | Path | Mechanism | Payload |
|---|---|---|---|
| Phone → Watch | `/tasks/snapshot` | `DataClient` (persistent) | `Json.encodeToString(List<TaskDto>)` as `ByteArray` in a `DataMap` |
| Watch → Phone | `/task/toggleComplete` | `MessageClient` (fire-and-forget) | UTF-8 bytes of `"$taskId:$newState"` |
| Watch → Phone | `/tasks/requestSnapshot` | `MessageClient` | Empty |

`WearTaskListenerService.onDataChanged`:
1. Read snapshot bytes from the changed `DataItem`.
2. Decode to `List<TaskDto>`.
3. In a transaction: `taskDao.deleteAll()` then `taskDao.insertAll(...)`.

`TaskDao` needs two new methods added in `:core`: `deleteAll()` and `insertAll(List<TaskEntity>)`.

**First-launch bootstrap:** watch sends `/tasks/requestSnapshot` on cold start so the list populates even if the phone DB hasn't changed since pairing.

---

## 5. Reused code (from `:core`, unchanged)

- `Task`, `TaskWithSubtasks`, `SyncStatus`, `Priority` domain models
- `TaskRepository` / `TaskRepositoryImpl` — watch instantiates its own Room file but the same implementation
- `GetTasksUseCase`, `GetTaskWithSubtasksUseCase` — used as-is on watch
- `CascadeCompleteUseCase` — only called on the phone side (triggered by `PhoneSyncService`)
- Catppuccin `Color` constants — reused in `KitsudoWearTheme` without modification

---

## 6. Verification

1. **Build:** `./gradlew :app:assembleDebug :wear:assembleDebug` — both succeed with no errors.
2. **Unit tests:** `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest` — existing tests still green after the module extraction.
3. **Static analysis:** `./gradlew ktlintCheck detekt lint` across all three modules.
4. **End-to-end on hardware/emulator:**
   - Install phone APK; pair a Wear OS emulator via Android Studio or `adb forward tcp:5601 tcp:5601`.
   - Install watch APK: `./gradlew :wear:installDebug`.
   - On phone: create three tasks, one with two subtasks. Watch list should populate within ~1 s.
   - On watch: tap a subtask's toggle → watch shows it ticked; phone reflects the change within ~1 s. When all siblings complete, parent auto-ticks on both devices.
   - On watch: toggle a top-level task → all its subtasks complete on both devices.
5. **Offline read check:** disable Bluetooth on phone, open watch app → locally cached list still displays.

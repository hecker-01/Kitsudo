# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> The coding standards, naming conventions, architecture rules, and CI/CD pipeline are documented in the existing `CLAUDE.md` that ships with this repo. This file adds the operational layer: build commands, actual implementation state, and non-obvious cross-file patterns.

---

## Build & Run Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease

# Install on connected device (primary workflow)
./gradlew installDebug

# Tests
./gradlew testDebugUnitTest                                         # all unit tests
./gradlew testDebugUnitTest --tests "dev.heckr.kitsudo.domain.*"   # single package
./gradlew testDebugUnitTest --tests "FullyQualifiedClass.methodName" # single test
./gradlew connectedAndroidTest                                      # instrumented (device needed)

# Static analysis (all must pass before committing)
./gradlew ktlintCheck
./gradlew ktlintFormat   # auto-fix formatting
./gradlew lint
./gradlew detekt
```

`versionCode` is derived from `git rev-list --count HEAD` in `app/build.gradle.kts` - no manual bump needed.

---

## Architecture as Implemented

### Package layout

```
app/src/main/java/dev/heckr/kitsudo/
├── data/
│   ├── local/          # Room: KitsudoDatabase (v2), TaskDao, entities, migrations
│   ├── mapper/         # TaskMapper (Entity ↔ Domain ↔ UI)
│   ├── notification/   # NotificationHelper, NotificationScheduler, DeadlineNotificationWorker
│   ├── preferences/    # ThemeRepositoryImpl (DataStore)
│   ├── repository/     # TaskRepositoryImpl
│   └── update/         # UpdateChecker (object), AppUpdater (@Singleton)
├── di/                 # DatabaseModule, PreferencesModule, RepositoryModule, WorkManagerModule
├── domain/
│   ├── model/          # Task, TaskWithSubtasks, ThemePalette, CatppuccinFlavor, SyncStatus
│   ├── repository/     # TaskRepository, ThemeRepository (interfaces)
│   └── usecase/        # One class per operation
├── presentation/
│   ├── navigation/     # Screen (sealed class), KitsudoNavHost
│   ├── settings/       # SettingsScreen, SettingsViewModel, SettingsUiState
│   ├── tasks/
│   │   ├── components/ # AddTaskSheet, DeadlineChip, DeadlinePicker, SwipeActionBox
│   │   ├── TaskListScreen / TaskListViewModel / TaskListUiState
│   │   └── TaskDetailScreen / TaskDetailViewModel / TaskDetailUiState
│   └── theme/          # ThemeViewModel, ThemeUiState, ThemePickerSheet (labelRes() only)
└── ui/theme/           # KitsudoTheme, Color (Catppuccin palettes), Type
```

### Theme system

`ThemePalette` (domain model) is the single persisted type - stored as its enum `name` under the key `"theme_flavor"` in DataStore. Values: `MATERIAL3`, `LATTE`, `FRAPPE`, `MACCHIATO`, `MOCHA`.

`KitsudoTheme` in `ui/theme/Theme.kt` maps `ThemePalette` to a `ColorScheme`:
- `MATERIAL3` → `dynamicDark/LightColorScheme(context)` (always available, min SDK 33)
- Others → hand-crafted Catppuccin schemes

`ThemeViewModel` lives in `MainActivity` (activity-scoped) and wraps the whole composition in `KitsudoTheme`. `SettingsViewModel` independently reads/writes the same DataStore key - both stay in sync because DataStore emits to all active collectors.

`CatppuccinFlavor` still exists as a domain model but is only used by the color scheme functions in `Theme.kt`. All public API uses `ThemePalette`.

### Task data model

`Task` has `parentId: String?` for subtasks (max 2 levels enforced by UX, not schema). `GetTasksUseCase` returns `Flow<List<TaskWithSubtasks>>` (parent + children), not `Flow<List<Task>>`. The list screen uses `TaskWithSubtasksUi`; the detail screen uses `TaskUi`.

Room schema is at **version 2**. Migration 1→2 (`data/local/migration/Migrations.kt`) adds `parentId`, `deadlineAt`, `sortOrder` via `ALTER TABLE`. Never use `fallbackToDestructiveMigration`.

`TaskDao.observeTaskById()` returns `Flow<TaskEntity?>` - used in `TaskDetailViewModel` to keep the detail screen reactive after edits.

### Cascade completion

`CascadeCompleteUseCase` handles bidirectional completion logic:
- Completing/un-completing a top-level task mirrors all its subtasks.
- Completing a subtask auto-completes the parent when all siblings are done.
- Un-completing a subtask un-completes the parent if it was complete.

Both `TaskListViewModel` and `TaskDetailViewModel` route all checkbox toggles through this use case.

### Notifications

`DeadlineNotificationWorker` is a `@HiltWorker`. `KitsudoApplication` implements `Configuration.Provider` and provides `HiltWorkerFactory` - this replaces the default WorkManager initializer. The manifest disables `WorkManagerInitializer` via `tools:node="remove"`.

`NotificationScheduler` uses `WorkManager.enqueueUniqueWork` tagged `"deadline_<taskId>"` so cancellation is straightforward.

Channel creation happens in `KitsudoApplication.onCreate()` via `NotificationHelper.createChannel()`.

### In-app updater

`UpdateChecker` is an `object` singleton - safe to call from `Application.onCreate()`. It hits `api.github.com/repos/hecker-01/kitsudo/releases/latest` using `HttpURLConnection` directly (no Ktor/Retrofit - the networking spec says "choose one", but for a two-endpoint update checker adding a full HTTP client was intentionally avoided).

`AppUpdater` is a Hilt `@Singleton` that exposes `StateFlow<Status>` and `SharedFlow<Intent>` for the install-permission flow. `SettingsViewModel` collects both and surfaces them to `SettingsScreen`, which owns the `ActivityResultLauncher`.

### SwipeActionBox

Custom swipe component in `presentation/tasks/components/`. **Do not replace with `SwipeToDismissBox`** - the M3 component auto-snaps at the positional threshold (mid-drag), not on finger release.

Critical implementation details:
- `pointerInput(Unit)` - never restarts on recomposition. Callbacks are read via `rememberUpdatedState`.
- Background uses `Modifier.matchParentSize()`, not `fillMaxSize()` - `fillMaxSize()` collapses to 0 height inside an unbounded `LazyColumn` item.
- Left-swipe (delete): slides content fully off-screen, *then* fires `onSwipeLeft()`. Prevents background flash when the item is removed from the list.
- Right-swipe (complete): fires `onSwipeRight()` immediately, then springs back.

### Navigation

Three routes in `Screen.kt`:
- `task_list` → `TaskListScreen`
- `settings` → `SettingsScreen`
- `task_detail/{taskId}` → `TaskDetailScreen` (reads `taskId` via `SavedStateHandle`)

`KitsudoNavHost` sets symmetric `fadeIn/fadeOut(tween(180ms))` on all transitions. `android:windowAnimationStyle = @null` in `themes.xml` disables the system activity transition so only the Compose animation runs.

`themes.xml` uses `android:Theme.Material.NoActionBar` with `windowBackground = #1E1E2E` (Mocha base) to prevent a light flash before Compose renders.

### Detail screen auto-save

`TaskDetailViewModel.saveTitle/saveDescription` use a 400 ms debounce (`saveJob?.cancel(); delay(400); updateTask()`). Text fields call these directly from `onValueChange` - there is no explicit save button. The `rememberSaveable(task.id)` key resets fields only when a *different* task is opened, not on every recomposition.

---

## Key Deviations from the Spec

| Spec says | What's actually in use | Why |
|---|---|---|
| Ktor or Retrofit for networking | `HttpURLConnection` in `UpdateChecker` | Only two HTTP calls needed; full client adds unnecessary deps |
| `ThemeFlavor` / `CatppuccinFlavor` as the persisted type | `ThemePalette` | Added Material You; old DataStore values migrate automatically (enum names match) |
| Use cases should be in `domain/usecase/` | `GetThemeFlavorUseCase` / `SetThemeFlavorUseCase` are misnamed | These now operate on `ThemePalette`, not `CatppuccinFlavor`; rename is pending |

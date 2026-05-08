# CLAUDE.md

This file defines the rules, conventions, and expectations for all code written in this codebase.
Follow every section. Do not deviate without a documented reason.

---

## Project Overview

This is a local-first planner and task management app for Android.
Data lives on-device first (Room), and syncs externally when connectivity is available.
All features must work fully offline.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (no Java) |
| UI | Jetpack Compose |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Local DB | Room |
| Async | Kotlin Coroutines + Flow |
| Navigation | Jetpack Navigation Compose |
| Networking | Ktor or Retrofit (choose one and stick to it) |
| Serialization | kotlinx.serialization |
| Build | Gradle with version catalogs (libs.versions.toml) |
| Min SDK | 33 (Android 13) |
| Target SDK | Always latest stable |

Keep all dependencies up to date. Check for newer stable versions before adding or updating any dependency.

---

## Architecture

Use Clean Architecture with three layers. Never skip or merge layers.

```
app/
  presentation/   # Composables, ViewModels, UI state
  domain/         # Use cases, repository interfaces, domain models
  data/           # Repository implementations, Room DAOs, remote data sources, mappers
```

### Rules

- Domain layer has zero Android dependencies. It is pure Kotlin.
- Use cases hold one piece of business logic each. Name them as verbs: `GetTasksUseCase`, `SyncTasksUseCase`.
- ViewModels expose state as `StateFlow<UiState>`. Never expose mutable state directly.
- Repositories are defined as interfaces in domain and implemented in data.
- Data flows upward only: data -> domain -> presentation.
- Composables only call ViewModel functions. No business logic inside Composables.

### MVVM Pattern

```kotlin
// UiState is a sealed class or data class
data class TaskListUiState(
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ViewModel
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()
}
```

---

## Sync Strategy

The app is local-first. Sync is additive, never destructive.

- Write to Room first. Sync to remote second.
- Use a `SyncManager` or `WorkManager`-backed worker for background sync.
- Attach a `syncStatus` field to every syncable entity: `PENDING`, `SYNCED`, `CONFLICT`.
- Handle conflicts explicitly. Do not silently overwrite local data.
- Expose sync state in UiState so the UI reflects it.
- All sync operations must be retryable and idempotent.

---

## Kotlin Code Style

Follow the official Kotlin coding conventions: https://kotlinlang.org/docs/coding-conventions.html

### Naming

- Classes and objects: `PascalCase`
- Functions and variables: `camelCase`
- Constants: `SCREAMING_SNAKE_CASE` inside `companion object` or top-level `val`
- Files: match the primary class name they contain
- Boolean variables/functions: prefix with `is`, `has`, or `should`

### General Rules

- Use `data class` for models. Never use raw maps or untyped structures for domain data.
- Prefer `val` over `var`. Only use `var` when mutation is required.
- Use `object` for singletons.
- Never use `!!`. Use safe calls, `let`, `elvis`, or explicit null handling.
- Use `sealed class` or `sealed interface` for state and result types.
- Wrap all results from data sources in a `Result<T>` or a sealed `Outcome<T>` type.
- Keep functions short. If a function exceeds 30 lines, split it.
- No unused imports.
- No commented-out code in commits.

### Coroutines

- Use `viewModelScope` in ViewModels. Never create unscoped coroutines.
- Use `Dispatchers.IO` for all database and network work.
- Never block the main thread.
- Prefer `Flow` for streams of data. Use `suspend` functions for one-shot operations.
- Cancel scopes properly. Avoid leaking coroutines.

---

## Jetpack Compose

- Keep Composables small and focused. One responsibility per Composable.
- Extract reusable UI into a `components/` or `ui/common/` package.
- Never put logic in Composables. Pass state down, events up.
- Use `remember` and `derivedStateOf` correctly. Do not over-remember.
- Always provide a `Modifier` parameter to every public Composable.
- Use `LazyColumn`/`LazyRow` for all lists, never a plain `Column` with loops.
- Annotate previews with `@Preview` and `@PreviewParameter` where useful.
- Use `MaterialTheme` tokens for all colors, typography, and spacing. No hardcoded values.

```kotlin
@Composable
fun TaskItem(
    task: TaskUi,
    onChecked: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) { ... }
```

---

## Strings

- Every user-facing string goes in `res/values/strings.xml`. No string literals in Kotlin or Composable code.
- Use string arguments for dynamic content: `<string name="tasks_count">%1$d tasks</string>`
- Plural strings use `<plurals>`.
- Error messages are also in `strings.xml`.
- Organize strings by feature using prefixes: `task_detail_title`, `settings_sync_label`.
- Never use string concatenation to build user-facing text.

---

## Dependency Injection (Hilt)

- Annotate every ViewModel with `@HiltViewModel`.
- Define modules per feature or layer: `DatabaseModule`, `NetworkModule`, `RepositoryModule`.
- Bind interfaces to implementations using `@Binds` in abstract modules.
- Use `@Singleton` for app-scoped dependencies. Use `@ViewModelScoped` for ViewModel-scoped ones.
- Never pass `Context` into domain or data classes directly. Use Hilt to provide it where needed.

---

## Room (Local Database)

- Define entities with `@Entity`. Use `@PrimaryKey(autoGenerate = false)` with UUIDs.
- DAOs return `Flow<T>` for observable queries, `suspend` functions for writes.
- Use database migrations for every schema change. Never use `fallbackToDestructiveMigration` in production.
- Keep entities in `data/local/entity/`. Map them to domain models with explicit mapper functions.
- Never expose Room entities beyond the data layer.

---

## Navigation

- Use Jetpack Navigation Compose with a single `NavHost`.
- Define all routes as constants or a sealed class in a dedicated `Screen.kt` file.
- Pass only primitive arguments through navigation. Load full objects in the destination ViewModel.
- No fragment-based navigation. Compose only.

---

## Testing

Every feature requires tests. Do not merge code without tests covering the new logic.

### Unit Tests

- Test every use case. Cover the happy path and all error/edge cases.
- Test every ViewModel. Use `kotlinx-coroutines-test` and `turbine` for Flow testing.
- Mock dependencies with `MockK`.
- Follow the AAA pattern: Arrange, Act, Assert.
- Name tests descriptively: `givenEmptyTaskList_whenLoaded_thenShowsEmptyState`.

### Integration Tests

- Test Room DAOs with `androidx.room:room-testing` and an in-memory database.
- Test repository implementations against the in-memory database.

### UI Tests

- Write Compose UI tests with `androidx.compose.ui:ui-test-junit4`.
- Test all critical user flows: creating a task, completing a task, syncing state.
- Use `composeTestRule.onNodeWithContentDescription()` for semantic queries.

### Coverage Targets

- Domain layer: 90% minimum.
- Data layer: 80% minimum.
- Presentation layer: 70% minimum.

---

## Accessibility

- Every interactive Composable must have a `contentDescription`.
- Touch targets are a minimum of 48x48dp.
- Use `semantics {}` blocks to annotate custom components.
- Test with TalkBack enabled before marking any UI feature as complete.
- Do not rely on color alone to convey meaning.
- Support dynamic text sizes. Test at the largest system font scale.
- Avoid disabling system back gestures or intercepting accessibility actions without a clear reason.

---

## Lint and Static Analysis

The following tools run on every build and every pull request. A failing check blocks the merge.

### Android Lint

- Severity: `error` for all issues that affect correctness or accessibility.
- Treat `HardcodedText` as an error.
- Treat `MissingContentDescription` as an error.
- Config file: `lint.xml` in the project root.

### Detekt

- Config file: `detekt.yml` in the project root.
- Enforce: max complexity, max function length (30 lines), max file length (300 lines).
- No suppression annotations without a comment explaining why.

### ktlint

- Runs via Gradle. All code must be formatted before commit.
- Run `./gradlew ktlintFormat` before pushing.

---

## Git Conventions

### Commit Messages

Use Conventional Commits: https://www.conventionalcommits.org

```
<type>(<scope>): <short description>

[optional body]
[optional footer]
```

Types: `feat`, `fix`, `test`, `refactor`, `chore`, `docs`, `style`, `perf`

Examples:
```
feat(tasks): add recurring task support
fix(sync): resolve conflict when remote task deleted locally
test(domain): add edge case coverage for CreateTaskUseCase
```

- Subject line: max 72 characters, imperative mood, no period at the end.
- Reference issue numbers in the footer: `Closes #42`.
- One logical change per commit. Do not bundle unrelated changes.
- Do not include your name, initials or any mention of claude or other AI-tools in the commit message. The author is tracked by Git.

### Branching

- `main`: production-ready only. Protected. No direct pushes.
- `develop`: integration branch.
- Feature branches: `feature/<short-description>`
- Fix branches: `fix/<short-description>`
- Open a PR into `develop`. Squash merge.

---

## CI/CD

All of the following run on every PR and every push to `main` and `develop`.

### Pipeline Steps (in order)

1. `./gradlew ktlintCheck` — fail on formatting issues
2. `./gradlew detekt` — fail on static analysis violations
3. `./gradlew lint` — fail on lint errors
4. `./gradlew testDebugUnitTest` — fail on any test failure
5. `./gradlew connectedAndroidTest` — run UI tests on emulator
6. `./gradlew assembleRelease` — verify release build compiles

### Build Variants

- `debug`: logging enabled, test credentials allowed, no obfuscation.
- `release`: ProGuard/R8 enabled, no logs, signed.

### Secrets

- Never commit API keys, tokens, or credentials.
- Use environment variables in CI. Use `local.properties` (gitignored) for local dev.

---

## Dependency Management

- All versions are in `gradle/libs.versions.toml`. No hardcoded version strings in build files.
- Review and update dependencies before starting a new feature sprint.
- Avoid adding a dependency if the needed functionality fits in under 20 lines of Kotlin.
- Every new dependency requires a reason in the PR description.

---

## Do Not

- Do not use Java in new files.
- Do not use `GlobalScope`.
- Do not use `runBlocking` outside of tests.
- Do not use `AsyncTask` (deprecated).
- Do not store sensitive data in `SharedPreferences` without encryption. Use `EncryptedSharedPreferences` or the Jetpack Security library.
- Do not access the database or network on the main thread.
- Do not hardcode colors, dimensions, or strings.
- Do not skip writing tests because the feature is "simple".

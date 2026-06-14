package dev.heckr.kitsudo.fake

import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.M3WearColors
import dev.heckr.kitsudo.domain.model.NotificationPreferences
import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.model.TaskSortMode
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.domain.model.ThemePalette
import dev.heckr.kitsudo.domain.repository.NotificationPreferencesRepository
import dev.heckr.kitsudo.domain.repository.TaskListPreferencesRepository
import dev.heckr.kitsudo.domain.repository.TaskRepository
import dev.heckr.kitsudo.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Builds a [Task] with sensible defaults for tests. */
fun task(
    id: String,
    title: String = id,
    isCompleted: Boolean = false,
    parentId: String? = null,
    deadlineAt: Long? = null,
    sortOrder: Int = 0,
    createdAt: Long = 0L,
): Task = Task(
    id = id,
    title = title,
    description = "",
    isCompleted = isCompleted,
    createdAt = createdAt,
    syncStatus = SyncStatus.SYNCED,
    parentId = parentId,
    deadlineAt = deadlineAt,
    sortOrder = sortOrder,
)

/** In-memory [TaskRepository] backed by an insertion-ordered map. */
class FakeTaskRepository(initial: List<Task> = emptyList()) : TaskRepository {
    val store = linkedMapOf<String, Task>().apply { initial.forEach { put(it.id, it) } }

    override fun getTopLevelTasksWithSubtasks(): Flow<List<TaskWithSubtasks>> =
        flowOf(
            store.values.filter { it.parentId == null }.map { parent ->
                TaskWithSubtasks(parent, store.values.filter { it.parentId == parent.id })
            },
        )

    override fun getSubtasks(parentId: String): Flow<List<Task>> =
        flowOf(store.values.filter { it.parentId == parentId })

    override suspend fun getSubtasksOnce(parentId: String): List<Task> =
        store.values.filter { it.parentId == parentId }

    override suspend fun getTaskById(id: String): Task? = store[id]

    override fun observeTask(id: String): Flow<Task?> = flowOf(store[id])

    override suspend fun getAllTasks(): List<Task> = store.values.toList()

    override suspend fun createTask(task: Task): Result<Unit> {
        store[task.id] = task
        return Result.success(Unit)
    }

    override suspend fun updateTask(task: Task): Result<Unit> {
        store[task.id] = task
        return Result.success(Unit)
    }

    override suspend fun setCompletedForTaskAndSubtasks(id: String, isCompleted: Boolean): Result<Unit> {
        store[id]?.let { store[id] = it.copy(isCompleted = isCompleted) }
        store.values.filter { it.parentId == id }.forEach {
            store[it.id] = it.copy(isCompleted = isCompleted)
        }
        return Result.success(Unit)
    }

    override suspend fun deleteTask(id: String): Result<Unit> {
        store.remove(id)
        return Result.success(Unit)
    }

    override suspend fun deleteSubtasks(parentId: String): Result<Unit> {
        store.values.filter { it.parentId == parentId }.map { it.id }.forEach { store.remove(it) }
        return Result.success(Unit)
    }

    override suspend fun insertTasks(tasks: List<Task>): Result<Unit> {
        tasks.forEach { store[it.id] = it }
        return Result.success(Unit)
    }

    override suspend fun replaceAllTasks(tasks: List<Task>): Result<Unit> {
        store.clear()
        tasks.forEach { store[it.id] = it }
        return Result.success(Unit)
    }
}

class FakeThemeRepository(
    var palette: ThemePalette = ThemePalette.MOCHA,
    var accent: CatppuccinAccent = CatppuccinAccent.default,
) : ThemeRepository {
    private var m3: M3WearColors? = null
    override fun getThemePalette() = flowOf(palette)
    override suspend fun setThemePalette(palette: ThemePalette) { this.palette = palette }
    override fun getAccent() = flowOf(accent)
    override suspend fun setAccent(accent: CatppuccinAccent) { this.accent = accent }
    override fun getM3Colors() = flowOf(m3)
    override suspend fun setM3Colors(colors: M3WearColors) { m3 = colors }
}

class FakeNotificationPreferencesRepository(
    var prefs: NotificationPreferences = NotificationPreferences(),
) : NotificationPreferencesRepository {
    override fun observe() = flowOf(prefs)
    override suspend fun setPreReminderLeadMinutes(minutes: Set<Int>) {
        prefs = prefs.copy(preReminderLeadMinutes = minutes)
    }
    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        prefs = prefs.copy(quietHoursEnabled = enabled)
    }
    override suspend fun setQuietStartMinutes(minutes: Int) {
        prefs = prefs.copy(quietStartMinutes = minutes)
    }
    override suspend fun setQuietEndMinutes(minutes: Int) {
        prefs = prefs.copy(quietEndMinutes = minutes)
    }
    override suspend fun setSnoozeMinutes(minutes: Int) {
        prefs = prefs.copy(snoozeMinutes = minutes)
    }
}

class FakeTaskListPreferencesRepository(
    var mode: TaskSortMode = TaskSortMode.SMART,
) : TaskListPreferencesRepository {
    override fun observeSortMode() = flowOf(mode)
    override suspend fun setSortMode(mode: TaskSortMode) { this.mode = mode }
}

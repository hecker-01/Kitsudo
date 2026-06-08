package dev.heckr.kitsudo.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.heckr.kitsudo.data.notification.NotificationScheduler
import dev.heckr.kitsudo.data.update.AppUpdater
import dev.heckr.kitsudo.domain.model.DeletedTask
import dev.heckr.kitsudo.domain.model.TaskSortMode
import dev.heckr.kitsudo.domain.repository.TaskListPreferencesRepository
import dev.heckr.kitsudo.domain.usecase.CascadeCompleteUseCase
import dev.heckr.kitsudo.domain.usecase.CreateTaskUseCase
import dev.heckr.kitsudo.domain.usecase.DeleteTaskUseCase
import dev.heckr.kitsudo.domain.usecase.GetTasksUseCase
import dev.heckr.kitsudo.domain.usecase.ReorderTasksUseCase
import dev.heckr.kitsudo.domain.usecase.RestoreTaskUseCase
import dev.heckr.kitsudo.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val cascadeCompleteUseCase: CascadeCompleteUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val restoreTaskUseCase: RestoreTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase,
    private val taskListPreferencesRepository: TaskListPreferencesRepository,
    private val notificationScheduler: NotificationScheduler,
    private val appUpdater: AppUpdater,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    /** Snapshot of the most recent deletion, kept until the undo window closes. */
    private var pendingDelete: DeletedTask? = null

    /** Emits whenever a task is deleted, so the UI can offer an undo snackbar. */
    private val _deleteEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteEvents: SharedFlow<Unit> = _deleteEvents.asSharedFlow()

    init {
        appUpdater.status
            .onEach { status ->
                _uiState.update { it.copy(updateAvailable = status is AppUpdater.Status.Available) }
            }
            .launchIn(viewModelScope)

        appUpdater.syncFromChecker()

        combine(
            getTasksUseCase(),
            taskListPreferencesRepository.observeSortMode(),
        ) { tasks, sortMode -> tasks to sortMode }
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { (tasks, sortMode) ->
                val now = System.currentTimeMillis()
                val mapped = tasks.map { t -> t.toWithSubtasksUi(now) }
                val ordered = mapped.sortedByMode(sortMode)
                val overdueCount = ordered.count { it.isDeadlineOverdue }
                _uiState.update { state ->
                    state.copy(
                        allTasks = ordered,
                        tasks = ordered.applyFilter(state.filter),
                        sortMode = sortMode,
                        overdueCount = overdueCount,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
    }

    fun setFilter(filter: TaskListFilter) {
        _uiState.update { state ->
            state.copy(
                filter = filter,
                tasks = state.allTasks.applyFilter(filter),
            )
        }
    }

    fun setSortMode(mode: TaskSortMode) {
        viewModelScope.launch { taskListPreferencesRepository.setSortMode(mode) }
    }

    /**
     * Persists a manual reorder. [orderedIds] is the full visible order after the
     * drag; sortOrder is rewritten to match. Only used in [TaskSortMode.CUSTOM].
     */
    fun reorderTasks(orderedIds: List<String>) {
        viewModelScope.launch { reorderTasksUseCase(orderedIds) }
    }

    fun showAddSheet() = _uiState.update { it.copy(showAddSheet = true) }
    fun hideAddSheet() = _uiState.update { it.copy(showAddSheet = false) }

    fun addTask(title: String, description: String, deadlineAt: Long?) {
        viewModelScope.launch {
            // In custom order, new tasks go to the bottom; in smart order, sortOrder is unused.
            val bottomOrder = (_uiState.value.allTasks.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val result = createTaskUseCase(
                title = title,
                description = description,
                deadlineAt = deadlineAt,
                sortOrder = bottomOrder,
            )
            result.onSuccess { task ->
                task.deadlineAt?.let { deadline ->
                    notificationScheduler.schedule(task.id, task.title, deadline)
                }
            }
        }
        hideAddSheet()
    }

    /** Cascades: completing/un-completing a top-level task mirrors all its subtasks. */
    fun toggleComplete(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            cascadeCompleteUseCase(taskId, isCompleted)
            if (isCompleted) notificationScheduler.cancel(taskId)
        }
    }

    /** Cascades: completing a subtask may auto-complete the parent; un-completing un-completes it. */
    fun toggleSubtaskComplete(subtaskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            cascadeCompleteUseCase(subtaskId, isCompleted)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            notificationScheduler.cancel(taskId)
            deleteTaskUseCase(taskId).onSuccess { deleted ->
                pendingDelete = deleted
                _deleteEvents.tryEmit(Unit)
            }
        }
    }

    /** Re-inserts the last deleted task (and its subtasks) and reschedules its notifications. */
    fun undoDelete() {
        val deleted = pendingDelete ?: return
        pendingDelete = null
        viewModelScope.launch {
            restoreTaskUseCase(deleted).onSuccess {
                rescheduleNotifications(deleted)
            }
        }
    }

    /** Reschedules deadline notifications for a restored task and its incomplete subtasks. */
    private suspend fun rescheduleNotifications(deleted: DeletedTask) {
        val task = deleted.task
        if (!task.isCompleted) {
            task.deadlineAt?.let { notificationScheduler.schedule(task.id, task.title, it) }
        }
        deleted.subtasks.forEach { sub ->
            if (!sub.isCompleted) {
                sub.deadlineAt?.let {
                    notificationScheduler.schedule(
                        taskId = sub.id,
                        taskTitle = sub.title,
                        deadlineAt = it,
                        parentId = task.id,
                        parentTitle = task.title,
                    )
                }
            }
        }
    }
}

// -- Smart sort -------------------------------------------------------------

/**
 * Groups tasks into four ordered buckets:
 *  1. Overdue incomplete  - nearest deadline first, HIGH priority breaks ties
 *  2. Upcoming deadline incomplete - soonest deadline first, HIGH priority breaks ties
 *  3. No-deadline incomplete - HIGH priority first, then by creation time
 *  4. Completed - most recently created first
 */
private fun List<TaskWithSubtasksUi>.smartSorted(): List<TaskWithSubtasksUi> {
    val overdue = filter { !it.isCompleted && it.isDeadlineOverdue }
        .sortedWith(
            compareByDescending<TaskWithSubtasksUi> { it.isHighPriority }
                .thenBy { it.deadlineAt },
        )
    val upcoming = filter { !it.isCompleted && it.deadlineAt != null && !it.isDeadlineOverdue }
        .sortedWith(
            compareBy<TaskWithSubtasksUi> { it.deadlineAt }
                .thenByDescending { it.isHighPriority },
        )
    val noDeadline = filter { !it.isCompleted && it.deadlineAt == null }
        .sortedWith(
            compareByDescending<TaskWithSubtasksUi> { it.isHighPriority }
                .thenBy { it.createdAt },
        )
    val completed = filter { it.isCompleted }
        .sortedByDescending { it.createdAt }
    return overdue + upcoming + noDeadline + completed
}

/** Dispatches to the ordering for [mode]. */
private fun List<TaskWithSubtasksUi>.sortedByMode(mode: TaskSortMode): List<TaskWithSubtasksUi> =
    when (mode) {
        TaskSortMode.SMART -> smartSorted()
        TaskSortMode.CUSTOM -> customSorted()
        TaskSortMode.ALPHABETICAL ->
            fieldSorted(compareBy { it.title.lowercase() })
        TaskSortMode.DEADLINE ->
            fieldSorted(
                compareBy<TaskWithSubtasksUi> { it.deadlineAt == null }
                    .thenBy { it.deadlineAt }
                    .thenByDescending { it.createdAt },
            )
        TaskSortMode.NEWEST ->
            fieldSorted(compareByDescending { it.createdAt })
        TaskSortMode.OLDEST ->
            fieldSorted(compareBy { it.createdAt })
        TaskSortMode.PRIORITY ->
            fieldSorted(
                compareByDescending<TaskWithSubtasksUi> { it.isHighPriority }
                    .thenByDescending { it.createdAt },
            )
    }

/**
 * Manual order: purely by each task's sortOrder, creation time breaking ties.
 * Completion state and priority deliberately do not influence the order here.
 */
private fun List<TaskWithSubtasksUi>.customSorted(): List<TaskWithSubtasksUi> =
    sortedWith(
        compareBy<TaskWithSubtasksUi> { it.sortOrder }
            .thenByDescending { it.createdAt },
    )

/**
 * Field sorts (alphabetical, deadline, etc.) always sink completed tasks to the
 * bottom, then apply [comparator] within the incomplete and completed groups.
 */
private fun List<TaskWithSubtasksUi>.fieldSorted(
    comparator: Comparator<TaskWithSubtasksUi>,
): List<TaskWithSubtasksUi> {
    val (completed, incomplete) = partition { it.isCompleted }
    return incomplete.sortedWith(comparator) + completed.sortedWith(comparator)
}

private fun List<TaskWithSubtasksUi>.applyFilter(filter: TaskListFilter): List<TaskWithSubtasksUi> =
    when (filter) {
        TaskListFilter.ALL -> this
        TaskListFilter.ACTIVE -> filter { !it.isCompleted }
        TaskListFilter.OVERDUE -> filter { it.isDeadlineOverdue }
        TaskListFilter.COMPLETED -> filter { it.isCompleted }
    }

package dev.heckr.kitsudo.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.heckr.kitsudo.data.notification.NotificationScheduler
import dev.heckr.kitsudo.data.sync.TaskDto
import dev.heckr.kitsudo.data.update.AppUpdater
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.DeletedTask
import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import dev.heckr.kitsudo.domain.model.Tag
import dev.heckr.kitsudo.domain.model.TaskSortMode
import dev.heckr.kitsudo.domain.repository.TagRepository
import dev.heckr.kitsudo.domain.repository.TaskListPreferencesRepository
import dev.heckr.kitsudo.domain.usecase.AdvanceRecurringTaskUseCase
import dev.heckr.kitsudo.domain.usecase.CascadeCompleteUseCase
import dev.heckr.kitsudo.domain.usecase.CreateTagUseCase
import dev.heckr.kitsudo.domain.usecase.CreateTaskUseCase
import dev.heckr.kitsudo.domain.usecase.DeleteTaskUseCase
import dev.heckr.kitsudo.domain.usecase.GetTasksUseCase
import dev.heckr.kitsudo.domain.usecase.ReorderTasksUseCase
import dev.heckr.kitsudo.domain.usecase.RestoreTaskUseCase
import dev.heckr.kitsudo.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val cascadeCompleteUseCase: CascadeCompleteUseCase,
    private val advanceRecurringTaskUseCase: AdvanceRecurringTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val restoreTaskUseCase: RestoreTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase,
    private val tagRepository: TagRepository,
    private val createTagUseCase: CreateTagUseCase,
    private val taskListPreferencesRepository: TaskListPreferencesRepository,
    private val notificationScheduler: NotificationScheduler,
    private val appUpdater: AppUpdater,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    /**
     * Snapshot of the most recent deletion, kept until the undo window closes.
     * Persisted in [SavedStateHandle] (as JSON) so a process death during the
     * undo window doesn't lose the ability to restore the task.
     */
    private var pendingDelete: DeletedTask?
        get() = savedStateHandle.get<String>(KEY_PENDING_DELETE)?.let(::decodeDeletedTask)
        set(value) {
            savedStateHandle[KEY_PENDING_DELETE] = value?.let(::encodeDeletedTask)
        }

    /** Emits whenever a task is deleted, so the UI can offer an undo snackbar. */
    private val _deleteEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteEvents: SharedFlow<Unit> = _deleteEvents.asSharedFlow()

    /**
     * Emits the new deadline (epoch millis) when a recurring task is "completed"
     * and rolled forward, so the UI can confirm the next occurrence in a snackbar.
     */
    private val _recurringRescheduledEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val recurringRescheduledEvents: SharedFlow<Long> = _recurringRescheduledEvents.asSharedFlow()

    /**
     * Ticks once immediately, then every minute. Folded into the list pipeline so
     * overdue state and the smart-sort buckets re-evaluate as deadlines pass, even
     * when the database itself hasn't changed.
     */
    private val minuteTicker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_INTERVAL_MS)
        }
    }

    init {
        appUpdater.status
            .onEach { status ->
                _uiState.update { it.copy(updateAvailable = status is AppUpdater.Status.Available) }
            }
            .launchIn(viewModelScope)

        // A pending delete that survived process death: re-offer undo on launch.
        if (pendingDelete != null) {
            _uiState.update { it.copy(pendingUndoRestore = true) }
        }

        combine(
            getTasksUseCase(),
            tagRepository.observeTagsByTask(),
            tagRepository.observeTags(),
            taskListPreferencesRepository.observeSortMode(),
            minuteTicker,
        ) { tasks, tagsByTask, allTags, sortMode, _ ->
            ListInputs(tasks, tagsByTask, allTags, sortMode)
        }
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { (tasks, tagsByTask, allTags, sortMode) ->
                val now = System.currentTimeMillis()
                val mapped = tasks.map { t ->
                    t.toWithSubtasksUi(now, tagsByTask[t.task.id].orEmpty())
                }
                val ordered = mapped.sortedByMode(sortMode)
                val overdueCount = ordered.count { it.isDeadlineOverdue }
                _uiState.update { state ->
                    // Drop a tag filter that no longer points at an existing tag.
                    val tagFilter = state.tagFilter?.takeIf { id -> allTags.any { it.id == id } }
                    state.copy(
                        allTasks = ordered,
                        tasks = ordered.applyFilter(state.filter, tagFilter),
                        allTags = allTags,
                        tagFilter = tagFilter,
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

    /** Bundled inputs for the list pipeline (combine carries a single value). */
    private data class ListInputs(
        val tasks: List<dev.heckr.kitsudo.domain.model.TaskWithSubtasks>,
        val tagsByTask: Map<String, List<Tag>>,
        val allTags: List<Tag>,
        val sortMode: TaskSortMode,
    )

    fun setFilter(filter: TaskListFilter) {
        _uiState.update { state ->
            state.copy(
                filter = filter,
                tasks = state.allTasks.applyFilter(filter, state.tagFilter),
            )
        }
    }

    /** Filters the list to a single tag, or clears the tag filter when null. */
    fun setTagFilter(tagId: String?) {
        _uiState.update { state ->
            state.copy(
                tagFilter = tagId,
                tasks = state.allTasks.applyFilter(state.filter, tagId),
            )
        }
    }

    /** Creates a tag (or reuses an existing same-named one) and reports it back. */
    fun createTag(name: String, color: CatppuccinAccent, onCreated: (Tag) -> Unit) {
        viewModelScope.launch {
            createTagUseCase(name, color).getOrNull()?.let(onCreated)
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch { tagRepository.deleteTag(tagId) }
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

    fun showAddSheet() = _uiState.update {
        it.copy(showAddSheet = true, addSheetInitialTitle = "", addSheetInitialDescription = "")
    }

    /** Opens the Add sheet prefilled from shared text (share-to-Kitsudo). */
    fun showAddSheetPrefilled(title: String, description: String) = _uiState.update {
        it.copy(
            showAddSheet = true,
            addSheetInitialTitle = title,
            addSheetInitialDescription = description,
        )
    }

    fun hideAddSheet() = _uiState.update {
        it.copy(showAddSheet = false, addSheetInitialTitle = "", addSheetInitialDescription = "")
    }

    fun addTask(
        title: String,
        description: String,
        deadlineAt: Long?,
        recurrenceUnit: RecurrenceUnit? = null,
        recurrenceInterval: Int = 1,
        tagIds: List<String> = emptyList(),
    ) {
        viewModelScope.launch {
            // In custom order, new tasks go to the bottom; in smart order, sortOrder is unused.
            val bottomOrder = (_uiState.value.allTasks.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val result = createTaskUseCase(
                title = title,
                description = description,
                deadlineAt = deadlineAt,
                sortOrder = bottomOrder,
                recurrenceUnit = recurrenceUnit,
                recurrenceInterval = recurrenceInterval,
            )
            result.onSuccess { task ->
                if (tagIds.isNotEmpty()) tagRepository.setTagsForTask(task.id, tagIds)
                task.deadlineAt?.let { deadline ->
                    notificationScheduler.schedule(task.id, task.title, deadline)
                }
            }
        }
        hideAddSheet()
    }

    /**
     * Cascades: completing/un-completing a top-level task mirrors all its subtasks.
     * For a recurring task, "completing" instead rolls the deadline to the next
     * occurrence and reschedules its notification.
     */
    fun toggleComplete(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            if (isCompleted) {
                val advanced = advanceRecurringTaskUseCase(taskId)
                if (advanced != null) {
                    notificationScheduler.cancel(taskId)
                    advanced.deadlineAt?.let { deadline ->
                        notificationScheduler.schedule(advanced.id, advanced.title, deadline)
                        _recurringRescheduledEvents.tryEmit(deadline)
                    }
                    return@launch
                }
            }
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

    /** Called when the undo snackbar is dismissed/times out without an undo: drop the snapshot. */
    fun onUndoWindowClosed() {
        pendingDelete = null
    }

    /** Marks the restored undo prompt as handled so it isn't shown again this composition. */
    fun consumePendingUndoRestore() {
        _uiState.update { it.copy(pendingUndoRestore = false) }
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

    // -- Pending-delete persistence --------------------------------------------

    private fun encodeDeletedTask(deleted: DeletedTask): String =
        Json.encodeToString(
            DeletedTaskState(
                task = TaskDto.fromDomain(deleted.task),
                subtasks = deleted.subtasks.map(TaskDto::fromDomain),
            ),
        )

    private fun decodeDeletedTask(json: String): DeletedTask? = try {
        val state = Json.decodeFromString<DeletedTaskState>(json)
        DeletedTask(state.task.toDomain(), state.subtasks.map { it.toDomain() })
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val TICK_INTERVAL_MS = 60_000L
        const val KEY_PENDING_DELETE = "pending_delete"
    }
}

/** Serializable form of [DeletedTask] for [SavedStateHandle] persistence. */
@Serializable
private data class DeletedTaskState(
    val task: TaskDto,
    val subtasks: List<TaskDto>,
)

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

/** Dispatches to the ordering for [mode]. Visible for testing. */
internal fun List<TaskWithSubtasksUi>.sortedByMode(mode: TaskSortMode): List<TaskWithSubtasksUi> =
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

private fun List<TaskWithSubtasksUi>.applyFilter(
    filter: TaskListFilter,
    tagFilter: String?,
): List<TaskWithSubtasksUi> {
    val byStatus = when (filter) {
        TaskListFilter.ALL -> this
        TaskListFilter.ACTIVE -> filter { !it.isCompleted }
        TaskListFilter.OVERDUE -> filter { it.isDeadlineOverdue }
        TaskListFilter.COMPLETED -> filter { it.isCompleted }
    }
    return if (tagFilter == null) {
        byStatus
    } else {
        byStatus.filter { task -> task.tags.any { it.id == tagFilter } }
    }
}

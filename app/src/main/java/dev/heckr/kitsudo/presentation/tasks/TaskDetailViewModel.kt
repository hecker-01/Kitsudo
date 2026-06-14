package dev.heckr.kitsudo.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.heckr.kitsudo.data.notification.NotificationScheduler
import dev.heckr.kitsudo.domain.model.CatppuccinAccent
import dev.heckr.kitsudo.domain.model.Priority
import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import dev.heckr.kitsudo.domain.repository.TagRepository
import dev.heckr.kitsudo.domain.repository.TaskRepository
import dev.heckr.kitsudo.domain.usecase.AdvanceRecurringTaskUseCase
import dev.heckr.kitsudo.domain.usecase.CascadeCompleteUseCase
import dev.heckr.kitsudo.domain.usecase.CompleteTaskUseCase
import dev.heckr.kitsudo.domain.usecase.CreateTagUseCase
import dev.heckr.kitsudo.domain.usecase.CreateTaskUseCase
import dev.heckr.kitsudo.domain.usecase.DeleteTaskUseCase
import dev.heckr.kitsudo.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val cascadeCompleteUseCase: CascadeCompleteUseCase,
    private val advanceRecurringTaskUseCase: AdvanceRecurringTaskUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val tagRepository: TagRepository,
    private val createTagUseCase: CreateTagUseCase,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    /** Subtask id to expand on first open (deep-link from the task list), if any. */
    val expandSubtaskId: String? = savedStateHandle["expandSubtaskId"]

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null
    private val subtaskSaveJobs = mutableMapOf<String, Job>()

    init {
        // Single reactive pipeline: task, subtasks, this task's tags, and the full
        // tag list all kept live from Room.
        combine(
            taskRepository.observeTask(taskId),
            taskRepository.getSubtasks(taskId),
            tagRepository.observeTagsForTask(taskId),
            tagRepository.observeTags(),
        ) { task, subtasks, tags, allTags ->
            if (task == null) {
                _uiState.update { it.copy(isLoading = false, error = "Task not found") }
            } else {
                _uiState.update {
                    it.copy(
                        task = task.toUi(),
                        subtasks = subtasks.map { s -> s.toUi() },
                        tags = tags,
                        allTags = allTags,
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
    }

    /** Adds or removes a tag assignment for this task. */
    fun toggleTag(tagId: String) {
        viewModelScope.launch {
            val assigned = _uiState.value.tags.any { it.id == tagId }
            tagRepository.setTagAssigned(taskId, tagId, !assigned)
        }
    }

    /** Creates a tag (reusing a same-named one) and assigns it to this task. */
    fun createAndAssignTag(name: String, color: CatppuccinAccent) {
        viewModelScope.launch {
            createTagUseCase(name, color).getOrNull()?.let { tag ->
                tagRepository.setTagAssigned(taskId, tag.id, true)
            }
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch { tagRepository.deleteTag(tagId) }
    }

    /**
     * Debounced save: cancels any in-flight save and schedules a new one 400 ms later.
     * Called directly from onValueChange inside the text fields.
     */
    fun saveTitle(title: String) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            val trimmed = title.trim().ifBlank { return@launch }
            if (task.title != trimmed) updateTaskUseCase(task.copy(title = trimmed))
        }
    }

    fun saveDescription(description: String) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            val trimmed = description.trim()
            if (task.description != trimmed) updateTaskUseCase(task.copy(description = trimmed))
        }
    }

    fun setDeadline(deadlineAt: Long?) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            // Recurrence is anchored to a deadline; clearing the deadline drops it too.
            val recurrenceUnit = if (deadlineAt == null) null else task.recurrenceUnit
            updateTaskUseCase(task.copy(deadlineAt = deadlineAt, recurrenceUnit = recurrenceUnit))
            if (deadlineAt != null) {
                notificationScheduler.schedule(task.id, task.title, deadlineAt)
            } else {
                notificationScheduler.cancel(task.id)
            }
        }
    }

    /** Sets (or clears, with [unit] == null) the repeat rule for this task. */
    fun setRecurrence(unit: RecurrenceUnit?, interval: Int) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            // No-op if there's no deadline to anchor the recurrence to.
            if (unit != null && task.deadlineAt == null) return@launch
            updateTaskUseCase(
                task.copy(
                    recurrenceUnit = unit,
                    recurrenceInterval = interval.coerceAtLeast(1),
                ),
            )
        }
    }

    /**
     * Cascades to all subtasks. A recurring task rolls forward to its next
     * occurrence (and reschedules its notification) instead of completing.
     */
    fun toggleComplete(isCompleted: Boolean) {
        viewModelScope.launch {
            if (isCompleted) {
                val advanced = advanceRecurringTaskUseCase(taskId)
                if (advanced != null) {
                    notificationScheduler.cancel(taskId)
                    advanced.deadlineAt?.let {
                        notificationScheduler.schedule(advanced.id, advanced.title, it)
                    }
                    return@launch
                }
            }
            cascadeCompleteUseCase(taskId, isCompleted)
            if (isCompleted) notificationScheduler.cancel(taskId)
        }
    }

    /** Cascades to parent if all siblings become complete, or un-completes parent. */
    fun toggleSubtaskComplete(subtaskId: String, isCompleted: Boolean) {
        viewModelScope.launch { cascadeCompleteUseCase(subtaskId, isCompleted) }
    }

    fun setSubtaskDeadline(subtaskId: String, deadlineAt: Long?) {
        viewModelScope.launch {
            val subtask = taskRepository.getTaskById(subtaskId) ?: return@launch
            updateTaskUseCase(subtask.copy(deadlineAt = deadlineAt))
            if (deadlineAt != null) {
                // taskId is always this subtask's parent - pass it so the notification
                // renders as a subtask notification (lower priority, grouped, no follow-up).
                val parentTitle = _uiState.value.task?.title
                notificationScheduler.schedule(
                    taskId = subtask.id,
                    taskTitle = subtask.title,
                    deadlineAt = deadlineAt,
                    parentId = taskId,
                    parentTitle = parentTitle,
                )
            } else {
                notificationScheduler.cancel(subtask.id)
            }
        }
    }

    fun saveSubtaskTitle(subtaskId: String, title: String) {
        subtaskSaveJobs[subtaskId]?.cancel()
        subtaskSaveJobs[subtaskId] = viewModelScope.launch {
            delay(400)
            val subtask = taskRepository.getTaskById(subtaskId) ?: return@launch
            val trimmed = title.trim().ifBlank { return@launch }
            if (subtask.title != trimmed) updateTaskUseCase(subtask.copy(title = trimmed))
        }
    }

    fun saveSubtaskDescription(subtaskId: String, description: String) {
        subtaskSaveJobs[subtaskId]?.cancel()
        subtaskSaveJobs[subtaskId] = viewModelScope.launch {
            delay(400)
            val subtask = taskRepository.getTaskById(subtaskId) ?: return@launch
            val trimmed = description.trim()
            if (subtask.description != trimmed) updateTaskUseCase(subtask.copy(description = trimmed))
        }
    }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            createTaskUseCase(
                title = title,
                parentId = taskId,
                sortOrder = _uiState.value.subtasks.size,
            )
        }
    }

    fun deleteSubtask(subtaskId: String) {
        viewModelScope.launch {
            notificationScheduler.cancel(subtaskId)
            deleteTaskUseCase(subtaskId)
        }
    }

    fun togglePriority() {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            val newPriority = if (task.priority == Priority.HIGH) Priority.NORMAL else Priority.HIGH
            updateTaskUseCase(task.copy(priority = newPriority))
        }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        viewModelScope.launch {
            notificationScheduler.cancel(taskId)
            deleteTaskUseCase(taskId)
            onDeleted()
        }
    }

    override fun onCleared() {
        saveJob?.cancel()
        subtaskSaveJobs.values.forEach { it.cancel() }
        super.onCleared()
    }
}

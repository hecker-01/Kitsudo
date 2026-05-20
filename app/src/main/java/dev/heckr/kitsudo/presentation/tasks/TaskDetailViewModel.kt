package dev.heckr.kitsudo.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.heckr.kitsudo.data.notification.NotificationScheduler
import dev.heckr.kitsudo.domain.model.Priority
import dev.heckr.kitsudo.domain.repository.TaskRepository
import dev.heckr.kitsudo.domain.usecase.CascadeCompleteUseCase
import dev.heckr.kitsudo.domain.usecase.CompleteTaskUseCase
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
    private val createTaskUseCase: CreateTaskUseCase,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null

    init {
        // Single reactive pipeline: both task and subtasks kept live from Room.
        combine(
            taskRepository.observeTask(taskId),
            taskRepository.getSubtasks(taskId),
        ) { task, subtasks ->
            if (task == null) {
                _uiState.update { it.copy(isLoading = false, error = "Task not found") }
            } else {
                _uiState.update {
                    it.copy(
                        task = task.toUi(),
                        subtasks = subtasks.map { s -> s.toUi() },
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
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
            updateTaskUseCase(task.copy(deadlineAt = deadlineAt))
            if (deadlineAt != null) {
                notificationScheduler.schedule(task.id, task.title, deadlineAt)
            } else {
                notificationScheduler.cancel(task.id)
            }
        }
    }

    /** Cascades to all subtasks. */
    fun toggleComplete(isCompleted: Boolean) {
        viewModelScope.launch {
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
                notificationScheduler.schedule(subtask.id, subtask.title, deadlineAt)
            } else {
                notificationScheduler.cancel(subtask.id)
            }
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
}

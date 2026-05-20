package dev.heckr.kitsudo.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.heckr.kitsudo.data.notification.NotificationScheduler
import dev.heckr.kitsudo.domain.usecase.CascadeCompleteUseCase
import dev.heckr.kitsudo.domain.usecase.CreateTaskUseCase
import dev.heckr.kitsudo.domain.usecase.DeleteTaskUseCase
import dev.heckr.kitsudo.domain.usecase.GetTasksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        getTasksUseCase()
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { tasks ->
                _uiState.update {
                    it.copy(
                        tasks = tasks.map { t -> t.toWithSubtasksUi() },
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
    }

    fun showAddSheet() = _uiState.update { it.copy(showAddSheet = true) }
    fun hideAddSheet() = _uiState.update { it.copy(showAddSheet = false) }

    fun addTask(title: String, description: String, deadlineAt: Long?) {
        viewModelScope.launch {
            val result = createTaskUseCase(
                title = title,
                description = description,
                deadlineAt = deadlineAt,
            )
            result.onSuccess { task ->
                if (task.deadlineAt != null) {
                    notificationScheduler.schedule(task.id, task.title, task.deadlineAt)
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
            deleteTaskUseCase(taskId)
        }
    }
}

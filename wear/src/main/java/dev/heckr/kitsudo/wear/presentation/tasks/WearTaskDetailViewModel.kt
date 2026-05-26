package dev.heckr.kitsudo.wear.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.heckr.kitsudo.data.local.dao.TaskDao
import dev.heckr.kitsudo.data.mapper.toDomain
import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.domain.repository.TaskRepository
import dev.heckr.kitsudo.wear.data.sync.TaskActionSender
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WearTaskDetailUiState {
    data object Loading : WearTaskDetailUiState
    data object NotFound : WearTaskDetailUiState
    data class Success(val data: TaskWithSubtasks) : WearTaskDetailUiState
}

@HiltViewModel
class WearTaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskActionSender: TaskActionSender,
    private val taskDao: TaskDao,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    /**
     * Fully reactive: both the parent task and its subtask list re-emit from
     * Room whenever [toggleTask] writes to the local DB, so the UI updates
     * immediately without a round-trip to the phone.
     */
    val uiState: StateFlow<WearTaskDetailUiState> = combine(
        taskDao.observeTaskById(taskId),       // Flow<TaskEntity?> — reactive
        taskRepository.getSubtasks(taskId),    // Flow<List<Task>> — already reactive
    ) { entity, subtasks ->
        if (entity == null) {
            WearTaskDetailUiState.NotFound
        } else {
            WearTaskDetailUiState.Success(
                TaskWithSubtasks(task = entity.toDomain(), subtasks = subtasks),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearTaskDetailUiState.Loading)

    fun toggleTask(taskId: String, currentIsCompleted: Boolean) {
        val newState = !currentIsCompleted
        viewModelScope.launch {
            // Optimistic local write → Room Flow emits → UI updates instantly
            val entity = taskDao.getTaskById(taskId) ?: return@launch
            taskDao.updateTask(entity.copy(isCompleted = newState))
            // Tell the phone to apply cascade logic in the background
            taskActionSender.toggleComplete(taskId, newState)
        }
    }
}

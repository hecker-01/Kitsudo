package dev.heckr.kitsudo.wear.presentation.tasks

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.heckr.kitsudo.data.local.dao.TaskDao
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.domain.usecase.GetTasksUseCase
import dev.heckr.kitsudo.wear.data.sync.TaskActionSender
import dev.heckr.kitsudo.wear.glance.GlanceUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WearTaskListUiState {
    /** First launch — request sent to phone, waiting for first snapshot. */
    data object Syncing : WearTaskListUiState

    /**
     * Sync timed out or the phone has no tasks. The user should add tasks
     * from the phone app and wait for the watch to receive them.
     */
    data object NoData : WearTaskListUiState

    data class Success(val tasks: List<TaskWithSubtasks>) : WearTaskListUiState
}

@HiltViewModel
class WearTaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val taskActionSender: TaskActionSender,
    private val taskDao: TaskDao,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * Becomes true after [SYNC_TIMEOUT_MS] ms if no data has arrived yet.
     * Prevents the watch from showing a spinner forever.
     */
    private val syncTimedOut = MutableStateFlow(false)

    val uiState: StateFlow<WearTaskListUiState> = combine(
        getTasksUseCase(),
        syncTimedOut,
    ) { tasks, timedOut ->
        when {
            tasks.isNotEmpty() -> WearTaskListUiState.Success(
                tasks.sortedWith(
                    compareBy<TaskWithSubtasks> { it.task.isCompleted }
                        .thenByDescending { it.task.priority.dbValue }
                        .thenBy { it.task.sortOrder },
                ),
            )
            timedOut -> WearTaskListUiState.NoData
            else     -> WearTaskListUiState.Syncing
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearTaskListUiState.Syncing)

    init {
        Log.d(TAG, "WearTaskListViewModel init — requesting sync from phone")
        // Ask phone for a fresh snapshot
        viewModelScope.launch {
            val result = taskActionSender.requestSync()
            Log.d(TAG, "requestSync result: $result")
        }
        // After timeout, stop showing spinner and explain what's happening
        viewModelScope.launch {
            delay(SYNC_TIMEOUT_MS)
            Log.d(TAG, "Sync timeout reached — flipping to NoData if still empty")
            syncTimedOut.value = true
        }
    }

    fun toggleTask(taskId: String, currentIsCompleted: Boolean) {
        val newState = !currentIsCompleted
        viewModelScope.launch {
            val entity = taskDao.getTaskById(taskId) ?: return@launch
            taskDao.updateTask(entity.copy(isCompleted = newState))
            taskActionSender.toggleComplete(taskId, newState)
            GlanceUpdater.requestUpdate(context)
        }
    }

    private companion object {
        const val SYNC_TIMEOUT_MS = 6_000L
        const val TAG = "WearTaskListViewModel"
    }
}

package dev.heckr.kitsudo.domain.repository

import dev.heckr.kitsudo.domain.model.TaskSortMode
import kotlinx.coroutines.flow.Flow

/** Persists user preferences for the task list itself (currently just the sort mode). */
interface TaskListPreferencesRepository {
    fun observeSortMode(): Flow<TaskSortMode>
    suspend fun setSortMode(mode: TaskSortMode)
}

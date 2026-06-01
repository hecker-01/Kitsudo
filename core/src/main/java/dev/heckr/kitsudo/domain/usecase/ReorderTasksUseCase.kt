package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Persists a new manual order for top-level tasks. Each task's sortOrder is set
 * to its index in [orderedIds]; only changed rows are written.
 */
class ReorderTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            val task = repository.getTaskById(id) ?: return@forEachIndexed
            if (task.sortOrder != index) {
                repository.updateTask(task.copy(sortOrder = index))
            }
        }
    }
}

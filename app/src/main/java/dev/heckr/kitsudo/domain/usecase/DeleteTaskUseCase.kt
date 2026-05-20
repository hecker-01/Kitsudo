package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    /** Deletes subtasks first, then the task itself. */
    suspend operator fun invoke(taskId: String): Result<Unit> = runCatching {
        repository.deleteSubtasks(taskId).getOrThrow()
        repository.deleteTask(taskId).getOrThrow()
    }
}

package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val recomputeParentCompletion: RecomputeParentCompletionUseCase,
) {
    /** Deletes subtasks first, then the task itself. */
    suspend operator fun invoke(taskId: String): Result<Unit> = runCatching {
        // Capture the parent before deletion so we can re-derive its completion.
        val parentId = repository.getTaskById(taskId)?.parentId
        repository.deleteSubtasks(taskId).getOrThrow()
        repository.deleteTask(taskId).getOrThrow()
        // Removing the last incomplete subtask may complete the parent.
        if (parentId != null) recomputeParentCompletion(parentId)
    }
}

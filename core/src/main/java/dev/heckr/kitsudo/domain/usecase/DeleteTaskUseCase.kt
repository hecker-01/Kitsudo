package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.DeletedTask
import dev.heckr.kitsudo.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val recomputeParentCompletion: RecomputeParentCompletionUseCase,
) {
    /**
     * Deletes subtasks first, then the task itself. Returns a [DeletedTask]
     * snapshot of everything removed so the deletion can be undone via
     * [RestoreTaskUseCase].
     */
    suspend operator fun invoke(taskId: String): Result<DeletedTask> = runCatching {
        val task = repository.getTaskById(taskId) ?: error("Task $taskId not found")
        val subtasks = repository.getSubtasksOnce(taskId)
        repository.deleteSubtasks(taskId).getOrThrow()
        repository.deleteTask(taskId).getOrThrow()
        // Removing the last incomplete subtask may complete the parent.
        task.parentId?.let { recomputeParentCompletion(it) }
        DeletedTask(task, subtasks)
    }
}

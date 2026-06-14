package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.DeletedTask
import dev.heckr.kitsudo.domain.repository.TagRepository
import dev.heckr.kitsudo.domain.repository.TaskRepository
import javax.inject.Inject

/** Re-inserts a [DeletedTask] snapshot captured by [DeleteTaskUseCase]. */
class RestoreTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val tagRepository: TagRepository,
    private val recomputeParentCompletion: RecomputeParentCompletionUseCase,
) {
    suspend operator fun invoke(deleted: DeletedTask): Result<Unit> = runCatching {
        repository.createTask(deleted.task).getOrThrow()
        deleted.subtasks.forEach { repository.createTask(it).getOrThrow() }
        // Re-attach the tags the task had (best-effort: tags still present in DB).
        if (deleted.tagIds.isNotEmpty()) {
            tagRepository.setTagsForTask(deleted.task.id, deleted.tagIds)
        }
        // Restoring a subtask may un-complete its parent.
        deleted.task.parentId?.let { recomputeParentCompletion(it) }
    }
}

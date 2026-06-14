package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.repository.TaskRepository
import java.util.UUID
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val recomputeParentCompletion: RecomputeParentCompletionUseCase,
) {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        parentId: String? = null,
        deadlineAt: Long? = null,
        sortOrder: Int = 0,
        recurrenceUnit: RecurrenceUnit? = null,
        recurrenceInterval: Int = 1,
    ): Result<Task> {
        // Enforce the 2-level depth limit at the data layer (not just in the UI):
        // a subtask's parent must itself be a top-level task.
        if (parentId != null) {
            val parent = repository.getTaskById(parentId)
                ?: return Result.failure(
                    IllegalArgumentException("Parent task $parentId not found"),
                )
            if (parent.parentId != null) {
                return Result.failure(
                    IllegalStateException("Subtasks cannot be nested more than one level deep"),
                )
            }
        }
        // Recurrence is a top-level concept anchored to a deadline.
        if (recurrenceUnit != null) {
            if (parentId != null) {
                return Result.failure(
                    IllegalStateException("Subtasks cannot be recurring"),
                )
            }
            if (deadlineAt == null) {
                return Result.failure(
                    IllegalStateException("A recurring task needs a deadline"),
                )
            }
        }
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING,
            parentId = parentId,
            deadlineAt = deadlineAt,
            sortOrder = sortOrder,
            recurrenceUnit = recurrenceUnit,
            recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
        )
        return repository.createTask(task)
            .onSuccess {
                // A new (incomplete) subtask must un-complete a parent that was done.
                if (parentId != null) recomputeParentCompletion(parentId)
            }
            .map { task }
    }
}

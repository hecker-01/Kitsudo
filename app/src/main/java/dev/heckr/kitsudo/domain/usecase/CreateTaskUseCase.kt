package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.repository.TaskRepository
import java.util.UUID
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        parentId: String? = null,
        deadlineAt: Long? = null,
        sortOrder: Int = 0,
    ): Result<Task> {
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
        )
        return repository.createTask(task).map { task }
    }
}

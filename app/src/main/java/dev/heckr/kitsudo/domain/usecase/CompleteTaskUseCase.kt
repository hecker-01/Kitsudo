package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.repository.TaskRepository
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String, isCompleted: Boolean): Result<Unit> {
        val task = repository.getTaskById(taskId) ?: return Result.failure(
            NoSuchElementException("Task $taskId not found"),
        )
        return repository.updateTask(task.copy(isCompleted = isCompleted))
    }
}

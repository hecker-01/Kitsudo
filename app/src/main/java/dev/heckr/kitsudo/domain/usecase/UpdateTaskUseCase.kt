package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(task: Task): Result<Unit> = repository.updateTask(task)
}

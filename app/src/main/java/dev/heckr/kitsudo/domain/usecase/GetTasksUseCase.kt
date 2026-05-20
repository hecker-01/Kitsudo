package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(): Flow<List<TaskWithSubtasks>> =
        repository.getTopLevelTasksWithSubtasks()
}

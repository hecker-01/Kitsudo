package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.TaskWithSubtasks
import dev.heckr.kitsudo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetTaskWithSubtasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(taskId: String): Flow<TaskWithSubtasks?> {
        val taskFlow = flow { emit(repository.getTaskById(taskId)) }
        val subtasksFlow = repository.getSubtasks(taskId)
        return combine(taskFlow, subtasksFlow) { task, subtasks ->
            task?.let { TaskWithSubtasks(task = it, subtasks = subtasks) }
        }
    }
}

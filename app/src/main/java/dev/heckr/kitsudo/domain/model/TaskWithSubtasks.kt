package dev.heckr.kitsudo.domain.model

data class TaskWithSubtasks(
    val task: Task,
    val subtasks: List<Task>,
) {
    val completedSubtaskCount: Int get() = subtasks.count { it.isCompleted }
    val totalSubtaskCount: Int get() = subtasks.size
}

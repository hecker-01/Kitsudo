package dev.heckr.kitsudo.domain.model

/**
 * Snapshot of a task and its subtasks captured at deletion time, so the delete
 * can be undone by re-inserting exactly what was removed.
 */
data class DeletedTask(
    val task: Task,
    val subtasks: List<Task>,
)

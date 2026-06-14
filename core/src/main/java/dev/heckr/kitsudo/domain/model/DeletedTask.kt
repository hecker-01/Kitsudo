package dev.heckr.kitsudo.domain.model

/**
 * Snapshot of a task and its subtasks captured at deletion time, so the delete
 * can be undone by re-inserting exactly what was removed.
 */
data class DeletedTask(
    val task: Task,
    val subtasks: List<Task>,
    /** Ids of tags that were assigned to [task], so undo can re-attach them. */
    val tagIds: List<String> = emptyList(),
)

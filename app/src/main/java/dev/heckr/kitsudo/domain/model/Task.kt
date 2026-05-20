package dev.heckr.kitsudo.domain.model

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val syncStatus: SyncStatus,
    /** Null for top-level tasks; set to the parent task's id for subtasks. */
    val parentId: String? = null,
    /** Epoch-millisecond deadline. Null means no deadline. */
    val deadlineAt: Long? = null,
    /** Ascending order within a parent (or among top-level tasks). */
    val sortOrder: Int = 0,
)

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
    /** User-set importance. HIGH tasks float above NORMAL within each sort bucket. */
    val priority: Priority = Priority.NORMAL,
    /**
     * Repeat period, or null for a one-off task. Only meaningful on top-level
     * tasks with a [deadlineAt]; completing such a task rolls [deadlineAt] forward
     * by [recurrenceInterval] of this unit instead of marking it done.
     */
    val recurrenceUnit: RecurrenceUnit? = null,
    /** How many [recurrenceUnit]s between occurrences (e.g. 2 = every 2 weeks). */
    val recurrenceInterval: Int = 1,
) {
    /** True when this task repeats (has a unit and a deadline to anchor it). */
    val isRecurring: Boolean get() = recurrenceUnit != null && deadlineAt != null
}

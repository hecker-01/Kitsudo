package dev.heckr.kitsudo.presentation.tasks

import dev.heckr.kitsudo.domain.model.Priority
import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks

/** Flat UI model used for individual task and subtask rows. */
data class TaskUi(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val deadlineAt: Long?,
    val isDeadlineOverdue: Boolean,
    val syncStatus: SyncStatus,
    val priority: Priority,
) {
    val isHighPriority: Boolean get() = priority == Priority.HIGH
}

/** UI model for the task list - includes the live subtask list for collapsing. */
data class TaskWithSubtasksUi(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val deadlineAt: Long?,
    val isDeadlineOverdue: Boolean,
    val syncStatus: SyncStatus,
    val subtasks: List<TaskUi>,
    val priority: Priority,
    val createdAt: Long,
) {
    val subtaskCount: Int get() = subtasks.size
    val subtaskCompletedCount: Int get() = subtasks.count { it.isCompleted }
    val isHighPriority: Boolean get() = priority == Priority.HIGH
}

// -- Mappers ----------------------------------------------------------------

fun Task.toUi(now: Long = System.currentTimeMillis()): TaskUi = TaskUi(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    deadlineAt = deadlineAt,
    isDeadlineOverdue = deadlineAt != null && deadlineAt < now && !isCompleted,
    syncStatus = syncStatus,
    priority = priority,
)

fun TaskWithSubtasks.toWithSubtasksUi(now: Long = System.currentTimeMillis()): TaskWithSubtasksUi =
    TaskWithSubtasksUi(
        id = task.id,
        title = task.title,
        description = task.description,
        isCompleted = task.isCompleted,
        deadlineAt = task.deadlineAt,
        isDeadlineOverdue = task.deadlineAt != null && task.deadlineAt < now && !task.isCompleted,
        syncStatus = task.syncStatus,
        subtasks = subtasks.map { it.toUi(now) },
        priority = task.priority,
        createdAt = task.createdAt,
    )

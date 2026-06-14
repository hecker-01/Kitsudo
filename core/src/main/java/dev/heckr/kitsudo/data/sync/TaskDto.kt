package dev.heckr.kitsudo.data.sync

import dev.heckr.kitsudo.domain.model.Priority
import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.domain.model.Task
import kotlinx.serialization.Serializable

/**
 * Wire-transfer representation of a [Task] used by both phone and watch.
 *
 * The phone serialises the full task tree (parents + subtasks flattened)
 * as `List<TaskDto>` JSON and pushes it via [DataClient] at path
 * [WearSyncPaths.TASKS_SNAPSHOT]. The watch deserialises and writes to
 * its local Room DB.
 */
@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val parentId: String? = null,
    val deadlineAt: Long? = null,
    val sortOrder: Int = 0,
    val priority: Int = 0,
    val recurrenceUnit: String? = null,
    val recurrenceInterval: Int = 1,
) {
    companion object {
        fun fromDomain(task: Task) = TaskDto(
            id = task.id,
            title = task.title,
            description = task.description,
            isCompleted = task.isCompleted,
            createdAt = task.createdAt,
            parentId = task.parentId,
            deadlineAt = task.deadlineAt,
            sortOrder = task.sortOrder,
            priority = task.priority.dbValue,
            recurrenceUnit = task.recurrenceUnit?.name,
            recurrenceInterval = task.recurrenceInterval,
        )
    }

    fun toDomain(): Task = Task(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        createdAt = createdAt,
        syncStatus = SyncStatus.SYNCED,
        parentId = parentId,
        deadlineAt = deadlineAt,
        sortOrder = sortOrder,
        priority = Priority.fromDb(priority),
        recurrenceUnit = RecurrenceUnit.fromDb(recurrenceUnit),
        recurrenceInterval = recurrenceInterval,
    )
}

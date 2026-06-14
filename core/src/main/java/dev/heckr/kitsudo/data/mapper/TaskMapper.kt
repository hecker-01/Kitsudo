package dev.heckr.kitsudo.data.mapper

import dev.heckr.kitsudo.data.local.entity.TaskEntity
import dev.heckr.kitsudo.data.local.entity.TaskWithSubtasksEntity
import dev.heckr.kitsudo.domain.model.Priority
import dev.heckr.kitsudo.domain.model.RecurrenceUnit
import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.model.TaskWithSubtasks

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    createdAt = createdAt,
    syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.PENDING),
    parentId = parentId,
    deadlineAt = deadlineAt,
    sortOrder = sortOrder,
    priority = Priority.fromDb(priority),
    recurrenceUnit = RecurrenceUnit.fromDb(recurrenceUnit),
    recurrenceInterval = recurrenceInterval,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    createdAt = createdAt,
    syncStatus = syncStatus.name,
    parentId = parentId,
    deadlineAt = deadlineAt,
    sortOrder = sortOrder,
    priority = priority.dbValue,
    recurrenceUnit = recurrenceUnit?.name,
    recurrenceInterval = recurrenceInterval,
)

fun TaskWithSubtasksEntity.toDomain(): TaskWithSubtasks = TaskWithSubtasks(
    task = task.toDomain(),
    subtasks = subtasks.map { it.toDomain() },
)

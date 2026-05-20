package dev.heckr.kitsudo.data.mapper

import dev.heckr.kitsudo.data.local.entity.TaskEntity
import dev.heckr.kitsudo.data.local.entity.TaskWithSubtasksEntity
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
)

fun TaskWithSubtasksEntity.toDomain(): TaskWithSubtasks = TaskWithSubtasks(
    task = task.toDomain(),
    subtasks = subtasks.map { it.toDomain() },
)

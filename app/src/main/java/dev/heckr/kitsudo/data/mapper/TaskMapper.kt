package dev.heckr.kitsudo.data.mapper

import dev.heckr.kitsudo.data.local.entity.TaskEntity
import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.domain.model.Task

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    createdAt = createdAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    createdAt = createdAt,
    syncStatus = syncStatus.name,
)

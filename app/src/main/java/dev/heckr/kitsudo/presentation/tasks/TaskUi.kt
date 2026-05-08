package dev.heckr.kitsudo.presentation.tasks

import dev.heckr.kitsudo.domain.model.SyncStatus
import dev.heckr.kitsudo.domain.model.Task

data class TaskUi(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val syncStatus: SyncStatus,
)

fun Task.toUi(): TaskUi = TaskUi(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    syncStatus = syncStatus,
)

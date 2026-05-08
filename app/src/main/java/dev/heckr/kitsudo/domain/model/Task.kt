package dev.heckr.kitsudo.domain.model

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val syncStatus: SyncStatus,
)

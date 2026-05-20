package dev.heckr.kitsudo.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithSubtasksEntity(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId",
    )
    val subtasks: List<TaskEntity>,
)

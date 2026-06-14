package dev.heckr.kitsudo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val syncStatus: String,
    /** Null for top-level tasks; parent task id for subtasks. */
    val parentId: String?,
    /** Epoch-millisecond deadline. Null means no deadline. */
    val deadlineAt: Long?,
    /** Ascending display order within a parent or among top-level tasks. */
    val sortOrder: Int,
    /** 0 = NORMAL, 1 = HIGH. Maps to Priority enum via Priority.fromDb(). */
    val priority: Int = 0,
    /** RecurrenceUnit name (DAY/WEEK/MONTH), or null for a non-recurring task. */
    val recurrenceUnit: String? = null,
    /** Periods between occurrences; only meaningful when recurrenceUnit is set. */
    val recurrenceInterval: Int = 1,
)

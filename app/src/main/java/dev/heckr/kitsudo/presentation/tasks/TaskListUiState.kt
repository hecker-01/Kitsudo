package dev.heckr.kitsudo.presentation.tasks

import dev.heckr.kitsudo.R

enum class TaskListFilter {
    ALL, ACTIVE, OVERDUE, COMPLETED;

    fun labelRes(): Int = when (this) {
        ALL -> R.string.task_filter_all
        ACTIVE -> R.string.task_filter_active
        OVERDUE -> R.string.task_filter_overdue
        COMPLETED -> R.string.task_filter_completed
    }
}

data class TaskListUiState(
    /** Filtered + sorted list shown in the list. */
    val tasks: List<TaskWithSubtasksUi> = emptyList(),
    /** Full sorted list - kept so filter changes don't need a DB round-trip. */
    val allTasks: List<TaskWithSubtasksUi> = emptyList(),
    val filter: TaskListFilter = TaskListFilter.ALL,
    val overdueCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddSheet: Boolean = false,
)

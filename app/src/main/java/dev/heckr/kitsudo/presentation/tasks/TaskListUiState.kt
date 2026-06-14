package dev.heckr.kitsudo.presentation.tasks

import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.TaskSortMode

enum class TaskListFilter {
    ALL, ACTIVE, OVERDUE, COMPLETED;

    fun labelRes(): Int = when (this) {
        ALL -> R.string.task_filter_all
        ACTIVE -> R.string.task_filter_active
        OVERDUE -> R.string.task_filter_overdue
        COMPLETED -> R.string.task_filter_completed
    }
}

fun TaskSortMode.labelRes(): Int = when (this) {
    TaskSortMode.SMART -> R.string.task_sort_smart
    TaskSortMode.CUSTOM -> R.string.task_sort_custom
    TaskSortMode.ALPHABETICAL -> R.string.task_sort_alphabetical
    TaskSortMode.DEADLINE -> R.string.task_sort_deadline
    TaskSortMode.NEWEST -> R.string.task_sort_newest
    TaskSortMode.OLDEST -> R.string.task_sort_oldest
    TaskSortMode.PRIORITY -> R.string.task_sort_priority
}

data class TaskListUiState(
    /** Filtered + sorted list shown in the list. */
    val tasks: List<TaskWithSubtasksUi> = emptyList(),
    /** Full sorted list - kept so filter changes don't need a DB round-trip. */
    val allTasks: List<TaskWithSubtasksUi> = emptyList(),
    val filter: TaskListFilter = TaskListFilter.ALL,
    val sortMode: TaskSortMode = TaskSortMode.SMART,
    val overdueCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddSheet: Boolean = false,
    /** True when a newer version is available - drives the badge dot on the settings icon. */
    val updateAvailable: Boolean = false,
    /** True when a deletion survived process death and the undo snackbar should be re-shown. */
    val pendingUndoRestore: Boolean = false,
)

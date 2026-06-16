package dev.heckr.kitsudo.presentation.tasks

import dev.heckr.kitsudo.R
import dev.heckr.kitsudo.domain.model.Tag
import dev.heckr.kitsudo.domain.model.TaskSortMode

enum class TaskListFilter {
    ACTIVE, OVERDUE, ALL, COMPLETED;

    fun labelRes(): Int = when (this) {
        ALL -> R.string.task_filter_all
        ACTIVE -> R.string.task_filter_active
        OVERDUE -> R.string.task_filter_overdue
        COMPLETED -> R.string.task_filter_completed
    }
}

/**
 * Toggleable attribute filters that stack on top of the status [TaskListFilter]
 * (combined with AND). Surfaced through the combined filter/sort menu.
 */
enum class TaskAttributeFilter {
    HIGH_PRIORITY, HAS_DEADLINE, HAS_SUBTASKS, RECURRING;

    fun labelRes(): Int = when (this) {
        HIGH_PRIORITY -> R.string.task_filter_high_priority
        HAS_DEADLINE -> R.string.task_filter_has_deadline
        HAS_SUBTASKS -> R.string.task_filter_has_subtasks
        RECURRING -> R.string.task_filter_recurring
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
    val filter: TaskListFilter = TaskListFilter.ACTIVE,
    /** Stackable attribute filters, ANDed with [filter] and [tagFilter]. */
    val attributeFilters: Set<TaskAttributeFilter> = emptySet(),
    /** All tags, for the filter row and pickers. */
    val allTags: List<Tag> = emptyList(),
    /** When set, the list is filtered to tasks carrying this tag id. */
    val tagFilter: String? = null,
    val sortMode: TaskSortMode = TaskSortMode.SMART,
    val overdueCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddSheet: Boolean = false,
    /** Prefill for the Add sheet (used by share-to-Kitsudo); blank otherwise. */
    val addSheetInitialTitle: String = "",
    val addSheetInitialDescription: String = "",
    /** True when a newer version is available - drives the badge dot on the settings icon. */
    val updateAvailable: Boolean = false,
    /** True when a deletion survived process death and the undo snackbar should be re-shown. */
    val pendingUndoRestore: Boolean = false,
)

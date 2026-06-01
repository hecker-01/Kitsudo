package dev.heckr.kitsudo.domain.model

/**
 * How the top-level task list is ordered.
 *
 * - [SMART]        - automatic grouping: overdue, upcoming deadline, priority, then completed.
 * - [CUSTOM]       - manual drag-to-reorder; ordered purely by each task's sortOrder.
 *   Completion and priority do not influence the order in this mode.
 * - [ALPHABETICAL] - by title, A→Z.
 * - [DEADLINE]     - soonest deadline first; tasks without a deadline last.
 * - [NEWEST]       - most recently created first.
 * - [OLDEST]       - oldest created first.
 * - [PRIORITY]     - starred (high priority) tasks first.
 *
 * Except for [CUSTOM], completed tasks always sink to the bottom and the chosen
 * field orders within the incomplete and completed groups separately.
 */
enum class TaskSortMode {
    SMART,
    CUSTOM,
    ALPHABETICAL,
    DEADLINE,
    NEWEST,
    OLDEST,
    PRIORITY,
    ;

    companion object {
        val default = SMART

        fun fromName(name: String?): TaskSortMode =
            entries.firstOrNull { it.name == name } ?: default
    }
}

package dev.heckr.kitsudo.wear.glance

import dev.heckr.kitsudo.domain.model.Task
import java.util.Calendar

/**
 * Glanceable summary of the task list, shared by the Tile and the complication.
 *
 * "Due" means an incomplete top-level task whose deadline is overdue or falls
 * later today. [nextTitle]/[nextDeadlineAt] describe the soonest-deadline
 * incomplete task (which may itself be overdue).
 */
data class TaskSummary(
    val dueCount: Int,
    val totalActive: Int,
    val completedCount: Int,
    val totalCount: Int,
    val nextTitle: String?,
    val nextDeadlineAt: Long?,
) {
    val hasNext: Boolean get() = nextTitle != null

    companion object {
        val EMPTY = TaskSummary(0, 0, 0, 0, null, null)
    }
}

/**
 * Computes a [TaskSummary] from the **top-level** tasks (callers pass only
 * parentId == null rows). Pure and JVM-testable.
 */
fun summarizeTasks(topLevel: List<Task>, now: Long): TaskSummary {
    val endOfToday = endOfTodayMillis(now)
    val active = topLevel.filter { !it.isCompleted }
    val due = active.count { val d = it.deadlineAt; d != null && d <= endOfToday }
    val next = active
        .filter { it.deadlineAt != null }
        .minByOrNull { it.deadlineAt!! }
    return TaskSummary(
        dueCount = due,
        totalActive = active.size,
        completedCount = topLevel.count { it.isCompleted },
        totalCount = topLevel.size,
        nextTitle = next?.title,
        nextDeadlineAt = next?.deadlineAt,
    )
}

/** Last millisecond of the local day containing [now]. */
fun endOfTodayMillis(now: Long): Long = Calendar.getInstance().apply {
    timeInMillis = now
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.timeInMillis

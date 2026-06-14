package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.model.Task
import dev.heckr.kitsudo.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Handles "completing" a recurring task. Instead of marking it done, the same task
 * (and its subtasks) is reset to incomplete and its [Task.deadlineAt] is rolled
 * forward to the next occurrence (see [RecurrenceCalculator.nextDeadline]).
 *
 * Returns the updated [Task] (with the new deadline) when the task was recurring,
 * or null when it was not, letting callers fall back to ordinary completion and,
 * for the recurring case, reschedule notifications for the new deadline.
 */
class AdvanceRecurringTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(
        taskId: String,
        now: Long = System.currentTimeMillis(),
    ): Task? {
        val task = repository.getTaskById(taskId) ?: return null
        val unit = task.recurrenceUnit ?: return null
        val deadline = task.deadlineAt ?: return null
        // Recurrence only applies to top-level tasks.
        if (task.parentId != null) return null

        val next = RecurrenceCalculator.nextDeadline(deadline, unit, task.recurrenceInterval, now)
        val updated = task.copy(deadlineAt = next, isCompleted = false)
        // Start the new cycle fresh: parent + every subtask back to incomplete.
        repository.setCompletedForTaskAndSubtasks(task.id, false)
        repository.updateTask(updated)
        return updated
    }
}

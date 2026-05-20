package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Bidirectional cascade completion:
 *
 * - **Top-level task completed/un-completed** → all its subtasks are set to
 *   the same state.
 * - **Subtask completed** → if every sibling is now complete, the parent is
 *   also marked complete.
 * - **Subtask un-completed** → if the parent was complete, it is un-completed.
 */
class CascadeCompleteUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val completeTask: CompleteTaskUseCase,
) {
    suspend operator fun invoke(taskId: String, isCompleted: Boolean) {
        val task = repository.getTaskById(taskId) ?: return
        completeTask(taskId, isCompleted)

        if (task.parentId == null) {
            // ── Top-level task ────────────────────────────────────────
            // Cascade the same state to every subtask.
            repository.getSubtasks(taskId).first().forEach { subtask ->
                completeTask(subtask.id, isCompleted)
            }
        } else {
            // ── Subtask ───────────────────────────────────────────────
            val siblings = repository.getSubtasks(task.parentId).first()
            if (isCompleted) {
                // All siblings now done → auto-complete parent.
                val allDone = siblings.all { s ->
                    if (s.id == taskId) true else s.isCompleted
                }
                if (allDone) completeTask(task.parentId, true)
            } else {
                // Un-completing a subtask → un-complete parent if it was done.
                val parent = repository.getTaskById(task.parentId)
                if (parent?.isCompleted == true) completeTask(task.parentId, false)
            }
        }
    }
}

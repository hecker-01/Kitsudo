package dev.heckr.kitsudo.domain.usecase

import dev.heckr.kitsudo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Re-derives a parent task's completion state from its current subtasks.
 *
 * Called after a subtask is added or removed, so structural changes (not just
 * checkbox toggles) keep the parent consistent:
 *
 * - Adding an incomplete subtask to a completed parent → parent becomes incomplete.
 * - Removing the last incomplete subtask so every remaining sibling is done →
 *   parent becomes complete.
 *
 * No-op when the parent has no subtasks left: a parent with zero subtasks keeps
 * whatever completion state it already had.
 */
class RecomputeParentCompletionUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(parentId: String) {
        val parent = repository.getTaskById(parentId) ?: return
        val subtasks = repository.getSubtasks(parentId).first()
        if (subtasks.isEmpty()) return
        val allComplete = subtasks.all { it.isCompleted }
        if (parent.isCompleted != allComplete) {
            repository.updateTask(parent.copy(isCompleted = allComplete))
        }
    }
}

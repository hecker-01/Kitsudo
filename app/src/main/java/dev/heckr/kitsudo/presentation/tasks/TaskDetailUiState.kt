package dev.heckr.kitsudo.presentation.tasks

import dev.heckr.kitsudo.domain.model.Tag

data class TaskDetailUiState(
    val task: TaskUi? = null,
    val subtasks: List<TaskUi> = emptyList(),
    /** Tags assigned to this task. */
    val tags: List<Tag> = emptyList(),
    /** Every tag, for the picker. */
    val allTags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
)

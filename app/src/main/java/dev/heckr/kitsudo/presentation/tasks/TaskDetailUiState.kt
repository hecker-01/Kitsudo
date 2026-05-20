package dev.heckr.kitsudo.presentation.tasks

data class TaskDetailUiState(
    val task: TaskUi? = null,
    val subtasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
)

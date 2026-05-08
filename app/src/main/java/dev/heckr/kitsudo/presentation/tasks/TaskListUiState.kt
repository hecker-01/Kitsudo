package dev.heckr.kitsudo.presentation.tasks

data class TaskListUiState(
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

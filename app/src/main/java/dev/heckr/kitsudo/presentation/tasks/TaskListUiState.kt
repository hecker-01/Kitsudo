package dev.heckr.kitsudo.presentation.tasks

data class TaskListUiState(
    val tasks: List<TaskWithSubtasksUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddSheet: Boolean = false,
)

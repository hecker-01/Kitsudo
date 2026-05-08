package dev.heckr.kitsudo.presentation.navigation

sealed class Screen(val route: String) {
    data object TaskList : Screen("task_list")
}

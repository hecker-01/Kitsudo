package dev.heckr.kitsudo.presentation.navigation

sealed class Screen(val route: String) {
    data object TaskList : Screen("task_list")
    data object Settings : Screen("settings")
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun routeFor(taskId: String) = "task_detail/$taskId"
    }
}

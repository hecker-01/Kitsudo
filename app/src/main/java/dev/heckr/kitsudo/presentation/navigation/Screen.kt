package dev.heckr.kitsudo.presentation.navigation

sealed class Screen(val route: String) {
    data object TaskList : Screen("task_list")
    data object Settings : Screen("settings")
    data object TaskDetail : Screen("task_detail/{taskId}?expandSubtaskId={expandSubtaskId}") {
        fun routeFor(taskId: String, expandSubtaskId: String? = null) =
            if (expandSubtaskId != null) {
                "task_detail/$taskId?expandSubtaskId=$expandSubtaskId"
            } else {
                "task_detail/$taskId"
            }
    }
}

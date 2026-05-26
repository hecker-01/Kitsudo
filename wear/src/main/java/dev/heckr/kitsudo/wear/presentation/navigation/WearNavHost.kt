package dev.heckr.kitsudo.wear.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dev.heckr.kitsudo.wear.presentation.tasks.WearTaskDetailScreen
import dev.heckr.kitsudo.wear.presentation.tasks.WearTaskListScreen

private const val ROUTE_TASK_LIST   = "task_list"
private const val ROUTE_TASK_DETAIL = "task_detail/{taskId}"

@Composable
fun WearNavHost() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = ROUTE_TASK_LIST,
    ) {
        composable(ROUTE_TASK_LIST) {
            WearTaskListScreen(
                onTaskClick = { taskId ->
                    navController.navigate("task_detail/$taskId")
                },
            )
        }
        composable(ROUTE_TASK_DETAIL) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            WearTaskDetailScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

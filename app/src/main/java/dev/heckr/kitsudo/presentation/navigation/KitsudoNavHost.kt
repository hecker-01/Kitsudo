package dev.heckr.kitsudo.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.heckr.kitsudo.presentation.settings.SettingsScreen
import dev.heckr.kitsudo.presentation.tasks.TaskDetailScreen
import dev.heckr.kitsudo.presentation.tasks.TaskListScreen

private val enter = fadeIn(animationSpec = tween(180))
private val exit = fadeOut(animationSpec = tween(180))

@Composable
fun KitsudoNavHost(
    navController: NavHostController = rememberNavController(),
    /** Task id to open immediately after the NavHost is composed. */
    startTaskId: String? = null,
    /** Called once [startTaskId] has been navigated to, so the caller can reset state. */
    onStartTaskHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Deep-link from a notification tap: navigate as soon as a non-null id arrives.
    LaunchedEffect(startTaskId) {
        val id = startTaskId ?: return@LaunchedEffect
        navController.navigate(Screen.TaskDetail.routeFor(id))
        onStartTaskHandled()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.TaskList.route,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit },
        modifier = modifier,
    ) {
        composable(Screen.TaskList.route) {
            TaskListScreen(
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenTask = { taskId ->
                    navController.navigate(Screen.TaskDetail.routeFor(taskId))
                },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) {
            TaskDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

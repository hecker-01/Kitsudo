package dev.heckr.kitsudo.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
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
private val exit  = fadeOut(animationSpec = tween(180))

@Composable
fun KitsudoNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
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

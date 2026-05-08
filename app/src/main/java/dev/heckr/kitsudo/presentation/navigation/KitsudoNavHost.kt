package dev.heckr.kitsudo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.heckr.kitsudo.domain.model.CatppuccinFlavor
import dev.heckr.kitsudo.presentation.tasks.TaskListScreen

@Composable
fun KitsudoNavHost(
    currentFlavor: CatppuccinFlavor,
    onFlavorChange: (CatppuccinFlavor) -> Unit,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TaskList.route,
        modifier = modifier,
    ) {
        composable(Screen.TaskList.route) {
            TaskListScreen(
                currentFlavor = currentFlavor,
                onFlavorChange = onFlavorChange,
            )
        }
    }
}

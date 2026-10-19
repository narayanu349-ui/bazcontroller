package com.baz.controller.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.baz.controller.ui.screens.ConnectionScreen
import com.baz.controller.ui.screens.ControllerScreen
import com.baz.controller.ui.screens.LayoutEditorScreen
import com.baz.controller.ui.screens.SettingsScreen
import com.baz.controller.ui.screens.TemplateScreen

sealed class Screen(val route: String) {
    object Controller : Screen("controller")
    object Connection : Screen("connection")
    object LayoutEditor : Screen("layout_editor")
    object Settings : Screen("settings")
    object Templates : Screen("templates")
}

@Composable
fun BazNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Controller.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Controller.route) {
            ControllerScreen(
                onNavigateToConnection = { navController.navigate(Screen.Connection.route) },
                onNavigateToEditor = { navController.navigate(Screen.LayoutEditor.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToTemplates = { navController.navigate(Screen.Templates.route) }
            )
        }
        composable(Screen.Connection.route) {
            ConnectionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LayoutEditor.route) {
            LayoutEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Templates.route) {
            TemplateScreen(
                onBack = { navController.popBackStack() },
                onSelectTemplate = {
                    navController.popBackStack(Screen.Controller.route, false)
                }
            )
        }
    }
}

package com.flow.pharos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flow.pharos.ui.navigation.Screen
import com.flow.pharos.ui.navigation.bottomNavItems
import com.flow.pharos.ui.screen.*
import com.flow.pharos.ui.theme.PharosTheme
import com.flow.pharos.ui.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { PharosTheme { PharosMainContent() } }
    }
}

@Composable
private fun PharosMainContent() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Dashboard.route, modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    hiltViewModel<DashboardViewModel>(),
                    onNavigateToFolders = { navController.navigate(Screen.Folders.route) },
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) }
                )
            }
            composable(Screen.Folders.route) { FolderScreen(hiltViewModel<FolderViewModel>()) }
            composable(Screen.Files.route) {
                FilesScreen(hiltViewModel<FilesViewModel>()) { fileId -> navController.navigate(Screen.FileDetail.createRoute(fileId)) }
            }
            composable(Screen.FileDetail.route, arguments = listOf(navArgument("fileId") { type = NavType.StringType })) { backStackEntry ->
                val fileId = backStackEntry.arguments?.getString("fileId") ?: return@composable
                FileDetailScreen(fileId, hiltViewModel<FilesViewModel>()) { navController.popBackStack() }
            }
            composable(Screen.Projects.route) {
                ProjectsScreen(hiltViewModel<ProjectsViewModel>()) { projectId -> navController.navigate(Screen.ProjectDetail.createRoute(projectId)) }
            }
            composable(Screen.ProjectDetail.route, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                ProjectDetailScreen(projectId, hiltViewModel<ProjectsViewModel>()) { navController.popBackStack() }
            }
            composable(Screen.Settings.route) { NewSettingsScreen(hiltViewModel<SettingsViewModel>()) }
            composable(Screen.Conflicts.route) { ConflictResolutionScreen(hiltViewModel<ConflictResolutionViewModel>()) }
            composable(Screen.Timeline.route) { TimelineScreen(hiltViewModel<TimelineViewModel>()) }
        }
    }
}


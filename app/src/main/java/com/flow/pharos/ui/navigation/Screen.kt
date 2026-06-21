package com.flow.pharos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Folders : Screen("folders", "Folders", Icons.Default.Folder)
    data object Files : Screen("files", "Files", Icons.Default.InsertDriveFile)
    data object Projects : Screen("projects", "Projects", Icons.Default.WorkspacePremium)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Conflicts : Screen("conflicts", "Conflicts", Icons.Default.SwapHoriz)
    data object Timeline : Screen("timeline", "Timeline", Icons.Default.Timeline)
    data object ProjectDetail : Screen("projects/{projectId}", "Project Detail", Icons.Default.WorkspacePremium) {
        fun createRoute(projectId: String) = "projects/$projectId"
    }
    data object FileDetail : Screen("files/{fileId}", "File Detail", Icons.Default.InsertDriveFile) {
        fun createRoute(fileId: String) = "files/$fileId"
    }
}

val bottomNavItems = listOf(Screen.Dashboard, Screen.Conflicts, Screen.Timeline, Screen.Files, Screen.Settings)

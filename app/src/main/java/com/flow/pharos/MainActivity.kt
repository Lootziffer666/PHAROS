package com.flow.pharos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flow.pharos.core.model.UiResult
import com.flow.pharos.feature.archive.ArchiveScreen
import com.flow.pharos.feature.relations.RelationsScreen
import com.flow.pharos.feature.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PharosApp() }
    }
}

private sealed class Screen(val route: String, val label: String) {
    data object Archive : Screen("archive", "Archive")
    data object Relations : Screen("relations", "Relations")
    data object Settings : Screen("settings", "Settings")
}

private val screens = listOf(Screen.Archive, Screen.Relations, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharosApp(vm: MainViewModel = hiltViewModel()) {
    val uiResult by vm.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pharos · ${BuildConfig.CHAT_ID}") }) },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {},
                        label = { Text(screen.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (val result = uiResult) {
            is UiResult.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiResult.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Error: ${result.message}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is UiResult.Success -> {
                val ui = result.data
                NavHost(
                    navController = navController,
                    startDestination = Screen.Archive.route,
                    modifier = Modifier.padding(padding),
                ) {
                    composable(Screen.Archive.route) {
                        ArchiveScreen(ui.archive.artifacts)
                    }
                    composable(Screen.Relations.route) {
                        RelationsScreen(ui.archive.relations)
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            state = ui,
                            onSaveBudget = vm::saveBudgetPolicy,
                            onPingProviders = vm::pingProviders,
                            onRefreshLocalModels = vm::refreshLocalModels,
                        )
                    }
                }
            }
        }
    }
}


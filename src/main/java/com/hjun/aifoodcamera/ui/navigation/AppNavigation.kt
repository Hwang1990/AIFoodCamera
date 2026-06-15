package com.hjun.aifoodcamera.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hjun.aifoodcamera.R
import com.hjun.aifoodcamera.ui.CameraScreen
import com.hjun.aifoodcamera.ui.DiaryScreen
import com.hjun.aifoodcamera.ui.SettingsScreen
import com.hjun.aifoodcamera.viewmodel.CameraViewModel
import com.hjun.aifoodcamera.viewmodel.DiaryViewModel
import com.hjun.aifoodcamera.viewmodel.SettingsViewModel

sealed class AppRoute(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Camera : AppRoute("camera", R.string.nav_camera, Icons.Default.CameraAlt)
    data object Diary : AppRoute("diary", R.string.nav_diary, Icons.Default.Book)
    data object Settings : AppRoute("settings", R.string.nav_settings, Icons.Default.Settings)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val routes = listOf(AppRoute.Camera, AppRoute.Diary, AppRoute.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(64.dp)
            ) {
                routes.forEach { route ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                route.icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(route.labelRes),
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == route.route } == true,
                        onClick = {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Camera.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Camera.route) {
                val viewModel: CameraViewModel = viewModel(
                    factory = CameraViewModel.Factory(context)
                )
                CameraScreen(viewModel = viewModel)
            }
            composable(AppRoute.Diary.route) {
                val viewModel: DiaryViewModel = viewModel(
                    factory = DiaryViewModel.Factory(context)
                )
                DiaryScreen(viewModel = viewModel)
            }
            composable(AppRoute.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(context)
                )
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

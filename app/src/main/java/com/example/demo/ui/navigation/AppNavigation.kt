package com.example.demo.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.demo.App
import com.example.demo.data.local.entity.TaskEntity
import com.example.demo.service.TaskForegroundService
import com.example.demo.ui.console.ConsoleScreen
import com.example.demo.ui.console.ConsoleViewModel
import com.example.demo.ui.dashboard.DashboardScreen
import com.example.demo.ui.dashboard.DashboardViewModel
import com.example.demo.ui.probe.ProbeScreen
import com.example.demo.ui.proxy.ProxyScreen
import com.example.demo.ui.proxy.ProxyViewModel
import com.example.demo.ui.task.TaskEditDialog
import com.example.demo.ui.task.TaskListScreen
import com.example.demo.ui.task.TaskViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "控制面板", Icons.Filled.Dashboard)
    @Suppress("DEPRECATION")
    data object Tasks : Screen("tasks", "任务", Icons.Filled.ListAlt)
    data object Console : Screen("console", "控制台", Icons.Filled.Terminal)
    data object Proxy : Screen("proxy", "代理", Icons.Filled.Settings)
}

private val screens = listOf(Screen.Dashboard, Screen.Tasks, Screen.Console, Screen.Proxy)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }

    val engine = remember { App.instance.executionEngine }
    val engineState = engine.engineState.collectAsStateWithLifecycle(initialValue = com.example.demo.engine.EngineState()).value
    val taskViewModel: TaskViewModel = viewModel()
    val allTasks = taskViewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList<TaskEntity>()).value

    // Manage foreground service based on engine state
    LaunchedEffect(engineState.isRunning) {
        if (engineState.isRunning) {
            val intent = Intent(context, TaskForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(Intent(context, TaskForegroundService::class.java))
        }
    }

    val isProbeScreen = currentRoute?.startsWith("probe") == true

    Scaffold(
        bottomBar = {
            if (!isProbeScreen) {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
            } // end if (!isProbeScreen)
        },
        floatingActionButton = {
            if (!isProbeScreen && currentRoute in listOf(Screen.Dashboard.route, Screen.Tasks.route)) {
                FloatingActionButton(onClick = {
                    editingTaskId = null
                    showTaskDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "新建任务")
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = viewModel()
                DashboardScreen(
                    viewModel = vm,
                    onNavigateToProbe = { goodsId ->
                        navController.navigate("probe/${Uri.encode(goodsId)}")
                    }
                )
            }
            composable(
                "probe/{goodsId}",
                arguments = listOf(navArgument("goodsId") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                ProbeScreen(
                    initialGoodsId = backStackEntry.arguments?.getString("goodsId") ?: "",
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Tasks.route) {
                TaskListScreen(
                    viewModel = taskViewModel,
                    onShowEditDialog = { taskId ->
                        editingTaskId = taskId
                        showTaskDialog = true
                    }
                )
            }
            composable(Screen.Console.route) {
                val vm: ConsoleViewModel = viewModel()
                ConsoleScreen(viewModel = vm)
            }
            composable(Screen.Proxy.route) {
                val vm: ProxyViewModel = viewModel()
                ProxyScreen(viewModel = vm)
            }
        }
    }

    // Task edit dialog overlay
    if (showTaskDialog) {
        val existingTask = editingTaskId?.let { id ->
            allTasks.find { it.id == id }
        }
        TaskEditDialog(
            existingTask = existingTask,
            onDismiss = { showTaskDialog = false },
            onSave = { goodsId, productName, intervalMs, turboMode, totalCount ->
                if (editingTaskId != null) {
                    val task = existingTask
                    if (task != null) {
                        taskViewModel.updateTask(
                            task.copy(
                                goodsId = goodsId,
                                productName = productName,
                                intervalMs = intervalMs,
                                turboMode = turboMode,
                                totalCount = totalCount
                            )
                        )
                    }
                } else {
                    taskViewModel.createTask(goodsId, productName, intervalMs, turboMode, totalCount)
                }
                showTaskDialog = false
            }
        )
    }
}

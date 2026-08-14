package com.willykez.fxetcher.ui.nav

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.willykez.fxetcher.ui.components.PulsingDot
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.willykez.fxetcher.ui.FxViewModel
import com.willykez.fxetcher.ui.calc.CalcScreen
import com.willykez.fxetcher.ui.convert.ConvertScreen
import com.willykez.fxetcher.ui.home.HomeScreen
import com.willykez.fxetcher.ui.markets.MarketsScreen
import com.willykez.fxetcher.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

sealed class Dest(val route: String, val label: String) {
    data object Home : Dest("home", "Home")
    data object Convert : Dest("convert", "Convert")
    data object Markets : Dest("markets", "Markets")
    data object Calc : Dest("calc", "Calc")
    data object Settings : Dest("settings", "Settings")
}

private val destinations = listOf(Dest.Home, Dest.Convert, Dest.Markets, Dest.Calc, Dest.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(vm: FxViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.snackbarMessages.collectLatest { msg -> snackbarHostState.showSnackbar(msg) }
    }

    val fetching by vm.fetching.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🇹🇿", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Text("FXetcher", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot(color = MaterialTheme.colorScheme.tertiary, active = fetching)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            vm.lastUpdateText(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { vm.refreshAll() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                destinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconFor(dest), contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Home.route) { HomeScreen(vm) }
            composable(Dest.Convert.route) { ConvertScreen(vm) }
            composable(Dest.Markets.route) { MarketsScreen(vm) }
            composable(Dest.Calc.route) { CalcScreen(vm) }
            composable(Dest.Settings.route) { SettingsScreen(vm) }
        }
    }
}

private fun iconFor(dest: Dest) = when (dest) {
    Dest.Home -> Icons.Filled.Home
    Dest.Convert -> Icons.Filled.CurrencyExchange
    Dest.Markets -> Icons.Filled.ShowChart
    Dest.Calc -> Icons.Filled.Calculate
    Dest.Settings -> Icons.Filled.Settings
}

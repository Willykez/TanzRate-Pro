package com.willykez.fxetcher.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.willykez.fxetcher.ui.components.GlobalSearchSheet
import com.willykez.fxetcher.ui.components.PulsingDot
import com.willykez.fxetcher.ui.components.QuickConvertSheet
import com.willykez.fxetcher.ui.theme.Orange
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.willykez.fxetcher.ui.FxViewModel
import com.willykez.fxetcher.ui.analytics.AnalyticsScreen
import com.willykez.fxetcher.ui.calc.CalcScreen
import com.willykez.fxetcher.ui.convert.ConvertScreen
import com.willykez.fxetcher.ui.home.HomeScreen
import com.willykez.fxetcher.ui.markets.MarketsScreen
import com.willykez.fxetcher.ui.settings.SettingsScreen
import com.willykez.fxetcher.ui.strings.LocalStrings
import kotlinx.coroutines.flow.collectLatest

sealed class Dest(val route: String) {
    data object Home : Dest("home")
    data object Convert : Dest("convert")
    data object Markets : Dest("markets")
    data object Calc : Dest("calc")
    data object Analytics : Dest("analytics")
    data object Settings : Dest("settings")
}

private val destinations = listOf(Dest.Home, Dest.Convert, Dest.Markets, Dest.Calc, Dest.Analytics, Dest.Settings)

/** Width, in dp, above which we switch from a bottom nav bar to a side nav rail (tablets/foldables). */
private const val WIDE_LAYOUT_BREAKPOINT_DP = 600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(vm: FxViewModel) {
    val strings = LocalStrings.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val fetching by vm.fetching.collectAsState()
    val offline by vm.offline.collectAsState()
    val rates by vm.rates.collectAsState()
    val pendingDeepLink by vm.pendingDeepLink.collectAsState()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isWideLayout = screenWidthDp >= WIDE_LAYOUT_BREAKPOINT_DP

    var searchOpen by remember { mutableStateOf(false) }
    var quickConvertCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.snackbarMessages.collectLatest { req ->
            val result = snackbarHostState.showSnackbar(
                message = req.message,
                actionLabel = req.actionLabel,
                withDismissAction = req.actionLabel == null
            )
            if (result == SnackbarResult.ActionPerformed) req.onAction?.invoke()
        }
    }

    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            vm.consumeDeepLink()
        }
    }

    fun labelFor(dest: Dest) = when (dest) {
        Dest.Home -> strings.navHome
        Dest.Convert -> strings.navConvert
        Dest.Markets -> strings.navMarkets
        Dest.Calc -> strings.navCalc
        Dest.Analytics -> strings.navAnalytics
        Dest.Settings -> strings.navSettings
    }

    val topBar: @Composable () -> Unit = {
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
                    IconButton(onClick = { searchOpen = true }) {
                        Icon(Icons.Filled.Search, contentDescription = strings.searchHint)
                    }
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
    }

    val offlineBanner: @Composable () -> Unit = {
        AnimatedVisibility(visible = offline, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Orange.copy(alpha = 0.16f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics(mergeDescendants = true) { contentDescription = strings.offlineBanner },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SignalWifiOff, contentDescription = null, tint = Orange, modifier = Modifier.width(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.offlineBanner, style = MaterialTheme.typography.labelMedium, color = Orange)
            }
        }
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            offlineBanner()
            NavHost(
                navController = navController,
                startDestination = Dest.Home.route,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                enterTransition = { slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { slideOutHorizontally(tween(260)) { it / 6 } + fadeOut(tween(150)) }
            ) {
                composable(Dest.Home.route) { HomeScreen(vm) }
                composable(Dest.Convert.route) { ConvertScreen(vm) }
                composable(Dest.Markets.route) { MarketsScreen(vm) }
                composable(Dest.Calc.route) { CalcScreen(vm) }
                composable(Dest.Analytics.route) { AnalyticsScreen(vm) }
                composable(Dest.Settings.route) { SettingsScreen(vm) }
            }
        }
    }

    fun navigate(dest: Dest) {
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (isWideLayout) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, topBar = topBar) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                NavigationRail(Modifier.fillMaxHeight()) {
                    destinations.forEach { dest ->
                        NavigationRailItem(
                            selected = currentRoute == dest.route,
                            onClick = { navigate(dest) },
                            icon = { Icon(iconFor(dest), contentDescription = labelFor(dest)) },
                            label = { Text(labelFor(dest)) }
                        )
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    content(Modifier.widthIn(max = 900.dp).fillMaxSize())
                }
            }
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = topBar,
            bottomBar = {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    destinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { navigate(dest) },
                            icon = { Icon(iconFor(dest), contentDescription = labelFor(dest)) },
                            label = { Text(labelFor(dest)) }
                        )
                    }
                }
            }
        ) { padding ->
            content(Modifier.padding(padding).fillMaxSize())
        }
    }

    if (searchOpen) {
        GlobalSearchSheet(
            rates = rates,
            fmt = vm.fmtTzs::format,
            hint = strings.searchHint,
            noResultsText = strings.searchNoResults,
            onDismiss = { searchOpen = false },
            onSelect = { code -> searchOpen = false; quickConvertCode = code }
        )
    }

    quickConvertCode?.let { code ->
        QuickConvertSheet(
            code = code,
            tzsRate = rates[code],
            fmt = vm.fmtTzs::format,
            onDismiss = { quickConvertCode = null },
            onSave = { amount, result -> vm.saveConversion(amount, code, "TZS", result) }
        )
    }
}

private fun iconFor(dest: Dest) = when (dest) {
    Dest.Home -> Icons.Filled.Home
    Dest.Convert -> Icons.Filled.CurrencyExchange
    Dest.Markets -> Icons.Filled.ShowChart
    Dest.Calc -> Icons.Filled.Calculate
    Dest.Analytics -> Icons.Filled.QueryStats
    Dest.Settings -> Icons.Filled.Settings
}

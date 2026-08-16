package com.willykez.fxetcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willykez.fxetcher.data.BotRate
import com.willykez.fxetcher.data.CurrencyMeta
import com.willykez.fxetcher.data.HomeSort
import com.willykez.fxetcher.data.PriceAlert
import com.willykez.fxetcher.data.RatePoint
import com.willykez.fxetcher.data.RatesRepository
import com.willykez.fxetcher.data.ThemeMode
import com.willykez.fxetcher.data.UserPreferencesRepository
import com.willykez.fxetcher.notifications.NotificationHelper
import com.willykez.fxetcher.ui.strings.AppLanguage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiSettings(
    val autoRefresh: Boolean = true,
    val refreshIntervalMs: Int = 300_000,
    val notifyUpdates: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val compactMode: Boolean = false,
    val homeSort: HomeSort = HomeSort.DEFAULT,
    val highContrast: Boolean = false
)

data class SnackbarRequest(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

class FxViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        /** How many rate snapshots to keep per currency for sparklines & analytics. */
        const val MAX_HISTORY_POINTS = 200
    }

    private val repo = RatesRepository()
    private val prefs = UserPreferencesRepository(app)

    val fmtTzs: DecimalFormat = DecimalFormat("#,##0.00")
    val fmtSmall: DecimalFormat = DecimalFormat("#,##0.0000")

    private val _rates = MutableStateFlow<Map<String, Double>>(RatesRepository.fallbackRates())
    val rates: StateFlow<Map<String, Double>> = _rates

    private val _prevRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val prevRates: StateFlow<Map<String, Double>> = _prevRates

    private val _rateHistory = MutableStateFlow<Map<String, List<RatePoint>>>(emptyMap())
    val rateHistory: StateFlow<Map<String, List<RatePoint>>> = _rateHistory

    private val _fetching = MutableStateFlow(false)
    val fetching: StateFlow<Boolean> = _fetching

    private val _botFetching = MutableStateFlow(false)
    val botFetching: StateFlow<Boolean> = _botFetching

    private val _botRates = MutableStateFlow<List<BotRate>>(emptyList())
    val botRates: StateFlow<List<BotRate>> = _botRates

    private val _botStatus = MutableStateFlow("Pull to fetch official rates")
    val botStatus: StateFlow<String> = _botStatus

    private val _lastUpdate = MutableStateFlow(0L)
    val lastUpdate: StateFlow<Long> = _lastUpdate

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline

    private val _pendingDeepLink = MutableStateFlow<String?>(null)
    val pendingDeepLink: StateFlow<String?> = _pendingDeepLink
    fun setPendingDeepLink(route: String?) { _pendingDeepLink.value = route }
    fun consumeDeepLink() { _pendingDeepLink.value = null }

    val alerts: StateFlow<List<PriceAlert>> = prefs.alertsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val watchlist: StateFlow<List<String>> = prefs.watchlistFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val convHistory: StateFlow<List<String>> = prefs.convHistoryFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val calcHistory: StateFlow<List<String>> = prefs.calcHistoryFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pinnedFrom: StateFlow<String> = prefs.pinnedFromFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "USD")
    val pinnedTo: StateFlow<String> = prefs.pinnedToFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "TZS")
    val language: StateFlow<AppLanguage> = prefs.languageFlow
        .map { AppLanguage.fromCode(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.ENGLISH)
    val onboardingDone: StateFlow<Boolean?> = prefs.onboardingDoneFlow
        .map { done -> done as Boolean? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<UiSettings> = combine<Any?, UiSettings>(
        prefs.autoRefreshFlow, prefs.refreshIntervalFlow, prefs.notifyUpdatesFlow,
        prefs.themeModeFlow, prefs.dynamicColorFlow, prefs.compactModeFlow, prefs.homeSortFlow,
        prefs.highContrastFlow
    ) { values ->
        UiSettings(
            autoRefresh = values[0] as Boolean,
            refreshIntervalMs = values[1] as Int,
            notifyUpdates = values[2] as Boolean,
            themeMode = values[3] as ThemeMode,
            dynamicColor = values[4] as Boolean,
            compactMode = values[5] as Boolean,
            homeSort = values[6] as HomeSort,
            highContrast = values[7] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())

    private val _snackbar = Channel<SnackbarRequest>(Channel.BUFFERED)
    val snackbarMessages = _snackbar.receiveAsFlow()
    private fun snack(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        _snackbar.trySend(SnackbarRequest(message, actionLabel, onAction))
    }

    init {
        viewModelScope.launch {
            val cachedRates = prefs.ratesFlow.first()
            if (cachedRates.isNotEmpty()) _rates.value = cachedRates
            _prevRates.value = prefs.prevRatesFlow.first()
            _rateHistory.value = prefs.rateHistoryFlow.first()
            val cachedBot = prefs.botRatesFlow.first()
            if (cachedBot.isNotEmpty()) {
                _botRates.value = cachedBot
                _botStatus.value = "Showing cached official rates"
            }
            _lastUpdate.value = prefs.lastUpdateFlow.first()
            fetchRates()
            fetchBotRates()
        }
        autoRefreshLoop()
    }

    private fun autoRefreshLoop() {
        viewModelScope.launch {
            while (true) {
                val s = settings.value
                delay(s.refreshIntervalMs.toLong().coerceAtLeast(15_000))
                if (settings.value.autoRefresh) {
                    fetchRates()
                }
            }
        }
    }

    fun fetchRates() {
        if (_fetching.value) return
        viewModelScope.launch {
            _fetching.value = true
            when (val result = repo.fetchRates()) {
                is RatesRepository.FetchResult.Success -> {
                    _offline.value = false
                    val newHistory = _rateHistory.value.toMutableMap()
                    val now = System.currentTimeMillis()
                    result.rates.forEach { (code, value) ->
                        val pts = (newHistory[code] ?: emptyList()).toMutableList()
                        pts.add(RatePoint(value, now))
                        if (pts.size > MAX_HISTORY_POINTS) pts.removeAt(0)
                        newHistory[code] = pts
                    }
                    _prevRates.value = _rates.value
                    _rates.value = result.rates
                    _rateHistory.value = newHistory
                    _lastUpdate.value = now
                    prefs.saveRates(result.rates, _prevRates.value)
                    prefs.saveRateHistory(newHistory)
                    checkAlerts()
                    runCatching { com.willykez.fxetcher.widget.refreshRatesWidget(getApplication()) }
                    if (settings.value.notifyUpdates) {
                        NotificationHelper.postRateUpdate(
                            getApplication(), result.rates["USD"], result.rates["EUR"], result.rates["GBP"]
                        )
                    }
                }
                is RatesRepository.FetchResult.Failure -> {
                    _offline.value = true
                    snack("Offline — showing cached rates")
                }
            }
            _fetching.value = false
        }
    }

    fun fetchBotRates() {
        if (_botFetching.value) return
        viewModelScope.launch {
            _botFetching.value = true
            _botStatus.value = "Fetching…"
            val result = repo.fetchBotRates()
            if (result.rates.isNotEmpty()) {
                _botRates.value = result.rates
                prefs.saveBotRates(result.rates)
            }
            _botStatus.value = result.status
            _botFetching.value = false
        }
    }

    fun refreshAll() {
        fetchRates()
        fetchBotRates()
    }

    private fun checkAlerts() {
        alerts.value.forEachIndexed { _, alert ->
            val cur = _rates.value[alert.currency] ?: return@forEachIndexed
            val hit = (alert.condition == 0 && cur >= alert.target) || (alert.condition == 1 && cur <= alert.target)
            if (hit) {
                NotificationHelper.postPriceAlert(getApplication(), alert.currency, alert.condition, alert.target, cur)
            }
        }
    }

    fun convert(amount: Double, from: String, to: String): Double {
        if (from == to) return amount
        val fr = _rates.value[from] ?: return 0.0
        val tr = _rates.value[to] ?: return 0.0
        if (from == "TZS") return if (tr > 0) amount / tr else 0.0
        if (to == "TZS") return amount * fr
        return if (tr > 0) (amount * fr) / tr else 0.0
    }

    fun rate(code: String): Double? = _rates.value[code]

    fun lastUpdateText(): String {
        val t = _lastUpdate.value
        if (t == 0L) return "—"
        val ago = System.currentTimeMillis() - t
        return when {
            ago < 60_000 -> "live"
            ago < 3_600_000 -> "${ago / 60_000}m ago"
            else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))
        }
    }

    fun shareRatesText(): String {
        val sb = StringBuilder("🇹🇿 Tanzania Exchange Rates\n")
        sb.append(SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())).append("\n\n")
        listOf("USD", "EUR", "GBP", "KES", "UGX", "RWF", "ZAR", "AED", "CNY", "XAU", "XAG").forEach { c ->
            _rates.value[c]?.let { sb.append("1 $c = ${fmtTzs.format(it)} TZS\n") }
        }
        sb.append("\nvia FXetcher 🇹🇿")
        return sb.toString()
    }

    // ── Conversion history ────────────────────────────────────────────────
    fun saveConversion(amount: Double, from: String, to: String, result: Double) = viewModelScope.launch {
        val entry = "${fmtTzs.format(amount)} $from → ${fmtTzs.format(result)} $to"
        prefs.addConversion(entry)
        snack("✓ Saved to history")
    }
    fun deleteConversion(index: Int) = viewModelScope.launch {
        val entry = convHistory.value.getOrNull(index) ?: return@launch
        prefs.deleteConversion(index)
        snack("Deleted", actionLabel = "Undo") {
            viewModelScope.launch { prefs.restoreConversion(index, entry) }
        }
    }
    fun clearConversions() = viewModelScope.launch { prefs.clearConversions(); snack("History cleared") }

    // ── Calculator history ───────────────────────────────────────────────
    fun saveCalc(entry: String) = viewModelScope.launch { prefs.addCalc(entry) }
    fun clearCalcHistory() = viewModelScope.launch { prefs.clearCalcHistory(); snack("History cleared") }

    // ── Alerts ────────────────────────────────────────────────────────────
    fun addAlert(currency: String, target: Double, condition: Int) = viewModelScope.launch {
        prefs.addAlert(PriceAlert(currency, target, condition))
        snack("✓ Alert created")
    }
    fun deleteAlert(index: Int) = viewModelScope.launch {
        val alert = alerts.value.getOrNull(index) ?: return@launch
        prefs.deleteAlert(index)
        snack("Alert deleted", actionLabel = "Undo") {
            viewModelScope.launch { prefs.restoreAlert(index, alert) }
        }
    }
    fun clearAlerts() = viewModelScope.launch { prefs.clearAlerts(); snack("Alerts cleared") }

    // ── Watchlist ─────────────────────────────────────────────────────────
    fun toggleWatchlist(code: String) = viewModelScope.launch {
        val wasWatched = watchlist.value.contains(code)
        prefs.toggleWatchlist(code)
        if (wasWatched) {
            snack("Removed from watchlist", actionLabel = "Undo") {
                viewModelScope.launch { prefs.toggleWatchlist(code) }
            }
        } else {
            snack("Added to watchlist")
        }
    }
    fun clearWatchlist() = viewModelScope.launch { prefs.clearWatchlist(); snack("Watchlist cleared") }

    // ── Settings ──────────────────────────────────────────────────────────
    fun setAutoRefresh(enabled: Boolean) = viewModelScope.launch { prefs.setAutoRefresh(enabled) }
    fun setRefreshInterval(ms: Int) = viewModelScope.launch { prefs.setRefreshInterval(ms) }
    fun setNotifyUpdates(enabled: Boolean) = viewModelScope.launch { prefs.setNotifyUpdates(enabled) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { prefs.setDynamicColor(enabled) }
    fun setCompactMode(enabled: Boolean) = viewModelScope.launch { prefs.setCompactMode(enabled) }
    fun setHomeSort(sort: HomeSort) = viewModelScope.launch { prefs.setHomeSort(sort) }
    fun setHighContrast(enabled: Boolean) = viewModelScope.launch { prefs.setHighContrast(enabled) }
    fun setPinnedPair(from: String, to: String) = viewModelScope.launch {
        prefs.setPinnedPair(from, to)
        snack("📌 Pinned: $from → $to")
    }
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { prefs.setLanguage(lang.code) }
    fun completeOnboarding() = viewModelScope.launch { prefs.setOnboardingDone(true) }
    fun replayOnboarding() = viewModelScope.launch { prefs.setOnboardingDone(false) }

    fun resetAllData() = viewModelScope.launch {
        prefs.resetAll()
        _rates.value = RatesRepository.fallbackRates()
        _prevRates.value = emptyMap()
        _rateHistory.value = emptyMap()
        _botRates.value = emptyList()
        snack("App data reset")
    }
}

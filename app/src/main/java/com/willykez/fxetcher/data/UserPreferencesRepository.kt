package com.willykez.fxetcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fxetcher_prefs")

/**
 * Wraps Jetpack DataStore to persist settings, cached rates, history, alerts
 * and the watchlist — the Compose-era replacement for the old SharedPreferences store.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val RATES = stringPreferencesKey("rates_v5")
        val PREV_RATES = stringPreferencesKey("prev_rates_v5")
        val RATE_HISTORY = stringPreferencesKey("rate_history_v5")
        val LAST_UPDATE = longPreferencesKey("last_update")
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        val REFRESH_INTERVAL = intPreferencesKey("refresh_interval")
        val NOTIFY_UPDATES = booleanPreferencesKey("notify_updates")
        val CONV_HISTORY = stringPreferencesKey("conv_history")
        val CALC_HISTORY = stringPreferencesKey("calc_history")
        val ALERTS = stringPreferencesKey("price_alerts")
        val WATCHLIST = stringPreferencesKey("watchlist_v5")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val COMPACT = booleanPreferencesKey("compact_mode")
        val PINNED_FROM = stringPreferencesKey("pinned_from")
        val PINNED_TO = stringPreferencesKey("pinned_to")
        val BOT_RATES = stringPreferencesKey("bot_rates_cache")
        val HOME_SORT = stringPreferencesKey("home_sort_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    }

    val ratesFlow: Flow<Map<String, Double>> =
        context.dataStore.data.map { parseRatesMap(it[Keys.RATES] ?: "") }

    val prevRatesFlow: Flow<Map<String, Double>> =
        context.dataStore.data.map { parseRatesMap(it[Keys.PREV_RATES] ?: "") }

    val rateHistoryFlow: Flow<Map<String, List<RatePoint>>> =
        context.dataStore.data.map { parseRateHistory(it[Keys.RATE_HISTORY] ?: "") }

    val lastUpdateFlow: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_UPDATE] ?: 0L }
    val autoRefreshFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_REFRESH] ?: true }
    val refreshIntervalFlow: Flow<Int> = context.dataStore.data.map { it[Keys.REFRESH_INTERVAL] ?: 300_000 }
    val notifyUpdatesFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFY_UPDATES] ?: true }
    val convHistoryFlow: Flow<List<String>> = context.dataStore.data.map { parseStringList(it[Keys.CONV_HISTORY] ?: "[]") }
    val calcHistoryFlow: Flow<List<String>> = context.dataStore.data.map { parseStringList(it[Keys.CALC_HISTORY] ?: "[]") }
    val alertsFlow: Flow<List<PriceAlert>> = context.dataStore.data.map { parseAlerts(it[Keys.ALERTS] ?: "[]") }
    val watchlistFlow: Flow<List<String>> = context.dataStore.data.map { parseStringList(it[Keys.WATCHLIST] ?: "[]") }
    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[Keys.THEME_MODE] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }
    val compactModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.COMPACT] ?: false }
    val pinnedFromFlow: Flow<String> = context.dataStore.data.map { it[Keys.PINNED_FROM] ?: "USD" }
    val pinnedToFlow: Flow<String> = context.dataStore.data.map { it[Keys.PINNED_TO] ?: "TZS" }
    val botRatesFlow: Flow<List<BotRate>> = context.dataStore.data.map { parseBotRates(it[Keys.BOT_RATES] ?: "[]") }
    val homeSortFlow: Flow<HomeSort> = context.dataStore.data.map {
        runCatching { HomeSort.valueOf(it[Keys.HOME_SORT] ?: "DEFAULT") }.getOrDefault(HomeSort.DEFAULT)
    }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    val onboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val highContrastFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.HIGH_CONTRAST] ?: false }

    suspend fun saveRates(rates: Map<String, Double>, previous: Map<String, Double>) {
        context.dataStore.edit {
            it[Keys.RATES] = rates.toJsonObject()
            it[Keys.PREV_RATES] = previous.toJsonObject()
            it[Keys.LAST_UPDATE] = System.currentTimeMillis()
        }
    }

    suspend fun saveRateHistory(history: Map<String, List<RatePoint>>) {
        context.dataStore.edit { it[Keys.RATE_HISTORY] = history.toJsonHistory() }
    }

    suspend fun setAutoRefresh(enabled: Boolean) = context.dataStore.edit { it[Keys.AUTO_REFRESH] = enabled }
    suspend fun setRefreshInterval(ms: Int) = context.dataStore.edit { it[Keys.REFRESH_INTERVAL] = ms }
    suspend fun setNotifyUpdates(enabled: Boolean) = context.dataStore.edit { it[Keys.NOTIFY_UPDATES] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setCompactMode(enabled: Boolean) = context.dataStore.edit { it[Keys.COMPACT] = enabled }
    suspend fun setPinnedPair(from: String, to: String) = context.dataStore.edit {
        it[Keys.PINNED_FROM] = from; it[Keys.PINNED_TO] = to
    }
    suspend fun setHomeSort(sort: HomeSort) = context.dataStore.edit { it[Keys.HOME_SORT] = sort.name }
    suspend fun setLanguage(code: String) = context.dataStore.edit { it[Keys.LANGUAGE] = code }
    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    suspend fun setHighContrast(enabled: Boolean) = context.dataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }

    suspend fun saveBotRates(rates: List<BotRate>) = context.dataStore.edit { it[Keys.BOT_RATES] = rates.toBotJson() }

    suspend fun addConversion(entry: String) = context.dataStore.edit { prefs ->
        val list = (parseStringList(prefs[Keys.CONV_HISTORY] ?: "[]").toMutableList())
        list.add(0, entry)
        while (list.size > 30) list.removeAt(list.size - 1)
        prefs[Keys.CONV_HISTORY] = list.toJsonArray()
    }
    suspend fun deleteConversion(index: Int) = context.dataStore.edit { prefs ->
        val list = parseStringList(prefs[Keys.CONV_HISTORY] ?: "[]").toMutableList()
        if (index in list.indices) list.removeAt(index)
        prefs[Keys.CONV_HISTORY] = list.toJsonArray()
    }
    suspend fun restoreConversion(index: Int, entry: String) = context.dataStore.edit { prefs ->
        val list = parseStringList(prefs[Keys.CONV_HISTORY] ?: "[]").toMutableList()
        val at = index.coerceIn(0, list.size)
        list.add(at, entry)
        prefs[Keys.CONV_HISTORY] = list.toJsonArray()
    }
    suspend fun clearConversions() = context.dataStore.edit { it[Keys.CONV_HISTORY] = "[]" }

    suspend fun addCalc(entry: String) = context.dataStore.edit { prefs ->
        val list = parseStringList(prefs[Keys.CALC_HISTORY] ?: "[]").toMutableList()
        list.add(0, entry)
        while (list.size > 20) list.removeAt(list.size - 1)
        prefs[Keys.CALC_HISTORY] = list.toJsonArray()
    }
    suspend fun clearCalcHistory() = context.dataStore.edit { it[Keys.CALC_HISTORY] = "[]" }

    suspend fun addAlert(alert: PriceAlert) = context.dataStore.edit { prefs ->
        val list = parseAlerts(prefs[Keys.ALERTS] ?: "[]").toMutableList()
        list.add(alert)
        prefs[Keys.ALERTS] = list.toAlertJson()
    }
    suspend fun deleteAlert(index: Int) = context.dataStore.edit { prefs ->
        val list = parseAlerts(prefs[Keys.ALERTS] ?: "[]").toMutableList()
        if (index in list.indices) list.removeAt(index)
        prefs[Keys.ALERTS] = list.toAlertJson()
    }
    suspend fun restoreAlert(index: Int, alert: PriceAlert) = context.dataStore.edit { prefs ->
        val list = parseAlerts(prefs[Keys.ALERTS] ?: "[]").toMutableList()
        val at = index.coerceIn(0, list.size)
        list.add(at, alert)
        prefs[Keys.ALERTS] = list.toAlertJson()
    }
    suspend fun clearAlerts() = context.dataStore.edit { it[Keys.ALERTS] = "[]" }

    suspend fun toggleWatchlist(code: String) = context.dataStore.edit { prefs ->
        val list = parseStringList(prefs[Keys.WATCHLIST] ?: "[]").toMutableList()
        if (list.contains(code)) list.remove(code) else list.add(code)
        prefs[Keys.WATCHLIST] = list.toJsonArray()
    }
    suspend fun clearWatchlist() = context.dataStore.edit { it[Keys.WATCHLIST] = "[]" }

    suspend fun resetAll() = context.dataStore.edit { it.clear() }
}

package com.willykez.fxetcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

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
        val PORTFOLIO = stringPreferencesKey("portfolio_holdings")
        val WIDGET_CURRENCIES = stringPreferencesKey("widget_currencies")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
    }

    companion object {
        val DEFAULT_WIDGET_CURRENCIES = listOf("USD", "EUR", "GBP", "XAU")
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
    val portfolioFlow: Flow<List<PortfolioHolding>> = context.dataStore.data.map { parsePortfolio(it[Keys.PORTFOLIO] ?: "[]") }
    val widgetCurrenciesFlow: Flow<List<String>> = context.dataStore.data.map {
        parseStringList(it[Keys.WIDGET_CURRENCIES] ?: "[]").ifEmpty { DEFAULT_WIDGET_CURRENCIES }
    }
    val accentThemeFlow: Flow<AccentTheme> = context.dataStore.data.map {
        runCatching { AccentTheme.valueOf(it[Keys.ACCENT_THEME] ?: "GOLD") }.getOrDefault(AccentTheme.GOLD)
    }
    val amoledModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.AMOLED_MODE] ?: false }
    val navBarStyleFlow: Flow<NavBarStyle> = context.dataStore.data.map {
        runCatching { NavBarStyle.valueOf(it[Keys.NAV_BAR_STYLE] ?: "CLASSIC") }.getOrDefault(NavBarStyle.CLASSIC)
    }

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

    suspend fun addHolding(holding: PortfolioHolding) = context.dataStore.edit { prefs ->
        val list = parsePortfolio(prefs[Keys.PORTFOLIO] ?: "[]").toMutableList()
        list.add(holding)
        prefs[Keys.PORTFOLIO] = list.toPortfolioJson()
    }
    suspend fun deleteHolding(index: Int) = context.dataStore.edit { prefs ->
        val list = parsePortfolio(prefs[Keys.PORTFOLIO] ?: "[]").toMutableList()
        if (index in list.indices) list.removeAt(index)
        prefs[Keys.PORTFOLIO] = list.toPortfolioJson()
    }
    suspend fun restoreHolding(index: Int, holding: PortfolioHolding) = context.dataStore.edit { prefs ->
        val list = parsePortfolio(prefs[Keys.PORTFOLIO] ?: "[]").toMutableList()
        val at = index.coerceIn(0, list.size)
        list.add(at, holding)
        prefs[Keys.PORTFOLIO] = list.toPortfolioJson()
    }
    suspend fun clearPortfolio() = context.dataStore.edit { it[Keys.PORTFOLIO] = "[]" }

    suspend fun setWidgetCurrencies(codes: List<String>) = context.dataStore.edit {
        it[Keys.WIDGET_CURRENCIES] = codes.toJsonArray()
    }
    suspend fun setAccentTheme(theme: AccentTheme) = context.dataStore.edit { it[Keys.ACCENT_THEME] = theme.name }
    suspend fun setAmoledMode(enabled: Boolean) = context.dataStore.edit { it[Keys.AMOLED_MODE] = enabled }
    suspend fun setNavBarStyle(style: NavBarStyle) = context.dataStore.edit { it[Keys.NAV_BAR_STYLE] = style.name }

    suspend fun resetAll() = context.dataStore.edit { it.clear() }

    // ── Backup & restore ────────────────────────────────────────────────
    // Deliberately excludes cached rates/history/BoT data — those are
    // re-fetched on next launch. Only user-authored data and preferences
    // are worth round-tripping through a file.
    suspend fun exportBackupJson(): String {
        val snapshot = context.dataStore.data.first()
        val obj = JSONObject()
        obj.put("backup_version", 1)
        obj.put("exported_at", System.currentTimeMillis())
        obj.put("conv_history", snapshot[Keys.CONV_HISTORY] ?: "[]")
        obj.put("calc_history", snapshot[Keys.CALC_HISTORY] ?: "[]")
        obj.put("alerts", snapshot[Keys.ALERTS] ?: "[]")
        obj.put("watchlist", snapshot[Keys.WATCHLIST] ?: "[]")
        obj.put("portfolio", snapshot[Keys.PORTFOLIO] ?: "[]")
        obj.put("widget_currencies", snapshot[Keys.WIDGET_CURRENCIES] ?: DEFAULT_WIDGET_CURRENCIES.toJsonArray())
        obj.put("pinned_from", snapshot[Keys.PINNED_FROM] ?: "USD")
        obj.put("pinned_to", snapshot[Keys.PINNED_TO] ?: "TZS")
        obj.put("auto_refresh", snapshot[Keys.AUTO_REFRESH] ?: true)
        obj.put("refresh_interval", snapshot[Keys.REFRESH_INTERVAL] ?: 300_000)
        obj.put("notify_updates", snapshot[Keys.NOTIFY_UPDATES] ?: true)
        obj.put("theme_mode", snapshot[Keys.THEME_MODE] ?: "SYSTEM")
        obj.put("dynamic_color", snapshot[Keys.DYNAMIC_COLOR] ?: true)
        obj.put("compact_mode", snapshot[Keys.COMPACT] ?: false)
        obj.put("home_sort", snapshot[Keys.HOME_SORT] ?: "DEFAULT")
        obj.put("app_language", snapshot[Keys.LANGUAGE] ?: "en")
        obj.put("high_contrast", snapshot[Keys.HIGH_CONTRAST] ?: false)
        obj.put("accent_theme", snapshot[Keys.ACCENT_THEME] ?: "GOLD")
        obj.put("amoled_mode", snapshot[Keys.AMOLED_MODE] ?: false)
        obj.put("nav_bar_style", snapshot[Keys.NAV_BAR_STYLE] ?: "CLASSIC")
        return obj.toString(2)
    }

    /** Returns true if the file looked like a valid FXetcher backup and was applied. */
    suspend fun importBackupJson(json: String): Boolean = runCatching {
        val obj = JSONObject(json)
        if (!obj.has("backup_version")) return@runCatching false
        context.dataStore.edit { p ->
            obj.optString("conv_history").takeIf { obj.has("conv_history") }?.let { p[Keys.CONV_HISTORY] = it }
            obj.optString("calc_history").takeIf { obj.has("calc_history") }?.let { p[Keys.CALC_HISTORY] = it }
            obj.optString("alerts").takeIf { obj.has("alerts") }?.let { p[Keys.ALERTS] = it }
            obj.optString("watchlist").takeIf { obj.has("watchlist") }?.let { p[Keys.WATCHLIST] = it }
            obj.optString("portfolio").takeIf { obj.has("portfolio") }?.let { p[Keys.PORTFOLIO] = it }
            obj.optString("widget_currencies").takeIf { obj.has("widget_currencies") }?.let { p[Keys.WIDGET_CURRENCIES] = it }
            if (obj.has("pinned_from")) p[Keys.PINNED_FROM] = obj.getString("pinned_from")
            if (obj.has("pinned_to")) p[Keys.PINNED_TO] = obj.getString("pinned_to")
            if (obj.has("auto_refresh")) p[Keys.AUTO_REFRESH] = obj.getBoolean("auto_refresh")
            if (obj.has("refresh_interval")) p[Keys.REFRESH_INTERVAL] = obj.getInt("refresh_interval")
            if (obj.has("notify_updates")) p[Keys.NOTIFY_UPDATES] = obj.getBoolean("notify_updates")
            if (obj.has("theme_mode")) p[Keys.THEME_MODE] = obj.getString("theme_mode")
            if (obj.has("dynamic_color")) p[Keys.DYNAMIC_COLOR] = obj.getBoolean("dynamic_color")
            if (obj.has("compact_mode")) p[Keys.COMPACT] = obj.getBoolean("compact_mode")
            if (obj.has("home_sort")) p[Keys.HOME_SORT] = obj.getString("home_sort")
            if (obj.has("app_language")) p[Keys.LANGUAGE] = obj.getString("app_language")
            if (obj.has("high_contrast")) p[Keys.HIGH_CONTRAST] = obj.getBoolean("high_contrast")
            if (obj.has("accent_theme")) p[Keys.ACCENT_THEME] = obj.getString("accent_theme")
            if (obj.has("amoled_mode")) p[Keys.AMOLED_MODE] = obj.getBoolean("amoled_mode")
            if (obj.has("nav_bar_style")) p[Keys.NAV_BAR_STYLE] = obj.getString("nav_bar_style")
        }
        true
    }.getOrDefault(false)
}

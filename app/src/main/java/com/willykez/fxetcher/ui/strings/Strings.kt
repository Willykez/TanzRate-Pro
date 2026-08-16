package com.willykez.fxetcher.ui.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    ENGLISH("en", "English", "🇬🇧"),
    SWAHILI("sw", "Kiswahili", "🇹🇿");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.find { it.code == code } ?: ENGLISH
    }
}

/**
 * Central copy deck for the app chrome. Not every single microcopy string is
 * routed through here — deep, rarely-seen strings fall back to English — but
 * navigation, section headers, buttons, settings, and onboarding are fully
 * covered in both languages.
 */
data class Strings(
    // Navigation
    val navHome: String, val navConvert: String, val navMarkets: String,
    val navCalc: String, val navAnalytics: String, val navSettings: String,

    // Common
    val save: String, val cancel: String, val clear: String, val delete: String,
    val share: String, val copy: String, val add: String, val loading: String,
    val live: String, val today: String, val current: String, val target: String,

    // Home
    val liveRatesTitle: String, val liveRatesSubtitle: String,
    val eastAfricaTitle: String, val eastAfricaSubtitle: String,
    val metalsTitle: String, val metalsSubtitle: String,
    val botTitle: String, val botSubtitle: String, val perTroyOz: String,

    // Convert
    val converterTitle: String, val converterSubtitle: String,
    val amount: String, val from: String, val to: String, val swap: String, val pinPair: String,
    val multiCurrencyTitle: String, val multiCurrencySubtitle: String,
    val quickAmountsTitle: String, val quickAmountsSubtitle: String,
    val historyTitle: String, val historySubtitle: String, val noConversions: String,

    // Markets
    val watchlistTitle: String, val watchlistSubtitle: String, val watchlistEmpty: String,
    val majorTitle: String, val majorSubtitle: String,
    val alertsTitle: String, val alertsSubtitle: String, val addAlert: String, val noAlerts: String,
    val newAlertTitle: String, val newAlertSubtitle: String, val risesAbove: String, val fallsBelow: String,
    val createAlert: String,

    // Calc
    val keypad: String, val split: String, val splitTitle: String, val splitSubtitle: String,
    val splitBetween: String, val calcHistoryTitle: String, val rateRefTitle: String, val rateRefSubtitle: String,

    // Analytics
    val analyticsTitle: String, val analyticsSubtitle: String,
    val statHigh: String, val statLow: String, val statAvg: String, val statChange: String,
    val topMoversTitle: String, val topMoversSubtitle: String, val notEnoughData: String,

    // Settings
    val autoRefreshTitle: String, val autoRefreshSubtitle: String,
    val refreshIntervalTitle: String, val refreshNow: String,
    val appearanceTitle: String, val appearanceSubtitle: String,
    val languageTitle: String, val languageSubtitle: String,
    val dynamicColorTitle: String, val dynamicColorSubtitle: String,
    val displayTitle: String, val displaySubtitle: String,
    val compactTitle: String, val compactSubtitle: String,
    val notificationsTitle: String, val notificationsSubtitle: String,
    val notifyUpdatesTitle: String, val notifyUpdatesSubtitle: String,
    val dataManagementTitle: String,
    val exportRates: String, val clearHistory: String, val clearWatchlist: String,
    val clearAlerts: String, val resetAll: String, val replayOnboarding: String,
    val aboutTitle: String,

    // Onboarding
    val onboardTitle1: String, val onboardBody1: String,
    val onboardTitle2: String, val onboardBody2: String,
    val onboardTitle3: String, val onboardBody3: String,
    val onboardTitle4: String, val onboardBody4: String,
    val onboardSkip: String, val onboardNext: String, val onboardStart: String,

    // Widget
    val widgetTitle: String,

    // Search / shortcuts / accessibility / hero (new)
    val searchHint: String, val searchNoResults: String,
    val undo: String, val deleted: String,
    val offlineBanner: String,
    val highContrastTitle: String, val highContrastSubtitle: String,
    val heroChangeToday: String,
    val shortcutConvert: String, val shortcutMarkets: String, val shortcutCalc: String
)

val EnglishStrings = Strings(
    navHome = "Home", navConvert = "Convert", navMarkets = "Markets",
    navCalc = "Calc", navAnalytics = "Analytics", navSettings = "Settings",

    save = "Save", cancel = "Cancel", clear = "Clear", delete = "Delete",
    share = "Share", copy = "Copy", add = "Add", loading = "Loading…",
    live = "live", today = "Today", current = "Current", target = "Target",

    liveRatesTitle = "Live Rates vs TZS", liveRatesSubtitle = "Tap any row for a quick conversion",
    eastAfricaTitle = "East African Currencies", eastAfricaSubtitle = "Regional currencies vs Tanzanian Shilling",
    metalsTitle = "Precious Metals", metalsSubtitle = "Gold & Silver · priced per troy ounce",
    botTitle = "BoT Official Rates", botSubtitle = "Bank of Tanzania · bot.go.tz", perTroyOz = "per troy oz",

    converterTitle = "Currency Converter", converterSubtitle = "Live rates · tap swap to reverse",
    amount = "Amount", from = "From", to = "To", swap = "Swap", pinPair = "Pin Pair",
    multiCurrencyTitle = "Multi-Currency Results", multiCurrencySubtitle = "Same amount in all major currencies",
    quickAmountsTitle = "Quick Amounts", quickAmountsSubtitle = "Tap to fill the amount field",
    historyTitle = "Recent Conversions", historySubtitle = "Last 30 conversions saved",
    noConversions = "No conversions yet. Tap Save to begin.",

    watchlistTitle = "My Watchlist", watchlistSubtitle = "Tap the star on any currency to pin it here",
    watchlistEmpty = "Your watchlist is empty.\nTap the star on any currency to pin it here.",
    majorTitle = "Major Currencies", majorSubtitle = "Top global currencies vs TZS",
    alertsTitle = "Price Alerts", alertsSubtitle = "Notify when rates cross your targets",
    addAlert = "Add New Alert", noAlerts = "No alerts yet. Tap Add New Alert.",
    newAlertTitle = "New Price Alert", newAlertSubtitle = "Notify me when a rate crosses my target",
    risesAbove = "Rises above", fallsBelow = "Falls below", createAlert = "Create Alert",

    keypad = "Keypad", split = "Split", splitTitle = "Split Calculator",
    splitSubtitle = "Divide a TZS amount evenly across currencies",
    splitBetween = "Split between", calcHistoryTitle = "Calculator History",
    rateRefTitle = "Quick Rate Reference", rateRefSubtitle = "Common conversions at a glance",

    analyticsTitle = "Rate Analytics", analyticsSubtitle = "Trends from your local rate history",
    statHigh = "High", statLow = "Low", statAvg = "Average", statChange = "Change",
    topMoversTitle = "Top Movers", topMoversSubtitle = "Biggest changes since first tracked point",
    notEnoughData = "Not enough history yet — keep the app open a while to build a trend.",

    autoRefreshTitle = "Auto-Refresh", autoRefreshSubtitle = "Keep rates up to date automatically",
    refreshIntervalTitle = "Refresh Interval", refreshNow = "Refresh Now",
    appearanceTitle = "Appearance", appearanceSubtitle = "Choose your preferred theme",
    languageTitle = "Language", languageSubtitle = "Choose your preferred language",
    dynamicColorTitle = "Dynamic Color (Material You)", dynamicColorSubtitle = "Use wallpaper-based colors on Android 12+",
    displayTitle = "Display Options", displaySubtitle = "Customize how rates are shown",
    compactTitle = "Compact Mode", compactSubtitle = "Show more rates with less spacing",
    notificationsTitle = "Notifications", notificationsSubtitle = "Rate alerts and refresh notices",
    notifyUpdatesTitle = "Rate Update Notifications", notifyUpdatesSubtitle = "Show a notification on each refresh",
    dataManagementTitle = "Data Management",
    exportRates = "Export Rates as Text", clearHistory = "Clear Conversion History",
    clearWatchlist = "Clear Watchlist", clearAlerts = "Clear All Alerts",
    resetAll = "Reset All App Data", replayOnboarding = "Replay Welcome Tour",
    aboutTitle = "About",

    onboardTitle1 = "Welcome to FXetcher", onboardBody1 = "Live exchange rates for the Tanzanian Shilling, updated automatically.",
    onboardTitle2 = "Convert Instantly", onboardBody2 = "Convert between 25+ currencies and precious metals in real time.",
    onboardTitle3 = "Track & Get Alerted", onboardBody3 = "Pin favorites to your watchlist and set price alerts for the moments that matter.",
    onboardTitle4 = "See the Trends", onboardBody4 = "Analytics turns your rate history into charts, so you can spot the moves.",
    onboardSkip = "Skip", onboardNext = "Next", onboardStart = "Get Started",

    widgetTitle = "FXetcher Rates",

    searchHint = "Search currencies…", searchNoResults = "No currencies found",
    undo = "Undo", deleted = "Deleted",
    offlineBanner = "You're offline — showing cached rates",
    highContrastTitle = "High Contrast Text", highContrastSubtitle = "Increase text contrast for readability",
    heroChangeToday = "since app opened",
    shortcutConvert = "Convert", shortcutMarkets = "Markets", shortcutCalc = "Calculator"
)

val SwahiliStrings = Strings(
    navHome = "Nyumbani", navConvert = "Badilisha", navMarkets = "Masoko",
    navCalc = "Kikokotoo", navAnalytics = "Uchambuzi", navSettings = "Mipangilio",

    save = "Hifadhi", cancel = "Ghairi", clear = "Futa", delete = "Ondoa",
    share = "Shiriki", copy = "Nakili", add = "Ongeza", loading = "Inapakia…",
    live = "moja kwa moja", today = "Leo", current = "Sasa", target = "Lengo",

    liveRatesTitle = "Viwango vya Moja kwa Moja dhidi ya TZS", liveRatesSubtitle = "Gusa mstari wowote kubadilisha haraka",
    eastAfricaTitle = "Sarafu za Afrika Mashariki", eastAfricaSubtitle = "Sarafu za kikanda dhidi ya Shilingi ya Tanzania",
    metalsTitle = "Madini ya Thamani", metalsSubtitle = "Dhahabu na Fedha · bei kwa aunzi",
    botTitle = "Viwango Rasmi vya BoT", botSubtitle = "Benki Kuu ya Tanzania · bot.go.tz", perTroyOz = "kwa aunzi",

    converterTitle = "Kibadilisha Sarafu", converterSubtitle = "Viwango vya moja kwa moja · gusa kubadilishana",
    amount = "Kiasi", from = "Kutoka", to = "Kwenda", swap = "Badilishana", pinPair = "Bandika Jozi",
    multiCurrencyTitle = "Matokeo ya Sarafu Nyingi", multiCurrencySubtitle = "Kiasi hicho hicho katika sarafu kuu zote",
    quickAmountsTitle = "Kiasi cha Haraka", quickAmountsSubtitle = "Gusa kujaza uwanja wa kiasi",
    historyTitle = "Ubadilishaji wa Hivi Karibuni", historySubtitle = "Ubadilishaji 30 wa mwisho uliohifadhiwa",
    noConversions = "Hakuna ubadilishaji bado. Gusa Hifadhi kuanza.",

    watchlistTitle = "Orodha Yangu", watchlistSubtitle = "Gusa nyota kwenye sarafu yoyote kuibandika hapa",
    watchlistEmpty = "Orodha yako ni tupu.\nGusa nyota kwenye sarafu yoyote kuibandika hapa.",
    majorTitle = "Sarafu Kuu", majorSubtitle = "Sarafu kuu za dunia dhidi ya TZS",
    alertsTitle = "Arifa za Bei", alertsSubtitle = "Pata arifa viwango vinapofikia lengo lako",
    addAlert = "Ongeza Arifa Mpya", noAlerts = "Hakuna arifa bado. Gusa Ongeza Arifa Mpya.",
    newAlertTitle = "Arifa Mpya ya Bei", newAlertSubtitle = "Nijulishe kiwango kinapofikia lengo langu",
    risesAbove = "Kinapopanda juu ya", fallsBelow = "Kinaposhuka chini ya", createAlert = "Unda Arifa",

    keypad = "Kibodi", split = "Gawanya", splitTitle = "Kikokotoo cha Kugawanya",
    splitSubtitle = "Gawanya kiasi cha TZS sawasawa kati ya sarafu",
    splitBetween = "Gawanya kati ya", calcHistoryTitle = "Historia ya Kikokotoo",
    rateRefTitle = "Rejea ya Haraka ya Viwango", rateRefSubtitle = "Ubadilishaji wa kawaida kwa muhtasari",

    analyticsTitle = "Uchambuzi wa Viwango", analyticsSubtitle = "Mienendo kutoka historia yako ya viwango",
    statHigh = "Juu Zaidi", statLow = "Chini Zaidi", statAvg = "Wastani", statChange = "Mabadiliko",
    topMoversTitle = "Mabadiliko Makubwa", topMoversSubtitle = "Mabadiliko makubwa zaidi tangu ufuatiliaji wa kwanza",
    notEnoughData = "Historia bado haitoshi — acha programu wazi kwa muda ili kujenga mwenendo.",

    autoRefreshTitle = "Usasishaji Kiotomatiki", autoRefreshSubtitle = "Weka viwango sasa kiotomatiki",
    refreshIntervalTitle = "Muda wa Usasishaji", refreshNow = "Sasisha Sasa",
    appearanceTitle = "Muonekano", appearanceSubtitle = "Chagua mandhari unayopendelea",
    languageTitle = "Lugha", languageSubtitle = "Chagua lugha unayopendelea",
    dynamicColorTitle = "Rangi Zinazobadilika (Material You)", dynamicColorSubtitle = "Tumia rangi za kutoka picha ya nyuma kwenye Android 12+",
    displayTitle = "Chaguo za Onyesho", displaySubtitle = "Badilisha jinsi viwango vinavyoonyeshwa",
    compactTitle = "Hali Fupi", compactSubtitle = "Onyesha viwango vingi kwa nafasi kidogo",
    notificationsTitle = "Arifa", notificationsSubtitle = "Arifa za viwango na usasishaji",
    notifyUpdatesTitle = "Arifa za Usasishaji wa Viwango", notifyUpdatesSubtitle = "Onyesha arifa kila usasishaji unapofanyika",
    dataManagementTitle = "Usimamizi wa Data",
    exportRates = "Hamisha Viwango kama Maandishi", clearHistory = "Futa Historia ya Ubadilishaji",
    clearWatchlist = "Futa Orodha", clearAlerts = "Futa Arifa Zote",
    resetAll = "Weka Upya Data Yote ya Programu", replayOnboarding = "Rudia Utangulizi",
    aboutTitle = "Kuhusu",

    onboardTitle1 = "Karibu FXetcher", onboardBody1 = "Viwango vya ubadilishaji vya Shilingi ya Tanzania, vinavyosasishwa kiotomatiki.",
    onboardTitle2 = "Badilisha Papo Hapo", onboardBody2 = "Badilisha kati ya sarafu 25+ na madini ya thamani kwa wakati halisi.",
    onboardTitle3 = "Fuatilia na Pata Arifa", onboardBody3 = "Bandika unazozipenda kwenye orodha yako na weka arifa za bei kwa nyakati muhimu.",
    onboardTitle4 = "Ona Mienendo", onboardBody4 = "Uchambuzi hubadilisha historia yako ya viwango kuwa michoro, ili uone mabadiliko.",
    onboardSkip = "Ruka", onboardNext = "Endelea", onboardStart = "Anza",

    widgetTitle = "Viwango vya FXetcher",

    searchHint = "Tafuta sarafu…", searchNoResults = "Hakuna sarafu iliyopatikana",
    undo = "Tendua", deleted = "Imefutwa",
    offlineBanner = "Uko nje ya mtandao — inaonyesha viwango vilivyohifadhiwa",
    highContrastTitle = "Maandishi Yenye Utofautishaji wa Juu", highContrastSubtitle = "Ongeza utofautishaji wa maandishi kwa usomaji bora",
    heroChangeToday = "tangu programu ilipofunguliwa",
    shortcutConvert = "Badilisha", shortcutMarkets = "Masoko", shortcutCalc = "Kikokotoo"
)

val LocalStrings = staticCompositionLocalOf { EnglishStrings }
val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

@Composable
fun ProvideStrings(language: AppLanguage, content: @Composable () -> Unit) {
    val strings = if (language == AppLanguage.SWAHILI) SwahiliStrings else EnglishStrings
    CompositionLocalProvider(LocalStrings provides strings, LocalAppLanguage provides language, content = content)
}

package com.willykez.fxetcher.data

/**
 * Single source of truth for currency metadata.
 */
data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val isMetal: Boolean = false
)

object CurrencyMeta {

    val ALL: List<Currency> = listOf(
        Currency("USD", "US Dollar", "$", "🇺🇸"),
        Currency("EUR", "Euro", "€", "🇪🇺"),
        Currency("GBP", "British Pound", "£", "🇬🇧"),
        Currency("JPY", "Japanese Yen", "¥", "🇯🇵"),
        Currency("CNY", "Chinese Yuan", "¥", "🇨🇳"),
        Currency("INR", "Indian Rupee", "₹", "🇮🇳"),
        Currency("AED", "UAE Dirham", "د.إ", "🇦🇪"),
        Currency("ZAR", "S.A. Rand", "R", "🇿🇦"),
        Currency("KES", "Kenyan Shilling", "KSh", "🇰🇪"),
        Currency("TZS", "Tanzanian Shilling", "TSh", "🇹🇿"),
        Currency("UGX", "Ugandan Shilling", "USh", "🇺🇬"),
        Currency("RWF", "Rwandan Franc", "RF", "🇷🇼"),
        Currency("XAU", "Gold (troy oz)", "🥇", "💰", isMetal = true),
        Currency("XAG", "Silver (troy oz)", "🥈", "💎", isMetal = true),
        Currency("CAD", "Canadian Dollar", "C$", "🇨🇦"),
        Currency("CHF", "Swiss Franc", "Fr", "🇨🇭"),
        Currency("SGD", "Singapore Dollar", "S$", "🇸🇬"),
        Currency("MYR", "Malaysian Ringgit", "RM", "🇲🇾"),
        Currency("SAR", "Saudi Riyal", "ر.س", "🇸🇦"),
        Currency("QAR", "Qatari Riyal", "ر.ق", "🇶🇦"),
        Currency("BRL", "Brazilian Real", "R$", "🇧🇷"),
        Currency("MXN", "Mexican Peso", "$", "🇲🇽"),
        Currency("NGN", "Nigerian Naira", "₦", "🇳🇬"),
        Currency("EGP", "Egyptian Pound", "E£", "🇪🇬"),
        Currency("ETB", "Ethiopian Birr", "Br", "🇪🇹")
    )

    val CODES: List<String> = ALL.map { it.code }
    val BY_CODE: Map<String, Currency> = ALL.associateBy { it.code }

    val LIVE_TOP: List<String> = listOf("USD", "EUR", "GBP", "JPY", "CNY", "INR", "AED", "ZAR", "KES", "CAD", "CHF", "SGD")
    val EAST_AFRICA: List<String> = listOf("KES", "UGX", "RWF", "ZAR", "AED", "NGN", "EGP", "ETB")
    val MULTI_TARGETS: List<String> = listOf("USD", "EUR", "GBP", "KES", "UGX", "ZAR", "AED", "CNY", "INR", "CAD", "CHF", "SAR", "NGN")
    val CALC_QUICK: List<String> = listOf("USD", "EUR", "GBP", "KES", "AED", "ZAR", "JPY", "CNY", "XAU", "XAG")
    val REF_CODES: List<String> = listOf("USD", "EUR", "GBP", "KES", "AED", "ZAR", "JPY", "XAU")

    fun isSmallRate(code: String): Boolean = code in setOf("UGX", "RWF", "NGN")

    fun of(code: String): Currency = BY_CODE[code] ?: Currency(code, code, code, "💱")

    fun indexOf(code: String): Int = CODES.indexOf(code).coerceAtLeast(0)

    /** Deterministic accent color index by position, cycled across a palette in the UI layer. */
    fun accentIndex(index: Int): Int = index % 7
}

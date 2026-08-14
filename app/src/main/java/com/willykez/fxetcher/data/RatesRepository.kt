package com.willykez.fxetcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Handles all outbound network calls: live FX rates, precious metals spot price,
 * and the Bank of Tanzania official rate table (scraped from bot.go.tz).
 */
class RatesRepository {

    companion object {
        private const val FX_URL = "https://v6.exchangerate-api.com/v6/56bff02e7e890d6fae47bb57/latest/USD"
        private const val MTL_URL = "https://api.metalpriceapi.com/v1/latest?api_key=28b227b94a7053b0c52456cd3f453c09&base=USD&currencies=XAU,XAG"
        const val BOT_URL = "https://www.bot.go.tz/ExchangeRate/excRates"

        private val FALLBACK: Map<String, Double> = mapOf(
            "TZS" to 1.0, "USD" to 2_600.0, "EUR" to 2_820.0, "GBP" to 3_310.0,
            "JPY" to 17.3, "CNY" to 360.0, "INR" to 31.2, "AED" to 708.0,
            "ZAR" to 142.0, "KES" to 20.1, "UGX" to 0.70, "RWF" to 1.90,
            "XAU" to 8_450_000.0, "XAG" to 92_000.0, "CAD" to 1_910.0,
            "CHF" to 2_900.0, "SGD" to 1_960.0, "MYR" to 580.0, "SAR" to 693.0,
            "QAR" to 714.0, "BRL" to 470.0, "MXN" to 130.0, "NGN" to 1.65,
            "EGP" to 53.0, "ETB" to 45.0
        )

        fun fallbackRates(): Map<String, Double> = FALLBACK
    }

    sealed class FetchResult {
        data class Success(val rates: Map<String, Double>) : FetchResult()
        data class Failure(val message: String) : FetchResult()
    }

    /** Fetches USD-based FX rates, re-based to TZS, plus gold/silver spot prices. */
    suspend fun fetchRates(): FetchResult = withContext(Dispatchers.IO) {
        try {
            val fx = JSONObject(httpGet(FX_URL))
            if (fx.optString("result") != "success") {
                return@withContext FetchResult.Failure(fx.optString("error-type", "API error"))
            }
            val conv = fx.getJSONObject("conversion_rates")
            val usdInTzs = conv.getDouble("TZS")

            val rates = LinkedHashMap<String, Double>()
            rates["TZS"] = 1.0
            rates["USD"] = usdInTzs
            for (code in CurrencyMeta.CODES) {
                if (code == "TZS" || code == "USD" || code == "XAU" || code == "XAG") continue
                if (conv.has(code)) {
                    val x = conv.getDouble(code)
                    if (x > 0) rates[code] = usdInTzs / x
                }
            }

            try {
                val mj = JSONObject(httpGet(MTL_URL))
                if (mj.optBoolean("success")) {
                    val mr = mj.getJSONObject("rates")
                    if (mr.has("XAU")) { val v = mr.getDouble("XAU"); if (v > 0) rates["XAU"] = (1.0 / v) * usdInTzs }
                    if (mr.has("XAG")) { val v = mr.getDouble("XAG"); if (v > 0) rates["XAG"] = (1.0 / v) * usdInTzs }
                } else {
                    rates["XAU"] = usdInTzs * 3_250.0
                    rates["XAG"] = usdInTzs * 35.0
                }
            } catch (e: Exception) {
                rates["XAU"] = usdInTzs * 3_250.0
                rates["XAG"] = usdInTzs * 35.0
            }

            FetchResult.Success(rates)
        } catch (e: Exception) {
            FetchResult.Failure(e.message ?: "Network error")
        }
    }

    data class BotFetchResult(val rates: List<BotRate>, val status: String)

    /** Scrapes the official Bank of Tanzania exchange-rate table. */
    suspend fun fetchBotRates(): BotFetchResult = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(BOT_URL)
                .userAgent("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .timeout(20_000)
                .get()
            var rows = doc.select("table tbody tr")
            if (rows.isEmpty()) rows = doc.select("tr")

            val scraped = mutableListOf<BotRate>()
            for (row in rows) {
                val cols = row.select("td")
                if (cols.size < 4) continue
                val code = cols[1].text().trim().uppercase(Locale.US)
                val buying = cols[2].text().trim()
                val selling = cols[3].text().trim()
                if (!code.matches(Regex("[A-Z]{3}")) || buying.isEmpty()) continue
                scraped.add(BotRate(code, buying, selling))
            }
            val status = if (scraped.isEmpty())
                "⚠️ No rates found (page layout may have changed)"
            else
                "✓ ${scraped.size} currencies · live from bot.go.tz"
            BotFetchResult(scraped, status)
        } catch (e: Exception) {
            BotFetchResult(emptyList(), "⚠️ Offline — showing cached official rates")
        }
    }

    private fun httpGet(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) sb.append(line)
                return sb.toString()
            }
        } finally {
            conn.disconnect()
        }
    }
}

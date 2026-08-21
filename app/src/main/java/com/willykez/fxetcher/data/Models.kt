package com.willykez.fxetcher.data

import org.json.JSONArray
import org.json.JSONObject

data class BotRate(val currency: String, val buying: String, val selling: String)

data class PriceAlert(
    val currency: String,
    val target: Double,
    /** 0 = rises above target, 1 = falls below target */
    val condition: Int,
    val enabled: Boolean = true
)

data class PortfolioHolding(val currency: String, val amount: Double, val addedAt: Long, val rateAtAdd: Double)

data class CalcRecord(val text: String, val timestamp: Long)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentTheme { GOLD, OCEAN, EMERALD, SUNSET }

enum class NavBarStyle { CLASSIC, LIQUID_GLASS }

enum class HomeSort { DEFAULT, ALPHA, VALUE }

/** A single point-in-time snapshot of a rate, used to draw sparkline trend charts. */
data class RatePoint(val value: Double, val timestamp: Long)

// ── Lightweight JSON (de)serialization helpers, kept dependency-free ─────────

fun List<BotRate>.toBotJson(): String {
    val arr = JSONArray()
    forEach {
        arr.put(JSONObject().apply {
            put("c", it.currency); put("b", it.buying); put("s", it.selling)
        })
    }
    return arr.toString()
}

fun parseBotRates(raw: String): List<BotRate> = runCatching {
    val arr = JSONArray(raw)
    (0 until arr.length()).map {
        val o = arr.getJSONObject(it)
        BotRate(o.getString("c"), o.getString("b"), o.getString("s"))
    }
}.getOrDefault(emptyList())

fun List<PriceAlert>.toAlertJson(): String {
    val arr = JSONArray()
    forEach {
        arr.put(JSONObject().apply {
            put("currency", it.currency); put("target", it.target)
            put("cond", it.condition); put("enabled", it.enabled)
        })
    }
    return arr.toString()
}

fun parseAlerts(raw: String): List<PriceAlert> = runCatching {
    val arr = JSONArray(raw)
    (0 until arr.length()).map {
        val o = arr.getJSONObject(it)
        PriceAlert(
            o.getString("currency"), o.getDouble("target"),
            o.getInt("cond"), o.optBoolean("enabled", true)
        )
    }
}.getOrDefault(emptyList())

fun List<PortfolioHolding>.toPortfolioJson(): String {
    val arr = JSONArray()
    forEach {
        arr.put(JSONObject().apply {
            put("currency", it.currency); put("amount", it.amount)
            put("addedAt", it.addedAt); put("rateAtAdd", it.rateAtAdd)
        })
    }
    return arr.toString()
}

fun parsePortfolio(raw: String): List<PortfolioHolding> = runCatching {
    val arr = JSONArray(raw)
    (0 until arr.length()).map {
        val o = arr.getJSONObject(it)
        PortfolioHolding(
            o.getString("currency"), o.getDouble("amount"),
            o.getLong("addedAt"), o.getDouble("rateAtAdd")
        )
    }
}.getOrDefault(emptyList())

fun List<String>.toJsonArray(): String {
    val arr = JSONArray(); forEach { arr.put(it) }; return arr.toString()
}

fun parseStringList(raw: String): List<String> = runCatching {
    val arr = JSONArray(raw)
    (0 until arr.length()).map { arr.getString(it) }
}.getOrDefault(emptyList())

fun Map<String, Double>.toJsonObject(): String {
    val o = JSONObject(); forEach { (k, v) -> o.put(k, v) }; return o.toString()
}

fun parseRatesMap(raw: String): Map<String, Double> = runCatching {
    val o = JSONObject(raw)
    val map = LinkedHashMap<String, Double>()
    o.keys().forEach { k -> map[k] = o.getDouble(k) }
    map
}.getOrDefault(emptyMap())

fun Map<String, List<RatePoint>>.toJsonHistory(): String {
    val root = JSONObject()
    forEach { (code, points) ->
        val arr = JSONArray()
        points.forEach { p -> arr.put(JSONObject().apply { put("v", p.value); put("t", p.timestamp) }) }
        root.put(code, arr)
    }
    return root.toString()
}

fun parseRateHistory(raw: String): Map<String, List<RatePoint>> = runCatching {
    val root = JSONObject(raw)
    val map = LinkedHashMap<String, List<RatePoint>>()
    root.keys().forEach { code ->
        val arr = root.getJSONArray(code)
        map[code] = (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            RatePoint(o.getDouble("v"), o.getLong("t"))
        }
    }
    map
}.getOrDefault(emptyMap())

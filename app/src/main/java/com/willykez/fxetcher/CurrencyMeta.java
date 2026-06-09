package com.willykez.fxetcher;

import java.util.HashMap;

/**
 * Single source of truth for currency metadata and SharedPreferences keys.
 * v4: expanded to 25 currencies + new prefs keys.
 */
public final class CurrencyMeta {

    public static final String[] CODES = {
        "USD","EUR","GBP","JPY","CNY","INR",
        "AED","ZAR","KES","TZS","UGX","RWF",
        "XAU","XAG",
        "CAD","CHF","SGD","MYR","SAR","QAR",
        "BRL","MXN","NGN","EGP","ETB"
    };

    public static final HashMap<String,String> NAMES   = new HashMap<>();
    public static final HashMap<String,String> SYMBOLS = new HashMap<>();
    public static final HashMap<String,String> FLAGS   = new HashMap<>();

    static {
        put("USD","US Dollar",           "$",    "🇺🇸");
        put("EUR","Euro",                "€",    "🇪🇺");
        put("GBP","British Pound",       "£",    "🇬🇧");
        put("JPY","Japanese Yen",        "¥",    "🇯🇵");
        put("CNY","Chinese Yuan",        "¥",    "🇨🇳");
        put("INR","Indian Rupee",        "₹",    "🇮🇳");
        put("AED","UAE Dirham",          "د.إ",  "🇦🇪");
        put("ZAR","S.A. Rand",           "R",    "🇿🇦");
        put("KES","Kenyan Shilling",     "KSh",  "🇰🇪");
        put("TZS","Tanzanian Shilling",  "TSh",  "🇹🇿");
        put("UGX","Ugandan Shilling",    "USh",  "🇺🇬");
        put("RWF","Rwandan Franc",       "RF",   "🇷🇼");
        put("XAU","Gold (oz)",           "🥇",   "💰");
        put("XAG","Silver (oz)",         "🥈",   "💎");
        put("CAD","Canadian Dollar",     "C$",   "🇨🇦");
        put("CHF","Swiss Franc",         "Fr",   "🇨🇭");
        put("SGD","Singapore Dollar",    "S$",   "🇸🇬");
        put("MYR","Malaysian Ringgit",   "RM",   "🇲🇾");
        put("SAR","Saudi Riyal",         "ر.س",  "🇸🇦");
        put("QAR","Qatari Riyal",        "ر.ق",  "🇶🇦");
        put("BRL","Brazilian Real",      "R$",   "🇧🇷");
        put("MXN","Mexican Peso",        "$",    "🇲🇽");
        put("NGN","Nigerian Naira",      "₦",    "🇳🇬");
        put("EGP","Egyptian Pound",      "E£",   "🇪🇬");
        put("ETB","Ethiopian Birr",      "Br",   "🇪🇹");
    }

    private static void put(String c,String n,String s,String f){
        NAMES.put(c,n); SYMBOLS.put(c,s); FLAGS.put(c,f);
    }

    public static boolean isSmallRate(String code){
        return "UGX".equals(code)||"RWF".equals(code)||"NGN".equals(code);
    }

    public static int indexOf(String code){
        for(int i=0;i<CODES.length;i++) if(CODES[i].equals(code)) return i;
        return 0;
    }

    private CurrencyMeta(){}
}

// ─────────────────────────────────────────────────────────────────────────────

final class AppPrefs {
    static final String FILE         = "FXetcherPrefs";
    static final String KEY_RATES    = "rates_v4";
    static final String KEY_UPDATE   = "last_update";
    static final String KEY_INTERVAL = "refresh_interval";
    static final String KEY_AUTO     = "auto_refresh";
    static final String KEY_NOTIFY   = "notify_updates";
    static final String KEY_CONVHIST = "conv_history";
    static final String KEY_ALERTS   = "price_alerts";
    static final String KEY_DARK     = "dark_theme";
    static final String KEY_WATCHLIST   = "watchlist_v4";
    static final String KEY_PINNED_FROM = "pinned_from";
    static final String KEY_PINNED_TO   = "pinned_to";
    static final String KEY_CALC_HIST   = "calc_history";
    static final String KEY_HOME_SORT   = "home_sort_mode";
    static final String KEY_COMPACT     = "compact_mode";
    private AppPrefs(){}
}

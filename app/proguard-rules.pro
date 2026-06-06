# TanzRate Pro – ProGuard rules

# Keep Activity entry point
-keep class com.willykez.tanzs.TanzaniaForexApp { *; }

# Keep Widget + Notification manager (used via XML / system callbacks)
-keep class com.willykez.tanzs.RateWidget { *; }
-keep class com.willykez.tanzs.AlertNotificationManager { *; }

# Keep Application class
-keep class com.willykez.tanzs.TanzRateApp { *; }

# Jsoup – keep for BoT scraping
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# AndroidX
-keep class androidx.** { *; }

# JSON
-keep class org.json.** { *; }

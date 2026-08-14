# Add project specific ProGuard rules here.

# Keep line numbers / file names so crash reports stay readable, and remap
# the source file attribute to the R8 mapping file convention.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata — several libraries (Compose, coroutines) read this via
# reflection at runtime; stripping it can break them silently.
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Jsoup (BoT rate-table scraping) — HTML parsing library, not Android-aware.
-dontwarn org.jsoup.**
-keep class org.jsoup.** { *; }

# org.json — on-device calls resolve to the Android framework's built-in
# implementation; this dependency only exists so the same code compiles for
# local JVM unit tests. Nothing here needs obfuscation protection, but avoid
# warnings about the framework classes it shadows.
-dontwarn org.json.**

# Coroutines — ship their own consumer rules, this just silences known
# reflective-lookup warnings from internal debug/agent hooks.
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# App data models are (de)serialized to/from JSON by hand using explicit
# string keys (see data/Models.kt) rather than reflection, so this isn't
# strictly required — kept anyway as a safety net against future changes.
-keep class com.willykez.fxetcher.data.** { *; }

# Notification / app entry points the system calls into directly.
-keep class com.willykez.fxetcher.FXetcherApplication { *; }
-keep class com.willykez.fxetcher.MainActivity { *; }

# WorkManager — ships its own consumer rules; nothing additional needed here.
-dontwarn androidx.work.**

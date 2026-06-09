<p align="center">
  <img src="https://img.shields.io/badge/Version-4.0.0-FFD700?style=for-the-badge&labelColor=0A0E27"/>
  <img src="https://img.shields.io/badge/Android-5.0%2B-4CAF50?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Language-Java-F44336?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/License-MIT-2196F3?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Ads-None-9C27B0?style=for-the-badge"/>
</p>

<h1 align="center">🇹🇿 TanzRate Pro — FXetcher v4</h1>
<p align="center"><strong>Real-time Tanzania Shilling Forex Tracker · Complete Ground-Up Rewrite</strong></p>
<p align="center">
  Live exchange rates · BoT official data · Price alert notifications · 5-tab modular UI · Precious metals · Forex calculator · Light & dark themes
</p>

---

> ⚠️ **v4.0.0 is a full architectural rewrite.** Package name, launcher icon, app structure, and all source files have changed. See the [Migration Notice](#-migration-notice-v23--v40) section before upgrading.

---

## 📦 What Changed vs v2.3.0

| | Change | v2.3.0 | v4.0.0 |
|---|---|---|---|
| 📦 | **Package name** | `com.willykez.tanzsx` | `com.willykez.fxetcher` |
| 🏷️ | **App name** | `TanzRate Pro` | `Fxetcher` |
| 🎨 | **Launcher icon** | Adaptive XML (mipmap set) | Custom adaptive icon with new foreground + PNG in `mipmap-xhdpi` |
| 🏗️ | **Architecture** | Single monolithic `TanzaniaForexApp.java` | Modular multi-file: `FXetcherApp` + `HomeScreen` + `Screens` + `CalcScreen` + `UiKit` + `Tokens` |
| 🗂️ | **Source layout** | `com.willykez.tanzsx.*` | `com.willykez.fxetcher.*` |
| 📱 | **Widget** | `RateWidget.java` (AppWidgetProvider) | **Removed** — no home-screen widget in v4 |
| 🎨 | **Theme engine** | `Theme` inner class | `Tokens.java` — Material3-mirrored design token system |
| 📐 | **UI construction** | Mix of XML + Java | Fully code-driven via `UiKit.java` (no XML activities) |
| 🔢 | **Currencies** | 14 | **25** (see full list below) |
| 🧮 | **Calculator** | None | `CalcScreen` — full forex keypad + history |
| ⭐ | **Watchlist** | Pinned conversions strip | Long-press any tile → Watchlist card in Markets tab |
| 🔔 | **Notifications** | 3 channels (alerts / updates / widget) | 2 channels: `price_alerts` (HIGH) · `rate_updates` (LOW) |
| 🌀 | **Loading state** | Pulsing skeleton cards | `ShimmerFrameLayout` via `com.facebook.shimmer:shimmer:0.5.0` |
| 🔄 | **Pull-to-refresh** | Manual refresh button | `SwipeRefreshLayout` (tri-color: gold, blue, green) |
| 📊 | **targetSdkVersion** | 35 | **36** |

---

## ✨ What's New in v4.0.0

| | Feature | Detail |
|---|---|---|
| 🏗️ | **Full Modular Architecture** | App split across `FXetcherApp`, `HomeScreen`, `Screens` (Convert / Markets / Settings), `CalcScreen`, `UiKit`, `Tokens`, `CurrencyMeta`, `AlertNotificationManager` |
| 🎨 | **Design Token System** | `Tokens.java` — every colour, spacing, radius, and typography constant in one file; two themes (dark default, light) swapped via `Tokens.apply(isDark)` |
| 🖌️ | **UiKit Component Library** | `UiKit.java` — reusable `card()`, `tv()`, `divider()`, `topPill()`, `sectionHeader()`, `rippleOver()`, `scaleAnim()`, `snack()`, and more — all programmatic, zero XML activities |
| 💱 | **25-Currency Coverage** | Expanded from 14 → 25: USD EUR GBP JPY CNY INR AED ZAR KES TZS UGX RWF + precious metals XAU/XAG + CAD CHF SGD MYR SAR QAR BRL MXN NGN EGP ETB |
| 🌍 | **Africa Tab** | Dedicated East/Central African currency section on the Home screen (KES, UGX, RWF, ZAR, AED, NGN, EGP, ETB) |
| 💰 | **Precious Metals Extended** | Gold & Silver shown per troy ounce, gram, and kilogram with tap-to-drill metal bottom sheet |
| 🧮 | **Forex Calculator (`CalcScreen`)** | Dedicated calculator tab — numeric keypad, currency chip selector, swap direction, live rate card, last-20-calculations history with clear |
| ⭐ | **Watchlist (Markets Tab)** | Long-press any major currency tile to pin it; persistent across sessions via `SharedPreferences` |
| 🔔 | **Price Alert System** | Set above/below thresholds per currency; alerts fire as HIGH-priority notifications with colour-coded icons |
| 🏦 | **BoT Official Rates (Home)** | Live buy/sell table scraped from `bot.go.tz` using Jsoup; cached, pull-to-refresh, timestamped |
| 🌀 | **Shimmer Loading Skeleton** | `ShimmerFrameLayout` placeholder rows shown while first fetch completes |
| 🔄 | **Pull-to-Refresh** | `SwipeRefreshLayout` on Home refreshes both live rates and BoT table simultaneously |
| ⚡ | **Quick Convert Bottom Sheet** | Tap any top-bar pill (USD · EUR · XAU) to open an inline converter without leaving the current screen |
| 🥇 | **Metal Detail Bottom Sheet** | Tap the Gold pill or any metals row for a full oz/gram/kg breakdown with accent-coloured card |
| ⏱️ | **Auto-Refresh** | Toggle in top bar; configurable 1–60 min interval; `ScheduledExecutorService` with `ConcurrentHashMap` for thread-safe rate storage |
| 🌗 | **Dark / Light Theme** | Full dual-theme support; preference persisted; all colours from `Tokens` — no hardcoded values in UI code |
| 📋 | **Calculator History** | Last 20 forex calculations stored as JSON in `SharedPreferences`; clearable |
| 📎 | **Copy to Clipboard** | Tap copy icon on converter result or calculator result |
| 📡 | **Dual Data Sources** | Primary: ExchangeRate-API (forex) + MetalPriceAPI (metals); secondary: Bank of Tanzania live scrape |

---

## 🗂️ File Structure

```
TanzRate-Pro/
└── app/src/main/java/com/willykez/fxetcher/
    ├── FXetcherApp.java            # Root Activity — top bar, nav bar, tab switching, fetch orchestration
    ├── HomeScreen.java             # Home tab — shimmer, live rates grid, BoT table, metals, pull-to-refresh
    ├── Screens.java                # ConvertScreen · MarketsScreen · SettingsScreen (all in one file)
    ├── CalcScreen.java             # Forex Calculator tab — keypad, currency chips, history
    ├── AlertNotificationManager.java # Notification channels, rate-update + price-alert builders
    ├── CurrencyMeta.java           # Single source of truth: 25 currency codes, names, symbols, flags
    ├── Tokens.java                 # Design token system: colours, spacing, typography (dark + light)
    ├── UiKit.java                  # Programmatic UI component library (card, tv, pill, anim, etc.)
    ├── FileUtil.java               # File I/O helpers
    ├── SketchwareUtil.java         # Utility helpers
    ├── MainActivity.java           # Thin launcher → delegates to FXetcherApp
    └── willykez.java               # Application subclass (app-level init)
└── app/src/main/res/
    ├── drawable/
    │   ├── ic_launcher.xml             # Adaptive icon definition
    │   ├── ic_launcher_foreground.xml  # Vector foreground layer
    │   └── ic_launcher_background.xml  # Vector background layer
    ├── mipmap-xhdpi/
    │   └── ic_launcher.png             # Raster launcher icon (xhdpi)
    ├── layout/
    │   ├── main.xml                    # Root FrameLayout for activity
    │   └── shimmer_item.xml            # Shimmer placeholder row layout
    └── values/
        ├── strings.xml                 # App name: "Fxetcher"
        ├── colors.xml
        └── styles.xml
```

---

## 🚀 Features

### 🏠 Home Tab
- Pull-to-refresh (`SwipeRefreshLayout`) for both live rates + BoT data
- **Shimmer loading skeleton** on first launch
- Live rate rows with animated ▲/▼ change indicators (colour flash)
- **East Africa section** — regional currencies at a glance
- **Precious Metals card** — Gold & Silver per oz, gram, kg
- **Bank of Tanzania official rates** — scraped live, cached, timestamped

### 💱 Convert Tab
- Live real-time calculation as you type
- 25 currencies, both directions, inverse rate
- One-tap swap with rotation animation
- Quick-amount chips (10 · 50 · 100 · 500 · 1K · 5K · 10K · 50K)
- Conversion history (last 20), clearable
- Copy result to clipboard

### 📊 Markets Tab
- Watchlist — long-press any tile to pin/unpin; persisted
- Major currencies grid — flag tiles, live rate, trend arrow
- Africa card — EAC + West/North Africa rates
- Precious metals extended card
- **Price Alert system** — set above/below targets; fires HIGH-priority notification

### 🧮 Calc Tab
- Forex-specific numeric keypad
- Currency chip selector (scrollable)
- Swap direction (to/from TZS)
- Live exchange rate reference card
- Last 20 calculations stored in `SharedPreferences`; clearable

### ⚙️ Settings Tab
- Auto-refresh toggle + interval picker (1 – 60 min)
- Dark / Light theme toggle
- Notification preferences
- Data source info (API keys + BoT)
- Clear cached rates

---

## ⚙️ Setup

### Requirements
- **Android Studio** or **Sketchware Pro**
- **minSdkVersion 21** (Android 5.0 Lollipop)
- **targetSdkVersion 36**
- Java source compatibility

### Dependencies

```gradle
implementation 'androidx.appcompat:appcompat:1.7.1'
implementation 'com.google.android.material:material:1.12.0'
implementation 'com.facebook.shimmer:shimmer:0.5.0'
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
implementation 'org.jsoup:jsoup:1.17.2'       // BoT live scraping
```

### AndroidManifest permissions

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

---

## 🔑 API Keys

> ⚠️ Move these to `BuildConfig` or a secrets manager before any public release.

| Service | Endpoint | Used for | Free tier |
|---|---|---|---|
| [ExchangeRate-API](https://exchangerate-api.com) | `v6.exchangerate-api.com/v6/.../latest/USD` | 25-currency forex rates | 1,500 req/month |
| [MetalPriceAPI](https://metalpriceapi.com) | `api.metalpriceapi.com/v1/latest` | Gold (XAU) & Silver (XAG) | 100 req/month |
| [Bank of Tanzania](https://www.bot.go.tz/ExchangeRate/excRates) | `bot.go.tz/ExchangeRate/excRates` | Official buy/sell TZS rates | Open web scrape |

---

## 🔄 Migration Notice — v2.3.0 → v4.0.0

| Step | Action |
|---|---|
| 1 | **Uninstall** the old `com.willykez.tanzsx` build before installing v4 — package name changed, they will not conflict but will coexist as separate apps |
| 2 | **No `RateWidget`** in v4 — remove the old widget from your home screen before upgrading |
| 3 | **SharedPreferences** from v2.x are under a different package and will not carry over automatically |
| 4 | Update your `build.gradle` `applicationId` to `com.willykez.fxetcher` |
| 5 | Remove any references to `com.willykez.tanzsx` package in manifests or deep-link URIs |

---

## 📋 Changelog

### v4.0.0 — FXetcher · Complete Rewrite *(current)*
- `BREAKING` Package renamed: `com.willykez.tanzsx` → `com.willykez.fxetcher`
- `BREAKING` App name changed: `TanzRate Pro` → `Fxetcher`
- `BREAKING` Launcher icon replaced with new adaptive icon + PNG asset
- `BREAKING` Home screen widget (`RateWidget`) removed
- `NEW` Fully modular architecture: 12 source files vs 1 monolith
- `NEW` `Tokens.java` design token system (Material3-mirrored naming)
- `NEW` `UiKit.java` programmatic component library
- `NEW` `CalcScreen` — dedicated forex calculator tab with history
- `NEW` 25-currency support (was 14); added CAD, CHF, SGD, MYR, SAR, QAR, BRL, MXN, NGN, EGP, ETB
- `NEW` Africa regional currency card (Home + Markets)
- `NEW` Watchlist — long-press to pin any currency; persisted
- `NEW` Shimmer loading skeleton via `facebook/shimmer`
- `NEW` SwipeRefreshLayout pull-to-refresh on Home
- `NEW` Quick-convert bottom sheet from top bar pills
- `NEW` Metal detail bottom sheet (oz / gram / kg)
- `NEW` `targetSdkVersion` bumped to 36
- `CHANGE` Notification channels reduced from 3 to 2 (widget channel removed with widget)

### v2.3.0 — Smart, Themed & Widget-Ready
- Adaptive light/dark theme engine
- Home screen widget (`RateWidget.java`)
- Rich price-alert notifications
- Smart "For You" dashboard card
- Drag-reorder customisable layout
- Skeleton loading animation

### v2.2.0 — BoT Scraping & Notifications
- Live BoT rates via Jsoup
- `AlertNotificationManager` with channel setup

### v2.1.0 — Converter History & Quick Amounts
- Conversion history, quick-amount chips, clipboard

### v2.0.0 — Complete Redesign
- Bottom navigation, top bar pills, live converter

### v1.0.0 — Initial Release
- Single-activity forex tracker, 14 currencies

---

## 🔒 Privacy

- ✅ No personal data collected
- ✅ No account required
- ✅ No analytics or tracking SDKs
- ✅ All data stored locally (SharedPreferences)
- ✅ HTTPS for all API calls
- ✅ No ads · No subscriptions · Free

---

## 📄 License

```
MIT License — Copyright (c) 2026 Willykez

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software to use, copy, modify, merge, publish, distribute and/or sell
copies, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
```

---

## 📬 Contact

**Developer:** Willykez  
**Email:** willykez01@gmail.com  
**Package:** `com.willykez.fxetcher`  
**GitHub:** [@Willykez](https://github.com/Willykez)

---

<p align="center">
  Made with ❤️ in Tanzania 🇹🇿 · If this helped you, please ⭐ the repo!
</p>

package com.willykez.tanzsx;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TanzaniaForexApp extends Activity {

    // ═══════════════════════════════════════════════════════════════════
    //  INNER: CONSTANTS
    // ═══════════════════════════════════════════════════════════════════
    static final class AC {            // App Colors
        static final String GOLD   = "#FFD700";
        static final String GREEN  = "#4CAF50";
        static final String BLUE   = "#2196F3";
        static final String RED    = "#F44336";
        static final String ORANGE = "#FF9800";
        static final String PURPLE = "#9C27B0";
        static final String TEAL   = "#009688";
    }

    static final class AP {            // App Prefs keys
        static final String FILE           = "TanzSPrefs";
        static final String KEY_RATES      = "rates_v3";
        static final String KEY_UPDATE     = "last_update";
        static final String KEY_INTERVAL   = "refresh_interval";
        static final String KEY_AUTO       = "auto_refresh";
        static final String KEY_DARK       = "dark_theme";
        static final String KEY_NOTIFY     = "notify_updates";
        static final String KEY_CONVHIST   = "conv_history";
        static final String KEY_ALERTS     = "price_alerts";
        static final String KEY_PINNED     = "pinned_convs";
        static final String KEY_USAGE      = "usage_stats";
        static final String KEY_DASHBOARD  = "dashboard_order";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  INNER: CURRENCY METADATA
    // ═══════════════════════════════════════════════════════════════════
    static final class CM {
        static final String[] CODES = {
            "USD","EUR","GBP","JPY","CNY","INR","AED","ZAR","KES","TZS","UGX","RWF","XAU","XAG"
        };
        static final HashMap<String,String> NAMES   = new HashMap<>();
        static final HashMap<String,String> SYMBOLS = new HashMap<>();
        static final HashMap<String,String> FLAGS   = new HashMap<>();
        static {
            NAMES.put("USD","US Dollar");         SYMBOLS.put("USD","$");     FLAGS.put("USD","🇺🇸");
            NAMES.put("EUR","Euro");              SYMBOLS.put("EUR","€");     FLAGS.put("EUR","🇪🇺");
            NAMES.put("GBP","British Pound");     SYMBOLS.put("GBP","£");     FLAGS.put("GBP","🇬🇧");
            NAMES.put("JPY","Japanese Yen");      SYMBOLS.put("JPY","¥");     FLAGS.put("JPY","🇯🇵");
            NAMES.put("CNY","Chinese Yuan");      SYMBOLS.put("CNY","¥");     FLAGS.put("CNY","🇨🇳");
            NAMES.put("INR","Indian Rupee");      SYMBOLS.put("INR","₹");     FLAGS.put("INR","🇮🇳");
            NAMES.put("AED","UAE Dirham");        SYMBOLS.put("AED","د.إ");   FLAGS.put("AED","🇦🇪");
            NAMES.put("ZAR","S.A. Rand");         SYMBOLS.put("ZAR","R");     FLAGS.put("ZAR","🇿🇦");
            NAMES.put("KES","Kenyan Shilling");   SYMBOLS.put("KES","KSh");   FLAGS.put("KES","🇰🇪");
            NAMES.put("TZS","Tanzanian Shilling");SYMBOLS.put("TZS","TSh");   FLAGS.put("TZS","🇹🇿");
            NAMES.put("UGX","Ugandan Shilling");  SYMBOLS.put("UGX","USh");   FLAGS.put("UGX","🇺🇬");
            NAMES.put("RWF","Rwandan Franc");     SYMBOLS.put("RWF","RF");    FLAGS.put("RWF","🇷🇼");
            NAMES.put("XAU","Gold (oz)");         SYMBOLS.put("XAU","🥇");   FLAGS.put("XAU","💰");
            NAMES.put("XAG","Silver (oz)");       SYMBOLS.put("XAG","🥈");   FLAGS.put("XAG","💎");
        }
        static int indexOf(String code) {
            for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) return i;
            return 0;
        }
        static boolean isSmallRate(String code) {
            return code.equals("UGX") || code.equals("RWF");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  INNER: THEME
    // ═══════════════════════════════════════════════════════════════════
    static final class Theme {
        boolean isDark;
        String bg, card, cardElevated, card2, border, borderStrong, divider;
        String textPrimary, textSecondary, textHint;
        String navBg, statusBar, inputBg, inputBorder, chipBg;

        Theme(boolean dark) {
            isDark = dark;
            if (dark) {
                bg = "#0A0E27"; card = "#161B35"; cardElevated = "#1E2442"; card2 = "#131829";
                border = "#2A2F4A"; borderStrong = "#3A4060"; divider = "#252A42";
                textPrimary = "#FFFFFF"; textSecondary = "#B0B4C8"; textHint = "#6A6E85";
                navBg = "#0F1328"; statusBar = "#0A0E27"; inputBg = "#0D1230"; inputBorder = "#2A3050";
                chipBg = "#1E2442";
            } else {
                bg = "#F5F7FF"; card = "#FFFFFF"; cardElevated = "#FFFFFF"; card2 = "#F8FAFF";
                border = "#E0E4F0"; borderStrong = "#C5CBE0"; divider = "#EBEEF7";
                textPrimary = "#1A1C2E"; textSecondary = "#5C607A"; textHint = "#9EA3B8";
                navBg = "#FFFFFF"; statusBar = "#1A237E"; inputBg = "#F7F9FF"; inputBorder = "#D0D6E8";
                chipBg = "#F0F4FF";
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  INNER: BOT RATE ROW
    // ═══════════════════════════════════════════════════════════════════
    private static final class BotRate {
        final String currency, buying, selling;
        BotRate(String c, String b, String s) { currency = c; buying = b; selling = s; }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  INTERFACES
    // ═══════════════════════════════════════════════════════════════════
    interface OnToggle  { void onChanged(boolean v); }
    interface OnAction  { void run(); }
    interface OnTextChanged { void onChanged(String t); }

    // ═══════════════════════════════════════════════════════════════════
    //  TAG CONSTANTS (avoid getId() clashes)
    // ═══════════════════════════════════════════════════════════════════
    private static final int TAG_PRICE_TV  = 0x7A450001;
    private static final int TAG_DOT_ANIM  = 0x7A450002;
    private static final int TAG_PULSE     = 0x7A450003;

    // ═══════════════════════════════════════════════════════════════════
    //  API URLS
    // ═══════════════════════════════════════════════════════════════════
    private static final String FX_URL  = "https://v6.exchangerate-api.com/v6/56bff02e7e890d6fae47bb57/latest/USD";
    private static final String MTL_URL = "https://api.metalpriceapi.com/v1/latest?api_key=28b227b94a7053b0c52456cd3f453c09&base=USD&currencies=XAU,XAG";
    private static final String BOT_URL = "https://www.bot.go.tz/ExchangeRate/excRates";

    // ═══════════════════════════════════════════════════════════════════
    //  FIELDS
    // ═══════════════════════════════════════════════════════════════════
    private Theme T;
    private SharedPreferences prefs;

    private final ConcurrentHashMap<String,Double> rates     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,Double> prevRates = new ConcurrentHashMap<>();
    private final DecimalFormat fmtTzs = new DecimalFormat("#,##0.00");
    private final DecimalFormat fmtSml = new DecimalFormat("#,##0.0000");
    private final DecimalFormat fmtFull = new DecimalFormat("#,##0.00####");

    private final Handler              uiHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService exec  = Executors.newScheduledThreadPool(3);
    private ScheduledFuture<?>          autoJob;
    private volatile boolean            fetching    = false;
    private volatile boolean            botFetching = false;
    private boolean                     initialLoad = true;

    // Nav
    private static final int T_HOME = 0, T_CONV = 1, T_MKT = 2, T_SET = 3;
    private static final String[][] TABS = {{"🏠","Home"},{"💱","Convert"},{"📊","Markets"},{"⚙️","Settings"}};
    private int currentTab = -1;
    private LinearLayout   rootLayout, navBar;
    private final View[]         screens  = new View[4];
    private final LinearLayout[] navItems = new LinearLayout[4];
    private final TextView[]     navIcons = new TextView[4];
    private final TextView[]     navLbls  = new TextView[4];
    private final View[]         navBars  = new View[4];

    // Top bar
    private TextView topUsd, topEur, topGold, topUpdate;
    private View     topDot;

    // Home screen live refs
    private LinearLayout homeContent;
    private final LinkedHashMap<String,LinearLayout> dashCards = new LinkedHashMap<>();
    private final HashMap<String,TextView> rateValViews = new HashMap<>();
    private final HashMap<String,TextView> ratePctViews = new HashMap<>();
    private final HashMap<String,View>     rateRowViews = new HashMap<>();
    private TextView goldPriceTv, silverPriceTv;
    private final List<BotRate> botRates = new ArrayList<>();
    private LinearLayout bankTableBody;
    private TextView     bankStatusTv;
    private LinearLayout smartRecsContainer;

    // Convert screen refs
    private EditText     convInput;
    private Spinner      convFrom, convTo;
    private TextView     convResult, convRate, convInverse;
    private LinearLayout convHistContainer, pinnedContainer;
    private TextView     convHistEmpty, pinnedEmpty;

    // Markets refs
    private final HashMap<String,TextView> cellRateViews = new HashMap<>();
    private LinearLayout alertsList;
    private TextView     alertsEmpty;

    // ═══════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(AP.FILE, MODE_PRIVATE);
        T = new Theme(prefs.getBoolean(AP.KEY_DARK, true));

        try {
            getWindow().setStatusBarColor(Color.parseColor(T.statusBar));
            getWindow().setNavigationBarColor(Color.parseColor(T.navBg));
        } catch (Exception ignored) {}

        try { AlertNotificationManager.createChannels(this); } catch (Exception ignored) {}

        setFallbackRates();
        loadSavedRates();

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor(T.bg));
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(MATCH, MATCH));

        buildTopBar();
        rootLayout.addView(thinDiv());

        FrameLayout container = new FrameLayout(this);
        container.setLayoutParams(new LinearLayout.LayoutParams(MATCH, 0, 1f));
        screens[T_HOME] = buildHomeScreen();
        screens[T_CONV] = buildConvertScreen();
        screens[T_MKT]  = buildMarketsScreen();
        screens[T_SET]  = buildSettingsScreen();
        for (View s : screens) { s.setVisibility(View.GONE); container.addView(s); }
        rootLayout.addView(container);

        rootLayout.addView(thinDiv());
        buildNavBar();
        setContentView(rootLayout);

        int startTab = getIntent().getIntExtra("startTab", T_HOME);
        switchTab(startTab);
        fetchRates();
        scheduleAutoRefresh();
    }

    @Override protected void onPause()   { super.onPause();   saveRates(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
        if (autoJob != null) { autoJob.cancel(false); autoJob = null; }
        try { exec.shutdownNow(); } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TOP BAR
    // ═══════════════════════════════════════════════════════════════════
    private void buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackground(mkGradient(
            T.isDark ? new String[]{"#0D1230","#161B3D","#1A2045"}
                     : new String[]{"#1A237E","#283593","#3949AB"}));

        // Gold accent line at top
        View accentLine = new View(this);
        accentLine.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(3)));
        accentLine.setBackgroundColor(Color.parseColor(AC.GOLD));
        accentLine.setAlpha(0.55f);
        bar.addView(accentLine);

        // Row 1: branding + controls
        LinearLayout r1 = hRow();
        r1.setPadding(dp(16), dp(12), dp(12), dp(6));

        TextView appName = tv("🇹🇿  TanzRate", 17, "#FFFFFF", true);
        appName.setLayoutParams(wt(1f));
        r1.addView(appName);

        topDot = mkDot(AC.GREEN);
        LinearLayout.LayoutParams dotP = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotP.rightMargin = dp(6);
        topDot.setLayoutParams(dotP);
        r1.addView(topDot);

        topUpdate = tv("—", 10, "#BBBBBB", false);
        r1.addView(topUpdate);

        Button shareBtn = ghostBtn("📤");
        shareBtn.setTextColor(Color.parseColor(AC.BLUE));
        press(shareBtn); shareBtn.setOnClickListener(v -> shareRates());
        r1.addView(shareBtn);

        Button refreshBtn = ghostBtn("🔄");
        refreshBtn.setTextColor(Color.parseColor(AC.GOLD));
        press(refreshBtn);
        refreshBtn.setOnClickListener(v -> {
            if (fetching) return;
            ObjectAnimator.ofFloat(refreshBtn, "rotation", 0f, 360f).setDuration(800).start();
            fetchRates();
        });
        r1.addView(refreshBtn);
        bar.addView(r1);

        // Row 2: live rate pills
        LinearLayout r2 = hRow();
        r2.setPadding(dp(12), dp(2), dp(12), dp(12));
        topUsd  = topPill("1 USD = " + fmtTzs.format(safeRate("USD")) + " TZS", AC.GREEN);
        topEur  = topPill("1 EUR = " + fmtTzs.format(safeRate("EUR")) + " TZS", AC.GOLD);
        topGold = topPill("XAU " + goldShort(safeRate("XAU")), AC.ORANGE);
        r2.addView(topUsd);  r2.addView(pillGap());
        r2.addView(topEur);  r2.addView(pillGap());
        r2.addView(topGold);
        r2.addView(spacerFlex());
        bar.addView(r2);

        rootLayout.addView(bar);
    }

    private String goldShort(double v) {
        return String.format(Locale.US, "%.2fM TZS", v / 1_000_000.0);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NAV BAR
    // ═══════════════════════════════════════════════════════════════════
    private void buildNavBar() {
        navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(Color.parseColor(T.navBg));
        navBar.setWeightSum(4f);
        navBar.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(62)));

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            tab.setLayoutParams(new LinearLayout.LayoutParams(0, MATCH, 1f));
            tab.setOnClickListener(v -> switchTab(idx));
            press(tab);

            View bar = new View(this);
            bar.setBackground(roundRect(dp(3), Color.parseColor(AC.GOLD), 0, null));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(32), dp(3));
            bp.bottomMargin = dp(4); bar.setLayoutParams(bp); bar.setAlpha(0f); tab.addView(bar);

            TextView icon = tv(TABS[i][0], 20, T.textHint, false); tab.addView(icon);

            TextView lbl = tv(TABS[i][1], 10, T.textHint, true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
            lp.topMargin = dp(2); lbl.setLayoutParams(lp); tab.addView(lbl);

            navItems[i] = tab; navIcons[i] = icon; navLbls[i] = lbl; navBars[i] = bar;
            navBar.addView(tab);
        }
        rootLayout.addView(navBar);
    }

    void switchTab(int idx) {
        if (idx == currentTab) return;
        currentTab = idx;
        for (int i = 0; i < 4; i++) {
            boolean a = (i == idx);
            screens[i].setVisibility(a ? View.VISIBLE : View.GONE);
            int col = a ? Color.parseColor(AC.GOLD) : Color.parseColor(T.textHint);
            navIcons[i].setTextColor(col); navLbls[i].setTextColor(col);
            navBars[i].animate().alpha(a ? 1f : 0f).setDuration(180).start();
            if (a) navIcons[i].animate().scaleX(1.1f).scaleY(1.1f).setDuration(80)
                    .withEndAction(() -> navIcons[idx].animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HOME SCREEN
    // ═══════════════════════════════════════════════════════════════════
    private View buildHomeScreen() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor(T.bg));
        homeContent = vStack(sv);
        homeContent.setPadding(dp(14), dp(12), dp(14), dp(80));

        dashCards.put("smart",   mkSmartCard());
        dashCards.put("live",    mkLiveRatesCard());
        dashCards.put("african", mkAfricanCard());
        dashCards.put("metals",  mkMetalsCard());
        dashCards.put("bot",     mkBankCard());

        renderDashboard();
        return sv;
    }

    private void renderDashboard() {
        stopPulses(homeContent);
        homeContent.removeAllViews();

        JSONArray order = loadDashboardOrder();
        for (int i = 0; i < order.length(); i++) {
            String id = order.optString(i);
            LinearLayout card = dashCards.get(id);
            if (card != null) {
                if (card.getParent() != null) ((ViewGroup) card.getParent()).removeView(card);
                homeContent.addView(card);
            }
        }

        Button custBtn = outlineBtn("⚙️  Customize Dashboard", T.textSecondary);
        press(custBtn); custBtn.setOnClickListener(v -> showDashboardDialog());
        homeContent.addView(custBtn);

        if (initialLoad) showSkeleton();
    }

    private void showSkeleton() {
        homeContent.removeAllViews();
        for (int i = 0; i < 5; i++) { homeContent.addView(mkSkeletonCard()); spacerLin(homeContent, 12); }
    }

    private void hideSkeleton() {
        initialLoad = false;
        uiHandler.post(this::renderDashboard);
    }

    private JSONArray loadDashboardOrder() {
        try { return new JSONArray(prefs.getString(AP.KEY_DASHBOARD, "[\"smart\",\"live\",\"african\",\"metals\",\"bot\"]")); }
        catch (Exception e) { return new JSONArray().put("smart").put("live").put("african").put("metals").put("bot"); }
    }

    private void showDashboardDialog() {
        final List<String> ids = new ArrayList<>();
        JSONArray cur = loadDashboardOrder();
        for (int i = 0; i < cur.length(); i++) ids.add(cur.optString(i));
        for (String k : dashCards.keySet()) if (!ids.contains(k)) ids.add(k);

        Map<String,String> labels = new LinkedHashMap<>();
        labels.put("smart","✨ For You"); labels.put("live","📈 Live Rates"); labels.put("african","🌍 African");
        labels.put("metals","💰 Metals"); labels.put("bot","🏦 BoT Rates");

        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor(T.card)); layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.addView(titleTv("⚙️  Customize Dashboard")); spacerLin(layout, 16);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); layout.addView(list);

        Runnable redraw = () -> {
            list.removeAllViews();
            for (int i = 0; i < ids.size(); i++) {
                final int fi = i; String id = ids.get(i);
                LinearLayout row = hRow(); row.setPadding(0, dp(8), 0, dp(8));
                TextView lbl = subtitleTv(labels.containsKey(id) ? labels.get(id) : id); lbl.setLayoutParams(wt(1f)); row.addView(lbl);
                if (fi > 0) { Button up = ghostBtn("▲"); up.setTextColor(Color.parseColor(AC.BLUE)); up.setOnClickListener(v -> { Collections.swap(ids, fi, fi-1); list.removeAllViews(); }); row.addView(up); }
                if (fi < ids.size()-1) { Button dn = ghostBtn("▼"); dn.setTextColor(Color.parseColor(AC.BLUE)); dn.setOnClickListener(v -> { Collections.swap(ids, fi, fi+1); list.removeAllViews(); }); row.addView(dn); }
                list.addView(row); list.addView(thinDiv());
            }
        };
        // Intercept each button's click to also redraw
        layout.post(() -> { list.removeAllViews(); redraw.run(); });

        spacerLin(layout, 16);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).setNegativeButton("Cancel", null).create();
        Button saveBtn = solidBtn("Save", AC.BLUE, "#FFFFFF"); saveBtn.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(48)));
        press(saveBtn); saveBtn.setOnClickListener(v -> {
            try { JSONArray arr = new JSONArray(); for (String id : ids) arr.put(id); prefs.edit().putString(AP.KEY_DASHBOARD, arr.toString()).apply(); } catch (Exception ignored) {}
            renderDashboard(); dialog.dismiss(); toast("Dashboard saved");
        }); layout.addView(saveBtn);
        dialog.show();
    }

    // ── Smart Card ─────────────────────────────────────────────────────
    private LinearLayout mkSmartCard() {
        LinearLayout card = card(null); cardHdr(card, "✨", "For You", AC.PURPLE);
        spacerLin(card, 4); card.addView(captionTv("Based on your conversion habits")); spacerLin(card, 10); card.addView(thinDiv()); spacerLin(card, 8);
        smartRecsContainer = new LinearLayout(this); smartRecsContainer.setOrientation(LinearLayout.VERTICAL); card.addView(smartRecsContainer);
        refreshSmartRecs(); return card;
    }

    private void refreshSmartRecs() {
        if (smartRecsContainer == null) return;
        smartRecsContainer.removeAllViews();
        try {
            JSONObject stats = new JSONObject(prefs.getString(AP.KEY_USAGE, "{}"));
            List<Map.Entry<String,Integer>> entries = new ArrayList<>();
            Iterator<String> it = stats.keys();
            while (it.hasNext()) { String k = it.next(); if (!k.equals("TZS")) entries.add(new AbstractMap.SimpleEntry<>(k, stats.optInt(k))); }
            if (entries.isEmpty()) { smartRecsContainer.addView(captionTv("Convert currencies to see personalized tips.")); return; }
            entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            String top = entries.get(0).getKey();
            if ("USD".equals(top)||"EUR".equals(top)) addSmartRow(smartRecsContainer, "XAU", "Gold hedges against currency risk.", AC.GOLD);
            else if ("KES".equals(top)||"UGX".equals(top)) addSmartRow(smartRecsContainer, "ZAR", "East-Southern Africa corridor.", AC.TEAL);
            else addSmartRow(smartRecsContainer, "GBP", "Major currency benchmark.", AC.BLUE);
        } catch (Exception e) { smartRecsContainer.addView(captionTv("Use the converter to get personalized suggestions.")); }
    }

    private void addSmartRow(LinearLayout parent, String code, String reason, String color) {
        LinearLayout row = hRow(); row.setPadding(dp(12), dp(12), dp(12), dp(12)); press(row); row.setOnClickListener(v -> quickConvertDlg(code));
        View acc = new View(this); acc.setLayoutParams(new LinearLayout.LayoutParams(dp(3), dp(30))); acc.setBackground(roundRect(dp(2), Color.parseColor(color), 0, null)); row.addView(acc); spacerH(row, 10);
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setLayoutParams(wt(1f));
        info.addView(subtitleTv("Trending: " + CM.FLAGS.getOrDefault(code,"💱") + " " + code)); info.addView(captionTv(reason)); row.addView(info);
        Double r = rates.get(code); TextView pTv = priceTv(r != null ? fmtTzs.format(r) + " TZS" : "—", color); row.addView(pTv);
        parent.addView(row);
    }

    // ── Live Rates Card ───────────────────────────────────────────────
    private LinearLayout mkLiveRatesCard() {
        LinearLayout card = card(null); cardHdr(card, "📈", "Live Rates vs TZS", AC.BLUE);
        spacerLin(card, 4); card.addView(captionTv("Tap any row for an instant conversion")); spacerLin(card, 10); card.addView(thinDiv()); spacerLin(card, 8);
        for (String c : new String[]{"USD","EUR","GBP","JPY","CNY","INR","AED","ZAR","KES"}) {
            View row = mkRateRow(c); rateRowViews.put(c, row); card.addView(row);
        }
        return card;
    }

    private View mkRateRow(String code) {
        LinearLayout row = hRow(); row.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH, WRAP); rp.bottomMargin = dp(4); row.setLayoutParams(rp);
        press(row); row.setOnClickListener(v -> quickConvertDlg(code));
        row.setBackground(roundRect(dp(10), Color.parseColor(T.card2), dp(1), T.border));

        View acc = new View(this); acc.setLayoutParams(new LinearLayout.LayoutParams(dp(3), dp(32))); acc.setBackground(roundRect(dp(2), Color.parseColor(AC.BLUE), 0, null)); row.addView(acc); spacerH(row, 10);

        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setLayoutParams(wt(1f));
        info.addView(subtitleTv(CM.FLAGS.getOrDefault(code,"💱") + "  " + CM.NAMES.getOrDefault(code,code))); info.addView(captionTv(code)); row.addView(info);

        LinearLayout right = new LinearLayout(this); right.setOrientation(LinearLayout.VERTICAL); right.setGravity(Gravity.END);
        Double r = rates.get(code); boolean sm = CM.isSmallRate(code);
        TextView rateTv = priceTv(r != null ? (sm ? fmtSml.format(r) : fmtTzs.format(r)) + " TZS" : "—", AC.GOLD);
        TextView pctTv  = captionTv(""); pctTv.setGravity(Gravity.END);
        right.addView(rateTv); right.addView(pctTv); row.addView(right);

        rateValViews.put(code, rateTv); ratePctViews.put(code, pctTv);
        return row;
    }

    private void updateRateRows() {
        for (String code : rateValViews.keySet()) {
            Double nv = rates.get(code); if (nv == null) continue;
            TextView rt = rateValViews.get(code), pt = ratePctViews.get(code);
            Double ov = prevRates.get(code);
            boolean sm = CM.isSmallRate(code);
            if (ov != null && ov > 0 && !ov.equals(nv)) {
                animateRate(rt, ov, nv, sm);
                double pct = (nv - ov) / ov * 100.0; boolean up = nv >= ov;
                if (pt != null) { pt.setText(String.format(Locale.US, "%s %.2f%%", up?"▲":"▼", Math.abs(pct))); pt.setTextColor(Color.parseColor(up?AC.GREEN:AC.RED)); }
                flashRow(rateRowViews.get(code), Color.parseColor(up?AC.GREEN:AC.RED));
            } else if (rt != null) {
                rt.setText((sm ? fmtSml.format(nv) : fmtTzs.format(nv)) + " TZS");
            }
        }
    }

    // ── African Card ──────────────────────────────────────────────────
    private LinearLayout mkAfricanCard() {
        LinearLayout card = card(null); cardHdr(card, "🌍", "East African Currencies", AC.ORANGE);
        spacerLin(card, 4); card.addView(captionTv("Regional currencies vs. Tanzanian Shilling")); spacerLin(card, 10); card.addView(thinDiv()); spacerLin(card, 8);
        String[] codes = {"KES","UGX","RWF","ZAR","AED"}; String[] names = {"Kenya","Uganda","Rwanda","South Africa","UAE"};
        String[] accents = {AC.GREEN, AC.ORANGE, AC.BLUE, AC.TEAL, AC.PURPLE};
        for (int i = 0; i < codes.length; i++) {
            final String code = codes[i], ac = accents[i];
            LinearLayout row = hRow(); row.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH, WRAP); rp.bottomMargin = dp(4); row.setLayoutParams(rp);
            row.setBackground(roundRect(dp(10), Color.parseColor(T.card2), dp(1), T.border));
            press(row); row.setOnClickListener(v -> quickConvertDlg(code));

            View a = new View(this); a.setLayoutParams(new LinearLayout.LayoutParams(dp(3), dp(30))); a.setBackground(roundRect(dp(2), Color.parseColor(ac), 0, null)); row.addView(a); spacerH(row, 10);
            LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setLayoutParams(wt(1f));
            info.addView(subtitleTv(CM.FLAGS.getOrDefault(code,"🌍") + "  " + names[i])); info.addView(captionTv(code)); row.addView(info);

            Double r = rates.get(code); boolean sm = CM.isSmallRate(code);
            row.addView(priceTv(r != null ? (sm ? fmtSml.format(r) : fmtTzs.format(r)) + " TZS" : "—", AC.GOLD));
            card.addView(row);
        }
        return card;
    }

    // ── Metals Card ───────────────────────────────────────────────────
    private LinearLayout mkMetalsCard() {
        LinearLayout card = card(null); cardHdr(card, "💰", "Precious Metals", AC.GOLD);
        spacerLin(card, 4); card.addView(captionTv("Live prices per troy ounce in TZS")); spacerLin(card, 10); card.addView(thinDiv()); spacerLin(card, 8);

        LinearLayout goldRow = metalRow("#FFD700", "🥇  GOLD", "XAU", rates.get("XAU"));
        goldPriceTv = (TextView) goldRow.getTag(TAG_PRICE_TV);
        card.addView(goldRow); spacerLin(card, 8);

        LinearLayout silRow = metalRow("#C0C0C0", "🥈  SILVER", "XAG", rates.get("XAG"));
        silverPriceTv = (TextView) silRow.getTag(TAG_PRICE_TV);
        card.addView(silRow);
        return card;
    }

    private LinearLayout metalRow(String accent, String label, String code, Double rate) {
        LinearLayout row = hRow(); row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackground(roundRect(dp(12), Color.parseColor(T.card2), dp(1), accent));
        press(row); row.setOnClickListener(v -> quickConvertDlg(code));

        View bar = new View(this); bar.setLayoutParams(new LinearLayout.LayoutParams(dp(4), dp(36))); bar.setBackground(roundRect(dp(2), Color.parseColor(accent), 0, null)); row.addView(bar); spacerH(row, 12);
        LinearLayout left = new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL); left.setLayoutParams(wt(1f));
        left.addView(subtitleTv(label + " (" + code + ")")); left.addView(captionTv("Per troy ounce")); row.addView(left);

        LinearLayout right = new LinearLayout(this); right.setOrientation(LinearLayout.VERTICAL); right.setGravity(Gravity.END);
        String rStr = rate != null ? fmtTzs.format(rate) + " TZS" : "Loading…";
        TextView priceTv = priceTv(rStr, T.textPrimary); priceTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        right.addView(priceTv); right.addView(captionTv("troy oz")); row.addView(right);

        row.setTag(TAG_PRICE_TV, priceTv);  // Safe retrieval — no index casting
        return row;
    }

    // ── BoT Bank Card ─────────────────────────────────────────────────
    private LinearLayout mkBankCard() {
        LinearLayout card = card(null); cardHdr(card, "🏦", "BoT Official Rates", AC.PURPLE);

        LinearLayout srcRow = hRow();
        LinearLayout srcBadge = new LinearLayout(this); srcBadge.setPadding(dp(8), dp(5), dp(8), dp(5));
        srcBadge.setBackground(roundRect(dp(6), alphaColor(AC.PURPLE, 0x22), dp(1), AC.PURPLE));
        srcBadge.setLayoutParams(wt(1f)); srcBadge.addView(tv("bot.go.tz · Official Source", 10, AC.PURPLE, true)); srcRow.addView(srcBadge);

        Button refBtn = smallBtn("🔄 Refresh", AC.BLUE);
        LinearLayout.LayoutParams rbP = new LinearLayout.LayoutParams(WRAP, dp(34)); rbP.leftMargin = dp(8); refBtn.setLayoutParams(rbP);
        press(refBtn); refBtn.setOnClickListener(v -> fetchBotRates()); srcRow.addView(refBtn);
        card.addView(srcRow);

        spacerLin(card, 8);
        bankStatusTv = captionTv("Fetching official rates…"); card.addView(bankStatusTv); spacerLin(card, 8);

        LinearLayout hdr = hRow(); hdr.setPadding(dp(12), dp(9), dp(12), dp(9)); hdr.setBackground(roundRect(dp(8), Color.parseColor(T.border), 0, null));
        hdr.addView(tableCell("Currency", AC.GOLD, true)); hdr.addView(tableCell("Buy (TZS)", AC.GOLD, true)); hdr.addView(tableCell("Sell (TZS)", AC.GOLD, true)); card.addView(hdr);

        bankTableBody = new LinearLayout(this); bankTableBody.setOrientation(LinearLayout.VERTICAL); card.addView(bankTableBody);
        spacerLin(card, 8); card.addView(captionTv("Official mid-market rates from Bank of Tanzania."));

        renderBotTable(); fetchBotRates();
        return card;
    }

    private void renderBotTable() {
        if (bankTableBody == null) return; bankTableBody.removeAllViews();
        if (botRates.isEmpty()) {
            LinearLayout ph = hRow(); ph.setPadding(dp(12), dp(14), dp(12), dp(14));
            ph.setBackground(roundRect(dp(8), Color.parseColor(T.card2), 0, null));
            ph.addView(captionTv("No data — tap 🔄 Refresh to fetch")); bankTableBody.addView(ph); return;
        }
        for (int i = 0; i < botRates.size(); i++) {
            BotRate r = botRates.get(i);
            if (r.currency.isEmpty() || r.currency.length() > 4) continue;
            LinearLayout tr = hRow(); tr.setPadding(dp(12), dp(10), dp(12), dp(10));
            if (i % 2 == 0) tr.setBackgroundColor(Color.parseColor(T.card2));

            LinearLayout cCol = new LinearLayout(this); cCol.setOrientation(LinearLayout.VERTICAL); cCol.setLayoutParams(wt(1f));
            cCol.addView(subtitleTv(CM.FLAGS.getOrDefault(r.currency,"💱") + " " + r.currency)); tr.addView(cCol);

            TextView bTv = priceTv(r.buying, AC.GREEN); bTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13); bTv.setLayoutParams(wt(1f)); tr.addView(bTv);
            TextView sTv = priceTv(r.selling, AC.RED);  sTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13); sTv.setLayoutParams(wt(1f)); tr.addView(sTv);
            bankTableBody.addView(tr);
        }
    }

    private void fetchBotRates() {
        if (botFetching || !isOnline()) {
            if (bankStatusTv != null) bankStatusTv.setText(isOnline() ? "Already loading…" : "⚠️ Offline");
            return;
        }
        botFetching = true;
        if (bankStatusTv != null) bankStatusTv.setText("Fetching official rates…");
        exec.execute(() -> {
            List<BotRate> scraped = new ArrayList<>(); String status;
            try {
                Document doc = Jsoup.connect(BOT_URL).userAgent("Mozilla/5.0 (Android; Mobile)").timeout(20_000).get();
                Elements rows = doc.select("table tbody tr"); if (rows.isEmpty()) rows = doc.select("tr");
                for (Element row : rows) {
                    Elements cols = row.select("td"); if (cols.size() < 4) continue;
                    String code = cols.get(1).text().trim().toUpperCase(Locale.US);
                    String buy  = cols.get(2).text().trim(), sell = cols.get(3).text().trim();
                    if (!code.matches("[A-Z]{3}") || buy.isEmpty()) continue;
                    scraped.add(new BotRate(code, buy, sell));
                }
                status = scraped.isEmpty() ? "⚠️ No rates found" : "✓ " + scraped.size() + " currencies · " + hhmm();
            } catch (Exception e) { status = "⚠️ Could not load: " + safeMsg(e); }
            final List<BotRate> res = new ArrayList<>(scraped); final String st = status;
            uiHandler.post(() -> {
                botFetching = false;
                if (!res.isEmpty()) { botRates.clear(); botRates.addAll(res); }
                if (bankStatusTv != null) bankStatusTv.setText(st);
                renderBotTable();
            });
        });
    }

    // ── Skeleton loading card ─────────────────────────────────────────
    private LinearLayout mkSkeletonCard() {
        LinearLayout card = card(null);
        for (int i = 0; i < 4; i++) {
            LinearLayout row = hRow(); row.setPadding(dp(4), dp(8), dp(4), dp(8));
            View a = ph(dp(3), dp(28)); row.addView(a); spacerH(row, 10);
            LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setLayoutParams(wt(1f));
            View t1 = ph(dp(120), dp(14)); View t2 = ph(dp(60), dp(10)); spacerLin(col, 4);
            col.addView(t1); spacerLin(col, 5); col.addView(t2); row.addView(col);
            View p = ph(dp(80), dp(18)); row.addView(p);
            card.addView(row); spacerLin(card, 4);
            pulse(a, t1, t2, p);
        }
        return card;
    }
    private View ph(int w, int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(w, h)); v.setBackground(roundRect(dp(4), Color.parseColor(T.border), 0, null)); return v; }
    private void pulse(View... views) { for (View v : views) { ObjectAnimator a = ObjectAnimator.ofFloat(v, "alpha", 0.3f, 0.65f, 0.3f); a.setDuration(1300); a.setRepeatCount(ObjectAnimator.INFINITE); a.start(); v.setTag(TAG_PULSE, a); } }
    private void stopPulses(ViewGroup p) { if (p == null) return; for (int i = 0; i < p.getChildCount(); i++) { View c = p.getChildAt(i); Object a = c.getTag(TAG_PULSE); if (a instanceof ObjectAnimator) ((ObjectAnimator) a).cancel(); if (c instanceof ViewGroup) stopPulses((ViewGroup) c); } }

    // ═══════════════════════════════════════════════════════════════════
    //  CONVERT SCREEN
    // ═══════════════════════════════════════════════════════════════════
    private View buildConvertScreen() {
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(Color.parseColor(T.bg));
        LinearLayout content = vStack(sv); content.setPadding(dp(14), dp(12), dp(14), dp(80));
        buildPinnedCard(content);
        buildConverterCard(content);
        buildQuickAmountsCard(content);
        buildHistoryCard(content);
        return sv;
    }

    private void buildPinnedCard(LinearLayout parent) {
        LinearLayout card = card(parent); cardHdr(card, "⭐", "Saved Pins", AC.ORANGE); spacerLin(card, 8);
        HorizontalScrollView hsv = new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false);
        pinnedContainer = new LinearLayout(this); pinnedContainer.setOrientation(LinearLayout.HORIZONTAL); hsv.addView(pinnedContainer); card.addView(hsv);
        pinnedEmpty = captionTv("Tap ⭐ Pin below to save frequent conversions."); card.addView(pinnedEmpty);
        loadPinnedConversions();
    }

    private void loadPinnedConversions() {
        if (pinnedContainer == null) return; pinnedContainer.removeAllViews();
        try {
            JSONArray pins = new JSONArray(prefs.getString(AP.KEY_PINNED, "[]"));
            if (pinnedEmpty != null) pinnedEmpty.setVisibility(pins.length() == 0 ? View.VISIBLE : View.GONE);
            for (int i = 0; i < pins.length(); i++) {
                final JSONObject pin = pins.getJSONObject(i); final int fi = i;
                final String amt = pin.optString("amount","1"), from = pin.optString("from","USD"), to = pin.optString("to","TZS");
                LinearLayout chip = new LinearLayout(this); chip.setOrientation(LinearLayout.VERTICAL); chip.setPadding(dp(14), dp(10), dp(14), dp(10));
                chip.setBackground(roundRect(dp(12), Color.parseColor(T.card2), dp(1), T.border));
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(WRAP, WRAP); cp.rightMargin = dp(8); chip.setLayoutParams(cp);
                press(chip);
                chip.setOnClickListener(v -> {
                    if (convInput != null) convInput.setText(amt);
                    if (convFrom != null) convFrom.setSelection(CM.indexOf(from));
                    if (convTo   != null) convTo.setSelection(CM.indexOf(to));
                    doCalc(); switchTab(T_CONV);
                });
                chip.addView(subtitleTv(amt + " " + CM.FLAGS.getOrDefault(from,"") + from)); chip.addView(captionTv("→ " + CM.FLAGS.getOrDefault(to,"") + to));
                chip.setOnLongClickListener(v -> { new AlertDialog.Builder(this).setMessage("Remove this pin?").setPositiveButton("Remove", (d,w) -> removePin(fi)).setNegativeButton("Cancel",null).show(); return true; });
                pinnedContainer.addView(chip);
            }
        } catch (Exception e) { if (pinnedEmpty != null) pinnedEmpty.setVisibility(View.VISIBLE); }
    }

    private void savePin(String amt, String from, String to) {
        try {
            JSONArray pins = new JSONArray(prefs.getString(AP.KEY_PINNED, "[]"));
            // Avoid duplicates
            for (int i = 0; i < pins.length(); i++) { JSONObject p = pins.getJSONObject(i); if (p.optString("from").equals(from) && p.optString("to").equals(to)) { toast("Already pinned!"); return; } }
            JSONObject o = new JSONObject(); o.put("amount",amt); o.put("from",from); o.put("to",to); pins.put(o);
            prefs.edit().putString(AP.KEY_PINNED, pins.toString()).apply(); loadPinnedConversions(); toast("⭐ Pinned!");
        } catch (Exception ignored) {}
    }

    private void removePin(int idx) {
        try {
            JSONArray pins = new JSONArray(prefs.getString(AP.KEY_PINNED, "[]")); JSONArray n = new JSONArray();
            for (int i = 0; i < pins.length(); i++) if (i != idx) n.put(pins.getJSONObject(i));
            prefs.edit().putString(AP.KEY_PINNED, n.toString()).apply(); loadPinnedConversions();
        } catch (Exception ignored) {}
    }

    private void buildConverterCard(LinearLayout parent) {
        LinearLayout card = card(parent); cardHdr(card, "💱", "Converter", AC.GOLD); spacerLin(card, 4);
        card.addView(captionTv("Result updates live as you type")); spacerLin(card, 16);

        card.addView(fieldLbl("AMOUNT")); spacerLin(card, 5);
        convInput = styledInput("0"); convInput.setText("1"); card.addView(convInput); spacerLin(card, 14);

        card.addView(fieldLbl("FROM")); spacerLin(card, 5);
        convFrom = mkSpinner(); convFrom.setSelection(CM.indexOf("USD")); card.addView(convFrom); spacerLin(card, 10);

        // Swap button
        LinearLayout swapRow = hRow(); swapRow.setGravity(Gravity.CENTER);
        Button swapBtn = solidBtn("⇅  Swap", AC.BLUE, "#FFFFFF"); swapBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(160), dp(38))); press(swapBtn);
        swapBtn.setOnClickListener(v -> {
            ObjectAnimator.ofFloat(swapBtn, "rotation", 0f, 180f).setDuration(280).start();
            int f = convFrom.getSelectedItemPosition(), t = convTo.getSelectedItemPosition();
            convFrom.setSelection(t); convTo.setSelection(f);
        });
        swapRow.addView(swapBtn); card.addView(swapRow); spacerLin(card, 10);

        card.addView(fieldLbl("TO")); spacerLin(card, 5);
        convTo = mkSpinner(); convTo.setSelection(CM.indexOf("TZS")); card.addView(convTo); spacerLin(card, 18);

        // Animated result box
        LinearLayout resultBox = new LinearLayout(this); resultBox.setOrientation(LinearLayout.VERTICAL); resultBox.setPadding(dp(20), dp(20), dp(20), dp(20)); resultBox.setGravity(Gravity.CENTER);
        resultBox.setBackground(mkGradient(T.isDark ? new String[]{"#0C3820","#0A2A18"} : new String[]{"#E8F5E9","#C8E6C9"}));
        convResult = priceTv("—", AC.GREEN); convResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30); convResult.setGravity(Gravity.CENTER); resultBox.addView(convResult);
        convRate    = subtitleTv(""); convRate.setGravity(Gravity.CENTER); convRate.setPadding(0, dp(6), 0, 0); resultBox.addView(convRate);
        convInverse = captionTv("");  convInverse.setGravity(Gravity.CENTER); convInverse.setPadding(0, dp(4), 0, 0); resultBox.addView(convInverse);
        card.addView(resultBox); spacerLin(card, 14);

        // Action buttons
        LinearLayout btnRow = hRow(); btnRow.setWeightSum(3.2f);
        Button copyBtn = solidBtn("📋", T.border, T.textPrimary); copyBtn.setLayoutParams(wt2(0.7f, dp(46), dp(6))); press(copyBtn);
        copyBtn.setOnClickListener(v -> {
            if (convResult == null) return;
            try { ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); if (cm != null) { cm.setPrimaryClip(ClipData.newPlainText("rate", convResult.getText().toString())); } } catch (Exception ignored) {}
            copyBtn.setText("✓"); copyBtn.setTextColor(Color.parseColor(AC.GREEN));
            uiHandler.postDelayed(() -> { copyBtn.setText("📋"); copyBtn.setTextColor(Color.parseColor(T.textPrimary)); }, 1200); toast("Copied");
        }); btnRow.addView(copyBtn);

        Button pinBtn = solidBtn("⭐ Pin", alphaColor(AC.ORANGE, 0x22), AC.ORANGE); pinBtn.setLayoutParams(wt2(0.8f, dp(46), dp(6))); press(pinBtn);
        pinBtn.setOnClickListener(v -> {
            if (convInput == null || convFrom == null || convTo == null) return;
            String raw = convInput.getText().toString().replace(",","").trim(); if (raw.isEmpty()) return;
            savePin(raw, CM.CODES[convFrom.getSelectedItemPosition()], CM.CODES[convTo.getSelectedItemPosition()]);
        }); btnRow.addView(pinBtn);

        Button saveBtn = solidBtn("✓ Save", AC.GREEN, "#FFFFFF"); saveBtn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(46), 1.7f)); press(saveBtn);
        saveBtn.setOnClickListener(v -> doConvertAndSave()); btnRow.addView(saveBtn);
        card.addView(btnRow);

        AdapterView.OnItemSelectedListener sl = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { doCalc(); }
            public void onNothingSelected(AdapterView<?> p) {}
        };
        convFrom.setOnItemSelectedListener(sl); convTo.setOnItemSelectedListener(sl);
        convInput.addTextChangedListener(afterChanged(s -> doCalc()));
        doCalc();
    }

    private void doCalc() {
        if (convInput == null || convFrom == null || convTo == null) return;
        try {
            String raw = convInput.getText().toString().replace(",","").trim();
            if (raw.isEmpty() || raw.equals(".")) { clearConvResult(); return; }
            double amount = Double.parseDouble(raw);
            String from = CM.CODES[convFrom.getSelectedItemPosition()];
            String to   = CM.CODES[convTo.getSelectedItemPosition()];
            double result = convert(amount, from, to);
            String sym = CM.SYMBOLS.getOrDefault(to, "");
            if (convResult  != null) { convResult.setText(sym + " " + fmtFull.format(result) + "  " + to); ObjectAnimator.ofFloat(convResult, "scaleX", 1f, 1.04f, 1f).setDuration(180).start(); ObjectAnimator.ofFloat(convResult, "scaleY", 1f, 1.04f, 1f).setDuration(180).start(); }
            if (convRate    != null) convRate.setText("1 " + from + "  =  " + fmtFull.format(convert(1, from, to)) + " " + to);
            if (convInverse != null) convInverse.setText("1 " + to + "  =  " + fmtFull.format(convert(1, to, from)) + " " + from);
        } catch (NumberFormatException ignored) { clearConvResult(); }
          catch (Exception e) { clearConvResult(); }
    }

    private void clearConvResult() {
        if (convResult  != null) convResult.setText("—");
        if (convRate    != null) convRate.setText("");
        if (convInverse != null) convInverse.setText("");
    }

    private void doConvertAndSave() {
        doCalc();
        try {
            String raw = convInput.getText().toString().replace(",","").trim();
            if (raw.isEmpty()) return;
            double amount = Double.parseDouble(raw);
            String from = CM.CODES[convFrom.getSelectedItemPosition()];
            String to   = CM.CODES[convTo.getSelectedItemPosition()];
            double result = convert(amount, from, to);
            recordUsage(from, to);
            String time  = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
            DecimalFormat f = new DecimalFormat("#,##0.##");
            saveConvEntry(time + "   " + f.format(amount) + " " + from + "  →  " + f.format(result) + " " + to);
            loadConvHistory(); toast("✓ Saved");
        } catch (Exception ignored) {}
    }

    private void buildQuickAmountsCard(LinearLayout parent) {
        LinearLayout card = card(parent); cardHdr(card, "⚡", "Quick Amounts", AC.BLUE); spacerLin(card, 10);
        double[] amounts = {10, 50, 100, 500, 1_000, 5_000, 10_000, 50_000};
        String[] accents = {AC.BLUE, AC.GREEN, AC.GOLD, AC.ORANGE, AC.PURPLE, AC.TEAL, AC.BLUE, AC.GREEN};
        LinearLayout r1 = chipRow(), r2 = chipRow();
        for (int i = 0; i < amounts.length; i++) {
            final double amt = amounts[i]; final String ac = accents[i];
            Button chip = buildChip(chipLbl(amt), ac); chip.setLayoutParams(chipParams());
            chip.setOnClickListener(v -> {
                ObjectAnimator.ofFloat(chip, "scaleX", 1f, 1.07f, 1f).setDuration(150).start();
                ObjectAnimator.ofFloat(chip, "scaleY", 1f, 1.07f, 1f).setDuration(150).start();
                if (convInput != null) convInput.setText(amt >= 1000 ? String.valueOf((long) amt) : String.valueOf(amt));
                doCalc();
            });
            (i < 4 ? r1 : r2).addView(chip);
        }
        card.addView(r1); spacerLin(card, 6); card.addView(r2);
    }

    private void buildHistoryCard(LinearLayout parent) {
        LinearLayout card = card(parent);
        LinearLayout titleRow = hRow(); LinearLayout titleCol = new LinearLayout(this); titleCol.setOrientation(LinearLayout.VERTICAL); titleCol.setLayoutParams(wt(1f));
        titleCol.addView(subtitleTv("🕐  Recent Conversions")); titleCol.addView(captionTv("Last 20 entries")); titleRow.addView(titleCol);
        Button clearBtn = smallBtn("Clear", AC.RED); press(clearBtn);
        clearBtn.setOnClickListener(v -> {
            List<String> h = loadConvList(); if (h.isEmpty()) { toast("History empty"); return; }
            new AlertDialog.Builder(this).setMessage("Delete all " + h.size() + " records?")
                .setPositiveButton("Clear", (d,w) -> { prefs.edit().putString(AP.KEY_CONVHIST, "[]").apply(); loadConvHistory(); toast("Cleared"); })
                .setNegativeButton("Cancel", null).show();
        });
        titleRow.addView(clearBtn); card.addView(titleRow); spacerLin(card, 10); card.addView(thinDiv()); spacerLin(card, 8);
        convHistContainer = new LinearLayout(this); convHistContainer.setOrientation(LinearLayout.VERTICAL); card.addView(convHistContainer);
        convHistEmpty = captionTv("No conversions yet. Tap Save to begin."); convHistEmpty.setGravity(Gravity.CENTER); convHistEmpty.setPadding(0, dp(16), 0, dp(16)); card.addView(convHistEmpty);
        loadConvHistory();
    }

    private void loadConvHistory() {
        if (convHistContainer == null) return; convHistContainer.removeAllViews();
        List<String> hist = loadConvList();
        if (convHistEmpty != null) convHistEmpty.setVisibility(hist.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < hist.size(); i++) {
            LinearLayout row = hRow(); row.setPadding(dp(8), dp(9), dp(8), dp(9));
            if (i % 2 == 0) row.setBackgroundColor(Color.parseColor(T.card2));
            TextView badge = captionTv((i+1) + "."); badge.setMinWidth(dp(22)); row.addView(badge);
            TextView entry = captionTv(hist.get(i)); entry.setLayoutParams(wt(1f)); row.addView(entry);
            convHistContainer.addView(row); if (i < hist.size()-1) convHistContainer.addView(thinDiv());
        }
    }

    private List<String> loadConvList() {
        List<String> l = new ArrayList<>();
        try { JSONArray a = new JSONArray(prefs.getString(AP.KEY_CONVHIST, "[]")); for (int i=0;i<a.length();i++) l.add(a.getString(i)); } catch (Exception ignored) {}
        return l;
    }

    private void saveConvEntry(String e) {
        List<String> l = loadConvList(); l.add(0, e); if (l.size() > 20) l.subList(20, l.size()).clear();
        try { JSONArray a = new JSONArray(); for (String s : l) a.put(s); prefs.edit().putString(AP.KEY_CONVHIST, a.toString()).apply(); } catch (Exception ignored) {}
    }

    private void recordUsage(String from, String to) {
        try { JSONObject s = new JSONObject(prefs.getString(AP.KEY_USAGE, "{}")); s.put(from, s.optInt(from)+1); s.put(to, s.optInt(to)+1); prefs.edit().putString(AP.KEY_USAGE, s.toString()).apply(); } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MARKETS SCREEN
    // ═══════════════════════════════════════════════════════════════════
    private View buildMarketsScreen() {
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(Color.parseColor(T.bg));
        LinearLayout content = vStack(sv); content.setPadding(dp(14), dp(12), dp(14), dp(80));
        buildMajorGrid(content); buildAlertsCard(content);
        return sv;
    }

    private void buildMajorGrid(LinearLayout parent) {
        LinearLayout card = card(parent); cardHdr(card, "🌐", "Major Currencies", AC.GREEN);
        spacerLin(card, 4); card.addView(captionTv("Tap a tile for quick conversion")); spacerLin(card, 14);
        String[][] grid = {{"USD","EUR","GBP"},{"JPY","CNY","INR"},{"AED","ZAR","KES"}};
        String[] accents = {AC.GREEN, AC.GOLD, AC.BLUE, AC.ORANGE, AC.RED, AC.TEAL, AC.PURPLE, AC.GREEN, AC.ORANGE};
        int ai = 0;
        for (String[] gridRow : grid) {
            LinearLayout lr = hRow(); lr.setWeightSum(3f); LinearLayout.LayoutParams grp = new LinearLayout.LayoutParams(MATCH, WRAP); grp.bottomMargin = dp(8); lr.setLayoutParams(grp);
            for (String code : gridRow) lr.addView(mkCurrencyCell(code, accents[ai++]));
            card.addView(lr);
        }
    }

    private LinearLayout mkCurrencyCell(String code, String accent) {
        LinearLayout cell = new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL); cell.setGravity(Gravity.CENTER); cell.setPadding(dp(8), dp(14), dp(8), dp(14));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(108), 1f); cp.setMargins(dp(4), 0, dp(4), 0); cell.setLayoutParams(cp);
        cell.setBackground(roundRect(dp(14), Color.parseColor(T.card2), dp(1), accent));
        press(cell); cell.setOnClickListener(v -> quickConvertDlg(code));

        LinearLayout fc = new LinearLayout(this); fc.setGravity(Gravity.CENTER); fc.setBackground(roundRect(dp(22), alphaColor(accent, 0x22), 0, null)); fc.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        fc.addView(tv(CM.FLAGS.getOrDefault(code,"💱"), 18, "#FFFFFF", false)); cell.addView(fc); spacerLin(cell, 4);

        TextView codeV = captionTv(code); codeV.setGravity(Gravity.CENTER); cell.addView(codeV);
        Double r = rates.get(code); String rStr = r != null ? (code.equals("JPY") ? String.format(Locale.US,"%.2f",r) : fmtTzs.format(r)) : "—";
        TextView rateTv = priceTv(rStr, AC.GOLD); rateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11); rateTv.setGravity(Gravity.CENTER); rateTv.setPadding(0, dp(3), 0, 0); cell.addView(rateTv);
        cellRateViews.put(code, rateTv); return cell;
    }

    private void updateCellRates() {
        for (Map.Entry<String,TextView> e : cellRateViews.entrySet()) {
            String code = e.getKey(); Double r = rates.get(code); if (r == null) continue;
            e.getValue().setText(code.equals("JPY") ? String.format(Locale.US,"%.2f",r) : fmtTzs.format(r));
        }
    }

    private void buildAlertsCard(LinearLayout parent) {
        LinearLayout card = card(parent); cardHdr(card, "🔔", "Price Alerts", AC.BLUE);
        spacerLin(card, 4); card.addView(captionTv("Get notified when rates cross your targets")); spacerLin(card, 10);
        Button addBtn = solidBtn("＋  Add Alert", AC.BLUE, "#FFFFFF"); addBtn.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(46))); press(addBtn); addBtn.setOnClickListener(v -> showAddAlertDlg()); card.addView(addBtn);
        spacerLin(card, 10); card.addView(thinDiv()); spacerLin(card, 8);
        alertsList = new LinearLayout(this); alertsList.setOrientation(LinearLayout.VERTICAL); card.addView(alertsList);
        alertsEmpty = captionTv("No alerts yet. Tap ＋ Add Alert."); alertsEmpty.setGravity(Gravity.CENTER); alertsEmpty.setPadding(0, dp(14), 0, dp(14)); card.addView(alertsEmpty);
        refreshAlerts();
    }

    private void refreshAlerts() {
        if (alertsList == null) return; alertsList.removeAllViews(); List<JSONObject> alerts = loadAlerts();
        if (alertsEmpty != null) alertsEmpty.setVisibility(alerts.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < alerts.size(); i++) { final int idx = i;
            try {
                JSONObject a = alerts.get(i); String code = a.getString("currency"); double target = a.getDouble("target"); int cond = a.getInt("cond");
                LinearLayout row = hRow(); row.setPadding(dp(12), dp(12), dp(12), dp(12));
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH, WRAP); rp.bottomMargin = dp(6); row.setLayoutParams(rp);
                row.setBackground(roundRect(dp(10), Color.parseColor(T.card2), dp(1), T.border));

                View acc = new View(this); acc.setLayoutParams(new LinearLayout.LayoutParams(dp(4), dp(40))); acc.setBackground(roundRect(dp(2), Color.parseColor(cond==0?AC.GREEN:AC.RED), 0, null)); row.addView(acc); spacerH(row, 10);
                LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setLayoutParams(wt(1f));
                info.addView(subtitleTv(CM.FLAGS.getOrDefault(code,"💱") + " " + code + "  " + (cond==0?"▲ above":"▼ below")));
                info.addView(captionTv("Target: " + fmtTzs.format(target) + " TZS"));
                Double cur = rates.get(code); if (cur != null) info.addView(captionTv("Now: " + fmtTzs.format(cur) + " TZS")); row.addView(info);
                Button del = smallBtn("🗑", AC.RED); press(del); del.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("Remove " + code + " alert?").setPositiveButton("Delete", (d,w) -> { deleteAlert(idx); refreshAlerts(); }).setNegativeButton("Cancel", null).show());
                row.addView(del); alertsList.addView(row);
            } catch (Exception ignored) {}
        }
    }

    private void showAddAlertDlg() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setBackgroundColor(Color.parseColor(T.card)); layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.addView(subtitleTv("🔔  New Price Alert")); spacerLin(layout, 18);
        layout.addView(fieldLbl("CURRENCY")); spacerLin(layout, 5);
        String[] dN = new String[CM.CODES.length]; for (int i=0;i<CM.CODES.length;i++) dN[i] = CM.FLAGS.getOrDefault(CM.CODES[i],"💱") + "  " + CM.CODES[i];
        Spinner currSpin = dlgSpinner(dN); layout.addView(currSpin); spacerLin(layout, 14);
        layout.addView(fieldLbl("CONDITION")); spacerLin(layout, 5);
        Spinner condSpin = dlgSpinner(new String[]{"📈  Rises above target", "📉  Falls below target"}); layout.addView(condSpin); spacerLin(layout, 14);
        layout.addView(fieldLbl("TARGET RATE (TZS)")); spacerLin(layout, 5);
        EditText targetIn = styledInput("e.g. 2,650.00"); layout.addView(targetIn); spacerLin(layout, 6);
        TextView hintTv = captionTv(""); hintTv.setTextColor(Color.parseColor(AC.GREEN)); layout.addView(hintTv);
        currSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { Double r = rates.get(CM.CODES[pos]); hintTv.setText(r != null ? "Current: " + fmtTzs.format(r) + " TZS" : ""); if (targetIn.getText().toString().isEmpty() && r != null) targetIn.setHint(fmtTzs.format(r)); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).setNegativeButton("Cancel", null).create();
        Button create = solidBtn("🔔  Create Alert", AC.BLUE, "#FFFFFF"); create.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(50))); press(create);
        create.setOnClickListener(v -> {
            String raw = targetIn.getText().toString().trim(); if (raw.isEmpty()) { toast("Enter a target rate"); return; }
            try { double t = Double.parseDouble(raw.replace(",","")); if (t <= 0) { toast("Rate must be > 0"); return; }
                saveAlert(CM.CODES[currSpin.getSelectedItemPosition()], t, condSpin.getSelectedItemPosition()); refreshAlerts(); dialog.dismiss(); toast("✓ Alert created");
            } catch (NumberFormatException e) { toast("Invalid number"); }
        }); layout.addView(create); spacerLin(layout, 2); dialog.show();
    }

    private List<JSONObject> loadAlerts() { List<JSONObject> l = new ArrayList<>(); try { JSONArray a = new JSONArray(prefs.getString(AP.KEY_ALERTS, "[]")); for(int i=0;i<a.length();i++) l.add(a.getJSONObject(i)); } catch (Exception ignored) {} return l; }
    private void saveAlert(String code, double t, int cond) { try { List<JSONObject> l = loadAlerts(); JSONObject a = new JSONObject(); a.put("currency",code); a.put("target",t); a.put("cond",cond); l.add(a); JSONArray arr = new JSONArray(); for(JSONObject o:l) arr.put(o); prefs.edit().putString(AP.KEY_ALERTS, arr.toString()).apply(); } catch (Exception ignored) {} }
    private void deleteAlert(int idx) { try { List<JSONObject> l = loadAlerts(); if(idx>=0&&idx<l.size()) l.remove(idx); JSONArray arr = new JSONArray(); for(JSONObject o:l) arr.put(o); prefs.edit().putString(AP.KEY_ALERTS, arr.toString()).apply(); } catch (Exception ignored) {} }

    // ═══════════════════════════════════════════════════════════════════
    //  SETTINGS SCREEN
    // ═══════════════════════════════════════════════════════════════════
    private View buildSettingsScreen() {
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(Color.parseColor(T.bg));
        LinearLayout c = vStack(sv); c.setPadding(dp(14), dp(12), dp(14), dp(80));

        // Refresh
        LinearLayout rCard = card(c); cardHdr(rCard, "🔄", "Auto-Refresh", AC.BLUE); spacerLin(rCard, 14);
        toggleRow(rCard, "Auto-Refresh Rates", "Fetch rates in background", prefs.getBoolean(AP.KEY_AUTO, true), v -> { prefs.edit().putBoolean(AP.KEY_AUTO, v).apply(); scheduleAutoRefresh(); });
        spacerLin(rCard, 12); rCard.addView(thinDiv()); spacerLin(rCard, 12);
        rCard.addView(subtitleTv("Refresh Interval")); spacerLin(rCard, 4);
        String[] il = {"1 min","5 min","10 min","15 min","30 min","1 hour"}; int[] iv = {60_000,300_000,600_000,900_000,1_800_000,3_600_000};
        Spinner iSpin = dlgSpinner(il); int curIv = prefs.getInt(AP.KEY_INTERVAL, 300_000); for (int i=0;i<iv.length;i++) if(iv[i]==curIv){iSpin.setSelection(i);break;}
        iSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { public void onItemSelected(AdapterView<?> p,View v,int pos,long id){prefs.edit().putInt(AP.KEY_INTERVAL,iv[pos]).apply();scheduleAutoRefresh();} public void onNothingSelected(AdapterView<?> p){} });
        rCard.addView(iSpin); spacerLin(rCard, 12);
        Button refNow = outlineBtn("🔄  Refresh Now", AC.BLUE); press(refNow); refNow.setOnClickListener(v -> { fetchRates(); toast("Fetching…"); }); rCard.addView(refNow);

        // Theme
        LinearLayout tCard = card(c); cardHdr(tCard, "🎨", "Appearance", AC.PURPLE); spacerLin(tCard, 14);
        boolean dark = prefs.getBoolean(AP.KEY_DARK, true); LinearLayout pRow = hRow(); pRow.setWeightSum(2f);
        LinearLayout dP = mkThemePreview(true, dark); LinearLayout.LayoutParams dpp = new LinearLayout.LayoutParams(0, dp(82), 1f); dpp.rightMargin = dp(6); dP.setLayoutParams(dpp); dP.setOnClickListener(v -> applyTheme(true)); pRow.addView(dP);
        LinearLayout lP = mkThemePreview(false, !dark); lP.setLayoutParams(new LinearLayout.LayoutParams(0, dp(82), 1f)); lP.setOnClickListener(v -> applyTheme(false)); pRow.addView(lP); tCard.addView(pRow);
        spacerLin(tCard, 8); tCard.addView(captionTv("Tap a theme to apply. App restarts to rebuild the UI."));

        // Notifications
        LinearLayout nCard = card(c); cardHdr(nCard, "🔔", "Notifications", AC.ORANGE); spacerLin(nCard, 14);
        toggleRow(nCard, "Rate Update Notifications", "Show notification after each refresh", prefs.getBoolean(AP.KEY_NOTIFY, true), v -> prefs.edit().putBoolean(AP.KEY_NOTIFY, v).apply());

        // Data
        LinearLayout dCard = card(c); cardHdr(dCard, "💾", "Data", AC.RED); spacerLin(dCard, 14);
        Button cHist = outlineBtn("🗑  Clear Conversion History", AC.RED); press(cHist); cHist.setOnClickListener(v -> confirmClear("Clear conversion history?", () -> { prefs.edit().putString(AP.KEY_CONVHIST,"[]").apply(); loadConvHistory(); })); dCard.addView(cHist);
        Button cAlerts = outlineBtn("🗑  Clear All Alerts", AC.RED); press(cAlerts); cAlerts.setOnClickListener(v -> confirmClear("Delete all alerts?", () -> { prefs.edit().putString(AP.KEY_ALERTS,"[]").apply(); refreshAlerts(); })); dCard.addView(cAlerts);
        Button cPins = outlineBtn("🗑  Clear Saved Pins", AC.RED); press(cPins); cPins.setOnClickListener(v -> confirmClear("Clear all pinned conversions?", () -> { prefs.edit().putString(AP.KEY_PINNED,"[]").apply(); loadPinnedConversions(); })); dCard.addView(cPins);
        Button reset = outlineBtn("🔁  Reset All App Data", AC.RED); press(reset); reset.setOnClickListener(v -> confirmClear("Reset ALL app data? This cannot be undone.", () -> { prefs.edit().clear().apply(); setFallbackRates(); updateAllDisplays(); toast("Reset complete"); })); dCard.addView(reset);

        // About
        LinearLayout aCard = card(c); aCard.addView(tv("🇹🇿  TanzRate", 22, AC.GOLD, true)); spacerLin(aCard, 4);
        aCard.addView(subtitleTv("Real-time Tanzania Forex Tracker")); spacerLin(aCard, 12); aCard.addView(thinDiv()); spacerLin(aCard, 10);
        for (String[] row : new String[][]{{"Version","2.3.0"},{"Package","com.willykez.tanzsx"},{"Forex","ExchangeRate-API v6"},{"Metals","MetalPriceAPI"},{"BoT Data","bot.go.tz (live scrape)"},{"Min SDK","Android 5.0+"}}) { infoRow(aCard, row[0], row[1]); }
        spacerLin(aCard, 10); aCard.addView(thinDiv()); spacerLin(aCard, 10);
        TextView disc = captionTv("⚠️  Rates are for informational purposes only. Always verify with your bank before making financial decisions."); disc.setLineSpacing(0f, 1.4f); aCard.addView(disc);

        return sv;
    }

    private void applyTheme(boolean dark) { prefs.edit().putBoolean(AP.KEY_DARK, dark).apply(); finish(); startActivity(getIntent()); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); }
    private void confirmClear(String msg, OnAction action) { new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("Confirm", (d,w) -> action.run()).setNegativeButton("Cancel", null).show(); }

    private LinearLayout mkThemePreview(boolean dark, boolean selected) {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setGravity(Gravity.CENTER); p.setPadding(dp(10), dp(12), dp(10), dp(12));
        p.setBackground(roundRect(dp(12), Color.parseColor(dark ? "#1A1F3A" : "#FFFFFF"), dp(selected?2:1), selected?AC.GOLD:(dark?"#2A2F4A":"#DDE0EF")));
        p.addView(tv(dark?"🌙":"☀️", 22, dark?"#FFFFFF":"#1A1A2E", false)); spacerLin(p, 4); p.addView(subtitleTv(dark?"Dark":"Light"));
        if (selected) { TextView badge = captionTv("Active"); badge.setBackground(roundRect(dp(8), alphaColor(AC.GOLD,0x22), dp(1), AC.GOLD)); badge.setPadding(dp(6),dp(2),dp(6),dp(2)); badge.setGravity(Gravity.CENTER); spacerLin(p, 4); p.addView(badge); }
        return p;
    }

    private void toggleRow(LinearLayout p, String title, String sub, boolean init, OnToggle l) {
        LinearLayout r = hRow(); r.setPadding(0, dp(8), 0, dp(8)); LinearLayout tc = new LinearLayout(this); tc.setOrientation(LinearLayout.VERTICAL); tc.setLayoutParams(wt(1f));
        tc.addView(subtitleTv(title)); tc.addView(captionTv(sub)); r.addView(tc);
        Switch sw = new Switch(this); sw.setChecked(init); sw.setOnCheckedChangeListener((bv, isC) -> { ObjectAnimator.ofFloat(sw, "scaleX", 1f, 1.08f, 1f).setDuration(100).start(); l.onChanged(isC); });
        LinearLayout.LayoutParams swP = new LinearLayout.LayoutParams(WRAP, WRAP); swP.leftMargin = dp(12); sw.setLayoutParams(swP); r.addView(sw); p.addView(r);
    }

    private void infoRow(LinearLayout p, String label, String value) { LinearLayout r = hRow(); r.setPadding(0, dp(4), 0, dp(4)); TextView l = captionTv(label); l.setLayoutParams(wt(1f)); r.addView(l); r.addView(subtitleTv(value)); p.addView(r); }

    // ═══════════════════════════════════════════════════════════════════
    //  QUICK CONVERT DIALOG
    // ═══════════════════════════════════════════════════════════════════
    private void quickConvertDlg(String code) {
        Double rate = rates.get(code);
        if (rate == null) { toast("Rate not available yet — refresh to load"); return; }
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setBackgroundColor(Color.parseColor(T.card)); layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.addView(subtitleTv(CM.FLAGS.getOrDefault(code,"💱") + "  " + CM.NAMES.getOrDefault(code,code))); spacerLin(layout, 8);
        LinearLayout rateBadge = new LinearLayout(this); rateBadge.setPadding(dp(10),dp(8),dp(10),dp(8)); rateBadge.setBackground(roundRect(dp(8), alphaColor(AC.GOLD,0x22), dp(1), AC.GOLD)); rateBadge.addView(tv("1 " + code + "  =  " + fmtTzs.format(rate) + " TZS", 13, AC.GOLD, true)); layout.addView(rateBadge); spacerLin(layout, 16);
        layout.addView(fieldLbl("AMOUNT IN " + code)); spacerLin(layout, 6);
        EditText input = styledInput("0"); layout.addView(input); spacerLin(layout, 12);
        LinearLayout rBox = new LinearLayout(this); rBox.setOrientation(LinearLayout.VERTICAL); rBox.setPadding(dp(14),dp(12),dp(14),dp(12)); rBox.setBackground(roundRect(dp(10), T.isDark ? Color.parseColor("#0C3820") : Color.parseColor("#E8F5E9"), dp(2), AC.GREEN));
        TextView resTv = priceTv("0.00 TZS", AC.GREEN); resTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22); resTv.setGravity(Gravity.CENTER); rBox.addView(resTv);
        TextView invTv = captionTv(""); invTv.setGravity(Gravity.CENTER); invTv.setPadding(0, dp(4), 0, 0); rBox.addView(invTv); layout.addView(rBox);
        final double r = rate;
        input.addTextChangedListener(afterChanged(s -> { try { double amt = Double.parseDouble(s); resTv.setText(fmtTzs.format(amt * r) + " TZS"); invTv.setText("100 TZS  =  " + String.format(Locale.US, "%.4f %s", 100.0/r, code)); } catch (Exception e) { resTv.setText("0.00 TZS"); invTv.setText(""); } }));
        new AlertDialog.Builder(this).setView(layout).setPositiveButton("Close", null).show();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NETWORK & DATA
    // ═══════════════════════════════════════════════════════════════════
    private void fetchRates() {
        if (fetching) return;
        if (!isOnline()) {
            uiHandler.post(() -> { fetching = false; pulseTopDot(false); if (topUpdate != null) topUpdate.setText("Offline"); hideSkeleton(); updateAllDisplays(); toast("⚠️ No internet. Using cached rates."); });
            return;
        }
        fetching = true; pulseTopDot(true);
        if (initialLoad) uiHandler.post(this::showSkeleton);

        exec.execute(() -> {
            boolean ok = false;
            try {
                prevRates.clear(); prevRates.putAll(rates);
                JSONObject fj = new JSONObject(httpGet(FX_URL));
                if ("success".equals(fj.optString("result"))) {
                    JSONObject cr = fj.getJSONObject("conversion_rates"); double usd = cr.getDouble("TZS"); rates.put("USD", usd); rates.put("TZS", 1.0);
                    for (String c : new String[]{"EUR","GBP","JPY","CNY","INR","AED","ZAR","KES","UGX","RWF"}) if (cr.has(c)) rates.put(c, usd / cr.getDouble(c));
                    try { JSONObject mj = new JSONObject(httpGet(MTL_URL)); if (mj.optBoolean("success")) { JSONObject mr = mj.getJSONObject("rates"); if (mr.has("XAU")&&mr.getDouble("XAU")>0) rates.put("XAU", usd/mr.getDouble("XAU")); if (mr.has("XAG")&&mr.getDouble("XAG")>0) rates.put("XAG", usd/mr.getDouble("XAG")); } } catch (Exception e) { rates.put("XAU", usd*3_250.0); rates.put("XAG", usd*35.0); }
                    saveRates(); checkAlerts(); try { RateWidget.refreshAll(this); } catch (Exception ignored) {}
                    ok = true;
                }
            } catch (Exception ignored) {}
            final boolean success = ok;
            uiHandler.post(() -> {
                fetching = false; pulseTopDot(false); hideSkeleton();
                if (success) {
                    updateAllDisplays(); refreshSmartRecs();
                    if (prefs.getBoolean(AP.KEY_NOTIFY, true)) {
                        try { AlertNotificationManager.postRateUpdate(this, rates.get("USD"), rates.get("EUR"), rates.get("GBP")); } catch (Exception ignored) {}
                    }
                } else {
                    if (topUpdate != null) topUpdate.setText("API error — cached rates"); toast("Could not fetch new rates");
                }
            });
        });
    }

    private String httpGet(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection(); c.setRequestMethod("GET"); c.setConnectTimeout(15_000); c.setReadTimeout(15_000);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()))) { StringBuilder sb = new StringBuilder(); String ln; while ((ln = br.readLine()) != null) sb.append(ln); return sb.toString(); } finally { c.disconnect(); }
    }

    private void checkAlerts() {
        for (JSONObject a : loadAlerts()) {
            try {
                String code = a.getString("currency"); double target = a.getDouble("target"); int cond = a.getInt("cond"); Double cur = rates.get(code); if (cur == null) continue;
                if ((cond==0 && cur>=target) || (cond==1 && cur<=target)) {
                    final double cr = cur; uiHandler.post(() -> { try { AlertNotificationManager.postPriceAlert(this, code, cond, target, cr); } catch (Exception ignored) {} });
                }
            } catch (Exception ignored) {}
        }
    }

    private void updateAllDisplays() {
        Double usd = rates.get("USD"), eur = rates.get("EUR"), gold = rates.get("XAU"), sil = rates.get("XAG");
        if (topUsd  != null && usd  != null) topUsd.setText("1 USD = " + fmtTzs.format(usd) + " TZS");
        if (topEur  != null && eur  != null) topEur.setText("1 EUR = " + fmtTzs.format(eur) + " TZS");
        if (topGold != null && gold != null) topGold.setText("XAU " + goldShort(gold));
        if (topUpdate != null) topUpdate.setText(lastUpdateText());
        updateRateRows();
        if (goldPriceTv   != null && gold != null) goldPriceTv.setText(fmtTzs.format(gold) + " TZS");
        if (silverPriceTv != null && sil  != null) silverPriceTv.setText(fmtTzs.format(sil)  + " TZS");
        updateCellRates(); refreshAlerts(); doCalc();
    }

    private void setFallbackRates() { rates.put("TZS",1.0); rates.put("USD",2600.0); rates.put("EUR",2820.0); rates.put("GBP",3310.0); rates.put("JPY",17.3); rates.put("CNY",360.0); rates.put("INR",31.2); rates.put("AED",708.0); rates.put("ZAR",142.0); rates.put("KES",20.1); rates.put("UGX",0.70); rates.put("RWF",1.90); rates.put("XAU",8_450_000.0); rates.put("XAG",92_000.0); }
    private void loadSavedRates() { try { JSONObject j = new JSONObject(prefs.getString(AP.KEY_RATES, "")); for (String c : CM.CODES) if (j.has(c)) rates.put(c, j.getDouble(c)); rates.put("TZS", 1.0); } catch (Exception ignored) {} }
    private void saveRates() { try { JSONObject j = new JSONObject(); for (Map.Entry<String,Double> e : rates.entrySet()) j.put(e.getKey(), e.getValue()); prefs.edit().putString(AP.KEY_RATES, j.toString()).putLong(AP.KEY_UPDATE, System.currentTimeMillis()).apply(); } catch (Exception ignored) {} }
    private void scheduleAutoRefresh() { if (autoJob != null) { autoJob.cancel(false); autoJob = null; } if (!prefs.getBoolean(AP.KEY_AUTO, true)) return; long intv = prefs.getInt(AP.KEY_INTERVAL, 300_000); try { autoJob = exec.scheduleAtFixedRate(() -> uiHandler.post(this::fetchRates), intv, intv, TimeUnit.MILLISECONDS); } catch (Exception ignored) {} }
    private void shareRates() { StringBuilder sb = new StringBuilder("🇹🇿 Tanzania Exchange Rates\n\n"); for (String c : new String[]{"USD","EUR","GBP","KES","XAU"}) { Double r = rates.get(c); if (r != null) sb.append("1 ").append(c).append(" = ").append(fmtTzs.format(r)).append(" TZS\n"); } try { Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT, sb.toString()); startActivity(Intent.createChooser(i, "Share rates")); } catch (Exception ignored) {} }

    // ═══════════════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════════════════
    private void animateRate(TextView target, double oldVal, double newVal, boolean small) {
        int color = Color.parseColor(newVal >= oldVal ? AC.GREEN : AC.RED);
        ValueAnimator ca = ValueAnimator.ofObject(new ArgbEvaluator(), Color.parseColor(T.textPrimary), color, Color.parseColor(T.textPrimary)); ca.setDuration(700); ca.addUpdateListener(a -> { if (target != null) target.setTextColor((int) a.getAnimatedValue()); }); ca.start();
        ValueAnimator va = ValueAnimator.ofFloat((float)oldVal, (float)newVal); va.setDuration(500); va.setInterpolator(new DecelerateInterpolator(1.5f)); DecimalFormat fmt = small ? fmtSml : fmtTzs;
        va.addUpdateListener(a -> { if (target != null) target.setText(fmt.format((float) a.getAnimatedValue()) + " TZS"); }); va.start();
    }

    private void flashRow(View v, int targetColor) {
        if (v == null) return; int start = Color.parseColor(T.card2); int mid = Color.argb(55, Color.red(targetColor), Color.green(targetColor), Color.blue(targetColor));
        ValueAnimator va = ValueAnimator.ofObject(new ArgbEvaluator(), start, mid, start); va.setDuration(700); va.addUpdateListener(a -> v.setBackground(roundRect(dp(10), (int) a.getAnimatedValue(), dp(1), T.border))); va.start();
    }

    private void pulseTopDot(boolean on) {
        if (topDot == null) return;
        if (on) {
            ObjectAnimator p = ObjectAnimator.ofFloat(topDot, "alpha", 1f, 0.4f, 1f); p.setDuration(700); p.setRepeatCount(ObjectAnimator.INFINITE); p.setRepeatMode(ValueAnimator.REVERSE); p.setInterpolator(new AccelerateDecelerateInterpolator()); p.start(); topDot.setTag(TAG_DOT_ANIM, p);
            topDot.setBackground(roundRect(dp(5), Color.parseColor(AC.ORANGE), 0, null));
        } else {
            Object t = topDot.getTag(TAG_DOT_ANIM); if (t instanceof ObjectAnimator) ((ObjectAnimator) t).cancel();
            topDot.setAlpha(1f); topDot.setBackground(roundRect(dp(5), Color.parseColor(AC.GREEN), 0, null));
        }
    }

    private void press(View v) {
        v.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { view.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.85f).setDuration(80).start(); }
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) { view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start(); }
            return false;
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════
    private boolean isOnline() { try { ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE); if (cm == null) return false; NetworkInfo ni = cm.getActiveNetworkInfo(); return ni != null && ni.isConnected(); } catch (Exception e) { return false; } }
    private double convert(double amount, String from, String to) { if (from.equals(to)) return amount; Double fr = rates.get(from), tr = rates.get(to); if (fr == null || tr == null || tr == 0) return 0; if ("TZS".equals(from)) return amount / tr; if ("TZS".equals(to)) return amount * fr; return (amount * fr) / tr; }
    private double safeRate(String c) { Double v = rates.get(c); return (v != null && v > 0) ? v : 1.0; }
    private String lastUpdateText() { long t = prefs.getLong(AP.KEY_UPDATE, 0); if (t == 0) return "Never updated"; long ago = System.currentTimeMillis() - t; if (ago < 60_000) return "Just updated"; if (ago < 3_600_000) return "Updated " + (ago/60_000) + "m ago"; return "Updated " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(t)); }
    private String hhmm() { return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()); }
    private String safeMsg(Exception e) { String m = e.getMessage(); return m != null ? m.substring(0, Math.min(m.length(), 50)) : "Unknown error"; }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // ── Layout constants ──────────────────────────────────────────────
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP  = ViewGroup.LayoutParams.WRAP_CONTENT;

    // ── Drawables ─────────────────────────────────────────────────────
    private GradientDrawable roundRect(int radius, int fill, int strokeW, String sh) { GradientDrawable d = new GradientDrawable(); d.setCornerRadius(radius); d.setColor(fill); if (strokeW > 0 && sh != null) d.setStroke(strokeW, Color.parseColor(sh)); return d; }
    private GradientDrawable roundRect(int radius, int fill, int strokeW, int sc) { GradientDrawable d = new GradientDrawable(); d.setCornerRadius(radius); d.setColor(fill); if (strokeW > 0) d.setStroke(strokeW, sc); return d; }
    private GradientDrawable mkGradient(String[] colors) { int[] cols = new int[colors.length]; for (int i=0;i<colors.length;i++) cols[i] = Color.parseColor(colors[i]); GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, cols); return g; }
    private static int alphaColor(String hex, int alpha) { try { int c = Color.parseColor(hex); return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c)); } catch (Exception e) { return Color.TRANSPARENT; } }
    private View mkDot(String color) { View v = new View(this); v.setBackground(roundRect(dp(5), Color.parseColor(color), 0, null)); return v; }

    // ── Layout helpers ────────────────────────────────────────────────
    private LinearLayout card(LinearLayout parent) { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16), dp(16), dp(16), dp(16)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP); p.bottomMargin = dp(12); c.setLayoutParams(p); GradientDrawable bg = new GradientDrawable(); bg.setCornerRadius(dp(16)); bg.setColor(Color.parseColor(T.cardElevated)); bg.setStroke(dp(1), Color.parseColor(T.borderStrong)); c.setBackground(bg); if (parent != null) parent.addView(c); return c; }
    private void cardHdr(LinearLayout card, String icon, String title, String hex) { LinearLayout row = hRow(); LinearLayout badge = new LinearLayout(this); badge.setGravity(Gravity.CENTER); badge.setPadding(dp(8), dp(6), dp(8), dp(6)); badge.setBackground(roundRect(dp(10), alphaColor(hex, 0x22), dp(1), hex)); badge.addView(tv(icon, 14, hex, false)); row.addView(badge); spacerH(row, 10); TextView t = subtitleTv(title); t.setLayoutParams(wt(1f)); row.addView(t); card.addView(row); spacerLin(card, 10); }
    private LinearLayout vStack(ScrollView sv) { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); sv.addView(c); return c; }
    private LinearLayout hRow() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    private LinearLayout.LayoutParams wt(float w) { return new LinearLayout.LayoutParams(0, WRAP, w); }
    private LinearLayout.LayoutParams wt2(float w, int h, int rightMargin) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, h, w); p.rightMargin = rightMargin; return p; }
    private void spacerLin(LinearLayout parent, int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(h))); parent.addView(v); }
    private void spacerH(LinearLayout parent, int w) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(dp(w), MATCH)); parent.addView(v); }
    private View spacerFlex() { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f)); return v; }
    private View thinDiv() { View d = new View(this); d.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(1))); d.setBackgroundColor(Color.parseColor(T.divider)); return d; }

    // ── TextView factory ──────────────────────────────────────────────
    private TextView tv(String t, int sp, String hex, boolean b) { TextView v = new TextView(this); v.setText(t); v.setTextColor(Color.parseColor(hex)); v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); v.setTypeface(null, b ? Typeface.BOLD : Typeface.NORMAL); return v; }
    private TextView tv(String t, int sp, int color, boolean b) { TextView v = new TextView(this); v.setText(t); v.setTextColor(color); v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); v.setTypeface(null, b ? Typeface.BOLD : Typeface.NORMAL); return v; }
    private TextView titleTv(String t) { return tv(t, 17, T.textPrimary, true); }
    private TextView subtitleTv(String t) { TextView v = tv(t, 13, T.textSecondary, false); v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); return v; }
    private TextView captionTv(String t) { return tv(t, 11, T.textHint, false); }
    private TextView priceTv(String t, String hex) { TextView v = tv(t, 14, hex, true); v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); v.setIncludeFontPadding(false); return v; }
    private TextView fieldLbl(String t) { TextView v = tv(t, 10, T.textHint, true); v.setLetterSpacing(0.1f); return v; }
    private TextView tableCell(String t, String hex, boolean b) { TextView v = tv(t, 12, hex, b); v.setLayoutParams(wt(1f)); return v; }

    // ── Button factory ────────────────────────────────────────────────
    private Button ghostBtn(String icon) { Button b = new Button(this); b.setText(icon); b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); b.setBackground(null); b.setMinWidth(dp(40)); b.setMinHeight(dp(40)); b.setAllCaps(false); return b; }
    private Button solidBtn(String txt, String bgHex, String fgHex) { Button b = new Button(this); b.setText(txt); b.setTextColor(Color.parseColor(fgHex)); b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13); b.setTypeface(null, Typeface.BOLD); b.setAllCaps(false); b.setBackground(roundRect(dp(10), Color.parseColor(bgHex), 0, null)); return b; }
    private Button solidBtn(String txt, int bgColor, String fgHex) { Button b = solidBtn(txt, String.format("#%06X", 0xFFFFFF & bgColor), fgHex); return b; }
    private Button outlineBtn(String txt, String hex) { Button b = new Button(this); b.setText(txt); b.setTextColor(Color.parseColor(hex)); b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13); b.setTypeface(null, Typeface.BOLD); b.setAllCaps(false); b.setBackground(roundRect(dp(10), alphaColor(hex, 0x22), dp(1), hex)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, dp(46)); p.bottomMargin = dp(4); b.setLayoutParams(p); return b; }
    private Button smallBtn(String txt, String hex) { Button b = new Button(this); b.setText(txt); b.setTextColor(Color.parseColor(hex)); b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11); b.setAllCaps(false); b.setBackground(roundRect(dp(8), alphaColor(hex, 0x22), dp(1), hex)); b.setPadding(dp(10), dp(4), dp(10), dp(4)); return b; }

    // ── Input / Spinner ───────────────────────────────────────────────
    private EditText styledInput(String hint) { EditText et = new EditText(this); et.setHint(hint); et.setHintTextColor(Color.parseColor(T.textHint)); et.setTextColor(Color.parseColor(T.textPrimary)); et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20); et.setTypeface(null, Typeface.BOLD); et.setSingleLine(true); et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); et.setSelectAllOnFocus(true); et.setBackground(roundRect(dp(12), Color.parseColor(T.inputBg), dp(2), AC.BLUE)); et.setPadding(dp(16), dp(14), dp(16), dp(14)); et.setLayoutParams(new LinearLayout.LayoutParams(MATCH, WRAP)); return et; }
    private Spinner mkSpinner() { String[] d = new String[CM.CODES.length]; for (int i=0;i<CM.CODES.length;i++) d[i] = CM.FLAGS.getOrDefault(CM.CODES[i],"💱") + "  " + CM.CODES[i]; Spinner s = new Spinner(this); s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, d)); s.setBackground(roundRect(dp(10), Color.parseColor(T.inputBg), dp(1), T.inputBorder)); s.setPadding(dp(12), dp(12), dp(12), dp(12)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, dp(52)); p.bottomMargin = dp(4); s.setLayoutParams(p); return s; }
    private Spinner dlgSpinner(String[] items) { Spinner s = new Spinner(this); s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items)); s.setBackground(roundRect(dp(10), Color.parseColor(T.inputBg), dp(1), T.inputBorder)); s.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(50))); return s; }

    // ── Chip helpers ──────────────────────────────────────────────────
    private LinearLayout chipRow() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setWeightSum(4f); return r; }
    private LinearLayout.LayoutParams chipParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(36), 1f); p.setMargins(dp(3), 0, dp(3), 0); return p; }
    private String chipLbl(double v) { if (v>=1_000_000) return (long)(v/1_000_000)+"M"; if (v>=1_000) return (long)(v/1_000)+"K"; return String.valueOf((long)v); }
    private Button buildChip(String text, String accent) { Button b = new Button(this); b.setText(text); b.setTextColor(Color.parseColor(T.textPrimary)); b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); b.setAllCaps(false); b.setTypeface(null, Typeface.BOLD); b.setBackground(roundRect(dp(20), Color.parseColor(T.chipBg), dp(1), T.border)); return b; }

    // ── TextWatcher factory ───────────────────────────────────────────
    private TextWatcher afterChanged(OnTextChanged l) { return new TextWatcher() { public void afterTextChanged(Editable s) { l.onChanged(s.toString()); } public void beforeTextChanged(CharSequence s, int a, int b, int c) {} public void onTextChanged(CharSequence s, int a, int b, int c) {} }; }

    // ── Top pill ──────────────────────────────────────────────────────
    private TextView topPill(String txt, String hex) { TextView t = new TextView(this); t.setText(txt); t.setTextColor(Color.parseColor(hex)); t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11); t.setTypeface(null, Typeface.BOLD); t.setBackground(roundRect(dp(20), alphaColor(hex, 0x1A), dp(1), alphaColor(hex, 0x66))); t.setPadding(dp(9), dp(5), dp(9), dp(5)); return t; }
    private View pillGap() { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(dp(7), 1)); return v; }
}

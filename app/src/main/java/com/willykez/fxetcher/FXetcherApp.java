package com.willykez.fxetcher;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.*;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class FXetcherApp extends Activity {

    // ── API ───────────────────────────────────────────────────────────────────
    private static final String FX_URL  = "https://v6.exchangerate-api.com/v6/56bff02e7e890d6fae47bb57/latest/USD";
    private static final String MTL_URL = "https://api.metalpriceapi.com/v1/latest?api_key=28b227b94a7053b0c52456cd3f453c09&base=USD&currencies=XAU,XAG";
    public static final String BOT_URL  = "https://www.bot.go.tz/ExchangeRate/excRates";

    // ── Data ──────────────────────────────────────────────────────────────────
    final ConcurrentHashMap<String,Double> rates     = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String,Double> prevRates = new ConcurrentHashMap<>();
    final DecimalFormat fmtTzs = new DecimalFormat("#,##0.00");
    final DecimalFormat fmtSml = new DecimalFormat("#,##0.0000");
    final String[]                  CODES   = CurrencyMeta.CODES;
    final HashMap<String,String>    NAMES   = CurrencyMeta.NAMES;
    final HashMap<String,String>    SYMBOLS = CurrencyMeta.SYMBOLS;
    final HashMap<String,String>    FLAGS   = CurrencyMeta.FLAGS;

    // ── Prefs / Async ─────────────────────────────────────────────────────────
    SharedPreferences prefs;
    final Handler handler = new Handler(Looper.getMainLooper());
    final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
    ScheduledFuture<?> autoRefreshFuture;
    volatile boolean fetching    = false;
    volatile boolean botFetching = false;

    // ── UI Kit ────────────────────────────────────────────────────────────────
    UiKit ui;

    // ── Root layout ───────────────────────────────────────────────────────────
    FrameLayout    rootFrame;
    LinearLayout   rootLayout;
    LinearLayout   navBar;

    // ── Screens ───────────────────────────────────────────────────────────────
    static final int T_HOME = 0, T_CONV = 1, T_MKT = 2, T_CALC = 3, T_SET = 4;
    final View[]          screens   = new View[5];
    final LinearLayout[]  navItems  = new LinearLayout[5];
    final TextView[]      navIcons  = new TextView[5];
    final TextView[]      navLabels = new TextView[5];
    final View[]          navBars   = new View[5];
    int currentTab = -1;

    static final String[][] TABS = {
        {"🏠","Home"}, {"💱","Convert"}, {"📊","Markets"}, {"🧮","Calc"}, {"⚙️","Settings"}
    };

    // ── Top bar refs ──────────────────────────────────────────────────────────
    TextView    topUsd, topEur, topGold, topTime;
    View        topDot;
    ObjectAnimator dotAnim;

    // ── Screen builders ───────────────────────────────────────────────────────
    HomeScreen      homeScreen;
    ConvertScreen   convertScreen;
    MarketsScreen   marketsScreen;
    CalcScreen      calcScreen;
    SettingsScreen  settingsScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(AppPrefs.FILE, MODE_PRIVATE);
        Tokens.apply(prefs.getBoolean(AppPrefs.KEY_DARK, true));
        ui = new UiKit(this);
        AlertNotificationManager.createChannels(this);
        setFallbackRates();
        loadSavedRates();

        rootFrame = new FrameLayout(this);
        rootFrame.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        rootFrame.setBackgroundColor(Tokens.bg);

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        rootLayout.setBackgroundColor(Tokens.bg);

        buildTopBar();

        View sep = new View(this);
        sep.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ui.dp(1)));
        sep.setBackgroundColor(Tokens.withAlpha(Tokens.GOLD, 35));
        rootLayout.addView(sep);

        FrameLayout container = new FrameLayout(this);
        container.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f));

        homeScreen      = new HomeScreen(this);
        convertScreen   = new ConvertScreen(this);
        marketsScreen   = new MarketsScreen(this);
        calcScreen      = new CalcScreen(this);
        settingsScreen  = new SettingsScreen(this);

        screens[T_HOME] = homeScreen.build();
        screens[T_CONV] = convertScreen.build();
        screens[T_MKT]  = marketsScreen.build();
        screens[T_CALC] = calcScreen.build();
        screens[T_SET]  = settingsScreen.build();

        for (View s : screens) { s.setVisibility(View.GONE); container.addView(s); }
        rootLayout.addView(container);
        buildNavBar();
        rootFrame.addView(rootLayout);
        setContentView(rootFrame);

        getWindow().setStatusBarColor(Tokens.statusBarColor);
        getWindow().setNavigationBarColor(Tokens.navBg);

        int startTab = getIntent().getIntExtra("startTab", T_HOME);
        switchTab(startTab);
        fetchRates();
        scheduleAutoRefresh();
    }

    @Override protected void onPause()   { super.onPause(); saveRates(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (autoRefreshFuture!=null) autoRefreshFuture.cancel(false);
        exec.shutdownNow();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TOP BAR
    // ════════════════════════════════════════════════════════════════════════
    private void buildTopBar() {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable topBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            Tokens.isDark ? new int[]{0xFF090C1A,0xFF0F1328,0xFF090C1A}
                          : new int[]{0xFF1A237E,0xFF283593,0xFF1A237E});
        topBar.setBackground(topBg);

        // Row 1: Brand · dot · time · auto switch
        LinearLayout row1 = ui.hRow();
        row1.setPadding(ui.dp(Tokens.S16), ui.dp(Tokens.S14), ui.dp(Tokens.S10), ui.dp(Tokens.S6));
        LinearLayout brand = ui.hRow();
        brand.addView(ui.tv("🇹🇿", Tokens.TEXT_LG, Tokens.GOLD, false));
        ui.spacerH(brand, Tokens.S6);
        LinearLayout brandText = ui.vCol();
        brandText.addView(ui.tv("FXetcher", Tokens.TEXT_LG, 0xFFECEFF8, true));
        brandText.addView(ui.tv("Tanzania Forex", Tokens.TEXT_XS, Tokens.withAlpha(0xFFECEFF8,140), false));
        brand.addView(brandText);
        brand.setLayoutParams(ui.wt(1f));
        row1.addView(brand);

        topDot = ui.liveDot(Tokens.GREEN);
        LinearLayout.LayoutParams dotLp = (LinearLayout.LayoutParams) topDot.getLayoutParams();
        dotLp.rightMargin = ui.dp(Tokens.S4); topDot.setLayoutParams(dotLp);
        row1.addView(topDot);

        topTime = ui.tv("—", Tokens.TEXT_XS, Tokens.onSurfaceVar, false);
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        timeLp.rightMargin = ui.dp(Tokens.S12); topTime.setLayoutParams(timeLp);
        row1.addView(topTime);

        LinearLayout autoBox = ui.hRow(); autoBox.setGravity(Gravity.CENTER_VERTICAL);
        autoBox.addView(ui.tv("Auto ", Tokens.TEXT_XS, Tokens.onSurfaceVar, false));
        Switch autoSwitch = new Switch(this);
        autoSwitch.setChecked(prefs.getBoolean(AppPrefs.KEY_AUTO, true));
        autoSwitch.setOnCheckedChangeListener((btn,isChecked) -> { prefs.edit().putBoolean(AppPrefs.KEY_AUTO,isChecked).apply(); scheduleAutoRefresh(); });
        autoBox.addView(autoSwitch); row1.addView(autoBox);
        topBar.addView(row1);

        // Row 2: Rate pills
        LinearLayout row2 = ui.hRow();
        row2.setPadding(ui.dp(Tokens.S16), ui.dp(Tokens.S2), ui.dp(Tokens.S16), ui.dp(Tokens.S10));
        topUsd  = ui.topPill("USD "+fmtTzs.format(safeRate("USD",2600)),  Tokens.GREEN);
        topEur  = ui.topPill("EUR "+fmtTzs.format(safeRate("EUR",2820)),  Tokens.GOLD);
        topGold = ui.topPill("XAU "+goldShort(safeRate("XAU",8_400_000)), Tokens.ORANGE);

        // Make pills tappable -> quick convert
        topUsd.setClickable(true);  topUsd.setOnClickListener(v  -> showQuickConvert("USD"));
        topEur.setClickable(true);  topEur.setOnClickListener(v  -> showQuickConvert("EUR"));
        topGold.setClickable(true); topGold.setOnClickListener(v -> showMetalSheet("XAU"));

        row2.addView(topUsd); ui.spacerH(row2,Tokens.S6);
        row2.addView(topEur); ui.spacerH(row2,Tokens.S6);
        row2.addView(topGold); row2.addView(ui.flexSpacer());
        topBar.addView(row2);
        rootLayout.addView(topBar);
    }

    private String goldShort(double v) { return String.format(Locale.US,"%.1fM TZS",v/1_000_000.0); }

    // ════════════════════════════════════════════════════════════════════════
    //  NAV BAR  (5 tabs)
    // ════════════════════════════════════════════════════════════════════════
    private void buildNavBar() {
        navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(Tokens.navBg);
        navBar.setWeightSum(5f);
        navBar.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ui.dp(Tokens.NAV_HEIGHT)));
        GradientDrawable navBg = new GradientDrawable();
        navBg.setColor(Tokens.navBg); navBg.setStroke(ui.dp(1), Tokens.outlineVar);
        navBar.setBackground(navBg);

        for (int i=0; i<5; i++) {
            final int idx=i;
            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL); tab.setGravity(Gravity.CENTER);
            tab.setLayoutParams(new LinearLayout.LayoutParams(0, MATCH_PARENT, 1f));
            tab.setBackground(ui.rippleOver(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)));
            tab.setClickable(true); tab.setFocusable(true);
            tab.setOnClickListener(v -> { ui.scaleAnim(v); switchTab(idx); });

            View bar = new View(this); GradientDrawable barBg = new GradientDrawable();
            barBg.setCornerRadius(ui.dp(Tokens.R2)); barBg.setColor(Tokens.GOLD); bar.setBackground(barBg);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ui.dp(24), ui.dp(3));
            bp.bottomMargin = ui.dp(Tokens.S4); bar.setLayoutParams(bp); bar.setAlpha(0f);
            tab.addView(bar);

            TextView icon = ui.tv(TABS[i][0], 18, Tokens.surfaceTint, false); icon.setGravity(Gravity.CENTER);
            tab.addView(icon);
            TextView lbl = ui.tv(TABS[i][1], Tokens.TEXT_XS-1, Tokens.surfaceTint, true); lbl.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT); llp.topMargin = ui.dp(2); lbl.setLayoutParams(llp);
            tab.addView(lbl);

            navItems[i]=tab; navIcons[i]=icon; navLabels[i]=lbl; navBars[i]=bar;
            navBar.addView(tab);
        }
        rootLayout.addView(navBar);
    }

    void switchTab(int idx) {
        if (idx==currentTab) return;
        currentTab=idx;
        for (int i=0; i<5; i++) {
            boolean active=(i==idx);
            screens[i].setVisibility(active?View.VISIBLE:View.GONE);
            int col = active?Tokens.GOLD:Tokens.surfaceTint;
            navIcons[i].setTextColor(col); navLabels[i].setTextColor(col);
            navBars[i].animate().alpha(active?1f:0f).setDuration(180).start();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  QUICK CONVERT BOTTOM SHEET
    // ════════════════════════════════════════════════════════════════════════
    void showQuickConvert(String code) {
        Double rateVal = rates.get(code);
        if (rateVal==null) { ui.snack(rootFrame,"Rate not yet available",Tokens.RED); return; }
        final double rate = rateVal;

        View dim = new View(this);
        dim.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT,MATCH_PARENT));
        dim.setBackgroundColor(0xAA000000); dim.setAlpha(0f); rootFrame.addView(dim);

        LinearLayout sheet = ui.vCol();
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(Tokens.surface);
        sheetBg.setCornerRadii(new float[]{ui.dp(Tokens.R20),ui.dp(Tokens.R20),ui.dp(Tokens.R20),ui.dp(Tokens.R20),0,0,0,0});
        sheetBg.setStroke(ui.dp(1),Tokens.outline); sheet.setBackground(sheetBg);
        sheet.setPadding(ui.dp(Tokens.S20),ui.dp(Tokens.S20),ui.dp(Tokens.S20),ui.dp(Tokens.S32));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT);
        slp.gravity=Gravity.BOTTOM; sheet.setLayoutParams(slp);

        View handle = new View(this);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(ui.dp(40),ui.dp(4));
        hlp.gravity=Gravity.CENTER_HORIZONTAL; hlp.bottomMargin=ui.dp(Tokens.S16); handle.setLayoutParams(hlp);
        GradientDrawable hd = new GradientDrawable(); hd.setCornerRadius(ui.dp(2)); hd.setColor(Tokens.outline); handle.setBackground(hd);
        sheet.addView(handle);

        LinearLayout header = ui.hRow();
        String flag = FLAGS.getOrDefault(code,"💱");
        TextView titleTv = ui.tv(flag+"  "+NAMES.getOrDefault(code,code), Tokens.TEXT_XL, Tokens.onSurface, true);
        titleTv.setLayoutParams(ui.wt(1f)); header.addView(titleTv);
        Button closeBtn = ui.ghostBtn("✕", Tokens.onSurfaceVar); header.addView(closeBtn);
        sheet.addView(header); ui.spacer(sheet,Tokens.S8);

        // Rate badge
        LinearLayout rateBadge = new LinearLayout(this);
        rateBadge.setPadding(ui.dp(Tokens.S12),ui.dp(Tokens.S8),ui.dp(Tokens.S12),ui.dp(Tokens.S8));
        GradientDrawable rb = new GradientDrawable(); rb.setCornerRadius(ui.dp(Tokens.R8));
        rb.setColor(Tokens.withAlpha(Tokens.GOLD,20)); rb.setStroke(ui.dp(1),Tokens.withAlpha(Tokens.GOLD,60));
        rateBadge.setBackground(rb);
        rateBadge.addView(ui.mono("1 "+code+"  =  "+fmtTzs.format(rate)+" TZS", Tokens.TEXT_MD, Tokens.GOLD, true));
        sheet.addView(rateBadge); ui.spacer(sheet,Tokens.S16);

        // Direction selector
        LinearLayout dirRow = ui.hRow(); dirRow.setGravity(Gravity.CENTER);
        final boolean[] toTzs = {true};
        Button dirBtn = ui.pillBtn(code+" → TZS", Tokens.BLUE, Color.WHITE);
        dirBtn.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ui.dp(40)));
        dirRow.addView(dirBtn); sheet.addView(dirRow); ui.spacer(sheet,Tokens.S10);

        sheet.addView(ui.fieldLabel("AMOUNT"));
        ui.spacer(sheet,Tokens.S6);
        EditText amtInput = ui.styledInput("1", InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        sheet.addView(amtInput); ui.spacer(sheet,Tokens.S14);

        LinearLayout resBox = ui.resultBox();
        TextView resTv = ui.mono("0.00 TZS", Tokens.TEXT_3XL, Tokens.GREEN, true);
        resTv.setGravity(Gravity.CENTER); resBox.addView(resTv);
        ui.spacer(resBox,Tokens.S4);
        TextView invTv = ui.tv("", Tokens.TEXT_XS, Tokens.onSurfaceVar, false);
        invTv.setGravity(Gravity.CENTER); resBox.addView(invTv);
        sheet.addView(resBox);

        // Direction toggle
        dirBtn.setOnClickListener(v -> {
            toTzs[0]=!toTzs[0];
            dirBtn.setText(toTzs[0]?(code+" → TZS"):("TZS → "+code));
            recalcSheet(amtInput, resTv, invTv, rate, code, toTzs[0]);
        });

        amtInput.addTextChangedListener(ui.afterChanged(s -> recalcSheet(amtInput, resTv, invTv, rate, code, toTzs[0])));

        // Copy result button
        ui.spacer(sheet,Tokens.S12);
        Button copyBtn = ui.solidBtn("📋  Copy Result", Tokens.surfaceVar, Tokens.onSurface);
        copyBtn.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ui.dp(Tokens.TOUCH_TARGET)));
        copyBtn.setOnClickListener(v -> {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm!=null) cm.setPrimaryClip(android.content.ClipData.newPlainText("conv",resTv.getText().toString()));
            ui.snack(rootFrame,"Copied!",Tokens.BLUE);
        });
        sheet.addView(copyBtn);

        rootFrame.addView(sheet);
        sheet.setTranslationY(ui.dp(600));
        sheet.animate().translationY(0f).setDuration(320).setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
        dim.animate().alpha(1f).setDuration(250).start();

        Runnable dismiss = () -> {
            sheet.animate().translationY(ui.dp(600)).setDuration(260).start();
            dim.animate().alpha(0f).setDuration(200).withEndAction(()->{rootFrame.removeView(sheet);rootFrame.removeView(dim);}).start();
        };
        dim.setOnClickListener(v->dismiss.run()); closeBtn.setOnClickListener(v->dismiss.run());
        amtInput.post(amtInput::requestFocus);
        recalcSheet(amtInput, resTv, invTv, rate, code, toTzs[0]);
    }

    private void recalcSheet(EditText input, TextView result, TextView inv, double rate, String code, boolean toTzs) {
        try {
            double amt = Double.parseDouble(input.getText().toString().replace(",","").trim());
            if (toTzs) {
                result.setText(fmtTzs.format(amt*rate)+" TZS");
                if (rate>0) inv.setText("100 TZS  =  "+String.format(Locale.US,"%.4f %s",100.0/rate,code));
            } else {
                if (rate>0) { result.setText(String.format(Locale.US,"%.4f %s",amt/rate,code)); inv.setText(fmtTzs.format(rate)+" TZS  =  1 "+code); }
            }
        } catch(Exception e) { result.setText("—"); inv.setText(""); }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  METAL DETAIL SHEET
    // ════════════════════════════════════════════════════════════════════════
    void showMetalSheet(String code) {
        Double rateVal = rates.get(code);
        if (rateVal==null) { ui.snack(rootFrame,"Rate not yet available",Tokens.RED); return; }
        final double rate = rateVal;
        final boolean isGold = "XAU".equals(code);
        int accent = isGold ? Tokens.GOLD : Color.parseColor("#C0C0C0");

        View dim = new View(this);
        dim.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT,MATCH_PARENT));
        dim.setBackgroundColor(0xAA000000); dim.setAlpha(0f); rootFrame.addView(dim);

        LinearLayout sheet = ui.vCol();
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(Tokens.surface);
        sheetBg.setCornerRadii(new float[]{ui.dp(Tokens.R20),ui.dp(Tokens.R20),ui.dp(Tokens.R20),ui.dp(Tokens.R20),0,0,0,0});
        sheetBg.setStroke(ui.dp(1),Tokens.withAlpha(accent,80)); sheet.setBackground(sheetBg);
        sheet.setPadding(ui.dp(Tokens.S20),ui.dp(Tokens.S20),ui.dp(Tokens.S20),ui.dp(Tokens.S32));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT);
        slp.gravity=Gravity.BOTTOM; sheet.setLayoutParams(slp);

        View handle = new View(this); LinearLayout.LayoutParams hlp=new LinearLayout.LayoutParams(ui.dp(40),ui.dp(4));
        hlp.gravity=Gravity.CENTER_HORIZONTAL; hlp.bottomMargin=ui.dp(Tokens.S16); handle.setLayoutParams(hlp);
        GradientDrawable hd=new GradientDrawable(); hd.setCornerRadius(ui.dp(2)); hd.setColor(Tokens.withAlpha(accent,80)); handle.setBackground(hd);
        sheet.addView(handle);

        LinearLayout header=ui.hRow();
        TextView ttl=ui.tv((isGold?"🥇 GOLD (XAU)":"🥈 SILVER (XAG)"), Tokens.TEXT_XL, accent, true);
        ttl.setLayoutParams(ui.wt(1f)); header.addView(ttl);
        Button closeBtn=ui.ghostBtn("✕",Tokens.onSurfaceVar); header.addView(closeBtn); sheet.addView(header);
        ui.spacer(sheet,Tokens.S4);
        sheet.addView(ui.tv("Enter any weight to see TZS value",Tokens.TEXT_XS,Tokens.onSurfaceVar,false));
        ui.spacer(sheet,Tokens.S16);

        // Weight unit selector
        sheet.addView(ui.fieldLabel("UNIT")); ui.spacer(sheet,Tokens.S6);
        final String[] UNITS = {"Troy Ounce","Gram","Kilogram"};
        final double[] FACTORS = {1.0, 1.0/31.1035, 1000.0/31.1035};
        Spinner unitSpin = ui.styledSpinner(UNITS); sheet.addView(unitSpin); ui.spacer(sheet,Tokens.S12);

        sheet.addView(ui.fieldLabel("WEIGHT")); ui.spacer(sheet,Tokens.S6);
        EditText wtInput = ui.styledInput("1",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        sheet.addView(wtInput); ui.spacer(sheet,Tokens.S14);

        LinearLayout resBox = ui.resultBox();
        TextView resTv = ui.mono("—", Tokens.TEXT_3XL, accent, true);
        resTv.setGravity(Gravity.CENTER); resBox.addView(resTv);
        ui.spacer(resBox,Tokens.S6);
        // Convenience breakdown
        LinearLayout breakdownRow = ui.hRow(); breakdownRow.setGravity(Gravity.CENTER);
        TextView perGramTv = ui.tv("", Tokens.TEXT_XS, Tokens.onSurfaceVar, false);
        perGramTv.setGravity(Gravity.CENTER); breakdownRow.addView(perGramTv);
        resBox.addView(breakdownRow); sheet.addView(resBox);

        Runnable recalc = () -> {
            try {
                double wt=Double.parseDouble(wtInput.getText().toString().replace(",","").trim());
                int unit=unitSpin.getSelectedItemPosition();
                double ozEquiv = wt * FACTORS[unit];
                double tzs = ozEquiv * rate;
                resTv.setText(fmtTzs.format(tzs)+" TZS");
                double pg=rate/31.1035, pk=pg*1000;
                perGramTv.setText("1g = "+fmtTzs.format(pg)+" TZS  ·  1kg = "+fmtTzs.format(pk)+" TZS");
            } catch(Exception e){resTv.setText("—");}
        };
        wtInput.addTextChangedListener(ui.afterChanged(s->recalc.run()));
        unitSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){recalc.run();}
            public void onNothingSelected(AdapterView<?> p){}
        });

        rootFrame.addView(sheet);
        sheet.setTranslationY(ui.dp(700));
        sheet.animate().translationY(0f).setDuration(320).setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
        dim.animate().alpha(1f).setDuration(250).start();
        Runnable dismiss=()->{sheet.animate().translationY(ui.dp(700)).setDuration(260).start(); dim.animate().alpha(0f).setDuration(200).withEndAction(()->{rootFrame.removeView(sheet);rootFrame.removeView(dim);}).start();};
        dim.setOnClickListener(v->dismiss.run()); closeBtn.setOnClickListener(v->dismiss.run());
        wtInput.post(()->{wtInput.requestFocus(); recalc.run();});
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NETWORK — FETCH RATES
    // ════════════════════════════════════════════════════════════════════════
    void fetchRates() {
        if (fetching) return;
        fetching=true; pulseTopDot(true);
        exec.execute(()->{
            boolean ok=false; String err=null;
            try {
                prevRates.clear(); prevRates.putAll(rates);
                JSONObject fj=new JSONObject(httpGet(FX_URL));
                if ("success".equals(fj.optString("result"))) {
                    JSONObject cr=fj.getJSONObject("conversion_rates");
                    double usd=cr.getDouble("TZS");
                    rates.put("USD",usd); rates.put("TZS",1.0);
                    // Core + extended currencies
                    String[] toFetch={"EUR","GBP","JPY","CNY","INR","AED","ZAR","KES","UGX","RWF",
                                      "CAD","CHF","SGD","MYR","SAR","QAR","BRL","MXN","NGN","EGP","ETB"};
                    for (String c : toFetch) {
                        if (cr.has(c)){double x=cr.getDouble(c); if(x>0) rates.put(c,usd/x);}
                    }
                    try {
                        JSONObject mj=new JSONObject(httpGet(MTL_URL));
                        if (mj.optBoolean("success")){
                            JSONObject mr=mj.getJSONObject("rates");
                            if(mr.has("XAU")){double v=mr.getDouble("XAU"); if(v>0) rates.put("XAU",(1.0/v)*usd);}
                            if(mr.has("XAG")){double v=mr.getDouble("XAG"); if(v>0) rates.put("XAG",(1.0/v)*usd);}
                        }
                    } catch(Exception e){ rates.put("XAU",usd*3_250.0); rates.put("XAG",usd*35.0); }
                    saveRates(); checkAlerts(); ok=true;
                } else { err=fj.optString("error-type","API error"); }
            } catch(Exception e){ err=e.getMessage(); }
            final boolean success=ok;
            handler.post(()->{
                fetching=false; pulseTopDot(false);
                if (success) {
                    if (prefs.getBoolean(AppPrefs.KEY_NOTIFY,true)){
                        Double u=rates.get("USD"),eu=rates.get("EUR"),g=rates.get("GBP");
                        if(u!=null&&eu!=null&&g!=null) AlertNotificationManager.postRateUpdate(this,u,eu,g);
                    }
                } else {
                    ui.snack(rootFrame,"Offline — showing cached rates",Tokens.ORANGE);
                    if(topTime!=null) topTime.setText("Offline");
                }
                updateAllDisplays();
            });
        });
    }

    String httpGet(String urlStr) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(urlStr).openConnection();
        c.setRequestMethod("GET"); c.setConnectTimeout(15_000); c.setReadTimeout(15_000);
        try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()))){
            StringBuilder sb=new StringBuilder(); String line;
            while((line=br.readLine())!=null) sb.append(line);
            return sb.toString();
        } finally{c.disconnect();}
    }

    void checkAlerts() {
        List<JSONObject> alerts=loadAlerts();
        for (JSONObject a:alerts) {
            try {
                String code=a.getString("currency"); double target=a.getDouble("target"); int cond=a.getInt("cond");
                Double cur=rates.get(code); if(cur==null) continue;
                boolean hit=(cond==0&&cur>=target)||(cond==1&&cur<=target);
                if(hit){final double c=cur; handler.post(()->AlertNotificationManager.postPriceAlert(this,code,cond,target,c));}
            } catch(Exception ignored){}
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UPDATE ALL DISPLAYS
    // ════════════════════════════════════════════════════════════════════════
    void updateAllDisplays() {
        Double usd=rates.get("USD"),eur=rates.get("EUR"),gold=rates.get("XAU");
        if(topUsd!=null&&usd!=null)   topUsd.setText("USD "+fmtTzs.format(usd));
        if(topEur!=null&&eur!=null)   topEur.setText("EUR "+fmtTzs.format(eur));
        if(topGold!=null&&gold!=null) topGold.setText("XAU "+goldShort(gold));
        if(topTime!=null) topTime.setText(lastUpdateText());
        homeScreen.update();
        convertScreen.recalc();
        marketsScreen.update();
        marketsScreen.refreshAlertsList();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SHARE
    // ════════════════════════════════════════════════════════════════════════
    void shareRates() {
        StringBuilder sb=new StringBuilder("🇹🇿 Tanzania Exchange Rates\n");
        sb.append(new SimpleDateFormat("dd MMM yyyy HH:mm",Locale.getDefault()).format(new Date())).append("\n\n");
        for (String c:new String[]{"USD","EUR","GBP","KES","UGX","RWF","ZAR","AED","CNY","XAU","XAG"}){
            Double r=rates.get(c); if(r!=null) sb.append("1 ").append(c).append(" = ").append(fmtTzs.format(r)).append(" TZS\n");
        }
        sb.append("\nvia FXetcher 🇹🇿");
        Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,sb.toString());
        startActivity(Intent.createChooser(i,"Share Rates"));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PERSISTENCE
    // ════════════════════════════════════════════════════════════════════════
    void setFallbackRates() {
        rates.put("TZS",1.0);        rates.put("USD",2_600.0); rates.put("EUR",2_820.0);
        rates.put("GBP",3_310.0);    rates.put("JPY",17.3);    rates.put("CNY",360.0);
        rates.put("INR",31.2);       rates.put("AED",708.0);   rates.put("ZAR",142.0);
        rates.put("KES",20.1);       rates.put("UGX",0.70);    rates.put("RWF",1.90);
        rates.put("XAU",8_450_000.0); rates.put("XAG",92_000.0);
        rates.put("CAD",1_910.0);    rates.put("CHF",2_900.0); rates.put("SGD",1_960.0);
        rates.put("MYR",580.0);      rates.put("SAR",693.0);   rates.put("QAR",714.0);
        rates.put("BRL",470.0);      rates.put("MXN",130.0);   rates.put("NGN",1.65);
        rates.put("EGP",53.0);       rates.put("ETB",45.0);
    }

    void loadSavedRates() {
        try {
            String raw=prefs.getString(AppPrefs.KEY_RATES,""); if(raw.isEmpty()) return;
            JSONObject j=new JSONObject(raw);
            for(String c:CODES) if(j.has(c)) rates.put(c,j.getDouble(c));
            rates.put("TZS",1.0);
        } catch(Exception ignored){}
    }

    void saveRates() {
        try {
            JSONObject j=new JSONObject();
            for(Map.Entry<String,Double> e:rates.entrySet()) j.put(e.getKey(),e.getValue());
            prefs.edit().putString(AppPrefs.KEY_RATES,j.toString()).putLong(AppPrefs.KEY_UPDATE,System.currentTimeMillis()).apply();
        } catch(Exception ignored){}
    }

    void scheduleAutoRefresh() {
        if(autoRefreshFuture!=null){autoRefreshFuture.cancel(false);autoRefreshFuture=null;}
        if(!prefs.getBoolean(AppPrefs.KEY_AUTO,true)) return;
        long ms=prefs.getInt(AppPrefs.KEY_INTERVAL,300_000);
        autoRefreshFuture=exec.scheduleAtFixedRate(()->handler.post(this::fetchRates),ms,ms,TimeUnit.MILLISECONDS);
    }

    String lastUpdateText() {
        long t=prefs.getLong(AppPrefs.KEY_UPDATE,0); if(t==0) return "—";
        long ago=System.currentTimeMillis()-t;
        if(ago<60_000) return "live";
        if(ago<3_600_000) return (ago/60_000)+"m ago";
        return new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(t));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ════════════════════════════════════════════════════════════════════════
    void pulseTopDot(boolean on) {
        if(topDot==null) return;
        if(dotAnim!=null) dotAnim.cancel();
        if(on) dotAnim=ui.pulseDot(topDot,true);
        else { ui.pulseDot(topDot,false); dotAnim=null; }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════
    double safeRate(String code, double fallback) {
        Double v=rates.get(code); return (v!=null&&v>0)?v:fallback;
    }

    double convert(double amount, String from, String to) {
        if(from.equals(to)) return amount;
        Double fr=rates.get(from),tr=rates.get(to);
        if(fr==null||tr==null) return 0;
        if("TZS".equals(from)) return tr>0?amount/tr:0;
        if("TZS".equals(to))   return amount*fr;
        return tr>0?(amount*fr)/tr:0;
    }

    List<JSONObject> loadAlerts() {
        List<JSONObject> list=new ArrayList<>();
        try{JSONArray arr=new JSONArray(prefs.getString(AppPrefs.KEY_ALERTS,"[]")); for(int i=0;i<arr.length();i++) list.add(arr.getJSONObject(i));}catch(Exception ignored){}
        return list;
    }

    void saveAlert(String code,double target,int cond){
        try{List<JSONObject> list=loadAlerts(); JSONObject a=new JSONObject(); a.put("currency",code); a.put("target",target); a.put("cond",cond); a.put("enabled",true); list.add(a); JSONArray arr=new JSONArray(); for(JSONObject o:list) arr.put(o); prefs.edit().putString(AppPrefs.KEY_ALERTS,arr.toString()).apply();}catch(Exception ignored){}
    }

    void deleteAlert(int idx){
        try{List<JSONObject> list=loadAlerts(); if(idx>=0&&idx<list.size()) list.remove(idx); JSONArray arr=new JSONArray(); for(JSONObject o:list) arr.put(o); prefs.edit().putString(AppPrefs.KEY_ALERTS,arr.toString()).apply();}catch(Exception ignored){}
    }
}

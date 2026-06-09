package com.willykez.fxetcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.*;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

class HomeScreen {

    private final FXetcherApp app;
    private final UiKit ui;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private SwipeRefreshLayout swipeRefresh;

    // Live rates section
    private final HashMap<String,LinearLayout> rateRowMap = new HashMap<>();
    private LinearLayout shimmerContainer;
    private LinearLayout ratesContainer;
    private boolean shimmerShown = true;

    // Metals
    private TextView goldPriceTv, silverPriceTv;

    // BoT table
    private static class BotRate {
        final String currency, buying, selling;
        BotRate(String c,String b,String s){currency=c;buying=b;selling=s;}
    }
    private final List<BotRate> botRates = new ArrayList<>();
    private LinearLayout bankTableBody;
    private TextView bankStatusTv;

    HomeScreen(FXetcherApp app) {
        this.app = app;
        this.ui  = app.ui;
        loadBotRatesCache();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    View build() {
        swipeRefresh = new SwipeRefreshLayout(app);
        swipeRefresh.setBackgroundColor(Tokens.bg);
        swipeRefresh.setColorSchemeColors(Tokens.GOLD, Tokens.BLUE, Tokens.GREEN);
        swipeRefresh.setOnRefreshListener(() -> {
            app.fetchRates();
            fetchBotRates();
        });

        ScrollView sv = new ScrollView(app);
        sv.setFillViewport(true);

        LinearLayout content = ui.vCol();
        content.setPadding(ui.dp(Tokens.S14), ui.dp(Tokens.S12),
                ui.dp(Tokens.S14), ui.dp(Tokens.NAV_HEIGHT + 16));
        
        // Pull-to-refresh Hint Text
        TextView hintTv = ui.tv("↓ Pull down to refresh for latest rates", Tokens.TEXT_XS, Tokens.surfaceTint, false);
        hintTv.setGravity(Gravity.CENTER);
        content.addView(hintTv);
        ui.spacer(content, Tokens.S12);

        buildLiveRatesCard(content);
        buildAfricanCard(content);
        buildMetalsCard(content);
        buildBotCard(content);

        sv.addView(content);
        swipeRefresh.addView(sv);
        return swipeRefresh;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── Live Rates Card ───────────────────────────────────────────────────────

    private void buildLiveRatesCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);

        // Header with Share Button
        LinearLayout hdr = ui.hRow();
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        TextView iconTv = ui.tv("📈", 20, Tokens.BLUE, false);
        hdr.addView(iconTv);
        ui.spacerH(hdr, Tokens.S8);

        LinearLayout titleCol = ui.vCol();
        titleCol.setLayoutParams(ui.wt(1f));
        titleCol.addView(ui.tv("Live Rates vs TZS", Tokens.TEXT_MD, Tokens.BLUE, true));
        titleCol.addView(ui.tv("Tap any row for quick conversion", Tokens.TEXT_XS, Tokens.onSurfaceVar, false));
        hdr.addView(titleCol);

        Button shareBtn = ui.ghostBtn("📤 Share", Tokens.BLUE);
        shareBtn.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4));
        shareBtn.setOnClickListener(v -> app.shareRates());
        hdr.addView(shareBtn);

        card.addView(hdr);
        ui.spacer(card, Tokens.S12);
        card.addView(ui.divider());
        ui.spacer(card, Tokens.S10);

        // Shimmer placeholder (visible until first successful fetch)
        shimmerContainer = ui.vCol();
        for (int i = 0; i < 5; i++) {
            LinearLayout sRow = ui.hRow();
            sRow.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ui.dp(52)));
            LinearLayout.LayoutParams slp = (LinearLayout.LayoutParams) sRow.getLayoutParams();
            slp.bottomMargin = ui.dp(Tokens.S6);
            sRow.setLayoutParams(slp);
            sRow.setBackground(ui.roundRect(ui.dp(Tokens.R12), Tokens.surfaceVar, ui.dp(1), Tokens.outline));
            sRow.setPadding(ui.dp(Tokens.S12), 0, ui.dp(Tokens.S12), 0);

            View bar = new View(app);
            bar.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(3), ui.dp(32)));
            GradientDrawable bd = new GradientDrawable();
            bd.setCornerRadius(ui.dp(2)); bd.setColor(Tokens.surfaceVar);
            bar.setBackground(bd);
            sRow.addView(bar); ui.spacerH(sRow, Tokens.S12);
            sRow.addView(ui.shimmerBar(36, 36)); ui.spacerH(sRow, Tokens.S10);
            LinearLayout tc = ui.vCol(); tc.setLayoutParams(ui.wt(1f));
            tc.addView(ui.shimmerBar(0, 9)); ui.spacer(tc, 4); tc.addView(ui.shimmerBar(60, 7));
            sRow.addView(tc); ui.spacerH(sRow, Tokens.S8);
            sRow.addView(ui.shimmerBar(70, 11));
            shimmerContainer.addView(sRow);
        }
        card.addView(shimmerContainer);

        // Real rows container (hidden until first fetch)
        ratesContainer = ui.vCol();
        ratesContainer.setVisibility(View.GONE);

        String[] liveCurrencies = {"USD","EUR","GBP","JPY","CNY","INR","AED","ZAR","KES","CAD","CHF","SGD"};
        for (int i = 0; i < liveCurrencies.length; i++) {
            String code = liveCurrencies[i];
            int accent  = Tokens.accentByIndex(i);
            Double r    = app.rates.get(code);
            String rStr = r != null ? app.fmtTzs.format(r) + " TZS" : "Loading…";

            LinearLayout row = ui.rateRow(
                    app.FLAGS.getOrDefault(code,"💱"),
                    app.NAMES.getOrDefault(code, code),
                    code, rStr, accent,
                    v -> app.showQuickConvert(code));
            rateRowMap.put(code, row);
            ratesContainer.addView(row);
        }
        card.addView(ratesContainer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── East African Card ─────────────────────────────────────────────────────

    private void buildAfricanCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card, "🌍", "East African Currencies",
                "Regional currencies vs Tanzanian Shilling", Tokens.ORANGE);
        ui.spacer(card, Tokens.S12);
        card.addView(ui.divider());
        ui.spacer(card, Tokens.S10);

        String[] codes   = {"KES","UGX","RWF","ZAR","AED","NGN","EGP","ETB"};
        String[] labels  = {"Kenya","Uganda","Rwanda","South Africa","UAE","Nigeria","Egypt","Ethiopia"};
        int[]    accents = {Tokens.GREEN, Tokens.ORANGE, Tokens.BLUE, Tokens.TEAL, Tokens.PURPLE, Tokens.AMBER, Tokens.RED, Tokens.GREEN};

        for (int i = 0; i < codes.length; i++) {
            String code = codes[i];
            int accent  = accents[i];
            Double r    = app.rates.get(code);
            boolean sml = CurrencyMeta.isSmallRate(code);
            String  rStr = r != null
                    ? (sml ? app.fmtSml.format(r) : app.fmtTzs.format(r)) + " TZS"
                    : "—";

            LinearLayout row = ui.rateRow(
                    app.FLAGS.getOrDefault(code,"🌍"),
                    labels[i], code, rStr, accent,
                    v -> app.showQuickConvert(code));
            card.addView(row);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── Metals Card ───────────────────────────────────────────────────────────

    private void buildMetalsCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card, "💰", "Precious Metals",
                "Live prices per troy ounce in TZS", Tokens.GOLD);
        ui.spacer(card, Tokens.S12);
        card.addView(ui.divider());
        ui.spacer(card, Tokens.S10);

        LinearLayout goldRow = metalRow("#FFD700", "🥇", "GOLD", "XAU", app.rates.get("XAU"));
        goldPriceTv = extractPriceTv(goldRow);
        card.addView(goldRow);
        ui.spacer(card, Tokens.S8);

        LinearLayout silverRow = metalRow("#C0C0C0", "🥈", "SILVER", "XAG", app.rates.get("XAG"));
        silverPriceTv = extractPriceTv(silverRow);
        card.addView(silverRow);
    }

    private LinearLayout metalRow(String accent, String icon, String label,
                                   String code, Double rate) {
        LinearLayout row = ui.hRow();
        row.setPadding(ui.dp(Tokens.S14), ui.dp(Tokens.S14),
                ui.dp(Tokens.S14), ui.dp(Tokens.S14));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(ui.dp(Tokens.R14));
        bg.setColor(Tokens.surfaceVar);
        bg.setStroke(ui.dp(2), android.graphics.Color.parseColor(accent));
        row.setBackground(ui.rippleOver(bg));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> app.showQuickConvert(code));

        View bar = new View(app);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(4), ui.dp(40)));
        GradientDrawable bd = new GradientDrawable();
        bd.setCornerRadius(ui.dp(2));
        bd.setColor(android.graphics.Color.parseColor(accent));
        bar.setBackground(bd);
        row.addView(bar);
        ui.spacerH(row, Tokens.S14);

        LinearLayout iconBadge = new LinearLayout(app);
        iconBadge.setGravity(Gravity.CENTER);
        int bs = ui.dp(44);
        iconBadge.setLayoutParams(new LinearLayout.LayoutParams(bs, bs));
        GradientDrawable fd = new GradientDrawable();
        fd.setShape(GradientDrawable.OVAL);
        fd.setColor(Tokens.withAlpha(android.graphics.Color.parseColor(accent), 25));
        iconBadge.setBackground(fd);
        iconBadge.addView(ui.tv(icon, 20, android.graphics.Color.parseColor(accent), false));
        row.addView(iconBadge);
        ui.spacerH(row, Tokens.S12);

        LinearLayout labelCol = ui.vCol();
        labelCol.setLayoutParams(ui.wt(1f));
        labelCol.addView(ui.tv(label + " (" + code + ")", Tokens.TEXT_MD,
                android.graphics.Color.parseColor(accent), true));
        labelCol.addView(ui.tv("Per troy ounce", Tokens.TEXT_XS, Tokens.onSurfaceVar, false));
        row.addView(labelCol);

        LinearLayout priceCol = ui.vCol();
        priceCol.setGravity(Gravity.END);
        String rStr = rate != null ? app.fmtTzs.format(rate) + " TZS" : "Loading…";
        TextView priceTv = ui.mono(rStr, Tokens.TEXT_MD, Tokens.onSurface, true);
        priceTv.setGravity(Gravity.END);
        priceCol.addView(priceTv);
        priceCol.addView(ui.tv("troy oz", Tokens.TEXT_XS, Tokens.surfaceTint, false));
        row.addView(priceCol);

        row.setTag(priceTv);
        return row;
    }

    private TextView extractPriceTv(LinearLayout row) { return (TextView) row.getTag(); }

    // ─────────────────────────────────────────────────────────────────────────
    // ── BoT Bank Card & Cache ─────────────────────────────────────────────────

    private void loadBotRatesCache() {
        try {
            String cache = app.prefs.getString("bot_rates_cache", "[]");
            JSONArray arr = new JSONArray(cache);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                botRates.add(new BotRate(o.getString("c"), o.getString("b"), o.getString("s")));
            }
        } catch (Exception ignored) {}
    }

    private void saveBotRatesCache() {
        try {
            JSONArray arr = new JSONArray();
            for (BotRate r : botRates) {
                JSONObject o = new JSONObject();
                o.put("c", r.currency);
                o.put("b", r.buying);
                o.put("s", r.selling);
                arr.put(o);
            }
            app.prefs.edit().putString("bot_rates_cache", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void buildBotCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card, "🏦", "BoT Official Rates",
                "Bank of Tanzania · bot.go.tz", Tokens.PURPLE);
        ui.spacer(card, Tokens.S10);

        LinearLayout srcRow = ui.hRow();
        LinearLayout srcBadge = new LinearLayout(app);
        srcBadge.setPadding(ui.dp(Tokens.S8), ui.dp(5), ui.dp(Tokens.S8), ui.dp(5));
        GradientDrawable sbd = new GradientDrawable();
        sbd.setCornerRadius(ui.dp(Tokens.R6)); sbd.setColor(Tokens.withAlpha(Tokens.PURPLE, 22));
        sbd.setStroke(ui.dp(1), Tokens.withAlpha(Tokens.PURPLE, 60));
        srcBadge.setBackground(sbd);
        srcBadge.setLayoutParams(ui.wt(1f));
        srcBadge.addView(ui.tv("Official mid-market rates", Tokens.TEXT_XS, Tokens.PURPLE, true));
        srcRow.addView(srcBadge);

        card.addView(srcRow);
        ui.spacer(card, Tokens.S10);

        bankStatusTv = ui.tv(botRates.isEmpty() ? "Pull to fetch official rates" : "Showing cached official rates", 
                Tokens.TEXT_XS, Tokens.onSurfaceVar, false);
        card.addView(bankStatusTv);
        ui.spacer(card, Tokens.S10);

        LinearLayout header = ui.hRow();
        header.setPadding(ui.dp(Tokens.S12), ui.dp(Tokens.S8), ui.dp(Tokens.S12), ui.dp(Tokens.S8));
        GradientDrawable hd = new GradientDrawable();
        hd.setCornerRadius(ui.dp(Tokens.R8)); hd.setColor(Tokens.outlineVar);
        header.setBackground(hd);

        TextView hc = tableCell("Currency"); hc.setTextColor(Tokens.GOLD); header.addView(hc);
        TextView hb = tableCell("Buying");   hb.setTextColor(Tokens.GOLD); header.addView(hb);
        TextView hs = tableCell("Selling");  hs.setTextColor(Tokens.GOLD); header.addView(hs);
        card.addView(header);
        ui.spacer(card, Tokens.S6);

        bankTableBody = ui.vCol();
        card.addView(bankTableBody);

        renderBotTable();
    }

    private TextView tableCell(String text) {
        TextView t = ui.tv(text, Tokens.TEXT_SM, Tokens.GOLD, true);
        t.setLayoutParams(ui.wt(1f));
        return t;
    }

    private void renderBotTable() {
        if (bankTableBody == null) return;
        bankTableBody.removeAllViews();

        if (botRates.isEmpty()) {
            LinearLayout ph = ui.hRow();
            ph.setPadding(ui.dp(Tokens.S12), ui.dp(Tokens.S14),
                    ui.dp(Tokens.S12), ui.dp(Tokens.S14));
            GradientDrawable pd = new GradientDrawable();
            pd.setCornerRadius(ui.dp(Tokens.R8)); pd.setColor(Tokens.surfaceVar);
            ph.setBackground(pd);
            ph.addView(ui.tv("No data — Pull down to fetch", Tokens.TEXT_SM,
                    Tokens.surfaceTint, false));
            bankTableBody.addView(ph);
            return;
        }

        for (int i = 0; i < botRates.size(); i++) {
            BotRate r = botRates.get(i);
            if (r.currency.isEmpty() || r.currency.length() > 4) continue;

            LinearLayout tr = ui.hRow();
            tr.setPadding(ui.dp(Tokens.S12), ui.dp(Tokens.S10),
                    ui.dp(Tokens.S12), ui.dp(Tokens.S10));
            if (i % 2 == 0) {
                GradientDrawable rd = new GradientDrawable();
                rd.setCornerRadius(ui.dp(Tokens.R6)); rd.setColor(Tokens.surfaceVar);
                tr.setBackground(rd);
            }

            LinearLayout codeCol = ui.vCol();
            codeCol.setLayoutParams(ui.wt(1f));
            codeCol.addView(ui.tv(
                    app.FLAGS.getOrDefault(r.currency,"💱") + "  " + r.currency,
                    Tokens.TEXT_SM, Tokens.onSurface, true));
            tr.addView(codeCol);

            TextView buyTv = ui.mono(r.buying, Tokens.TEXT_SM, Tokens.GREEN, true);
            buyTv.setLayoutParams(ui.wt(1f));
            tr.addView(buyTv);

            TextView sellTv = ui.mono(r.selling, Tokens.TEXT_SM, Tokens.RED, true);
            sellTv.setLayoutParams(ui.wt(1f));
            tr.addView(sellTv);

            bankTableBody.addView(tr);
        }
    }

    private void fetchBotRates() {
        if (app.botFetching) return;
        app.botFetching = true;
        handler.post(() -> { if (bankStatusTv!=null) bankStatusTv.setText("Fetching…"); });

        app.exec.execute(() -> {
            List<BotRate> scraped = new ArrayList<>();
            String status;
            try {
                Document doc = Jsoup.connect(app.BOT_URL)
                        .userAgent("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                        .timeout(20_000).get();
                Elements rows = doc.select("table tbody tr");
                if (rows.isEmpty()) rows = doc.select("tr");
                for (Element row : rows) {
                    Elements cols = row.select("td");
                    if (cols.size() < 4) continue;
                    String code    = cols.get(1).text().trim().toUpperCase(Locale.US);
                    String buying  = cols.get(2).text().trim();
                    String selling = cols.get(3).text().trim();
                    if (!code.matches("[A-Z]{3}") || buying.isEmpty()) continue;
                    scraped.add(new BotRate(code, buying, selling));
                }
                status = scraped.isEmpty()
                    ? "⚠️  No rates found (page layout may have changed)"
                    : "✓  " + scraped.size() + " currencies · "
                        + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date());
            } catch (Exception e) {
                status = "⚠️  Offline — showing cached official rates";
            }

            final List<BotRate> res = new ArrayList<>(scraped);
            final String st = status;
            handler.post(() -> {
                app.botFetching = false;
                if (!res.isEmpty()) { 
                    botRates.clear(); 
                    botRates.addAll(res); 
                    saveBotRatesCache(); 
                }
                if (bankStatusTv != null) bankStatusTv.setText(st);
                renderBotTable();
                checkStopRefresh();
            });
        });
    }

    private void checkStopRefresh() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            if (!app.fetching && !app.botFetching) {
                swipeRefresh.setRefreshing(false);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── Update  ───────────────────────────────────────────────────────────────

    void update() {
        if (shimmerShown) {
            shimmerShown = false;
            shimmerContainer.setVisibility(View.GONE);
            ratesContainer.setVisibility(View.VISIBLE);
        }

        for (Map.Entry<String,LinearLayout> e : rateRowMap.entrySet()) {
            String code = e.getKey();
            Double nv = app.rates.get(code);
            if (nv == null) continue;
            Double ov  = app.prevRates.get(code);
            double pct = (ov!=null && ov>0) ? (nv-ov)/ov*100.0 : 0.0;
            boolean up = nv >= (ov != null ? ov : nv);
            ui.updateRateRow(e.getValue(), app.fmtTzs.format(nv) + " TZS", pct, up);
            if (ov != null && ov > 0 && pct != 0) {
                ui.flashRow(e.getValue(), up ? Tokens.GREEN : Tokens.RED, Tokens.surfaceVar);
            }
        }

        Double gold = app.rates.get("XAU"), sil = app.rates.get("XAG");
        if (goldPriceTv   != null && gold != null) goldPriceTv.setText(app.fmtTzs.format(gold) + " TZS");
        if (silverPriceTv != null && sil  != null) silverPriceTv.setText(app.fmtTzs.format(sil) + " TZS");

        checkStopRefresh();
    }
}
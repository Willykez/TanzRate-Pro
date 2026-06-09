package com.willykez.fxetcher;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

// ─────────────────────────────────────────────────────────────────────────────
//  CONVERT SCREEN  v4
// ─────────────────────────────────────────────────────────────────────────────
class ConvertScreen {

    private final FXetcherApp app;
    private final UiKit ui;

    private EditText      convInput;
    private Spinner       convFrom, convTo;
    private TextView      convResult, convRate, convInverse, convUsdRef;
    private LinearLayout  convHistContainer;
    private TextView      convHistEmpty;
    private Button        lastSelectedChip;
    private TextView      multiResultsContainer;
    private boolean       showMulti = false;

    ConvertScreen(FXetcherApp app) { this.app = app; this.ui = app.ui; }

    View build() {
        ScrollView sv = new ScrollView(app);
        sv.setBackgroundColor(Tokens.bg);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout content = ui.vCol();
        content.setPadding(ui.dp(Tokens.S14), ui.dp(Tokens.S12),
                ui.dp(Tokens.S14), ui.dp(Tokens.NAV_HEIGHT + 16));
        sv.addView(content);
        buildConverterCard(content);
        buildMultiResultCard(content);
        buildQuickAmountsCard(content);
        buildHistoryCard(content);
        return sv;
    }

    // ── Converter Card ────────────────────────────────────────────────────────
    private void buildConverterCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card, "💱", "Currency Converter", "Live rates · tap swap to reverse", Tokens.GOLD);
        ui.spacer(card, Tokens.S14);

        card.addView(ui.fieldLabel("AMOUNT"));
        ui.spacer(card, Tokens.S6);
        convInput = ui.styledInput("1", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        convInput.setText("1");
        card.addView(convInput);
        ui.spacer(card, Tokens.S16);

        card.addView(ui.fieldLabel("FROM"));
        ui.spacer(card, Tokens.S6);
        convFrom = buildCurrencySpinner();
        convFrom.setSelection(CurrencyMeta.indexOf(app.prefs.getString(AppPrefs.KEY_PINNED_FROM, "USD")));
        card.addView(convFrom);
        ui.spacer(card, Tokens.S12);

        // Swap row with pin button
        LinearLayout swapRow = ui.hRow();
        swapRow.setGravity(Gravity.CENTER);
        Button swapBtn = ui.pillBtn("⇅  Swap", Tokens.BLUE, 0xFFFFFFFF);
        LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(ui.dp(150), ui.dp(44));
        sbp.rightMargin = ui.dp(Tokens.S8);
        swapBtn.setLayoutParams(sbp);
        swapBtn.setOnClickListener(v -> {
            ObjectAnimator.ofFloat(swapBtn,"rotation",0f,180f).setDuration(280).start();
            int f = convFrom.getSelectedItemPosition();
            int t = convTo.getSelectedItemPosition();
            convFrom.setSelection(t); convTo.setSelection(f);
        });
        swapRow.addView(swapBtn);

        Button pinBtn = ui.solidBtn("📌 Pin Pair", Tokens.withAlpha(Tokens.GOLD,40), Tokens.GOLD);
        pinBtn.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(120), ui.dp(44)));
        pinBtn.setOnClickListener(v -> {
            String from = CurrencyMeta.CODES[convFrom.getSelectedItemPosition()];
            String to   = CurrencyMeta.CODES[convTo.getSelectedItemPosition()];
            app.prefs.edit()
                .putString(AppPrefs.KEY_PINNED_FROM, from)
                .putString(AppPrefs.KEY_PINNED_TO,   to).apply();
            ui.snack(app.rootFrame, "📌 Pinned: " + from + " → " + to, Tokens.GOLD);
        });
        swapRow.addView(pinBtn);
        card.addView(swapRow);
        ui.spacer(card, Tokens.S12);

        card.addView(ui.fieldLabel("TO"));
        ui.spacer(card, Tokens.S6);
        convTo = buildCurrencySpinner();
        convTo.setSelection(CurrencyMeta.indexOf(app.prefs.getString(AppPrefs.KEY_PINNED_TO, "TZS")));
        card.addView(convTo);
        ui.spacer(card, Tokens.S20);

        // Result box
        LinearLayout resultBox = ui.resultBox();
        convResult = ui.mono("—", Tokens.TEXT_4XL, Tokens.GREEN, true);
        convResult.setGravity(Gravity.CENTER);
        resultBox.addView(convResult);
        ui.spacer(resultBox, Tokens.S6);
        convRate = ui.tv("", Tokens.TEXT_SM, Tokens.onSurfaceVar, false);
        convRate.setGravity(Gravity.CENTER);
        resultBox.addView(convRate);
        convInverse = ui.tv("", Tokens.TEXT_XS, Tokens.surfaceTint, false);
        convInverse.setGravity(Gravity.CENTER);
        convInverse.setPadding(0, ui.dp(2), 0, 0);
        resultBox.addView(convInverse);
        // USD reference line
        convUsdRef = ui.tv("", Tokens.TEXT_XS, Tokens.withAlpha(Tokens.GOLD, 160), false);
        convUsdRef.setGravity(Gravity.CENTER);
        convUsdRef.setPadding(0, ui.dp(3), 0, 0);
        resultBox.addView(convUsdRef);
        card.addView(resultBox);
        ui.spacer(card, Tokens.S16);

        // Action buttons
        LinearLayout btnRow = ui.hRow();
        btnRow.setWeightSum(3f);
        Button copyBtn = ui.solidBtn("📋", Tokens.surfaceVar, Tokens.onSurface);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ui.dp(Tokens.TOUCH_TARGET+4), 1f);
        cp.rightMargin = ui.dp(Tokens.S6); copyBtn.setLayoutParams(cp);
        copyBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm!=null) cm.setPrimaryClip(ClipData.newPlainText("rate", convResult.getText().toString()));
            ui.snack(app.rootFrame, "Copied to clipboard", Tokens.BLUE);
        });
        btnRow.addView(copyBtn);

        Button shareConvBtn = ui.solidBtn("📤", Tokens.surfaceVar, Tokens.ORANGE);
        LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(0, ui.dp(Tokens.TOUCH_TARGET+4), 1f);
        scp.rightMargin = ui.dp(Tokens.S6); shareConvBtn.setLayoutParams(scp);
        shareConvBtn.setOnClickListener(v -> shareConversion());
        btnRow.addView(shareConvBtn);

        Button saveBtn = ui.solidBtn("✓ Save", Tokens.GREEN, 0xFF0A0E27);
        saveBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ui.dp(Tokens.TOUCH_TARGET+4), 1f));
        saveBtn.setOnClickListener(v -> doConvertAndSave());
        btnRow.addView(saveBtn);
        card.addView(btnRow);

        // Multi-currency toggle
        ui.spacer(card, Tokens.S10);
        Button multiBtn = ui.outlineBtn("🌐  Show Multi-Currency Results", Tokens.TEAL);
        multiBtn.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ui.dp(Tokens.TOUCH_TARGET)));
        multiBtn.setOnClickListener(v -> {
            showMulti = !showMulti;
            multiBtn.setText(showMulti ? "🌐  Hide Multi-Currency" : "🌐  Show Multi-Currency Results");
            updateMultiResults();
            // find the multi card and toggle visibility
            if (multiResultsContainer != null)
                ((View)multiResultsContainer.getParent().getParent()).setVisibility(showMulti ? View.VISIBLE : View.GONE);
        });
        card.addView(multiBtn);

        convInput.addTextChangedListener(ui.afterChanged(s -> recalc()));
        AdapterView.OnItemSelectedListener sl = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { recalc(); updateMultiResults(); }
            public void onNothingSelected(AdapterView<?> p) {}
        };
        convFrom.setOnItemSelectedListener(sl);
        convTo.setOnItemSelectedListener(sl);
        recalc();
    }

    // ── Multi-Currency Results Card ───────────────────────────────────────────
    private void buildMultiResultCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        card.setVisibility(View.GONE);
        ui.sectionHeader(card, "🌐", "Multi-Currency Results", "Same amount in all major currencies", Tokens.TEAL);
        ui.spacer(card, Tokens.S10);

        multiResultsContainer = new TextView(app); // placeholder tag
        multiResultsContainer.setTag("multiParent");

        LinearLayout grid = ui.vCol();
        grid.setTag("multiGrid");
        card.addView(grid);

        // store card ref in multiResultsContainer as tag trick
        multiResultsContainer.setTag(card);
    }

    private void updateMultiResults() {
        if (!showMulti || multiResultsContainer == null) return;
        LinearLayout card = (LinearLayout) multiResultsContainer.getTag();
        if (card == null) return;
        // find grid by tag
        LinearLayout grid = null;
        for (int i = 0; i < card.getChildCount(); i++) {
            if ("multiGrid".equals(card.getChildAt(i).getTag())) {
                grid = (LinearLayout) card.getChildAt(i); break;
            }
        }
        if (grid == null) return;
        grid.removeAllViews();

        double amount;
        try {
            amount = Double.parseDouble(convInput.getText().toString().replace(",","").trim());
        } catch (Exception e) { return; }
        String from = CurrencyMeta.CODES[convFrom.getSelectedItemPosition()];

        String[] targets = {"USD","EUR","GBP","KES","UGX","ZAR","AED","CNY","INR","CAD","CHF","SAR","NGN"};
        for (int i=0; i<targets.length; i++) {
            String tc = targets[i];
            if (tc.equals(from)) continue;
            double res = app.convert(amount, from, tc);
            if (res <= 0) continue;
            DecimalFormat f = res>=1000 ? new DecimalFormat("#,##0.00") : new DecimalFormat("#,##0.00##");

            LinearLayout row = ui.hRow();
            row.setPadding(ui.dp(Tokens.S10), ui.dp(Tokens.S10), ui.dp(Tokens.S10), ui.dp(Tokens.S10));
            if (i%2==0) { GradientDrawable d=new GradientDrawable(); d.setCornerRadius(ui.dp(Tokens.R8)); d.setColor(Tokens.surfaceVar); row.setBackground(d); }

            TextView flagTv = ui.tv(app.FLAGS.getOrDefault(tc,"💱")+" "+tc, Tokens.TEXT_SM, Tokens.onSurfaceVar, true);
            flagTv.setLayoutParams(ui.wt(1f));
            row.addView(flagTv);

            TextView valTv = ui.mono(f.format(res), Tokens.TEXT_SM, Tokens.accentByIndex(i), true);
            row.addView(valTv);

            // copy on tap
            row.setClickable(true); row.setFocusable(true);
            String copyVal = f.format(res) + " " + tc;
            row.setOnClickListener(v -> {
                ClipboardManager cm2 = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm2!=null) cm2.setPrimaryClip(ClipData.newPlainText("conv",copyVal));
                ui.snack(app.rootFrame, "Copied: "+copyVal, Tokens.TEAL);
            });
            grid.addView(row);
        }
    }

    void recalc() {
        if (convInput == null) return;
        try {
            String raw = convInput.getText().toString().replace(",","").trim();
            if (raw.isEmpty() || raw.equals(".")) { clear(); return; }
            double amount = Double.parseDouble(raw);
            String from   = CurrencyMeta.CODES[convFrom.getSelectedItemPosition()];
            String to     = CurrencyMeta.CODES[convTo.getSelectedItemPosition()];
            double result = app.convert(amount, from, to);

            DecimalFormat f = (result >= 1000)
                    ? new DecimalFormat("#,##0.00")
                    : new DecimalFormat("#,##0.00####");
            String resText = f.format(result) + " " + to;
            convResult.setText(resText);

            int len = resText.length();
            if (len > 18)      convResult.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_LG);
            else if (len > 14) convResult.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_XL);
            else if (len > 10) convResult.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_2XL);
            else if (len > 8)  convResult.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_3XL);
            else               convResult.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_4XL);

            DecimalFormat rf = new DecimalFormat("#,##0.00####");
            convRate.setText("1 " + from + "  =  " + rf.format(app.convert(1,from,to)) + " " + to);
            convInverse.setText("1 " + to + "  =  " + rf.format(app.convert(1,to,from)) + " " + from);

            // USD reference (if neither is TZS/USD show USD equiv)
            if (!from.equals("USD") && !to.equals("USD") && !from.equals("TZS")) {
                double usdEquiv = app.convert(amount, from, "USD");
                convUsdRef.setText("≈ " + new DecimalFormat("#,##0.00").format(usdEquiv) + " USD");
            } else { convUsdRef.setText(""); }
        } catch (Exception e) { clear(); }
    }

    private void clear() {
        if (convResult!=null) { convResult.setText("—"); convResult.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_4XL); }
        if (convRate!=null) convRate.setText("");
        if (convInverse!=null) convInverse.setText("");
        if (convUsdRef!=null) convUsdRef.setText("");
    }

    private void doConvertAndSave() {
        recalc();
        try {
            String raw = convInput.getText().toString().replace(",","").trim();
            if (raw.isEmpty()) return;
            double amount = Double.parseDouble(raw);
            String from = CurrencyMeta.CODES[convFrom.getSelectedItemPosition()];
            String to   = CurrencyMeta.CODES[convTo.getSelectedItemPosition()];
            double result = app.convert(amount, from, to);
            String time  = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
            DecimalFormat f    = result>=1000 ? new DecimalFormat("#,##0.00") : new DecimalFormat("#,##0.00####");
            DecimalFormat fAmt = amount>=1000 ? new DecimalFormat("#,##0.00") : new DecimalFormat("#,##0.00####");
            saveConvEntry(time + "   " + fAmt.format(amount) + " " + from
                    + "  →  " + f.format(result) + " " + to);
            loadHistory();
            ui.snack(app.rootFrame, "✓ Saved to history", Tokens.GREEN);
        } catch (Exception ignored){}
    }

    private void shareConversion() {
        try {
            String raw = convInput.getText().toString().replace(",","").trim();
            if (raw.isEmpty()) return;
            double amount = Double.parseDouble(raw);
            String from = CurrencyMeta.CODES[convFrom.getSelectedItemPosition()];
            String to   = CurrencyMeta.CODES[convTo.getSelectedItemPosition()];
            double result = app.convert(amount, from, to);
            DecimalFormat f    = result>=1000 ? new DecimalFormat("#,##0.00") : new DecimalFormat("#,##0.00####");
            DecimalFormat fAmt = amount>=1000 ? new DecimalFormat("#,##0.00") : new DecimalFormat("#,##0.00####");
            String text = "💱 " + fAmt.format(amount) + " " + from
                + " = " + f.format(result) + " " + to
                + "\n📅 " + new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(new Date())
                + "\nvia FXetcher 🇹🇿";
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_SEND);
            i.setType("text/plain"); i.putExtra(android.content.Intent.EXTRA_TEXT, text);
            app.startActivity(android.content.Intent.createChooser(i,"Share Conversion"));
        } catch (Exception ignored){}
    }

    // ── Quick Amounts Card ────────────────────────────────────────────────────
    private void buildQuickAmountsCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card, "⚡", "Quick Amounts", "Tap a preset to convert instantly", Tokens.BLUE);
        ui.spacer(card, Tokens.S12);
        double[] amounts = {1, 10, 50, 100, 500, 1_000, 5_000, 10_000, 50_000, 100_000, 500_000, 1_000_000};
        LinearLayout r1 = ui.chipRow(), r2 = ui.chipRow(), r3 = ui.chipRow();
        for (int i=0; i<amounts.length; i++) {
            final double amt = amounts[i];
            Button chip = ui.chip(chipLabel(amt));
            chip.setLayoutParams(ui.chipParams());
            chip.setOnClickListener(v -> {
                if (lastSelectedChip!=null) ui.chipUnselected(lastSelectedChip);
                lastSelectedChip = chip; ui.chipSelected(chip, Tokens.BLUE);
                convInput.setText(amt >= 1000 ? String.valueOf((long)amt) : String.valueOf(amt));
                recalc();
            });
            (i<4?r1:(i<8?r2:r3)).addView(chip);
        }
        card.addView(r1); ui.spacer(card, Tokens.S6);
        card.addView(r2); ui.spacer(card, Tokens.S6);
        card.addView(r3);
    }

    private String chipLabel(double v) {
        if (v >= 1_000_000) return String.format(Locale.US, "%dM", (long)(v/1_000_000));
        if (v >= 1_000)     return String.format(Locale.US, "%dK", (long)(v/1_000));
        return String.valueOf((long)v);
    }

    // ── History Card ──────────────────────────────────────────────────────────
    private void buildHistoryCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        LinearLayout titleRow = ui.hRow();
        LinearLayout titleCol = ui.vCol();
        titleCol.setLayoutParams(ui.wt(1f));
        titleCol.addView(ui.tv("🕐  Recent Conversions", Tokens.TEXT_MD, Tokens.ORANGE, true));
        titleCol.addView(ui.tv("Last 30 conversions saved", Tokens.TEXT_XS, Tokens.surfaceTint, false));
        titleRow.addView(titleCol);
        Button clearBtn = ui.solidBtn("Clear", Tokens.withAlpha(Tokens.RED,30), Tokens.RED);
        clearBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_XS);
        clearBtn.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(70), ui.dp(34)));
        clearBtn.setOnClickListener(v -> {
            List<String> h = loadConvList();
            if (h.isEmpty()) { ui.snack(app.rootFrame, "History already empty", Tokens.ORANGE); return; }
            new AlertDialog.Builder(app).setTitle("Clear History")
                .setMessage("Delete all "+h.size()+" records?")
                .setPositiveButton("Clear",(d,w)->{ app.prefs.edit().putString(AppPrefs.KEY_CONVHIST,"[]").apply(); loadHistory(); ui.snack(app.rootFrame,"History cleared",Tokens.ORANGE); })
                .setNegativeButton("Cancel",null).show();
        });
        titleRow.addView(clearBtn); card.addView(titleRow);
        ui.spacer(card, Tokens.S10); card.addView(ui.divider()); ui.spacer(card, Tokens.S8);
        convHistContainer = ui.vCol(); card.addView(convHistContainer);
        convHistEmpty = ui.tv("No conversions yet.\nTap Convert & Save to begin.", Tokens.TEXT_SM, Tokens.surfaceTint, false);
        convHistEmpty.setGravity(Gravity.CENTER);
        convHistEmpty.setPadding(0, ui.dp(Tokens.S20), 0, ui.dp(Tokens.S16));
        convHistEmpty.setVisibility(View.GONE); card.addView(convHistEmpty);
        loadHistory();
    }

    private void loadHistory() {
        if (convHistContainer==null) return;
        convHistContainer.removeAllViews();
        List<String> hist = loadConvList();
        if (convHistEmpty!=null) convHistEmpty.setVisibility(hist.isEmpty()?View.VISIBLE:View.GONE);
        for (int i=0; i<hist.size(); i++) {
            LinearLayout row = ui.hRow();
            row.setPadding(ui.dp(Tokens.S8), ui.dp(Tokens.S10), ui.dp(Tokens.S8), ui.dp(Tokens.S10));
            if (i%2==0) { GradientDrawable rd=new GradientDrawable(); rd.setCornerRadius(ui.dp(Tokens.R6)); rd.setColor(Tokens.surfaceVar); row.setBackground(rd); }
            TextView badge = ui.badge(String.valueOf(i+1), Tokens.BLUE);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            bp.rightMargin = ui.dp(Tokens.S8); badge.setLayoutParams(bp); row.addView(badge);
            TextView entry = ui.tv(hist.get(i), Tokens.TEXT_SM, Tokens.onSurfaceVar, false);
            entry.setLayoutParams(ui.wt(1f)); row.addView(entry);
            // delete single entry
            final int idx = i;
            Button del = ui.solidBtn("✕", Tokens.withAlpha(Tokens.RED,20), Tokens.RED);
            del.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(32), ui.dp(32)));
            del.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_XS);
            del.setOnClickListener(v -> { List<String> l=loadConvList(); if(idx<l.size()){l.remove(idx);} saveConvList(l); loadHistory(); });
            row.addView(del);
            convHistContainer.addView(row);
            if (i<hist.size()-1) convHistContainer.addView(ui.divider());
        }
    }

    private List<String> loadConvList() {
        List<String> list = new ArrayList<>();
        try { JSONArray a=new JSONArray(app.prefs.getString(AppPrefs.KEY_CONVHIST,"[]")); for(int i=0;i<a.length();i++) list.add(a.getString(i)); } catch(Exception ignored){}
        return list;
    }
    private void saveConvEntry(String e) {
        List<String> l=loadConvList(); l.add(0,e); if(l.size()>30) l.subList(30,l.size()).clear(); saveConvList(l);
    }
    private void saveConvList(List<String> list) {
        try { JSONArray a=new JSONArray(); for(String s:list) a.put(s); app.prefs.edit().putString(AppPrefs.KEY_CONVHIST,a.toString()).apply(); } catch(Exception ignored){}
    }

    private Spinner buildCurrencySpinner() {
        String[] display = new String[CurrencyMeta.CODES.length];
        for (int i=0; i<CurrencyMeta.CODES.length; i++) {
            String c = CurrencyMeta.CODES[i];
            display[i] = app.FLAGS.getOrDefault(c,"💱")+"  "+c+" – "+app.NAMES.getOrDefault(c,"");
        }
        return ui.styledSpinner(display);
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  MARKETS SCREEN  v4
// ─────────────────────────────────────────────────────────────────────────────
class MarketsScreen {

    private final FXetcherApp app;
    private final UiKit ui;

    private final HashMap<String,TextView> cellRateViews   = new HashMap<>();
    private final HashMap<String,TextView> cellChangeViews = new HashMap<>();
    private LinearLayout alertsListContainer;
    private TextView     alertsEmptyTv;

    // Watchlist
    private LinearLayout watchlistContainer;
    private TextView     watchlistEmpty;

    MarketsScreen(FXetcherApp app) { this.app=app; this.ui=app.ui; }

    View build() {
        ScrollView sv = new ScrollView(app);
        sv.setBackgroundColor(Tokens.bg);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout content = ui.vCol();
        content.setPadding(ui.dp(Tokens.S14), ui.dp(Tokens.S12),
                ui.dp(Tokens.S14), ui.dp(Tokens.NAV_HEIGHT+16));
        sv.addView(content);
        buildWatchlistCard(content);
        buildMajorGrid(content);
        buildAfricaCard(content);
        buildMetalsExtCard(content);
        buildAlertsCard(content);
        return sv;
    }

    // ── Watchlist Card ────────────────────────────────────────────────────────
    private void buildWatchlistCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);

        LinearLayout hdr = ui.hRow();
        LinearLayout hdrCol = ui.vCol(); hdrCol.setLayoutParams(ui.wt(1f));
        hdrCol.addView(ui.tv("⭐  My Watchlist", Tokens.TEXT_MD, Tokens.GOLD, true));
        hdrCol.addView(ui.tv("Long-press any currency to add", Tokens.TEXT_XS, Tokens.surfaceTint, false));
        hdr.addView(hdrCol);
        card.addView(hdr);
        ui.spacer(card, Tokens.S10);

        watchlistEmpty = ui.tv("Your watchlist is empty.\nLong-press any currency tile to pin it here.", Tokens.TEXT_SM, Tokens.surfaceTint, false);
        watchlistEmpty.setGravity(Gravity.CENTER);
        watchlistEmpty.setPadding(0, ui.dp(Tokens.S8), 0, ui.dp(Tokens.S8));
        card.addView(watchlistEmpty);

        watchlistContainer = ui.vCol();
        card.addView(watchlistContainer);

        refreshWatchlist();
    }

    void refreshWatchlist() {
        if (watchlistContainer==null) return;
        watchlistContainer.removeAllViews();
        List<String> wl = loadWatchlist();
        if (watchlistEmpty!=null) watchlistEmpty.setVisibility(wl.isEmpty()?View.VISIBLE:View.GONE);
        for (int i=0; i<wl.size(); i++) {
            String code = wl.get(i);
            Double r = app.rates.get(code);
            if (r==null) continue;
            Double ov = app.prevRates.get(code);
            double pct = (ov!=null&&ov>0)?(r-ov)/ov*100.0:0.0;
            boolean up = pct>=0;
            int accent = Tokens.accentByIndex(i);
            String rStr = CurrencyMeta.isSmallRate(code)?app.fmtSml.format(r)+" TZS":app.fmtTzs.format(r)+" TZS";

            LinearLayout row = ui.rateRow(
                app.FLAGS.getOrDefault(code,"💱"),
                app.NAMES.getOrDefault(code,code),
                code, rStr, accent,
                v -> app.showQuickConvert(code));

            // remove button
            row.setLongClickable(true);
            final int idx=i;
            row.setOnLongClickListener(v -> {
                new AlertDialog.Builder(app).setTitle("Remove from Watchlist")
                    .setMessage("Remove "+code+"?")
                    .setPositiveButton("Remove",(d,w)->{
                        List<String> l=loadWatchlist(); if(idx<l.size()) l.remove(idx);
                        saveWatchlist(l); refreshWatchlist();
                    }).setNegativeButton("Cancel",null).show();
                return true;
            });
            watchlistContainer.addView(row);
        }
    }

    // ── Major Grid ────────────────────────────────────────────────────────────
    private void buildMajorGrid(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card, "🌐", "Major Currencies",
                "Tap = quick convert · Long-press = watchlist", Tokens.GREEN);
        ui.spacer(card, Tokens.S14);

        String[][] grid = {{"USD","EUR","GBP"},{"JPY","CNY","INR"},{"AED","ZAR","KES"},{"CAD","CHF","SGD"}};
        for (int r=0; r<grid.length; r++) {
            LinearLayout gridRow = new LinearLayout(app);
            gridRow.setOrientation(LinearLayout.HORIZONTAL); gridRow.setWeightSum(3f);
            LinearLayout.LayoutParams grp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            grp.bottomMargin = ui.dp(Tokens.S8); gridRow.setLayoutParams(grp);
            for (int c=0; c<grid[r].length; c++) {
                gridRow.addView(buildCell(grid[r][c], Tokens.accentByIndex(r*3+c)));
            }
            card.addView(gridRow);
        }
    }

    private LinearLayout buildCell(String code, int accent) {
        LinearLayout cell = new LinearLayout(app);
        cell.setOrientation(LinearLayout.VERTICAL); cell.setGravity(Gravity.CENTER);
        cell.setPadding(ui.dp(Tokens.S8), ui.dp(Tokens.S12), ui.dp(Tokens.S8), ui.dp(Tokens.S12));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ui.dp(120), 1f);
        cp.setMargins(ui.dp(Tokens.S4), 0, ui.dp(Tokens.S4), 0); cell.setLayoutParams(cp);
        GradientDrawable cellBg = new GradientDrawable();
        cellBg.setCornerRadius(ui.dp(Tokens.R14)); cellBg.setColor(Tokens.surfaceVar);
        cellBg.setStroke(ui.dp(1), Tokens.withAlpha(accent, 80));
        cell.setBackground(ui.rippleOver(cellBg)); cell.setClickable(true); cell.setFocusable(true);
        cell.setOnClickListener(v -> { ui.scaleAnim(cell); app.showQuickConvert(code); });
        cell.setLongClickable(true);
        cell.setOnLongClickListener(v -> { addToWatchlist(code); return true; });

        // Flag
        LinearLayout flagCircle = new LinearLayout(app); flagCircle.setGravity(Gravity.CENTER);
        int bs = ui.dp(38); flagCircle.setLayoutParams(new LinearLayout.LayoutParams(bs, bs));
        GradientDrawable fd = new GradientDrawable(); fd.setShape(GradientDrawable.OVAL);
        fd.setColor(Tokens.withAlpha(accent,30)); flagCircle.setBackground(fd);
        flagCircle.addView(ui.tv(app.FLAGS.getOrDefault(code,"💱"), 16, Color.WHITE, false));
        cell.addView(flagCircle); ui.spacer(cell, Tokens.S4);

        TextView codeTv = ui.tv(code, Tokens.TEXT_SM, Tokens.onSurfaceVar, true);
        codeTv.setGravity(Gravity.CENTER); cell.addView(codeTv); ui.spacer(cell, 2);

        Double r = app.rates.get(code);
        String rStr = r!=null ? (code.equals("JPY")||code.equals("UGX")||code.equals("RWF")?
                new DecimalFormat("#,##0.0").format(r): app.fmtTzs.format(r)) : "—";
        TextView rateTv = ui.mono(rStr, Tokens.TEXT_XS, Tokens.GOLD, true);
        rateTv.setGravity(Gravity.CENTER); cell.addView(rateTv);
        cellRateViews.put(code, rateTv);

        // Change indicator
        TextView changeTv = ui.tv("", Tokens.TEXT_XS, Tokens.surfaceTint, false);
        changeTv.setGravity(Gravity.CENTER); cell.addView(changeTv);
        cellChangeViews.put(code, changeTv);
        return cell;
    }

    private void addToWatchlist(String code) {
        List<String> wl = loadWatchlist();
        if (wl.contains(code)) { ui.snack(app.rootFrame, code+" already in watchlist", Tokens.ORANGE); return; }
        wl.add(code); saveWatchlist(wl); refreshWatchlist();
        ui.snack(app.rootFrame, "⭐ "+code+" added to watchlist", Tokens.GOLD);
    }

    // ── Africa Card ───────────────────────────────────────────────────────────
    private void buildAfricaCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card, "🌍", "African Currencies",
                "East & West African markets", Tokens.ORANGE);
        ui.spacer(card, Tokens.S12); card.addView(ui.divider()); ui.spacer(card, Tokens.S10);

        String[] codes   = {"KES","UGX","RWF","ZAR","NGN","EGP","ETB"};
        String[] labels  = {"Kenya","Uganda","Rwanda","South Africa","Nigeria","Egypt","Ethiopia"};
        int[]    accents = {Tokens.GREEN,Tokens.ORANGE,Tokens.BLUE,Tokens.TEAL,Tokens.PURPLE,Tokens.AMBER,Tokens.RED};
        for (int i=0; i<codes.length; i++) {
            String code=codes[i]; int accent=accents[i]; Double r=app.rates.get(code);
            boolean sml=CurrencyMeta.isSmallRate(code);
            String rStr=r!=null?(sml?app.fmtSml.format(r):app.fmtTzs.format(r))+" TZS":"—";
            LinearLayout row=ui.rateRow(app.FLAGS.getOrDefault(code,"🌍"),labels[i],code,rStr,accent,
                v -> app.showQuickConvert(code));
            row.setLongClickable(true);
            row.setOnLongClickListener(v -> { addToWatchlist(code); return true; });
            card.addView(row);
        }
    }

    // ── Metals Extended Card ──────────────────────────────────────────────────
    private void buildMetalsExtCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card, "💰", "Precious Metals", "Gold & Silver per troy ounce · gram · kilogram", Tokens.GOLD);
        ui.spacer(card, Tokens.S10);

        // Gold row with gram/kg conversion
        buildMetalExtRow(card, "XAU", "🥇", "GOLD", "#FFD700");
        ui.spacer(card, Tokens.S8);
        buildMetalExtRow(card, "XAG", "🥈", "SILVER", "#C0C0C0");

        ui.spacer(card, Tokens.S12); card.addView(ui.divider()); ui.spacer(card, Tokens.S8);
        // Conversions note
        LinearLayout noteRow = ui.hRow();
        noteRow.setPadding(ui.dp(Tokens.S10), ui.dp(Tokens.S8), ui.dp(Tokens.S10), ui.dp(Tokens.S8));
        GradientDrawable nd = new GradientDrawable(); nd.setCornerRadius(ui.dp(Tokens.R8));
        nd.setColor(Tokens.withAlpha(Tokens.GOLD,12)); noteRow.setBackground(nd);
        noteRow.addView(ui.tv("ℹ️  1 troy oz = 31.1035g   ·   Tap for quick convert", Tokens.TEXT_XS, Tokens.withAlpha(Tokens.GOLD,180), false));
        card.addView(noteRow);
    }

    private void buildMetalExtRow(LinearLayout parent, String code, String icon, String label, String hex) {
        int accent = Color.parseColor(hex);
        LinearLayout row = ui.hRow();
        row.setPadding(ui.dp(Tokens.S12), ui.dp(Tokens.S12), ui.dp(Tokens.S12), ui.dp(Tokens.S12));
        GradientDrawable bg = new GradientDrawable(); bg.setCornerRadius(ui.dp(Tokens.R14));
        bg.setColor(Tokens.surfaceVar); bg.setStroke(ui.dp(2), Tokens.withAlpha(accent,80));
        row.setBackground(ui.rippleOver(bg)); row.setClickable(true); row.setFocusable(true);
        row.setOnClickListener(v -> app.showMetalSheet(code));

        View bar = new View(app); bar.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(4),ui.dp(44)));
        GradientDrawable bd = new GradientDrawable(); bd.setCornerRadius(ui.dp(2)); bd.setColor(accent); bar.setBackground(bd);
        row.addView(bar); ui.spacerH(row, Tokens.S12);

        LinearLayout iconBadge = new LinearLayout(app); iconBadge.setGravity(Gravity.CENTER);
        int bs=ui.dp(44); iconBadge.setLayoutParams(new LinearLayout.LayoutParams(bs,bs));
        GradientDrawable fd=new GradientDrawable(); fd.setShape(GradientDrawable.OVAL);
        fd.setColor(Tokens.withAlpha(accent,25)); iconBadge.setBackground(fd);
        iconBadge.addView(ui.tv(icon,20,accent,false)); row.addView(iconBadge); ui.spacerH(row,Tokens.S12);

        LinearLayout labelCol = ui.vCol(); labelCol.setLayoutParams(ui.wt(1f));
        labelCol.addView(ui.tv(label+" ("+code+")", Tokens.TEXT_MD, accent, true));
        Double r = app.rates.get(code);
        if (r!=null&&r>0) {
            double perGram = r/31.1035;
            double perKg   = perGram*1000;
            labelCol.addView(ui.tv(app.fmtTzs.format(perGram)+" /g  ·  "+app.fmtTzs.format(perKg)+" /kg",
                    Tokens.TEXT_XS, Tokens.onSurfaceVar, false));
        }
        row.addView(labelCol);

        LinearLayout priceCol = ui.vCol(); priceCol.setGravity(Gravity.END);
        String rStr = r!=null ? app.fmtTzs.format(r)+" TZS" : "Loading…";
        TextView priceTv = ui.mono(rStr, Tokens.TEXT_SM, Tokens.onSurface, true);
        priceTv.setGravity(Gravity.END); priceCol.addView(priceTv);
        priceCol.addView(ui.tv("per troy oz", Tokens.TEXT_XS, Tokens.surfaceTint, false));
        row.addView(priceCol);
        cellRateViews.put(code+"_ext", priceTv);
        parent.addView(row);
    }

    void update() {
        for (Map.Entry<String,TextView> e : cellRateViews.entrySet()) {
            String key = e.getKey(); String code = key.replace("_ext","");
            Double r = app.rates.get(code); if (r==null) continue;
            String rStr = code.equals("JPY")||code.equals("UGX")||code.equals("RWF")||code.equals("NGN")?
                new DecimalFormat("#,##0.0").format(r) : app.fmtTzs.format(r);
            e.getValue().setText(key.endsWith("_ext") ? rStr+" TZS" : rStr);
        }
        // Update change indicators
        for (Map.Entry<String,TextView> e : cellChangeViews.entrySet()) {
            String code = e.getKey(); Double r=app.rates.get(code), ov=app.prevRates.get(code);
            if (r==null||ov==null||ov==0) continue;
            double pct=(r-ov)/ov*100.0;
            e.getValue().setText(String.format(Locale.US, "%s%.2f%%", pct>=0?"▲":"▼", Math.abs(pct)));
            e.getValue().setTextColor(pct>=0?Tokens.GREEN:Tokens.RED);
        }
        refreshWatchlist();
    }

    // ── Alerts Card ───────────────────────────────────────────────────────────
    private void buildAlertsCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card, "🔔", "Price Alerts",
                "Notify when rates cross your targets", Tokens.BLUE);
        ui.spacer(card, Tokens.S12);
        Button addBtn = ui.solidBtn("＋  Add New Alert", Tokens.BLUE, 0xFFFFFFFF);
        addBtn.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ui.dp(Tokens.TOUCH_TARGET+4)));
        addBtn.setOnClickListener(v -> showAddAlertSheet());
        card.addView(addBtn); ui.spacer(card, Tokens.S12); card.addView(ui.divider()); ui.spacer(card, Tokens.S8);
        alertsListContainer = ui.vCol(); card.addView(alertsListContainer);
        alertsEmptyTv = ui.tv("No alerts yet. Tap ＋ Add New Alert.", Tokens.TEXT_SM, Tokens.surfaceTint, false);
        alertsEmptyTv.setGravity(Gravity.CENTER);
        alertsEmptyTv.setPadding(0, ui.dp(Tokens.S16), 0, ui.dp(Tokens.S16));
        alertsEmptyTv.setVisibility(View.GONE); card.addView(alertsEmptyTv);
        refreshAlertsList();
    }

    void refreshAlertsList() {
        if (alertsListContainer==null) return;
        alertsListContainer.removeAllViews();
        List<JSONObject> alerts = app.loadAlerts();
        if (alertsEmptyTv!=null) alertsEmptyTv.setVisibility(alerts.isEmpty()?View.VISIBLE:View.GONE);
        for (int i=0; i<alerts.size(); i++) {
            final int idx=i;
            try {
                JSONObject a=alerts.get(i);
                String code=a.getString("currency"); double target=a.getDouble("target"); int cond=a.getInt("cond"); boolean up=(cond==0);
                LinearLayout row=ui.hRow();
                row.setPadding(ui.dp(Tokens.S12),ui.dp(Tokens.S12),ui.dp(Tokens.S12),ui.dp(Tokens.S12));
                LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT); rp.bottomMargin=ui.dp(Tokens.S6); row.setLayoutParams(rp);
                View accentBar=new View(app); accentBar.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(4),ui.dp(40)));
                GradientDrawable abd=new GradientDrawable(); abd.setCornerRadius(ui.dp(2)); abd.setColor(up?Tokens.GREEN:Tokens.RED); accentBar.setBackground(abd); row.addView(accentBar); ui.spacerH(row,Tokens.S10);
                LinearLayout infoCol=ui.vCol(); infoCol.setLayoutParams(ui.wt(1f));
                infoCol.addView(ui.tv(app.FLAGS.getOrDefault(code,"💱")+"  "+code+"  "+(up?"▲ rises above":"▼ falls below"), Tokens.TEXT_MD, Tokens.onSurface, true));
                ui.spacer(infoCol,2);
                infoCol.addView(ui.mono("Target: "+app.fmtTzs.format(target)+" TZS", Tokens.TEXT_XS, Tokens.GOLD, false));
                Double cur=app.rates.get(code);
                if (cur!=null) {
                    double diff=cur-target; String sign=diff>=0?"+":"";
                    infoCol.addView(ui.tv("Now: "+app.fmtTzs.format(cur)+" TZS  ("+sign+app.fmtTzs.format(diff)+")", Tokens.TEXT_XS, cur>=target?Tokens.GREEN:Tokens.RED, false));
                }
                row.addView(infoCol);
                Button del=ui.solidBtn("🗑",Tokens.withAlpha(Tokens.RED,25),Tokens.RED);
                del.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(40),ui.dp(40)));
                del.setOnClickListener(v->new AlertDialog.Builder(app).setTitle("Delete Alert").setMessage("Remove "+code+" alert?").setPositiveButton("Delete",(d,w)->{app.deleteAlert(idx);refreshAlertsList();}).setNegativeButton("Cancel",null).show()); row.addView(del);
                GradientDrawable rowBg=new GradientDrawable(); rowBg.setCornerRadius(ui.dp(Tokens.R12)); rowBg.setColor(Tokens.surfaceVar); rowBg.setStroke(ui.dp(1),Tokens.withAlpha(up?Tokens.GREEN:Tokens.RED,50)); row.setBackground(rowBg);
                alertsListContainer.addView(row);
            } catch(Exception ignored){}
        }
    }

    private void showAddAlertSheet() {
        View dim=new View(app); dim.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT,MATCH_PARENT)); dim.setBackgroundColor(0xAA000000); dim.setAlpha(0f); app.rootFrame.addView(dim);
        LinearLayout sheet=ui.vCol();
        GradientDrawable sheetBg=new GradientDrawable(); sheetBg.setColor(Tokens.surface);
        sheetBg.setCornerRadii(new float[]{ui.dp(Tokens.R20),ui.dp(Tokens.R20),ui.dp(Tokens.R20),ui.dp(Tokens.R20),0,0,0,0});
        sheetBg.setStroke(ui.dp(1),Tokens.outline); sheet.setBackground(sheetBg);
        sheet.setPadding(ui.dp(Tokens.S20),ui.dp(Tokens.S16),ui.dp(Tokens.S20),ui.dp(Tokens.S32));
        FrameLayout.LayoutParams slp=new FrameLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT); slp.gravity=Gravity.BOTTOM; sheet.setLayoutParams(slp);
        View handle=new View(app); LinearLayout.LayoutParams hlp=new LinearLayout.LayoutParams(ui.dp(40),ui.dp(4)); hlp.gravity=Gravity.CENTER_HORIZONTAL; hlp.bottomMargin=ui.dp(Tokens.S16); handle.setLayoutParams(hlp);
        GradientDrawable hd=new GradientDrawable(); hd.setCornerRadius(ui.dp(2)); hd.setColor(Tokens.outline); handle.setBackground(hd); sheet.addView(handle);
        LinearLayout header=ui.hRow(); TextView ttl=ui.tv("🔔  New Price Alert",Tokens.TEXT_XL,Tokens.onSurface,true); ttl.setLayoutParams(ui.wt(1f)); header.addView(ttl);
        Button closeBtn=ui.ghostBtn("✕",Tokens.onSurfaceVar); header.addView(closeBtn); sheet.addView(header);
        ui.spacer(sheet,Tokens.S4); sheet.addView(ui.tv("Notify me when a rate crosses my target",Tokens.TEXT_XS,Tokens.onSurfaceVar,false)); ui.spacer(sheet,Tokens.S16);
        sheet.addView(ui.fieldLabel("CURRENCY")); ui.spacer(sheet,Tokens.S6);
        String[] dNames=new String[CurrencyMeta.CODES.length];
        for(int i=0;i<CurrencyMeta.CODES.length;i++){String c=CurrencyMeta.CODES[i]; dNames[i]=app.FLAGS.getOrDefault(c,"💱")+"  "+c+" – "+app.NAMES.getOrDefault(c,"");}
        Spinner currSpinner=ui.styledSpinner(dNames); sheet.addView(currSpinner); ui.spacer(sheet,Tokens.S12);
        sheet.addView(ui.fieldLabel("CONDITION")); ui.spacer(sheet,Tokens.S6);
        Spinner condSpinner=ui.styledSpinner(new String[]{"📈  Rate rises above target","📉  Rate falls below target"}); sheet.addView(condSpinner); ui.spacer(sheet,Tokens.S12);
        sheet.addView(ui.fieldLabel("TARGET RATE (TZS)")); ui.spacer(sheet,Tokens.S6);
        EditText targetInput=ui.styledInput("e.g. 2650.00",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); sheet.addView(targetInput); ui.spacer(sheet,Tokens.S6);
        TextView hintTv=ui.tv("",Tokens.TEXT_XS,Tokens.GREEN,false); sheet.addView(hintTv);
        currSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){Double r=app.rates.get(CurrencyMeta.CODES[pos]); hintTv.setText(r!=null?"Current: "+app.fmtTzs.format(r)+" TZS":""); if(targetInput.getText().toString().isEmpty()&&r!=null) targetInput.setHint(app.fmtTzs.format(r));}
            public void onNothingSelected(AdapterView<?> p){}
        });
        ui.spacer(sheet,Tokens.S16);
        Button createBtn=ui.solidBtn("🔔  Create Alert",Tokens.BLUE,0xFFFFFFFF);
        createBtn.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT,ui.dp(Tokens.TOUCH_TARGET+8)));
        app.rootFrame.addView(sheet);
        Runnable dismiss=()->{sheet.animate().translationY(ui.dp(600)).setDuration(260).start(); dim.animate().alpha(0f).setDuration(200).withEndAction(()->{app.rootFrame.removeView(sheet);app.rootFrame.removeView(dim);}).start();};
        createBtn.setOnClickListener(v->{String raw=targetInput.getText().toString().trim(); if(raw.isEmpty()){ui.snack(app.rootFrame,"Enter a target rate",Tokens.ORANGE);return;} try{double target=Double.parseDouble(raw); if(target<=0){ui.snack(app.rootFrame,"Rate must be > 0",Tokens.RED);return;} app.saveAlert(CurrencyMeta.CODES[currSpinner.getSelectedItemPosition()],target,condSpinner.getSelectedItemPosition()); refreshAlertsList(); dismiss.run(); ui.snack(app.rootFrame,"✓ Alert created",Tokens.GREEN);}catch(Exception e){ui.snack(app.rootFrame,"Invalid rate value",Tokens.RED);}});
        sheet.addView(createBtn);
        sheet.setTranslationY(ui.dp(800)); sheet.animate().translationY(0f).setDuration(320).setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
        dim.animate().alpha(1f).setDuration(250).start();
        dim.setOnClickListener(v->dismiss.run()); closeBtn.setOnClickListener(v->dismiss.run());
    }

    // Watchlist persistence
    private List<String> loadWatchlist() {
        List<String> l=new ArrayList<>();
        try{JSONArray a=new JSONArray(app.prefs.getString(AppPrefs.KEY_WATCHLIST,"[]")); for(int i=0;i<a.length();i++) l.add(a.getString(i));}catch(Exception ignored){}
        return l;
    }
    private void saveWatchlist(List<String> list) {
        try{JSONArray a=new JSONArray(); for(String s:list) a.put(s); app.prefs.edit().putString(AppPrefs.KEY_WATCHLIST,a.toString()).apply();}catch(Exception ignored){}
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  SETTINGS SCREEN  v4
// ─────────────────────────────────────────────────────────────────────────────
class SettingsScreen {

    private final FXetcherApp app;
    private final UiKit ui;

    SettingsScreen(FXetcherApp app) { this.app=app; this.ui=app.ui; }

    View build() {
        ScrollView sv = new ScrollView(app);
        sv.setBackgroundColor(Tokens.bg);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout content = ui.vCol();
        content.setPadding(ui.dp(Tokens.S14), ui.dp(Tokens.S12),
                ui.dp(Tokens.S14), ui.dp(Tokens.NAV_HEIGHT+16));
        sv.addView(content);
        buildRefreshCard(content);
        buildThemeCard(content);
        buildDisplayCard(content);
        buildNotifCard(content);
        buildDataCard(content);
        buildMetalsInfoCard(content);
        buildAboutCard(content);
        return sv;
    }

    private void buildRefreshCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card,"🔄","Auto-Refresh","Keep rates up to date automatically",Tokens.BLUE);
        ui.spacer(card,Tokens.S14);
        card.addView(ui.toggleRow("Auto-Refresh Rates","Fetch latest rates in the background",
            app.prefs.getBoolean(AppPrefs.KEY_AUTO,true), isChecked->{app.prefs.edit().putBoolean(AppPrefs.KEY_AUTO,isChecked).apply(); app.scheduleAutoRefresh();}));
        ui.spacer(card,Tokens.S10); card.addView(ui.divider()); ui.spacer(card,Tokens.S10);
        card.addView(ui.tv("Refresh Interval",Tokens.TEXT_MD,Tokens.onSurface,true));
        ui.spacer(card,Tokens.S4);
        card.addView(ui.tv("How often to auto-refresh rates",Tokens.TEXT_XS,Tokens.onSurfaceVar,false));
        ui.spacer(card,Tokens.S8);
        String[] labels={"30 sec","1 min","5 min","10 min","15 min","30 min","1 hour"};
        int[]    vals  ={30_000,60_000,300_000,600_000,900_000,1_800_000,3_600_000};
        Spinner iSpin=ui.styledSpinner(labels);
        int cur=app.prefs.getInt(AppPrefs.KEY_INTERVAL,300_000);
        for(int i=0;i<vals.length;i++) if(vals[i]==cur){iSpin.setSelection(i);break;}
        iSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){app.prefs.edit().putInt(AppPrefs.KEY_INTERVAL,vals[pos]).apply(); app.scheduleAutoRefresh();}
            public void onNothingSelected(AdapterView<?> p){}
        });
        card.addView(iSpin); ui.spacer(card,Tokens.S10);
        Button rn=ui.outlineBtn("🔄  Refresh Now",Tokens.BLUE);
        rn.setOnClickListener(v->{app.fetchRates(); ui.snack(app.rootFrame,"Fetching latest rates…",Tokens.BLUE);}); card.addView(rn);
    }

    private void buildThemeCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card,"🎨","Appearance","Choose your preferred theme",Tokens.PURPLE);
        ui.spacer(card,Tokens.S14);
        boolean dark=app.prefs.getBoolean(AppPrefs.KEY_DARK,true);
        LinearLayout pRow=ui.hRow(); pRow.setWeightSum(2f);
        LinearLayout darkP=themePreview(true,dark); LinearLayout.LayoutParams dpp=new LinearLayout.LayoutParams(0,ui.dp(90),1f); dpp.rightMargin=ui.dp(Tokens.S8); darkP.setLayoutParams(dpp); darkP.setOnClickListener(v->applyAndRecreate(true)); pRow.addView(darkP);
        LinearLayout lightP=themePreview(false,!dark); lightP.setLayoutParams(new LinearLayout.LayoutParams(0,ui.dp(90),1f)); lightP.setOnClickListener(v->applyAndRecreate(false)); pRow.addView(lightP);
        card.addView(pRow); ui.spacer(card,Tokens.S8);
        card.addView(ui.tv("Tap a theme to switch. App restarts to apply.",Tokens.TEXT_XS,Tokens.surfaceTint,false));
    }

    private LinearLayout themePreview(boolean dark, boolean selected) {
        LinearLayout p=new LinearLayout(app); p.setOrientation(LinearLayout.VERTICAL); p.setGravity(Gravity.CENTER);
        p.setPadding(ui.dp(Tokens.S12),ui.dp(Tokens.S14),ui.dp(Tokens.S12),ui.dp(Tokens.S14));
        GradientDrawable pd=new GradientDrawable(); pd.setCornerRadius(ui.dp(Tokens.R14));
        pd.setColor(dark?0xFF1A1F3A:0xFFFFFFFF); pd.setStroke(ui.dp(selected?2:1),selected?Tokens.GOLD:(dark?0xFF2A2F4A:0xFFDDE0EF));
        p.setBackground(ui.rippleOver(pd)); p.setClickable(true); p.setFocusable(true);
        p.addView(ui.tv(dark?"🌙":"☀️",22,dark?0xFFFFFFFF:0xFF1A1A2E,false)); ui.spacer(p,Tokens.S4);
        p.addView(ui.tv(dark?"Dark":"Light",Tokens.TEXT_MD,dark?0xFFFFFFFF:0xFF1A1A2E,true));
        if(selected){ui.spacer(p,Tokens.S4); p.addView(ui.badge("Active",Tokens.GOLD));}
        return p;
    }

    private void applyAndRecreate(boolean dark) {
        app.prefs.edit().putBoolean(AppPrefs.KEY_DARK,dark).apply();
        app.finish(); app.startActivity(app.getIntent());
        app.overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

    private void buildDisplayCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card,"📐","Display Options","Customize how rates are shown",Tokens.TEAL);
        ui.spacer(card,Tokens.S14);
        card.addView(ui.toggleRow("Compact Mode","Show more rates with less spacing",
            app.prefs.getBoolean(AppPrefs.KEY_COMPACT,false),
            isChecked->{ app.prefs.edit().putBoolean(AppPrefs.KEY_COMPACT,isChecked).apply(); ui.snack(app.rootFrame,"Restart app to fully apply",Tokens.TEAL); }));
    }

    private void buildNotifCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card,"🔔","Notifications","Rate alerts and refresh notices",Tokens.ORANGE);
        ui.spacer(card,Tokens.S14);
        card.addView(ui.toggleRow("Rate Update Notifications","Show a notification on each refresh",
            app.prefs.getBoolean(AppPrefs.KEY_NOTIFY,true),
            isChecked->app.prefs.edit().putBoolean(AppPrefs.KEY_NOTIFY,isChecked).apply()));
    }

    private void buildDataCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card,"💾","Data Management","Clear stored data",Tokens.PURPLE);
        ui.spacer(card,Tokens.S14);

        Button exportBtn = ui.outlineBtn("📤  Export Rates as Text", Tokens.TEAL);
        exportBtn.setOnClickListener(v -> app.shareRates()); card.addView(exportBtn); ui.spacer(card,Tokens.S6);

        Button clearHist=ui.outlineBtn("🗑  Clear Conversion History",Tokens.RED);
        clearHist.setOnClickListener(v->{
            List<String> h=getConvList(); if(h.isEmpty()){ui.snack(app.rootFrame,"History already empty",Tokens.ORANGE);return;}
            new AlertDialog.Builder(app).setTitle("Clear History").setMessage("Delete all "+h.size()+" records?").setPositiveButton("Clear",(d,w)->{app.prefs.edit().putString(AppPrefs.KEY_CONVHIST,"[]").apply(); ui.snack(app.rootFrame,"History cleared",Tokens.ORANGE);}).setNegativeButton("Cancel",null).show();
        }); card.addView(clearHist); ui.spacer(card,Tokens.S6);

        Button clearWatchlist=ui.outlineBtn("⭐  Clear Watchlist",Tokens.ORANGE);
        clearWatchlist.setOnClickListener(v->new AlertDialog.Builder(app).setTitle("Clear Watchlist").setMessage("Remove all watchlist currencies?").setPositiveButton("Clear",(d,w)->{app.prefs.edit().putString(AppPrefs.KEY_WATCHLIST,"[]").apply(); ui.snack(app.rootFrame,"Watchlist cleared",Tokens.ORANGE);}).setNegativeButton("Cancel",null).show()); card.addView(clearWatchlist); ui.spacer(card,Tokens.S6);

        Button clearAlerts=ui.outlineBtn("🗑  Clear All Alerts",Tokens.RED);
        clearAlerts.setOnClickListener(v->new AlertDialog.Builder(app).setTitle("Clear Alerts").setMessage("Delete all price alerts?").setPositiveButton("Clear",(d,w)->{app.prefs.edit().putString(AppPrefs.KEY_ALERTS,"[]").apply(); ui.snack(app.rootFrame,"Alerts cleared",Tokens.ORANGE);}).setNegativeButton("Cancel",null).show()); card.addView(clearAlerts); ui.spacer(card,Tokens.S6);

        Button resetAll=ui.outlineBtn("🔁  Reset All App Data",Tokens.RED);
        resetAll.setOnClickListener(v->new AlertDialog.Builder(app).setTitle("Reset App").setMessage("Clear all stored rates, history, alerts, and settings?").setPositiveButton("Reset",(d,w)->{app.prefs.edit().clear().apply(); app.setFallbackRates(); app.updateAllDisplays(); ui.snack(app.rootFrame,"App data reset",Tokens.ORANGE);}).setNegativeButton("Cancel",null).show()); card.addView(resetAll);
    }

    private List<String> getConvList(){List<String> l=new ArrayList<>();try{JSONArray a=new JSONArray(app.prefs.getString(AppPrefs.KEY_CONVHIST,"[]")); for(int i=0;i<a.length();i++) l.add(a.getString(i));}catch(Exception ignored){} return l;}

    private void buildMetalsInfoCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        ui.sectionHeader(card,"⚖️","Metals Measurement","Troy Ounce conversions",Tokens.GOLD);
        ui.spacer(card,Tokens.S14);
        TextView info=ui.tv("Precious metals (Gold & Silver) are universally priced in Troy Ounces (oz t), which are heavier than standard avoirdupois ounces.", Tokens.TEXT_XS,Tokens.surfaceTint,false);
        info.setLineSpacing(0f,1.4f); card.addView(info);
        ui.spacer(card,Tokens.S14); card.addView(ui.divider()); ui.spacer(card,Tokens.S10);
        card.addView(ui.infoRow("1 Troy Ounce","= 31.1035 Grams"));
        card.addView(ui.infoRow("1 Kilogram","= 32.1507 Troy Oz"));
        card.addView(ui.infoRow("1 Standard Oz","= 28.3495 Grams"));
        card.addView(ui.infoRow("Gold Purity 24K","= 99.9% pure"));
        card.addView(ui.infoRow("Gold Purity 18K","= 75% pure"));
    }

    private void buildAboutCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        LinearLayout brand=ui.hRow();
        brand.addView(ui.tv("🇹🇿",28,Tokens.GOLD,false)); ui.spacerH(brand,Tokens.S10);
        LinearLayout bCol=ui.vCol();
        bCol.addView(ui.tv("FXetcher",Tokens.TEXT_2XL,Tokens.GOLD,true));
        bCol.addView(ui.tv("Tanzania Forex Tracker",Tokens.TEXT_SM,Tokens.onSurfaceVar,false));
        brand.addView(bCol); card.addView(brand);
        ui.spacer(card,Tokens.S14); card.addView(ui.divider()); ui.spacer(card,Tokens.S10);
        card.addView(ui.infoRow("Version",      "4.0.0"));
        card.addView(ui.infoRow("Package",       "com.willykez.fxetcher"));
        card.addView(ui.infoRow("Currencies",    "25 currencies + 2 metals"));
        card.addView(ui.infoRow("Forex Data",    "ExchangeRate-API v6"));
        card.addView(ui.infoRow("Metals Data",   "MetalPriceAPI"));
        card.addView(ui.infoRow("BoT Data",      "bot.go.tz (scraped)"));
        card.addView(ui.infoRow("Base Currency", "Tanzanian Shilling (TZS)"));
        ui.spacer(card,Tokens.S12); card.addView(ui.divider()); ui.spacer(card,Tokens.S10);
        TextView disc=ui.tv("⚠️  Exchange rates are for informational purposes only. Always verify with your bank before financial decisions. BoT rates are official but may have a brief publication delay.",Tokens.TEXT_XS,Tokens.surfaceTint,false);
        disc.setLineSpacing(0f,1.5f); card.addView(disc);
        ui.spacer(card,Tokens.S12);
        Button shareApp=ui.outlineBtn("📤  Share FXetcher",Tokens.GOLD);
        shareApp.setOnClickListener(v->{ android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(android.content.Intent.EXTRA_TEXT,"🇹🇿 FXetcher — Tanzania Forex Tracker\nLive exchange rates for Tanzanian Shilling.\npackage: com.willykez.fxetcher"); app.startActivity(android.content.Intent.createChooser(i,"Share FXetcher")); });
        card.addView(shareApp);
    }
}

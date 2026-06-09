package com.willykez.fxetcher;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import org.json.JSONArray;

import java.text.DecimalFormat;
import java.util.*;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * CalcScreen v4 — Forex Calculator
 *
 * Features:
 *  • Keypad-style large buttons (no system keyboard needed)
 *  • Bidirectional: enter TZS → get foreign, or enter foreign → get TZS
 *  • Currency selector chip row (pinned + recently used)
 *  • History of last 20 calculations
 *  • Rate reference card at the bottom
 *  • Split calculator: split a TZS amount into multiple currencies
 */
class CalcScreen {

    private final FXetcherApp app;
    private final UiKit ui;

    private TextView displayCurrency;
    private TextView displayInput;
    private TextView displayResult;
    private TextView displayRate;
    private String   selectedCode = "USD";
    private boolean  inputIsTzs   = false;  // false = input is foreign, result is TZS
    private final StringBuilder expr = new StringBuilder();
    private LinearLayout histContainer;
    private TextView    histEmpty;
    private Button      lastCurrChip;

    CalcScreen(FXetcherApp app) { this.app=app; this.ui=app.ui; }

    View build() {
        ScrollView sv = new ScrollView(app);
        sv.setBackgroundColor(Tokens.bg);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout content = ui.vCol();
        content.setPadding(ui.dp(Tokens.S14), ui.dp(Tokens.S10),
                ui.dp(Tokens.S14), ui.dp(Tokens.NAV_HEIGHT+16));
        sv.addView(content);

        buildDisplay(content);
        buildCurrencyChips(content);
        buildDirectionRow(content);
        buildKeypad(content);
        buildRateCard(content);
        buildHistoryCard(content);
        return sv;
    }

    // ── Display ───────────────────────────────────────────────────────────────
    private void buildDisplay(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        card.setBackground(ui.roundRect(ui.dp(Tokens.R16), Tokens.surface, ui.dp(1), Tokens.withAlpha(Tokens.GOLD,60)));

        // Currency label
        LinearLayout topRow = ui.hRow();
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        displayCurrency = ui.tv("USD  🇺🇸", Tokens.TEXT_SM, Tokens.GOLD, true);
        displayCurrency.setLayoutParams(ui.wt(1f));
        topRow.addView(displayCurrency);
        Button clearBtn = ui.solidBtn("AC", Tokens.withAlpha(Tokens.RED,40), Tokens.RED);
        clearBtn.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(56), ui.dp(36)));
        clearBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_SM);
        clearBtn.setOnClickListener(v -> { expr.setLength(0); updateDisplay(); });
        topRow.addView(clearBtn);
        card.addView(topRow);
        ui.spacer(card, Tokens.S10);

        // Input display
        displayInput = ui.mono("0", Tokens.TEXT_3XL, Tokens.onSurface, true);
        displayInput.setGravity(Gravity.END);
        displayInput.setSingleLine(true);
        displayInput.setEllipsize(android.text.TextUtils.TruncateAt.START);
        card.addView(displayInput);
        ui.spacer(card, Tokens.S6);

        // Arrow + result
        LinearLayout resRow = ui.hRow(); resRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView arrow = ui.tv("→", Tokens.TEXT_LG, Tokens.withAlpha(Tokens.GOLD,160), false);
        arrow.setPadding(0,0,ui.dp(Tokens.S8),0);
        resRow.addView(arrow);
        displayResult = ui.mono("0.00 TZS", Tokens.TEXT_2XL, Tokens.GREEN, true);
        displayResult.setGravity(Gravity.END);
        displayResult.setLayoutParams(ui.wt(1f));
        displayResult.setSingleLine(true);
        displayResult.setEllipsize(android.text.TextUtils.TruncateAt.START);
        resRow.addView(displayResult);
        card.addView(resRow);
        ui.spacer(card, Tokens.S4);

        displayRate = ui.tv("", Tokens.TEXT_XS, Tokens.surfaceTint, false);
        displayRate.setGravity(Gravity.END);
        card.addView(displayRate);

        // Tap result to copy
        displayResult.setClickable(true);
        displayResult.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm!=null) cm.setPrimaryClip(ClipData.newPlainText("calc",displayResult.getText().toString()));
            ui.snack(app.rootFrame,"Copied!",Tokens.GREEN);
        });
    }

    // ── Currency chips ─────────────────────────────────────────────────────────
    private void buildCurrencyChips(LinearLayout parent) {
        String[] quickCodes = {"USD","EUR","GBP","KES","AED","ZAR","JPY","CNY","XAU","XAG"};
        LinearLayout row = ui.hRow();
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        rp.bottomMargin = ui.dp(Tokens.S8); row.setLayoutParams(rp);

        HorizontalScrollView hsv = new HorizontalScrollView(app);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hsp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        hsp.bottomMargin = ui.dp(Tokens.S6); hsv.setLayoutParams(hsp);

        LinearLayout chipRow = ui.hRow();
        chipRow.setPadding(0, 0, ui.dp(Tokens.S8), 0);

        for (String c : quickCodes) {
            String label = app.FLAGS.getOrDefault(c,"💱")+" "+c;
            Button chip = ui.chip(label);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(WRAP_CONTENT, ui.dp(36));
            cp.rightMargin = ui.dp(Tokens.S6); chip.setLayoutParams(cp);
            if (c.equals(selectedCode)) { ui.chipSelected(chip, Tokens.GOLD); lastCurrChip=chip; }
            chip.setOnClickListener(v -> {
                if (lastCurrChip!=null) ui.chipUnselected(lastCurrChip);
                lastCurrChip=chip; ui.chipSelected(chip, Tokens.GOLD);
                selectedCode=c; updateDisplay();
            });
            chipRow.addView(chip);
        }
        hsv.addView(chipRow);
        parent.addView(hsv);
    }

    // ── Direction toggle ──────────────────────────────────────────────────────
    private void buildDirectionRow(LinearLayout parent) {
        LinearLayout row = ui.hRow();
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        rp.bottomMargin = ui.dp(Tokens.S10); row.setLayoutParams(rp);

        Button dirBtn = ui.solidBtn(selectedCode+" → TZS", Tokens.withAlpha(Tokens.BLUE,60), Tokens.BLUE);
        dirBtn.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ui.dp(40)));
        dirBtn.setTag("dirBtn");
        dirBtn.setOnClickListener(v -> {
            inputIsTzs = !inputIsTzs;
            dirBtn.setText(inputIsTzs ? "TZS → "+selectedCode : selectedCode+" → TZS");
            expr.setLength(0); updateDisplay();
        });
        row.addView(dirBtn);
        parent.addView(row);
    }

    // ── Keypad ────────────────────────────────────────────────────────────────
    private void buildKeypad(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        card.setPadding(ui.dp(Tokens.S8), ui.dp(Tokens.S8), ui.dp(Tokens.S8), ui.dp(Tokens.S8));

        String[][] keys = {
            {"7","8","9","⌫"},
            {"4","5","6","×"},
            {"1","2","3","+"},
            {".","0","=","%"}
        };
        int[][] colors = {
            {0,0,0,1},  // 1=accent, 0=normal
            {0,0,0,2},  // 2=operator
            {0,0,0,2},
            {0,0,0,3}   // 3=special
        };

        for (int r=0; r<4; r++) {
            LinearLayout keyRow = ui.hRow();
            keyRow.setWeightSum(4f);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH_PARENT, ui.dp(64));
            if (r<3) rp.bottomMargin = ui.dp(Tokens.S8); keyRow.setLayoutParams(rp);
            for (int c=0; c<4; c++) {
                final String k = keys[r][c];
                int type = colors[r][c];
                int bg, fg;
                switch(type){
                    case 1: bg=Tokens.withAlpha(Tokens.RED,80);  fg=Tokens.RED;   break;
                    case 2: bg=Tokens.withAlpha(Tokens.BLUE,40); fg=Tokens.BLUE;  break;
                    case 3: bg=Tokens.withAlpha(Tokens.GOLD,40); fg=Tokens.GOLD;  break;
                    default: bg=Tokens.surfaceVar; fg=Tokens.onSurface;
                }
                LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(0, MATCH_PARENT, 1f);
                if (c<3) kp.rightMargin = ui.dp(Tokens.S8);
                Button btn = ui.solidBtn(k, bg, fg);
                btn.setLayoutParams(kp);
                btn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,
                    k.equals("=") ? Tokens.TEXT_XL : Tokens.TEXT_2XL);
                btn.setTypeface(type==0 ? Tokens.fontNormal : Tokens.fontBold);
                btn.setOnClickListener(v -> { ui.scaleAnim(btn); onKey(k); });
                keyRow.addView(btn);
            }
            card.addView(keyRow);
        }
    }

    private void onKey(String k) {
        switch(k) {
            case "⌫":
                if (expr.length()>0) expr.deleteCharAt(expr.length()-1);
                break;
            case "=":
                try {
                    double val = evalExpr(expr.toString());
                    String res = new DecimalFormat("#,##0.########").format(val);
                    saveCalcHistory(expr.toString()+" = "+res+" "+(inputIsTzs?"TZS":selectedCode));
                    expr.setLength(0); expr.append(res);
                    loadHistory();
                } catch(Exception ignored){}
                break;
            case "×": expr.append("×"); break;
            case "%":
                try {
                    double v=evalExpr(expr.toString()); expr.setLength(0); expr.append(v/100.0);
                } catch(Exception ignored){} break;
            default: expr.append(k);
        }
        updateDisplay();
    }

    private double evalExpr(String s) {
        s = s.replace(",","").replace("×","*");
        // simple eval: handle +, -, *, /
        try {
            // split on last + or - not inside decimal
            for (int i=s.length()-1;i>=0;i--) {
                char c=s.charAt(i);
                if ((c=='+'||c=='-')&&i>0) {
                    double l=evalExpr(s.substring(0,i)), r=evalExpr(s.substring(i+1));
                    return c=='+'?l+r:l-r;
                }
            }
            for (int i=s.length()-1;i>=0;i--) {
                char c=s.charAt(i);
                if (c=='*'||c=='/') {
                    double l=evalExpr(s.substring(0,i)), r=evalExpr(s.substring(i+1));
                    return c=='*'?l*r:(r!=0?l/r:0);
                }
            }
            return Double.parseDouble(s.trim());
        } catch(Exception e){ return Double.parseDouble(s.replace("[^0-9.]","").trim()); }
    }

    private void updateDisplay() {
        String raw = expr.length()==0 ? "0" : expr.toString();
        displayInput.setText(raw);
        try {
            double val = evalExpr(raw);
            Double rate = app.rates.get(selectedCode);
            if (rate==null||rate==0) { displayResult.setText("—"); displayRate.setText(""); return; }
            double result;
            String resUnit;
            if (inputIsTzs) { result=val/rate; resUnit=selectedCode; }
            else             { result=val*rate; resUnit="TZS"; }
            DecimalFormat f = result>=1000?new DecimalFormat("#,##0.00"):new DecimalFormat("#,##0.0000");
            displayResult.setText(f.format(result)+" "+resUnit);
            displayRate.setText("1 "+selectedCode+" = "+app.fmtTzs.format(rate)+" TZS");
            displayCurrency.setText(selectedCode+"  "+app.FLAGS.getOrDefault(selectedCode,"💱"));
        } catch(Exception e){ displayResult.setText("—"); }
    }

    // ── Rate reference card ───────────────────────────────────────────────────
    private void buildRateCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        LinearLayout hdr = ui.hRow(); hdr.setGravity(Gravity.CENTER_VERTICAL);
        TextView ttl = ui.tv("📋  Quick Rate Reference", Tokens.TEXT_MD, Tokens.TEAL, true);
        ttl.setLayoutParams(ui.wt(1f)); hdr.addView(ttl); card.addView(hdr);
        ui.spacer(card, Tokens.S10); card.addView(ui.divider()); ui.spacer(card, Tokens.S8);

        String[] refCodes = {"USD","EUR","GBP","KES","AED","ZAR","JPY","XAU"};
        for (int i=0; i<refCodes.length; i++) {
            String c = refCodes[i]; Double r = app.rates.get(c);
            if (r==null) continue;
            LinearLayout row = ui.hRow();
            row.setPadding(ui.dp(Tokens.S8),ui.dp(Tokens.S8),ui.dp(Tokens.S8),ui.dp(Tokens.S8));
            if(i%2==0){GradientDrawable d=new GradientDrawable();d.setCornerRadius(ui.dp(Tokens.R6));d.setColor(Tokens.surfaceVar);row.setBackground(d);}
            TextView cTv=ui.tv(app.FLAGS.getOrDefault(c,"💱")+" "+c, Tokens.TEXT_SM, Tokens.onSurfaceVar, true);
            cTv.setLayoutParams(ui.wt(1f)); row.addView(cTv);
            DecimalFormat rf = c.equals("JPY")||CurrencyMeta.isSmallRate(c)?new DecimalFormat("#,##0.00"):new DecimalFormat("#,##0.00");
            TextView rTv = ui.mono("1 = "+rf.format(r)+" TZS", Tokens.TEXT_SM, Tokens.accentByIndex(i), true);
            row.addView(rTv); card.addView(row);
        }
    }

    // ── History card ──────────────────────────────────────────────────────────
    private void buildHistoryCard(LinearLayout parent) {
        LinearLayout card = ui.card(parent);
        LinearLayout hRow = ui.hRow();
        LinearLayout hCol = ui.vCol(); hCol.setLayoutParams(ui.wt(1f));
        hCol.addView(ui.tv("🕐  Calculator History", Tokens.TEXT_MD, Tokens.ORANGE, true));
        hCol.addView(ui.tv("Last 20 calculations", Tokens.TEXT_XS, Tokens.surfaceTint, false));
        hRow.addView(hCol);
        Button clr=ui.solidBtn("Clear",Tokens.withAlpha(Tokens.RED,30),Tokens.RED);
        clr.setLayoutParams(new LinearLayout.LayoutParams(ui.dp(70),ui.dp(34)));
        clr.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_XS);
        clr.setOnClickListener(v->{app.prefs.edit().putString(AppPrefs.KEY_CALC_HIST,"[]").apply();loadHistory();ui.snack(app.rootFrame,"History cleared",Tokens.ORANGE);});
        hRow.addView(clr); card.addView(hRow); ui.spacer(card,Tokens.S10); card.addView(ui.divider()); ui.spacer(card,Tokens.S8);
        histContainer=ui.vCol(); card.addView(histContainer);
        histEmpty=ui.tv("No calculations yet.",Tokens.TEXT_SM,Tokens.surfaceTint,false);
        histEmpty.setGravity(Gravity.CENTER); histEmpty.setPadding(0,ui.dp(Tokens.S16),0,ui.dp(Tokens.S16));
        histEmpty.setVisibility(View.GONE); card.addView(histEmpty);
        loadHistory();
    }

    private void loadHistory() {
        if (histContainer==null) return;
        histContainer.removeAllViews();
        List<String> hist=loadCalcList();
        if(histEmpty!=null) histEmpty.setVisibility(hist.isEmpty()?View.VISIBLE:View.GONE);
        for(int i=0;i<hist.size();i++){
            LinearLayout row=ui.hRow();
            row.setPadding(ui.dp(Tokens.S8),ui.dp(Tokens.S10),ui.dp(Tokens.S8),ui.dp(Tokens.S10));
            if(i%2==0){GradientDrawable d=new GradientDrawable();d.setCornerRadius(ui.dp(Tokens.R6));d.setColor(Tokens.surfaceVar);row.setBackground(d);}
            TextView badge=ui.badge(String.valueOf(i+1),Tokens.TEAL);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(WRAP_CONTENT,WRAP_CONTENT); bp.rightMargin=ui.dp(Tokens.S8); badge.setLayoutParams(bp); row.addView(badge);
            TextView entry=ui.tv(hist.get(i),Tokens.TEXT_SM,Tokens.onSurfaceVar,false); entry.setLayoutParams(ui.wt(1f)); row.addView(entry);
            histContainer.addView(row);
            if(i<hist.size()-1) histContainer.addView(ui.divider());
        }
    }

    private List<String> loadCalcList(){List<String> l=new ArrayList<>();try{JSONArray a=new JSONArray(app.prefs.getString(AppPrefs.KEY_CALC_HIST,"[]")); for(int i=0;i<a.length();i++) l.add(a.getString(i));}catch(Exception ignored){} return l;}
    private void saveCalcHistory(String e){List<String> l=loadCalcList();l.add(0,e);if(l.size()>20)l.subList(20,l.size()).clear();try{JSONArray a=new JSONArray();for(String s:l)a.put(s);app.prefs.edit().putString(AppPrefs.KEY_CALC_HIST,a.toString()).apply();}catch(Exception ignored){}}
}

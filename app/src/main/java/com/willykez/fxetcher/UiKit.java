package com.willykez.fxetcher;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.ViewParent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import androidx.appcompat.widget.SwitchCompat;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * FXetcher UI Kit
 *
 * A stateless factory of pre-styled Android views implementing the Tokens design
 * system. Every component method returns a fully configured view — no XML needed.
 *
 * Components:
 *   Typography   — tv(), label(), mono()
 *   Layout       — card(), hRow(), vCol(), spacer(), spacerH(), divider()
 *   Buttons      — solidBtn(), outlineBtn(), ghostBtn(), iconBtn(), pillBtn()
 *   Inputs       — styledInput(), styledSpinner()
 *   Chips        — chip(), chipRow()
 *   Rows         — rateRow() building block, sectionHeader()
 *   Shimmer      — shimmerBlock(), shimmerCard()
 *   Ripple       — rippleDrawable() utility
 *   Snackbar     — snack()
 *   Misc         — badge(), dot(), separator()
 */
public final class UiKit {

    private final Context ctx;

    public UiKit(Context ctx) { this.ctx = ctx; }

    // ════════════════════════════════════════════════════════════════════════
    //  DIMENSION HELPER
    // ════════════════════════════════════════════════════════════════════════

    public int dp(int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    public int sp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, v,
                ctx.getResources().getDisplayMetrics()));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TYPOGRAPHY
    // ════════════════════════════════════════════════════════════════════════

    /** General-purpose TextView. */
    public TextView tv(String text, int sizeSp, int colorInt, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(colorInt);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTypeface(bold ? Tokens.fontBold : Tokens.fontNormal);
        return t;
    }

    /** Small uppercase label (letter-spaced). */
    public TextView label(String text, int colorInt) {
        TextView t = tv(text, Tokens.TEXT_XS, colorInt, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            t.setLetterSpacing(0.12f);
        }
        return t;
    }

    /** Monospace text — for currency amounts, rates. */
    public TextView mono(String text, int sizeSp, int colorInt, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(colorInt);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTypeface(bold ? Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                           : Typeface.MONOSPACE);
        return t;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LAYOUT CONTAINERS
    // ════════════════════════════════════════════════════════════════════════

    public LinearLayout hRow() {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    public LinearLayout vCol() {
        LinearLayout c = new LinearLayout(ctx);
        c.setOrientation(LinearLayout.VERTICAL);
        return c;
    }

    /** Weighted horizontal spacer. */
    public View flexSpacer() {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    /**
     * Elevated card with subtle glow border.
     * Adds itself to {@code parent} if non-null.
     */
    public LinearLayout card(LinearLayout parent) {
        LinearLayout c = vCol();
        c.setPadding(dp(Tokens.S16), dp(Tokens.S16), dp(Tokens.S16), dp(Tokens.S16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        lp.bottomMargin = dp(Tokens.S12);
        c.setLayoutParams(lp);

        // Two-layer drawable: fill + border with subtle glow
        c.setBackground(cardDrawable());
        if (parent != null) parent.addView(c);
        return c;
    }

    public GradientDrawable cardDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(Tokens.R16));
        d.setColor(Tokens.surface);
        d.setStroke(dp(1), Tokens.outline);
        return d;
    }

    /** Fixed-height vertical spacer added to parent. */
    public void spacer(LinearLayout parent, int heightDp) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, dp(heightDp)));
        parent.addView(v);
    }

    /** Fixed-width horizontal spacer added to parent. */
    public void spacerH(LinearLayout parent, int widthDp) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), MATCH_PARENT));
        parent.addView(v);
    }

    /** 1dp divider line. */
    public View divider() {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, dp(1)));
        v.setBackgroundColor(Tokens.outlineVar);
        return v;
    }

    public LinearLayout.LayoutParams wt(float weight) {
        return new LinearLayout.LayoutParams(0, WRAP_CONTENT, weight);
    }

    public LinearLayout.LayoutParams matchWt(float weight) {
        return new LinearLayout.LayoutParams(0, MATCH_PARENT, weight);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SECTION HEADER
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Coloured icon badge + bold title + optional subtitle.
     * Added directly to {@code parent}.
     */
    public void sectionHeader(LinearLayout parent, String icon, String title,
                               String subtitle, int accentColor) {
        LinearLayout row = hRow();

        // Badge circle
        LinearLayout badge = new LinearLayout(ctx);
        badge.setGravity(Gravity.CENTER);
        int bs = dp(36);
        badge.setLayoutParams(new LinearLayout.LayoutParams(bs, bs));
        GradientDrawable bd = new GradientDrawable();
        bd.setShape(GradientDrawable.OVAL);
        bd.setColor(Tokens.withAlpha(accentColor, 30));
        bd.setStroke(dp(1), Tokens.withAlpha(accentColor, 70));
        badge.setBackground(bd);
        badge.addView(tv(icon, Tokens.TEXT_MD, accentColor, false));
        row.addView(badge);
        spacerH(row, Tokens.S12);

        LinearLayout textCol = vCol();
        textCol.setLayoutParams(wt(1f));
        textCol.addView(tv(title, Tokens.TEXT_LG, Tokens.onSurface, true));
        if (subtitle != null && !subtitle.isEmpty()) {
            spacer(textCol, 1);
            textCol.addView(tv(subtitle, Tokens.TEXT_XS, Tokens.onSurfaceVar, false));
        }
        row.addView(textCol);
        parent.addView(row);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BUTTONS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Solid filled button with ripple.
     * Minimum 48dp height to meet touch target guidelines.
     */
    public Button solidBtn(String text, int bgColor, int fgColor) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setTextColor(fgColor);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_MD);
        b.setTypeface(Tokens.fontBold);
        b.setAllCaps(false);
        b.setMinHeight(dp(Tokens.TOUCH_TARGET));
        b.setPadding(dp(Tokens.S20), 0, dp(Tokens.S20), 0);
        b.setBackground(rippleOver(roundRect(dp(Tokens.R10), bgColor, 0, 0)));
        return b;
    }

    /** Outlined ghost button. */
    public Button outlineBtn(String text, int accentColor) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setTextColor(accentColor);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_MD);
        b.setTypeface(Tokens.fontBold);
        b.setAllCaps(false);
        b.setMinHeight(dp(Tokens.TOUCH_TARGET));
        b.setPadding(dp(Tokens.S16), 0, dp(Tokens.S16), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, dp(Tokens.TOUCH_TARGET + 4));
        lp.bottomMargin = dp(4);
        b.setLayoutParams(lp);
        b.setBackground(rippleOver(roundRect(dp(Tokens.R10),
                Tokens.withAlpha(accentColor, 20), dp(1), accentColor)));
        return b;
    }

    /** Transparent icon-only button (for top bar actions). */
    public Button ghostBtn(String icon, int colorInt) {
        Button b = new Button(ctx);
        b.setText(icon);
        b.setTextColor(colorInt);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_LG);
        b.setAllCaps(false);
        b.setBackground(rippleOver(new ColorDrawable(Color.TRANSPARENT)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
        b.setLayoutParams(lp);
        b.setPadding(0, 0, 0, 0);
        b.setGravity(Gravity.CENTER);
        return b;
    }

    /** Pill-shaped filled button (for "Swap", etc.). */
    public Button pillBtn(String text, int bgColor, int fgColor) {
        Button b = solidBtn(text, bgColor, fgColor);
        b.setBackground(rippleOver(roundRect(dp(Tokens.R99), bgColor, 0, 0)));
        return b;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INPUTS
    // ════════════════════════════════════════════════════════════════════════

    /** Styled EditText for amount / search inputs. */
    public EditText styledInput(String hint, int inputType) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setHintTextColor(Tokens.surfaceTint);
        et.setTextColor(Tokens.onSurface);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_2XL);
        et.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        et.setSingleLine(true);
        et.setInputType(inputType);
        et.setSelectAllOnFocus(true);
        et.setHighlightColor(Tokens.withAlpha(Tokens.BLUE, 60));
        et.setBackground(roundRect(dp(Tokens.R12),
                Tokens.surfaceVar, dp(2), Tokens.BLUE));
        et.setPadding(dp(Tokens.S16), dp(Tokens.S14), dp(Tokens.S16), dp(Tokens.S14));
        et.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        return et;
    }

    /** Styled Spinner (FROM / TO currency pickers). */
    public Spinner styledSpinner(String[] items) {
        Spinner s = new Spinner(ctx);
        ArrayAdapter<String> a = new ArrayAdapter<>(
                ctx, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        s.setBackground(roundRect(dp(Tokens.R10),
                Tokens.surfaceVar, dp(1), Tokens.outline));
        s.setPadding(dp(Tokens.S12), dp(Tokens.S12), dp(Tokens.S12), dp(Tokens.S12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                MATCH_PARENT, dp(Tokens.TOUCH_TARGET + 8));
        lp.bottomMargin = dp(4);
        s.setLayoutParams(lp);
        return s;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CHIPS
    // ════════════════════════════════════════════════════════════════════════

    /** A tappable amount chip (Quick Amounts panel). */
    public Button chip(String text) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setTextColor(Tokens.onSurfaceVar);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_SM);
        b.setTypeface(Tokens.fontBold);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(Tokens.S4), 0, dp(Tokens.S4), 0);
        b.setBackground(rippleOver(roundRect(dp(Tokens.R8),
                Tokens.surfaceVar, dp(1), Tokens.outline)));
        return b;
    }

    /** Mark a chip as selected (replaces background). */
    public void chipSelected(Button chip, int accentColor) {
        chip.setTextColor(accentColor);
        chip.setBackground(rippleOver(roundRect(dp(Tokens.R8),
                Tokens.withAlpha(accentColor, 35), dp(1), accentColor)));
    }

    /** Reset chip to unselected state. */
    public void chipUnselected(Button chip) {
        chip.setTextColor(Tokens.onSurfaceVar);
        chip.setBackground(rippleOver(roundRect(dp(Tokens.R8),
                Tokens.surfaceVar, dp(1), Tokens.outline)));
    }

    /** Row of equal-weight chips (4 per row). */
    public LinearLayout chipRow() {
        LinearLayout r = hRow();
        r.setWeightSum(4f);
        return r;
    }

    public LinearLayout.LayoutParams chipParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(38), 1f);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RATE ROW BUILDING BLOCK
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Standard rate list row — flag / name / code on left, rate on right.
     * Returns the root view; callers add to parent themselves.
     */
    public LinearLayout rateRow(String flag, String name, String code,
                                 String rateText, int accentColor,
                                 View.OnClickListener onClick) {
        LinearLayout row = hRow();
        row.setPadding(dp(Tokens.S12), dp(Tokens.S12), dp(Tokens.S12), dp(Tokens.S12));
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        rp.bottomMargin = dp(Tokens.S6);
        row.setLayoutParams(rp);

        // Coloured left accent bar
        View accent = new View(ctx);
        accent.setLayoutParams(new LinearLayout.LayoutParams(dp(3), dp(36)));
        accent.setBackground(roundRect(dp(2), accentColor, 0, 0));
        row.addView(accent);
        spacerH(row, Tokens.S12);

        // Flag badge
        LinearLayout flagBadge = new LinearLayout(ctx);
        flagBadge.setGravity(Gravity.CENTER);
        int bs = dp(38);
        flagBadge.setLayoutParams(new LinearLayout.LayoutParams(bs, bs));
        GradientDrawable fd = new GradientDrawable();
        fd.setShape(GradientDrawable.OVAL);
        fd.setColor(Tokens.withAlpha(accentColor, 22));
        flagBadge.setBackground(fd);
        flagBadge.addView(tv(flag, Tokens.TEXT_LG, Tokens.onSurface, false));
        row.addView(flagBadge);
        spacerH(row, Tokens.S10);

        // Name + code column
        LinearLayout nameCol = vCol();
        nameCol.setLayoutParams(wt(1f));
        nameCol.addView(tv(name, Tokens.TEXT_MD, Tokens.onSurface, true));
        nameCol.addView(tv(code, Tokens.TEXT_XS, Tokens.surfaceTint, false));
        row.addView(nameCol);

        // Rate + change column
        LinearLayout rateCol = vCol();
        rateCol.setGravity(Gravity.END);
        TextView rateTv = mono(rateText, Tokens.TEXT_MD, Tokens.GOLD, true);
        rateTv.setGravity(Gravity.END);
        rateCol.addView(rateTv);

        // Change pill (populated later by updateRateRow)
        TextView changePill = tv("", Tokens.TEXT_XS, Tokens.onSurfaceVar, true);
        changePill.setGravity(Gravity.END);
        changePill.setPadding(0, dp(2), 0, 0);
        rateCol.addView(changePill);
        row.addView(rateCol);

        // Ripple + rounded card background
        row.setBackground(rippleOver(roundRect(dp(Tokens.R12), Tokens.surfaceVar, dp(1), Tokens.outline)));
        row.setClickable(true);
        row.setFocusable(true);
        if (onClick != null) row.setOnClickListener(onClick);

        // Tag sub-views for later updates (rate TV at [0], change pill at [1])
        row.setTag(new TextView[]{rateTv, changePill});
        return row;
    }

    /** Update the rate and change pill on an existing rateRow. */
    public void updateRateRow(LinearLayout row, String rateText,
                               double pct, boolean up) {
        if (!(row.getTag() instanceof TextView[])) return;
        TextView[] tvs = (TextView[]) row.getTag();
        tvs[0].setText(rateText);
        if (pct != 0) {
            String arrow = up ? "▲" : "▼";
            int col = up ? Tokens.GREEN : Tokens.RED;
            tvs[1].setText(String.format(java.util.Locale.US,
                    "%s %.2f%%", arrow, Math.abs(pct)));
            tvs[1].setTextColor(col);
            // Pill background
            GradientDrawable pg = new GradientDrawable();
            pg.setCornerRadius(dp(Tokens.R99));
            pg.setColor(Tokens.withAlpha(col, 25));
            tvs[1].setBackground(pg);
            tvs[1].setPadding(dp(5), dp(1), dp(5), dp(1));
        } else {
            tvs[1].setText("");
            tvs[1].setBackground(null);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SHIMMER LOADING SKELETON
    // ════════════════════════════════════════════════════════════════════════

    /** A single shimmering placeholder bar. */
    public View shimmerBar(int widthDp, int heightDp) {
        View v = new View(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                widthDp > 0 ? dp(widthDp) : MATCH_PARENT, dp(heightDp));
        lp.bottomMargin = dp(Tokens.S8);
        v.setLayoutParams(lp);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(heightDp / 2));
        d.setColor(Tokens.surfaceVar);
        v.setBackground(d);
        startShimmer(v);
        return v;
    }

    /** A complete shimmer card (3 rows = one rate card placeholder). */
    public LinearLayout shimmerCard(LinearLayout parent) {
        LinearLayout card = card(parent);
        card.addView(shimmerBar(120, 10));
        spacer(card, Tokens.S8);
        for (int i = 0; i < 4; i++) {
            LinearLayout row = hRow();
            row.addView(shimmerBar(38, 38));
            spacerH(row, Tokens.S12);
            LinearLayout tc = vCol();
            tc.setLayoutParams(wt(1f));
            tc.addView(shimmerBar(0, 10));
            spacer(tc, 4);
            tc.addView(shimmerBar(0, 8));
            row.addView(tc);
            spacerH(row, Tokens.S8);
            row.addView(shimmerBar(80, 12));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            rp.bottomMargin = dp(Tokens.S12);
            row.setLayoutParams(rp);
            card.addView(row);
        }
        return card;
    }

    /** Pulse-shimmer animation on a single view. */
    public void startShimmer(View v) {
        ValueAnimator anim = ValueAnimator.ofFloat(0.4f, 1f, 0.4f);
        anim.setDuration(Tokens.SHIMMER_DURATION);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> v.setAlpha((float) a.getAnimatedValue()));
        anim.start();
        v.setTag(anim); // store so caller can cancel
    }

    public void stopShimmer(View v) {
        Object tag = v.getTag();
        if (tag instanceof ValueAnimator) ((ValueAnimator) tag).cancel();
        v.setAlpha(1f);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BADGE / DOT / STATUS INDICATORS
    // ════════════════════════════════════════════════════════════════════════

    /** Small coloured tag badge (e.g. "Active", "LIVE"). */
    public TextView badge(String text, int accentColor) {
        TextView t = tv(text, Tokens.TEXT_XS, accentColor, true);
        t.setPadding(dp(Tokens.S6), dp(2), dp(Tokens.S6), dp(2));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(Tokens.R99));
        bg.setColor(Tokens.withAlpha(accentColor, 30));
        bg.setStroke(dp(1), Tokens.withAlpha(accentColor, 80));
        t.setBackground(bg);
        return t;
    }

    /** Pulsing live indicator dot. */
    public View liveDot(int colorInt) {
        View dot = new View(ctx);
        int size = dp(10);
        dot.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(colorInt);
        dot.setBackground(d);
        return dot;
    }

    /** Start the pulsing animation on a live dot. */
    public ObjectAnimator pulseDot(View dot, boolean active) {
        if (!active) {
            dot.setAlpha(1f);
            ((GradientDrawable) dot.getBackground()).setColor(Tokens.GREEN);
            return null;
        }
        ((GradientDrawable) dot.getBackground()).setColor(Tokens.AMBER);
        ObjectAnimator anim = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.2f, 1f);
        anim.setRepeatCount(ObjectAnimator.INFINITE);
        anim.setDuration(700);
        anim.start();
        return anim;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TOP BAR RATE PILL
    // ════════════════════════════════════════════════════════════════════════

    /** Compact rate pill for the sticky top bar. */
    public TextView topPill(String text, int accentColor) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(accentColor);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.TEXT_XS);
        t.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        t.setPadding(dp(Tokens.S8), dp(Tokens.S4), dp(Tokens.S8), dp(Tokens.S4));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(Tokens.R99));
        bg.setColor(Tokens.withAlpha(accentColor, 20));
        bg.setStroke(dp(1), Tokens.withAlpha(accentColor, 50));
        t.setBackground(bg);
        return t;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RESULT BOX (Convert screen)
    // ════════════════════════════════════════════════════════════════════════

    /** The large green result display box in the converter. */
    public LinearLayout resultBox() {
        LinearLayout box = vCol();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(Tokens.S20), dp(Tokens.S20), dp(Tokens.S20), dp(Tokens.S20));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(Tokens.R16));
        bg.setColor(Tokens.withAlpha(Tokens.GREEN, Tokens.isDark ? 18 : 25));
        bg.setStroke(dp(2), Tokens.withAlpha(Tokens.GREEN, 80));
        box.setBackground(bg);
        return box;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SNACKBAR
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Show a styled Snackbar-style bottom message.
     * Slides in from the bottom, auto-dismisses after 2.5s.
     * Uses the provided root view as anchor.
     */
    public void snack(View rootView, String message, int accentColor) {
        // Find or create the snack container
        ViewGroup root = findSnackHost(rootView);
        if (root == null) { android.widget.Toast.makeText(ctx, message, android.widget.Toast.LENGTH_SHORT).show(); return; }

        // Build snack view
        LinearLayout snack = hRow();
        int pad = dp(Tokens.S16);
        snack.setPadding(pad, dp(Tokens.S14), pad, dp(Tokens.S14));
        GradientDrawable sd = new GradientDrawable();
        sd.setCornerRadius(dp(Tokens.R12));
        sd.setColor(Tokens.isDark ? 0xFF1E2440 : 0xFF1A1A2E);
        sd.setStroke(dp(1), accentColor);
        snack.setBackground(sd);

        // Coloured left accent stripe
        View stripe = new View(ctx);
        stripe.setLayoutParams(new LinearLayout.LayoutParams(dp(3), MATCH_PARENT));
        GradientDrawable stripeD = new GradientDrawable();
        stripeD.setCornerRadius(dp(2));
        stripeD.setColor(accentColor);
        stripe.setBackground(stripeD);
        snack.addView(stripe);
        spacerH(snack, Tokens.S12);

        TextView msg = tv(message, Tokens.TEXT_SM, 0xFFECEFF8, false);
        msg.setLayoutParams(wt(1f));
        snack.addView(msg);

        // Position it at the bottom
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT);
        flp.gravity = Gravity.BOTTOM;
        flp.setMargins(dp(Tokens.S16), 0, dp(Tokens.S16), dp(80));
        snack.setLayoutParams(flp);
        snack.setAlpha(0f);
        snack.setTranslationY(dp(40));

        root.addView(snack);

        snack.animate().alpha(1f).translationY(0f).setDuration(240).start();

        rootView.postDelayed(() -> snack.animate()
                .alpha(0f)
                .translationY(dp(20))
                .setDuration(200)
                .withEndAction(() -> {
                    try { root.removeView(snack); } catch (Exception ignored) {}
                })
                .start(), 2500);
    }

    private ViewGroup findSnackHost(View v) {
        if (v instanceof ViewGroup) {
            // Walk up to find a FrameLayout (typical root)
            View current = v;
            while (current != null) {
                if (current instanceof FrameLayout) return (ViewGroup) current;
                ViewParent p = current.getParent();
                current = (p instanceof View) ? (View) p : null;
            }
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TOGGLE / PREFERENCE ROW
    // ════════════════════════════════════════════════════════════════════════

    public interface OnToggle { void onChanged(boolean checked); }

    /** Settings preference row with Switch. */
    public LinearLayout toggleRow(String title, String subtitle,
                                   boolean checked, OnToggle listener) {
        LinearLayout row = hRow();
        row.setPadding(0, dp(Tokens.S6), 0, dp(Tokens.S6));
        row.setMinimumHeight(dp(Tokens.TOUCH_TARGET));

        LinearLayout textCol = vCol();
        textCol.setLayoutParams(wt(1f));
        textCol.addView(tv(title, Tokens.TEXT_MD, Tokens.onSurface, true));
        spacer(textCol, 2);
        textCol.addView(tv(subtitle, Tokens.TEXT_XS, Tokens.onSurfaceVar, false));
        row.addView(textCol);

        SwitchCompat sw = new SwitchCompat(ctx);
        sw.setChecked(checked);
        sw.getThumbDrawable().setTint(checked ? Tokens.GOLD : Tokens.outline);
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            sw.getThumbDrawable().setTint(isChecked ? Tokens.GOLD : Tokens.outline);
            listener.onChanged(isChecked);
        });
        row.addView(sw);
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FLASH ANIMATION  (rate change highlight)
    // ════════════════════════════════════════════════════════════════════════

    /** Flash a view's background to highlight a rate change. */
    public void flashRow(View v, int accentColor, int baseColor) {
        if (v == null) return;
        GradientDrawable d = roundRect(dp(Tokens.R12), baseColor, dp(1), Tokens.outline);
        v.setBackground(d);
        int flash = Tokens.withAlpha(accentColor, 55);
        ValueAnimator anim = ValueAnimator.ofObject(
                new ArgbEvaluator(), baseColor, flash, baseColor);
        anim.setDuration(700);
        anim.addUpdateListener(a -> d.setColor((int) a.getAnimatedValue()));
        anim.start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DRAWABLE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    public GradientDrawable roundRect(int radiusPx, int fillColor,
                                       int strokeWidthPx, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(radiusPx);
        d.setColor(fillColor);
        if (strokeWidthPx > 0) d.setStroke(strokeWidthPx, strokeColor);
        return d;
    }

    /** Wrap any drawable with a Material ripple. */
    public RippleDrawable rippleOver(android.graphics.drawable.Drawable content) {
        int rippleColor = Tokens.withAlpha(Tokens.GOLD, 40);
        return new RippleDrawable(
                android.content.res.ColorStateList.valueOf(rippleColor),
                content, null);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FIELD LABEL
    // ════════════════════════════════════════════════════════════════════════

    /** Small uppercase spaced field label (AMOUNT, FROM, TO). */
    public TextView fieldLabel(String text) {
        return label(text, Tokens.surfaceTint);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GENERAL TEXT WATCHER HELPER
    // ════════════════════════════════════════════════════════════════════════

    public interface AfterChanged { void run(String s); }

    public TextWatcher afterChanged(AfterChanged cb) {
        return new TextWatcher() {
            public void afterTextChanged(Editable s) { cb.run(s.toString()); }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        };
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INFO ROW (Settings / About)
    // ════════════════════════════════════════════════════════════════════════

    public LinearLayout infoRow(String label, String value) {
        LinearLayout row = hRow();
        row.setPadding(0, dp(Tokens.S6), 0, dp(Tokens.S6));
        TextView lv = tv(label, Tokens.TEXT_SM, Tokens.onSurfaceVar, false);
        lv.setLayoutParams(wt(1f));
        row.addView(lv);
        row.addView(tv(value, Tokens.TEXT_SM, Tokens.onSurface, true));
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SCALE ANIMATION (nav tap feedback)
    // ════════════════════════════════════════════════════════════════════════

    public void scaleAnim(View v) {
        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }
}

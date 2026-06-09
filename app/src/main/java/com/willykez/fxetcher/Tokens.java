package com.willykez.fxetcher;

import android.graphics.Color;
import android.graphics.Typeface;

/**
* FXetcher Design Tokens
*
* Every colour, spacing, radius, elevation and typography value lives here.
* Two themes are supported: DARK (default) and LIGHT.
* Swap {@link #apply(boolean)} on recreate — no XML required.
*
* Naming convention mirrors Material3 role names so migrating later is trivial:
*   surface      → card background
*   surfaceVar   → alternate row / input background
*   outline      → border / divider
*   onSurface    → primary text
*   onSurfaceVar → secondary text
*   surfaceTint  → subtle hint text
*/
public final class Tokens {
	
	// ── Accent palette (theme-invariant) ──────────────────────────────────────
	public static final int GOLD    = 0xFFFFD700;
	public static final int GREEN   = 0xFF4CAF50;
	public static final int RED     = 0xFFEF5350;
	public static final int BLUE    = 0xFF42A5F5;
	public static final int ORANGE  = 0xFFFF9800;
	public static final int PURPLE  = 0xFFAB47BC;
	public static final int TEAL    = 0xFF26A69A;
	public static final int AMBER   = 0xFFFFC107;
	
	// ── Hex strings (needed by some APIs) ────────────────────────────────────
	public static final String GOLD_HEX   = "#FFD700";
	public static final String GREEN_HEX  = "#4CAF50";
	public static final String RED_HEX    = "#EF5350";
	public static final String BLUE_HEX   = "#42A5F5";
	public static final String ORANGE_HEX = "#FF9800";
	public static final String PURPLE_HEX = "#AB47BC";
	public static final String TEAL_HEX   = "#26A69A";
	public static final String AMBER_HEX  = "#FFC107";
	
	// ── Per-theme surface roles (mutated by apply()) ──────────────────────────
	public static boolean isDark = true;
	
	// Background layers
	public static int bg;           // screen/page background
	public static int surface;      // card surface
	public static int surfaceVar;   // alternate row, input bg
	public static int overlay;      // modal scrim
	
	// Border / divider
	public static int outline;
	public static int outlineVar;   // subtle divider
	
	// Text
	public static int onSurface;    // primary body text
	public static int onSurfaceVar; // secondary/muted text
	public static int surfaceTint;  // placeholder / hint
	
	// Navigation
	public static int navBg;
	public static int statusBarColor;
	
	// Elevation shadow tint (for card glow)
	public static int cardGlow;
	
	// ── Spacing ───────────────────────────────────────────────────────────────
	public static final int S2  = 2;
	public static final int S4  = 4;
	public static final int S6  = 6;
	public static final int S8  = 8;
	public static final int S10 = 10;
	public static final int S12 = 12;
	public static final int S14 = 14;
	public static final int S16 = 16;
	public static final int S20 = 20;
	public static final int S24 = 24;
	public static final int S32 = 32;
	
	// ── Corner radii (dp) ─────────────────────────────────────────────────────
	// ── Corner radii (dp) ─────────────────────────────────────────────────────
	public static final int R2  = 2;
	public static final int R4  = 4;
	public static final int R6  = 6;
	public static final int R8  = 8;
	public static final int R10 = 10;
	public static final int R12 = 12;
	public static final int R14 = 14;
	public static final int R16 = 16;
	public static final int R20 = 20;
	public static final int R24 = 24;
	public static final int R99 = 99;
	// ── Typography (sp) ───────────────────────────────────────────────────────
	public static final int TEXT_XS  = 10;
	public static final int TEXT_SM  = 12;
	public static final int TEXT_MD  = 14;
	public static final int TEXT_LG  = 16;
	public static final int TEXT_XL  = 18;
	public static final int TEXT_2XL = 22;
	public static final int TEXT_3XL = 28;
	public static final int TEXT_4XL = 36;
	
	// Typefaces — set lazily by the Activity once the Context is available
	public static Typeface fontBold   = Typeface.DEFAULT_BOLD;
	public static Typeface fontNormal = Typeface.DEFAULT;
	// Monospace for numbers — feels like a real financial terminal
	public static Typeface fontMono   = Typeface.MONOSPACE;
	
	// ── Touch targets ─────────────────────────────────────────────────────────
	public static final int TOUCH_TARGET = 48; // dp — Material guideline
	
	// ── Nav bar height ────────────────────────────────────────────────────────
	public static final int NAV_HEIGHT = 64; // dp
	
	// ── Shimmer timing ────────────────────────────────────────────────────────
	public static final long SHIMMER_DURATION = 1200; // ms per cycle
	
	// ─────────────────────────────────────────────────────────────────────────
	/**
* Apply the dark or light theme. Call once in Activity.onCreate() and again
* after the user toggles theme before recreating the activity.
*/	
	public static void apply(boolean dark) {
		isDark = dark;
		if (dark) {
			bg           = 0xFF090C1A; // very deep navy — not pure black
			surface      = 0xFF131829; // card bg — slightly lighter
			surfaceVar   = 0xFF0F1320; // alt row / input bg
			overlay      = 0xCC090C1A; // modal scrim
			
			outline      = 0xFF1E2440; // card border
			outlineVar   = 0xFF161C34; // subtle divider
			
			onSurface    = 0xFFECEFF8; // near-white, slightly cool
			onSurfaceVar = 0xFF8A8FA8; // muted secondary
			surfaceTint  = 0xFF444966; // hint / placeholder
			
			navBg        = 0xFF0C0F1E;
			statusBarColor = 0xFF090C1A;
			cardGlow     = 0x1A42A5F5; // faint blue glow
		} else {
			bg           = 0xFFF0F3FC;
			surface      = 0xFFFFFFFF;
			surfaceVar   = 0xFFF5F7FF;
			overlay      = 0xBBFFFFFF;
			
			outline      = 0xFFDDE2F5;
			outlineVar   = 0xFFE8ECF8;
			
			onSurface    = 0xFF0F1328;
			onSurfaceVar = 0xFF5A607A;
			surfaceTint  = 0xFF9CA2BC;
			
			navBg        = 0xFFFFFFFF;
			statusBarColor = 0xFF1A237E;
			cardGlow     = 0x0A1A237E;
		}
	}
	
	// ── Colour utilities ──────────────────────────────────────────────────────
	
	/** Blend alpha into a colour int. alpha = 0..255 */
	public static int withAlpha(int color, int alpha) {
		return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
	}
	
	/** Parse hex and apply alpha 0..255 */
	public static int withAlpha(String hexColor, int alpha) {
		return withAlpha(Color.parseColor(hexColor), alpha);
	}
	
	/** Row accent colours by index — used in rate rows, grid cells, etc. */
	public static int accentByIndex(int i) {
		int[] palette = {BLUE, GOLD, GREEN, ORANGE, RED, TEAL, PURPLE, AMBER, GREEN};
		return palette[i % palette.length];
	}
	
	private Tokens() {}
}

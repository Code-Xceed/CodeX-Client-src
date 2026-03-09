package com.codex.client.gui.navigator.settings;

public final class GuiSettings {
    public static final int DEFAULT_ACCENT_R = 58;
    public static final int DEFAULT_ACCENT_G = 179;
    public static final int DEFAULT_ACCENT_B = 87;
    public static final int DEFAULT_ACCENT_A = 255;

    public static final int DEFAULT_BG_TINT_R = 28;
    public static final int DEFAULT_BG_TINT_G = 44;
    public static final int DEFAULT_BG_TINT_B = 68;
    public static final int DEFAULT_BG_TINT_A = 144;

    public static final int DEFAULT_PANEL_OPACITY = 149;
    public static final boolean DEFAULT_ANIMATIONS_ENABLED = true;
    public static final double DEFAULT_ANIMATION_SPEED = 1.0d;
    public static final boolean DEFAULT_CLICK_SOUNDS = true;

    private static final GuiSettings INSTANCE = new GuiSettings();

    private int accentR = DEFAULT_ACCENT_R;
    private int accentG = DEFAULT_ACCENT_G;
    private int accentB = DEFAULT_ACCENT_B;
    private int accentA = DEFAULT_ACCENT_A;

    private int backgroundTintR = DEFAULT_BG_TINT_R;
    private int backgroundTintG = DEFAULT_BG_TINT_G;
    private int backgroundTintB = DEFAULT_BG_TINT_B;
    private int backgroundTintA = DEFAULT_BG_TINT_A;

    private int panelOpacity = DEFAULT_PANEL_OPACITY;
    private boolean animationsEnabled = DEFAULT_ANIMATIONS_ENABLED;
    private double animationSpeedMultiplier = DEFAULT_ANIMATION_SPEED;
    private boolean clickSoundsEnabled = DEFAULT_CLICK_SOUNDS;

    private GuiSettings() {}

    public static GuiSettings get() {
        return INSTANCE;
    }

    public synchronized void resetVisualDefaults() {
        accentR = DEFAULT_ACCENT_R;
        accentG = DEFAULT_ACCENT_G;
        accentB = DEFAULT_ACCENT_B;
        accentA = DEFAULT_ACCENT_A;

        backgroundTintR = DEFAULT_BG_TINT_R;
        backgroundTintG = DEFAULT_BG_TINT_G;
        backgroundTintB = DEFAULT_BG_TINT_B;
        backgroundTintA = DEFAULT_BG_TINT_A;

        panelOpacity = DEFAULT_PANEL_OPACITY;
    }

    public synchronized int accentColor(int alpha) {
        int scaledAlpha = clamp((alpha * accentA) / 255, 0, 255);
        return argb(scaledAlpha, accentR, accentG, accentB);
    }

    public synchronized int backgroundTopColor() {
        int topAlpha = clamp((int) Math.round(backgroundTintA * 0.56d), 0, 255);
        return argb(topAlpha, backgroundTintR, backgroundTintG, backgroundTintB);
    }

    public synchronized int backgroundBottomColor() {
        return argb(backgroundTintA, backgroundTintR, backgroundTintG, backgroundTintB);
    }

    public synchronized int panelFillColor() {
        return argb(panelOpacity, 5, 5, 5);
    }

    public synchronized int accentPreviewColor() {
        return argb(accentA, accentR, accentG, accentB);
    }

    public synchronized int backgroundPreviewColor() {
        return argb(backgroundTintA, backgroundTintR, backgroundTintG, backgroundTintB);
    }

    public synchronized int getAccentR() {
        return accentR;
    }

    public synchronized void setAccentR(int accentR) {
        this.accentR = clamp(accentR, 0, 255);
    }

    public synchronized int getAccentG() {
        return accentG;
    }

    public synchronized void setAccentG(int accentG) {
        this.accentG = clamp(accentG, 0, 255);
    }

    public synchronized int getAccentB() {
        return accentB;
    }

    public synchronized void setAccentB(int accentB) {
        this.accentB = clamp(accentB, 0, 255);
    }

    public synchronized int getAccentA() {
        return accentA;
    }

    public synchronized void setAccentA(int accentA) {
        this.accentA = clamp(accentA, 0, 255);
    }

    public synchronized int getBackgroundTintR() {
        return backgroundTintR;
    }

    public synchronized void setBackgroundTintR(int backgroundTintR) {
        this.backgroundTintR = clamp(backgroundTintR, 0, 255);
    }

    public synchronized int getBackgroundTintG() {
        return backgroundTintG;
    }

    public synchronized void setBackgroundTintG(int backgroundTintG) {
        this.backgroundTintG = clamp(backgroundTintG, 0, 255);
    }

    public synchronized int getBackgroundTintB() {
        return backgroundTintB;
    }

    public synchronized void setBackgroundTintB(int backgroundTintB) {
        this.backgroundTintB = clamp(backgroundTintB, 0, 255);
    }

    public synchronized int getBackgroundOpacity() {
        return backgroundTintA;
    }

    public synchronized void setBackgroundOpacity(int backgroundOpacity) {
        this.backgroundTintA = clamp(backgroundOpacity, 0, 255);
    }

    public synchronized int getBackgroundTintA() {
        return backgroundTintA;
    }

    public synchronized void setBackgroundTintA(int backgroundTintA) {
        this.backgroundTintA = clamp(backgroundTintA, 0, 255);
    }

    public synchronized int getPanelOpacity() {
        return panelOpacity;
    }

    public synchronized void setPanelOpacity(int panelOpacity) {
        this.panelOpacity = clamp(panelOpacity, 0, 255);
    }

    public synchronized float panelOpacityScale() {
        return panelOpacity / (float) DEFAULT_PANEL_OPACITY;
    }

    public synchronized boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public synchronized void setAnimationsEnabled(boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }

    public synchronized double getAnimationSpeedMultiplier() {
        return animationSpeedMultiplier;
    }

    public synchronized void setAnimationSpeedMultiplier(double animationSpeedMultiplier) {
        this.animationSpeedMultiplier = clamp(animationSpeedMultiplier, 0.2d, 2.0d);
    }

    public synchronized boolean isClickSoundsEnabled() {
        return clickSoundsEnabled;
    }

    public synchronized void setClickSoundsEnabled(boolean clickSoundsEnabled) {
        this.clickSoundsEnabled = clickSoundsEnabled;
    }

    public synchronized void setAccentFromRgb(int red, int green, int blue) {
        accentR = clamp(red, 0, 255);
        accentG = clamp(green, 0, 255);
        accentB = clamp(blue, 0, 255);
    }

    public synchronized void setBackgroundTintFromRgb(int red, int green, int blue) {
        backgroundTintR = clamp(red, 0, 255);
        backgroundTintG = clamp(green, 0, 255);
        backgroundTintB = clamp(blue, 0, 255);
    }

    // Legacy migration support for older hue-based configs.
    public synchronized int getAccentHue() {
        return hueFromRgb(accentR, accentG, accentB, 120);
    }

    public synchronized void setAccentHue(int hue) {
        int[] rgb = hsvToRgb(clamp(hue, 0, 360), 0.85f, 1.0f);
        accentR = rgb[0];
        accentG = rgb[1];
        accentB = rgb[2];
    }

    public synchronized int getBackgroundTintHue() {
        return hueFromRgb(backgroundTintR, backgroundTintG, backgroundTintB, 215);
    }

    public synchronized void setBackgroundTintHue(int hue) {
        int[] rgb = hsvToRgb(clamp(hue, 0, 360), 0.70f, 0.45f);
        backgroundTintR = rgb[0];
        backgroundTintG = rgb[1];
        backgroundTintB = rgb[2];
    }

    private int[] hsvToRgb(int hueDegrees, float saturation, float value) {
        float hue = ((hueDegrees % 360) + 360) % 360;
        float sat = clamp(saturation, 0.0f, 1.0f);
        float val = clamp(value, 0.0f, 1.0f);

        if (sat <= 0.0001f) {
            int gray = clamp(Math.round(val * 255.0f), 0, 255);
            return new int[]{gray, gray, gray};
        }

        float sector = hue / 60.0f;
        int index = (int) Math.floor(sector);
        float fraction = sector - index;

        float p = val * (1.0f - sat);
        float q = val * (1.0f - (sat * fraction));
        float t = val * (1.0f - (sat * (1.0f - fraction)));

        float red;
        float green;
        float blue;

        switch (index) {
            case 0 -> {
                red = val;
                green = t;
                blue = p;
            }
            case 1 -> {
                red = q;
                green = val;
                blue = p;
            }
            case 2 -> {
                red = p;
                green = val;
                blue = t;
            }
            case 3 -> {
                red = p;
                green = q;
                blue = val;
            }
            case 4 -> {
                red = t;
                green = p;
                blue = val;
            }
            default -> {
                red = val;
                green = p;
                blue = q;
            }
        }

        return new int[]{
            clamp(Math.round(red * 255.0f), 0, 255),
            clamp(Math.round(green * 255.0f), 0, 255),
            clamp(Math.round(blue * 255.0f), 0, 255)
        };
    }

    private int hueFromRgb(int red, int green, int blue, int fallbackHue) {
        float r = clamp(red, 0, 255) / 255.0f;
        float g = clamp(green, 0, 255) / 255.0f;
        float b = clamp(blue, 0, 255) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        if (delta <= 0.0001f) {
            return clamp(fallbackHue, 0, 360);
        }

        float hue;
        if (max == r) {
            hue = ((g - b) / delta) % 6.0f;
        } else if (max == g) {
            hue = ((b - r) / delta) + 2.0f;
        } else {
            hue = ((r - g) / delta) + 4.0f;
        }
        hue *= 60.0f;
        if (hue < 0.0f) {
            hue += 360.0f;
        }
        return clamp(Math.round(hue), 0, 360);
    }

    private int argb(int alpha, int red, int green, int blue) {
        return (clamp(alpha, 0, 255) << 24)
            | (clamp(red, 0, 255) << 16)
            | (clamp(green, 0, 255) << 8)
            | clamp(blue, 0, 255);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

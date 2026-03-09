package com.codex.client.utils;

import com.codex.client.gui.navigator.settings.GuiSettings;
import net.minecraft.client.gui.DrawContext;

public class RenderUtils {

    public static void drawRect(DrawContext context, float x, float y, float width, float height, int color) {
        context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), applyPanelOpacityScale(color));
    }

    public static void drawOutline(DrawContext context, float x, float y, float width, float height, int color) {
        drawRect(context, x, y, width, 1, color);
        drawRect(context, x, y + height - 1, width, 1, color);
        drawRect(context, x, y, 1, height, color);
        drawRect(context, x + width - 1, y, 1, height, color);
    }

    public static void drawBeveledBox(DrawContext context, float x, float y, float width, float height, int bgColor, int highlightColor, int shadowColor) {
        // Base fill
        drawRect(context, x, y, width, height, bgColor);
        // Top edge
        drawRect(context, x, y, width, 1, highlightColor);
        // Left edge
        drawRect(context, x, y, 1, height, highlightColor);
        // Bottom edge
        drawRect(context, x, y + height - 1, width, 1, shadowColor);
        // Right edge
        drawRect(context, x + width - 1, y, 1, height, shadowColor);
    }
    
    public static void drawModernBox(DrawContext context, float x, float y, float width, float height, int bgColor, int outlineColor) {
        drawRect(context, x, y, width, height, bgColor);
        if (outlineColor != 0) {
            drawOutline(context, x, y, width, height, outlineColor);
        }
    }

    public static int applyAlpha(int color, float alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int interpolateColor(int color1, int color2, float fraction) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * fraction);
        int r = (int) (r1 + (r2 - r1) * fraction);
        int g = (int) (g1 + (g2 - g1) * fraction);
        int b = (int) (b1 + (b2 - b1) * fraction);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int getChromaColor(float x, float y, float speedMultiplier) {
        // Global Chroma Sync offset based on screen position
        float offset = (x * 2.0f + y * 1.5f) / 1000.0f;
        float time = (System.currentTimeMillis() % (int)(4000 / speedMultiplier)) / (4000.0f / speedMultiplier);
        float hue = (time - offset) % 1.0f;
        if (hue < 0) hue += 1.0f;
        return java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
    }
    
    public static int getChromaColor(float x, float y, float speedMultiplier, float saturation, float brightness) {
        float offset = (x * 2.0f + y * 1.5f) / 1000.0f;
        float time = (System.currentTimeMillis() % (int)(4000 / speedMultiplier)) / (4000.0f / speedMultiplier);
        float hue = (time - offset) % 1.0f;
        if (hue < 0) hue += 1.0f;
        return java.awt.Color.HSBtoRGB(hue, saturation, brightness);
    }

    private static int applyPanelOpacityScale(int color) {
        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0) {
            return color;
        }

        float scale = GuiSettings.get().panelOpacityScale();
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(alpha * scale)));
        return (scaledAlpha << 24) | (color & 0x00FFFFFF);
    }
}

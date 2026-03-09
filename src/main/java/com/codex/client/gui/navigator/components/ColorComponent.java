package com.codex.client.gui.navigator.components;

import com.codex.api.value.ColorValue;
import com.codex.client.gui.navigator.NavigatorStyle;
import com.codex.client.gui.navigator.UiInteractionFeedback;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public class ColorComponent extends ValueComponent<ColorValue> {
    private boolean draggingPicker = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    private float currentHue;
    private float currentSaturation;
    private float currentBrightness;
    private float currentAlpha;
    private int lastKnownValue;

    public ColorComponent(ColorValue value) {
        super(value);
        this.height = 80; // Fixed height to always fit layout
        this.lastKnownValue = value.get();
        updateHSBFromValue();
    }

    private void updateHSBFromValue() {
        float[] hsb = value.getHSB();
        currentHue = hsb[0];
        currentSaturation = hsb[1];
        currentBrightness = hsb[2];
        currentAlpha = value.getAlpha() / 255f;
    }

    private void updateValueFromHSB() {
        int rgb = Color.HSBtoRGB(currentHue, currentSaturation, currentBrightness);
        int alphaInt = (int) (currentAlpha * 255);
        int newValue = (alphaInt << 24) | (rgb & 0x00FFFFFF);
        value.set(newValue);
        lastKnownValue = newValue;
        notifyValueChanged();
    }

    private void checkValueSync() {
        if (!draggingPicker && !draggingHue && !draggingAlpha) {
            if (value.get() != lastKnownValue) {
                lastKnownValue = value.get();
                updateHSBFromValue();
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        checkValueSync();
        MinecraftClient mc = MinecraftClient.getInstance();

        context.drawText(mc.textRenderer, value.getName(), x, y + 4, NavigatorStyle.Feature.TEXT_PRIMARY, false);

        int boxSize = 12;
        int boxX = x + width - boxSize;
        int boxY = y + 3;
        RenderUtils.drawModernBox(context, boxX, boxY, boxSize, boxSize, value.get(), NavigatorStyle.Colors.HAIRLINE);

        int startY = y + 18;
        int pickerSize = 55; // Square shape
        
        render2DPicker(context, startY, pickerSize, mouseX, mouseY);
        
        int sliderX = x + pickerSize + 10;
        int sliderWidth = width - pickerSize - 10;
        int trackHeight = 10;

        int hueY = startY + 5;
        context.drawText(mc.textRenderer, "Hue", sliderX, hueY - 9, NavigatorStyle.Feature.TEXT_SECONDARY, false);
        renderHueTrack(context, sliderX, hueY, sliderWidth, trackHeight, mouseX, mouseY);
        
        int alphaY = startY + 35;
        context.drawText(mc.textRenderer, "Alpha", sliderX, alphaY - 9, NavigatorStyle.Feature.TEXT_SECONDARY, false);
        renderAlphaTrack(context, sliderX, alphaY, sliderWidth, trackHeight, mouseX, mouseY);
    }

    private void render2DPicker(DrawContext context, int startY, int size, int mouseX, int mouseY) {
        int segments = 15;
        float segSize = size / (float) segments;
        for (int ix = 0; ix < segments; ix++) {
            for (int iy = 0; iy < segments; iy++) {
                float sat = ix / (float) (segments - 1);
                float bri = 1.0f - (iy / (float) (segments - 1));
                int color = Color.HSBtoRGB(currentHue, sat, bri);
                RenderUtils.drawRect(context, x + (ix * segSize), startY + (iy * segSize), segSize + 1, segSize + 1, color | 0xFF000000);
            }
        }
        RenderUtils.drawOutline(context, x, startY, size, size, NavigatorStyle.Colors.HAIRLINE);

        float indX = x + (currentSaturation * size);
        float indY = startY + ((1.0f - currentBrightness) * size);
        RenderUtils.drawRect(context, indX - 1, indY - 1, 3, 3, 0xFFFFFFFF);
        RenderUtils.drawOutline(context, indX - 2, indY - 2, 5, 5, 0xFF000000);

        if (draggingPicker) {
            currentSaturation = Math.max(0, Math.min(1, (mouseX - x) / (float) size));
            currentBrightness = 1.0f - Math.max(0, Math.min(1, (mouseY - startY) / (float) size));
            updateValueFromHSB();
        }
    }

    private void renderHueTrack(DrawContext context, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
        int segments = 24;
        float segW = sw / (float) segments;
        for (int i = 0; i < segments; i++) {
            float hue = i / (float) segments;
            int color = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            RenderUtils.drawRect(context, sx + (i * segW), sy, segW + 1, sh, color | 0xFF000000);
        }
        RenderUtils.drawOutline(context, sx, sy, sw, sh, NavigatorStyle.Colors.HAIRLINE);

        float indX = sx + (currentHue * sw);
        RenderUtils.drawRect(context, indX - 1, sy - 1, 2, sh + 2, 0xFFFFFFFF);
        RenderUtils.drawOutline(context, indX - 2, sy - 2, 4, sh + 4, 0xFF000000);

        if (draggingHue) {
            currentHue = Math.max(0, Math.min(1, (mouseX - sx) / (float) sw));
            updateValueFromHSB();
        }
    }

    private void renderAlphaTrack(DrawContext context, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
        int checks = sw / 4;
        for (int i = 0; i < checks; i++) {
            int color = (i % 2 == 0) ? 0xFF888888 : 0xFF444444;
            RenderUtils.drawRect(context, sx + (i * 4), sy, 4, sh, color);
        }
        
        int rgb = Color.HSBtoRGB(currentHue, currentSaturation, currentBrightness) & 0x00FFFFFF;
        
        int segments = 24;
        float segW = sw / (float) segments;
        for (int i = 0; i < segments; i++) {
            float alpha = i / (float) segments;
            int aInt = (int) (alpha * 255);
            RenderUtils.drawRect(context, sx + (i * segW), sy, segW + 1, sh, (aInt << 24) | rgb);
        }
        RenderUtils.drawOutline(context, sx, sy, sw, sh, NavigatorStyle.Colors.HAIRLINE);

        float indX = sx + (currentAlpha * sw);
        RenderUtils.drawRect(context, indX - 1, sy - 1, 2, sh + 2, 0xFFFFFFFF);
        RenderUtils.drawOutline(context, indX - 2, sy - 2, 4, sh + 4, 0xFF000000);

        if (draggingAlpha) {
            currentAlpha = Math.max(0, Math.min(1, (mouseX - sx) / (float) sw));
            updateValueFromHSB();
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        
        int startY = y + 18;
        int pickerSize = 55;
        
        if (mouseX >= x && mouseX <= x + pickerSize && mouseY >= startY && mouseY <= startY + pickerSize) {
            draggingPicker = true;
            UiInteractionFeedback.click();
            return;
        }
        
        int sliderX = x + pickerSize + 10;
        int sliderWidth = width - pickerSize - 10;
        int trackHeight = 10;
        
        int hueY = startY + 5;
        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= hueY - 2 && mouseY <= hueY + trackHeight + 2) {
            draggingHue = true;
            UiInteractionFeedback.click();
            return;
        }
        
        int alphaY = startY + 35;
        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= alphaY - 2 && mouseY <= alphaY + trackHeight + 2) {
            draggingAlpha = true;
            UiInteractionFeedback.click();
            return;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingPicker = false;
        draggingHue = false;
        draggingAlpha = false;
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    }
}

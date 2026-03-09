package com.codex.client.gui.navigator.settings;

import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.NumberValue;
import com.codex.api.value.Value;

import java.util.Arrays;
import java.util.List;

public final class ClientSettingsModel {
    private final ColorValue accentColor = new ColorValue("Accent Color", 0xFFFFFFFF);
    private final ColorValue backgroundColor = new ColorValue("Background Color", 0xFFFFFFFF);

    private final NumberValue panelOpacity = new NumberValue("Panel Opacity", GuiSettings.DEFAULT_PANEL_OPACITY, 60, 255, 1);

    private final BoolValue animationsEnabled = new BoolValue("Animations Enabled", GuiSettings.DEFAULT_ANIMATIONS_ENABLED);
    private final NumberValue animationSpeed = new NumberValue("Animation Speed", GuiSettings.DEFAULT_ANIMATION_SPEED, 0.2, 2.0, 0.05);
    private final BoolValue clickSounds = new BoolValue("UI Click Sounds", GuiSettings.DEFAULT_CLICK_SOUNDS);

    public ClientSettingsModel() {
        pullFromSettings();
    }

    public List<Value<?>> appearanceValues() {
        return Arrays.asList(
            accentColor, backgroundColor, panelOpacity
        );
    }

    public List<Value<?>> behaviorValues() {
        return Arrays.asList(animationsEnabled, animationSpeed, clickSounds);
    }

    public List<Value<?>> allValues() {
        List<Value<?>> values = Arrays.asList(
            accentColor, backgroundColor, panelOpacity,
            animationsEnabled, animationSpeed, clickSounds
        );
        return values;
    }

    public void pullFromSettings() {
        GuiSettings settings = GuiSettings.get();
        accentColor.setColor(settings.accentPreviewColor());
        backgroundColor.setColor(settings.backgroundPreviewColor());

        panelOpacity.set(settings.getPanelOpacity());

        animationsEnabled.set(settings.isAnimationsEnabled());
        animationSpeed.set(settings.getAnimationSpeedMultiplier());
        clickSounds.set(settings.isClickSoundsEnabled());
    }

    public void applyToSettings() {
        GuiSettings settings = GuiSettings.get();
        settings.setAccentA(accentColor.getAlpha());
        settings.setAccentR(accentColor.getRed());
        settings.setAccentG(accentColor.getGreen());
        settings.setAccentB(accentColor.getBlue());

        settings.setBackgroundTintA(backgroundColor.getAlpha());
        settings.setBackgroundTintR(backgroundColor.getRed());
        settings.setBackgroundTintG(backgroundColor.getGreen());
        settings.setBackgroundTintB(backgroundColor.getBlue());

        settings.setPanelOpacity(asInt(panelOpacity));

        settings.setAnimationsEnabled(Boolean.TRUE.equals(animationsEnabled.get()));
        settings.setAnimationSpeedMultiplier(animationSpeed.asDouble());
        settings.setClickSoundsEnabled(Boolean.TRUE.equals(clickSounds.get()));
    }

    public void resetVisualDefaults() {
        GuiSettings settings = GuiSettings.get();
        settings.resetVisualDefaults();
        pullFromSettings();
    }

    private int asInt(NumberValue value) {
        return (int) Math.round(value.asDouble());
    }
}

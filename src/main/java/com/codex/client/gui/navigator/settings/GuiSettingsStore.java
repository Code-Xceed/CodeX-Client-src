package com.codex.client.gui.navigator.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class GuiSettingsStore {
    private GuiSettingsStore() {}

    public static boolean load(Path path) {
        if (path == null || !Files.exists(path)) {
            return false;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        } catch (IOException ioException) {
            return false;
        }

        GuiSettings settings = GuiSettings.get();
        settings.setAnimationsEnabled(parseBoolean(properties, "gui.animations.enabled", settings.isAnimationsEnabled()));
        settings.setAnimationSpeedMultiplier(parseDouble(properties, "gui.animations.speed", settings.getAnimationSpeedMultiplier()));
        settings.setClickSoundsEnabled(parseBoolean(properties, "gui.feedback.click_sounds", settings.isClickSoundsEnabled()));

        if (properties.containsKey("gui.color.accent.r")
            || properties.containsKey("gui.color.accent.g")
            || properties.containsKey("gui.color.accent.b")) {
            settings.setAccentR(parseInt(properties, "gui.color.accent.r", settings.getAccentR()));
            settings.setAccentG(parseInt(properties, "gui.color.accent.g", settings.getAccentG()));
            settings.setAccentB(parseInt(properties, "gui.color.accent.b", settings.getAccentB()));
        } else if (properties.containsKey("gui.color.accent.hue")) {
            settings.setAccentHue(parseInt(properties, "gui.color.accent.hue", settings.getAccentHue()));
        }
        settings.setAccentA(parseInt(properties, "gui.color.accent.a", settings.getAccentA()));

        if (properties.containsKey("gui.color.background.r")
            || properties.containsKey("gui.color.background.g")
            || properties.containsKey("gui.color.background.b")) {
            settings.setBackgroundTintR(parseInt(properties, "gui.color.background.r", settings.getBackgroundTintR()));
            settings.setBackgroundTintG(parseInt(properties, "gui.color.background.g", settings.getBackgroundTintG()));
            settings.setBackgroundTintB(parseInt(properties, "gui.color.background.b", settings.getBackgroundTintB()));
        } else if (properties.containsKey("gui.color.background.hue")) {
            settings.setBackgroundTintHue(parseInt(properties, "gui.color.background.hue", settings.getBackgroundTintHue()));
        }
        settings.setBackgroundTintA(parseInt(
            properties,
            "gui.color.background.a",
            parseInt(properties, "gui.color.background.opacity", settings.getBackgroundTintA())
        ));
        settings.setPanelOpacity(parseInt(properties, "gui.color.panel.opacity", settings.getPanelOpacity()));
        return true;
    }

    public static boolean save(Path path) {
        if (path == null) {
            return false;
        }

        Path parent = path.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ioException) {
            return false;
        }

        GuiSettings settings = GuiSettings.get();
        Properties properties = new Properties();
        properties.setProperty("gui.animations.enabled", Boolean.toString(settings.isAnimationsEnabled()));
        properties.setProperty("gui.animations.speed", Double.toString(settings.getAnimationSpeedMultiplier()));
        properties.setProperty("gui.feedback.click_sounds", Boolean.toString(settings.isClickSoundsEnabled()));

        properties.setProperty("gui.color.accent.r", Integer.toString(settings.getAccentR()));
        properties.setProperty("gui.color.accent.g", Integer.toString(settings.getAccentG()));
        properties.setProperty("gui.color.accent.b", Integer.toString(settings.getAccentB()));
        properties.setProperty("gui.color.accent.a", Integer.toString(settings.getAccentA()));

        properties.setProperty("gui.color.background.r", Integer.toString(settings.getBackgroundTintR()));
        properties.setProperty("gui.color.background.g", Integer.toString(settings.getBackgroundTintG()));
        properties.setProperty("gui.color.background.b", Integer.toString(settings.getBackgroundTintB()));
        properties.setProperty("gui.color.background.a", Integer.toString(settings.getBackgroundTintA()));
        properties.setProperty("gui.color.accent.hue", Integer.toString(settings.getAccentHue()));
        properties.setProperty("gui.color.background.hue", Integer.toString(settings.getBackgroundTintHue()));
        properties.setProperty("gui.color.background.opacity", Integer.toString(settings.getBackgroundTintA()));
        properties.setProperty("gui.color.panel.opacity", Integer.toString(settings.getPanelOpacity()));

        try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, "CodeX GUI settings");
            return true;
        } catch (IOException ioException) {
            return false;
        }
    }

    private static boolean parseBoolean(Properties properties, String key, boolean fallback) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        return Boolean.parseBoolean(raw);
    }

    private static int parseInt(Properties properties, String key, int fallback) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double parseDouble(Properties properties, String key, double fallback) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}

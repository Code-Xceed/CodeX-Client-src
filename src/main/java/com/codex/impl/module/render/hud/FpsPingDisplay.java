package com.codex.impl.module.render.hud;

import com.codex.api.module.Category;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.gui.DrawContext;

import java.util.LinkedList;

public class FpsPingDisplay extends HudElementModule {

    private final BoolValue showFps = new BoolValue("Show FPS", true);
    private final BoolValue showPing = new BoolValue("Show Ping", true);
    private final BoolValue showGraph = new BoolValue("Show FPS Graph", true);

    private final ModeValue formatStyle = new ModeValue("Format Style", "FPS: %s | Ping: %sms", "FPS: %s", "%s FPS", "FPS: %s | Ping: %sms", "Compact");

    private final NumberValue scale = new NumberValue("Scale", 1.0, 0.5, 3.0, 0.1);
    private final BoolValue backgroundBlur = new BoolValue("Background Blur", true);
    private final BoolValue shadowText = new BoolValue("Shadow Text", true);

    private final ModeValue colorMode = new ModeValue("Color Mode", "Dynamic", "Static", "Dynamic", "Rainbow");
    private final ColorValue staticColor = new ColorValue("Text Color", 0xFFFFFFFF);

    private final ColorValue goodColor = new ColorValue("Good Color (High FPS)", 0xFF00FF00); // Green
    private final ColorValue mediumColor = new ColorValue("Medium Color", 0xFFFFFF00); // Yellow
    private final ColorValue badColor = new ColorValue("Bad Color (Low FPS)", 0xFFFF0000); // Red

    private final LinkedList<Integer> fpsHistory = new LinkedList<>();
    private long lastGraphUpdate = 0;

    public FpsPingDisplay() {
        super("FPS & Ping", "Lightweight real-time performance display.", 10, 10, 50, 15, false);

        addValue(showFps);
        addValue(showPing);
        addValue(showGraph);

        addValue(formatStyle);
        addValue(scale);

        addValue(backgroundBlur);
        addValue(shadowText);
        addValue(colorMode);

        addValue(staticColor);
        addValue(goodColor);
        addValue(mediumColor);
        addValue(badColor);
    }
    @Override
    public int defaultX() {
        return 10;
    }

    @Override
    public int defaultY() {
        return 10;
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        if (mc == null) return;

        int fps = mc.getCurrentFps();
        int ping = getPing();
        boolean isEditing = mc.currentScreen instanceof com.codex.client.gui.navigator.CodeXNavigatorScreen;
        if (isEditing) {
            fps = 144;
            ping = 35;
        }

        long timeNow = System.currentTimeMillis();
        if (timeNow - lastGraphUpdate > 50) { // Update graph every 50ms (20fps capture rate)
            fpsHistory.add(fps);
            if (fpsHistory.size() > 40) { // Keep last 2 seconds (40 points * 50ms)
                fpsHistory.removeFirst();
            }
            lastGraphUpdate = timeNow;
        }

        StringBuilder sb = new StringBuilder();
        String style = formatStyle.get();

        if ("FPS: %s".equals(style)) {
            if (showFps.get()) sb.append("FPS: ").append(fps);
        } else if ("%s FPS".equals(style)) {
            if (showFps.get()) sb.append(fps).append(" FPS");
        } else if ("FPS: %s | Ping: %sms".equals(style)) {
            if (showFps.get()) sb.append("FPS: ").append(fps);
            if (showPing.get()) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append("Ping: ").append(ping).append("ms");
            }
        } else if ("Compact".equals(style)) {
            if (showFps.get()) sb.append(fps);
            if (showPing.get()) {
                if (sb.length() > 0) sb.append(" / ");
                sb.append(ping).append("ms");
            }
        }

        String displayString = sb.toString();
        if (displayString.isEmpty() && !showGraph.get()) {
            setElementWidth(50);
            setElementHeight(15);
            return;
        }

        context.getMatrices().push();
        float sc = (float) scale.asDouble();
        context.getMatrices().translate(Math.round(getEditorX()), Math.round(getEditorY()), 0);
        context.getMatrices().scale(sc, sc, 1.0f);

        int tw = displayString.isEmpty() ? 40 : mc.textRenderer.getWidth(displayString);
        int th = displayString.isEmpty() ? 0 : mc.textRenderer.fontHeight;
        
        int graphHeight = showGraph.get() ? 15 : 0;
        int totalHeight = th + (showGraph.get() && !displayString.isEmpty() ? 4 : 0) + graphHeight;
        int totalWidth = Math.max(tw, 40);

        if (backgroundBlur.get()) {
            RenderUtils.drawRect(context, -3, -3, totalWidth + 6, totalHeight + 6, 0x55000000);
        }

        int textColor = getColor(fps);
        
        if (!displayString.isEmpty()) {
            context.drawText(mc.textRenderer, displayString, 0, 0, textColor, shadowText.get());
        }
        
        if (showGraph.get() && !fpsHistory.isEmpty()) {
            int graphY = displayString.isEmpty() ? 0 : th + 4;
            
            // Background for graph
            RenderUtils.drawRect(context, 0, graphY, totalWidth, graphHeight, 0x40000000);
            
            int maxFps = 0;
            for (int f : fpsHistory) {
                if (f > maxFps) maxFps = f;
            }
            if (maxFps < 60) maxFps = 60; // minimum ceiling
            
            float stepX = (float) totalWidth / (Math.max(1, fpsHistory.size() - 1));
            
            for (int i = 0; i < fpsHistory.size() - 1; i++) {
                int f1 = fpsHistory.get(i);
                int f2 = fpsHistory.get(i + 1);
                
                float h1 = ((float) f1 / maxFps) * graphHeight;
                float h2 = ((float) f2 / maxFps) * graphHeight;
                
                float x1 = i * stepX;
                float y1 = graphY + graphHeight - h1;
                float x2 = (i + 1) * stepX;
                float y2 = graphY + graphHeight - h2;
                
                int lineColor = getColor(f1);
                
                // Draw graph line segments (naive line via thin quads)
                RenderUtils.drawRect(context, x1, Math.min(y1, y2), x2 - x1, Math.abs(y2 - y1) + 1, lineColor);
            }
        }

        context.getMatrices().pop();

        setElementWidth((int) ((totalWidth + 6) * sc));
        setElementHeight((int) ((totalHeight + 6) * sc));
    }

    private int getPing() {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;
        var playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return playerListEntry == null ? 0 : playerListEntry.getLatency();
    }

    private int getColor(int fps) {
        String mode = colorMode.get();
        if ("Static".equals(mode)) {
            return staticColor.get();
        } else if ("Rainbow".equals(mode)) {
            return RenderUtils.getChromaColor(getEditorX(), getEditorY(), 1.0f);
        } else {
            // Dynamic Mode
            if (fps >= 100) return goodColor.get();
            if (fps >= 40) return RenderUtils.interpolateColor(mediumColor.get(), goodColor.get(), (fps - 40) / 60.0f);
            if (fps >= 10) return RenderUtils.interpolateColor(badColor.get(), mediumColor.get(), (fps - 10) / 30.0f);
            return badColor.get();
        }
    }
}

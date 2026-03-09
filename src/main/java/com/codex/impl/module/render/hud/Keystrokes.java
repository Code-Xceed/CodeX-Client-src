package com.codex.impl.module.render.hud;

import com.codex.api.event.EventTarget;
import com.codex.api.event.events.KeyEvent;
import com.codex.api.event.events.MouseEvent;
import com.codex.api.event.events.UpdateEvent;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Keystrokes extends HudElementModule {
    private final NumberValue scale = new NumberValue("Scale", 1.0, 0.5, 3.0, 0.1);
    private final ModeValue layoutMode = new ModeValue("Layout Mode", "Compact Grid", "Compact Grid", "Horizontal", "Vertical");
    private final NumberValue spacing = new NumberValue("Spacing", 2.0, 0.0, 10.0, 1.0);
    private final NumberValue spacebarWidth = new NumberValue("Spacebar Multiplier", 3.0, 1.0, 5.0, 0.1);

    private final ColorValue backgroundColor = new ColorValue("Background Color", 0x80000000);
    private final ColorValue pressedColor = new ColorValue("Pressed Color", 0x80FFFFFF);
    private final ColorValue textColor = new ColorValue("Text Color", 0xFFFFFFFF);
    private final ColorValue borderColor = new ColorValue("Border Color", 0x44FFFFFF);
    private final NumberValue borderThickness = new NumberValue("Border Thickness", 1.0, 0.0, 5.0, 1.0);

    private final ModeValue mouseDisplayMode = new ModeValue("Mouse Text Display", "Both", "Both", "CPS Only", "Button Only", "None");
    private final ModeValue cpsWindow = new ModeValue("CPS Window", "1s", "1s", "2s", "3s");
    private final ModeValue pressAnimation = new ModeValue("Press Animation", "Fade", "None", "Fade", "Scale Up", "Fill", "Bounce", "Ripple");
    private final NumberValue animSpeed = new NumberValue("Animation Speed", 0.3, 0.05, 1.0, 0.05);

    private final BoolValue chromaWave = new BoolValue("Chroma Wave Sync", false);
    private final NumberValue chromaSpeed = new NumberValue("Chroma Speed", 1.0, 0.1, 5.0, 0.1);

    private final BoolValue keySounds = new BoolValue("Mechanical Key Sounds", false);

    private final List<Long> lmbClicks = new ArrayList<>();
    private final List<Long> rmbClicks = new ArrayList<>();

    private final Key wKey = new Key("W", () -> mc.options.forwardKey);
    private final Key aKey = new Key("A", () -> mc.options.leftKey);
    private final Key sKey = new Key("S", () -> mc.options.backKey);
    private final Key dKey = new Key("D", () -> mc.options.rightKey);
    private final Key spaceKey = new Key("SPACE", () -> mc.options.jumpKey);
    private final Key lmbKey = new Key("LMB", () -> mc.options.attackKey);
    private final Key rmbKey = new Key("RMB", () -> mc.options.useKey);

    public Keystrokes() {
        super("Keystrokes", "Displays WASD and mouse buttons with CPS", 10, 10, 60, 60, false);

        addValue(scale);
        addValue(layoutMode);
        addValue(spacing);
        addValue(spacebarWidth);

        addValue(backgroundColor);
        addValue(pressedColor);
        addValue(textColor);
        addValue(borderColor);
        addValue(borderThickness);

        addValue(mouseDisplayMode);
        addValue(cpsWindow);
        addValue(pressAnimation);
        addValue(animSpeed);

        addValue(chromaWave);
        addValue(chromaSpeed);
        addValue(keySounds);
    }
    @Override
    public int defaultX() {
        return 10;
    }

    @Override
    public int defaultY() {
        return 10;
    }

    @EventTarget
    public void onMouse(MouseEvent event) {
        if (event.isPressed()) {
            if (event.getButton() == 0) { // Left click
                lmbClicks.add(System.currentTimeMillis());
            } else if (event.getButton() == 1) { // Right click
                rmbClicks.add(System.currentTimeMillis());
            }
        }
    }

    private void updateCps() {
        long time = System.currentTimeMillis();
        long windowSize = Long.parseLong(cpsWindow.get().replace("s", "")) * 1000L;
        lmbClicks.removeIf(t -> time - t > windowSize);
        rmbClicks.removeIf(t -> time - t > windowSize);
    }

    private int getLmbCps() {
        long windowSize = Long.parseLong(cpsWindow.get().replace("s", ""));
        return (int) (lmbClicks.size() / windowSize);
    }

    private int getRmbCps() {
        long windowSize = Long.parseLong(cpsWindow.get().replace("s", ""));
        return (int) (rmbClicks.size() / windowSize);
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        if (mc == null || mc.player == null) return;
        updateCps();

        float sc = (float) scale.asDouble();
        context.getMatrices().push();
        // Crucial fix: Math.round() the root editor coordinates to prevent the entire scaled HUD matrix from living on a sub-pixel boundary, which causes the font to distort/jitter as it moves.
        context.getMatrices().translate(Math.round(getEditorX()), Math.round(getEditorY()), 0);
        context.getMatrices().scale(sc, sc, 1.0f);

        int gap = (int) spacing.asDouble();
        int size = 26; // Increased base square size
        int w = 0;
        int h = 0;

        String mode = layoutMode.get();
        if ("Compact Grid".equals(mode)) {
            // Row 1: W
            drawKey(context, wKey, size + gap, 0, size, size, tickDelta);
            // Row 2: A S D
            drawKey(context, aKey, 0, size + gap, size, size, tickDelta);
            drawKey(context, sKey, size + gap, size + gap, size, size, tickDelta);
            drawKey(context, dKey, (size + gap) * 2, size + gap, size, size, tickDelta);
            // Row 3: LMB RMB
            int mouseWidth = (int) ((size * 3 + gap * 2) / 2.0f - gap / 2.0f);
            int mouseHeight = size - 2; // Slightly shorter than regular keys
            
            String displayMode = mouseDisplayMode.get();
            String lmbCpsStr = getLmbCps() + " CPS";
            String rmbCpsStr = getRmbCps() + " CPS";
            
            boolean showLmbButton = "Both".equals(displayMode) || "Button Only".equals(displayMode);
            boolean showLmbCps = "Both".equals(displayMode) || "CPS Only".equals(displayMode);
            
            boolean showRmbButton = "Both".equals(displayMode) || "Button Only".equals(displayMode);
            boolean showRmbCps = "Both".equals(displayMode) || "CPS Only".equals(displayMode);

            drawMouseKey(context, lmbKey, 0, (size + gap) * 2, mouseWidth, mouseHeight, tickDelta, showLmbButton ? "LMB" : null, showLmbCps ? lmbCpsStr : null);
            drawMouseKey(context, rmbKey, mouseWidth + gap, (size + gap) * 2, mouseWidth, mouseHeight, tickDelta, showRmbButton ? "RMB" : null, showRmbCps ? rmbCpsStr : null);
            
            // Row 4: Space
            int spaceW = (int) (size * spacebarWidth.asDouble());
            int totalGridW = size * 3 + gap * 2;
            int spaceX = (totalGridW - spaceW) / 2;
            drawKey(context, spaceKey, spaceX, (size + gap) * 2 + mouseHeight + gap, spaceW, size / 2 + 4, tickDelta);
            
            w = totalGridW;
            h = (size + gap) * 2 + mouseHeight + gap + (size / 2 + 4);
        } else if ("Horizontal".equals(mode)) {
            Key[] keys = {wKey, aKey, sKey, dKey, spaceKey, lmbKey, rmbKey};
            for (int i = 0; i < keys.length; i++) {
                int kw = keys[i] == spaceKey ? (int)(size * 2) : size;
                if (keys[i] == lmbKey || keys[i] == rmbKey) {
                    String displayMode = mouseDisplayMode.get();
                    boolean isLmb = keys[i] == lmbKey;
                    String cpsStr = isLmb ? getLmbCps() + " CPS" : getRmbCps() + " CPS";
                    boolean showButton = "Both".equals(displayMode) || "Button Only".equals(displayMode);
                    boolean showCps = "Both".equals(displayMode) || "CPS Only".equals(displayMode);
                    drawMouseKey(context, keys[i], w, 0, kw, size, tickDelta, showButton ? keys[i].name : null, showCps ? cpsStr : null);
                } else {
                    drawKey(context, keys[i], w, 0, kw, size, tickDelta);
                }
                w += kw + gap;
            }
            w -= gap; // remove last gap
            h = size;
        } else if ("Vertical".equals(mode)) {
            Key[] keys = {wKey, aKey, sKey, dKey, spaceKey, lmbKey, rmbKey};
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] == lmbKey || keys[i] == rmbKey) {
                    String displayMode = mouseDisplayMode.get();
                    boolean isLmb = keys[i] == lmbKey;
                    String cpsStr = isLmb ? getLmbCps() + " CPS" : getRmbCps() + " CPS";
                    boolean showButton = "Both".equals(displayMode) || "Button Only".equals(displayMode);
                    boolean showCps = "Both".equals(displayMode) || "CPS Only".equals(displayMode);
                    drawMouseKey(context, keys[i], 0, h, size * 2, size, tickDelta, showButton ? keys[i].name : null, showCps ? cpsStr : null);
                } else {
                    drawKey(context, keys[i], 0, h, size * 2, size, tickDelta);
                }
                h += size + gap;
            }
            w = size * 2;
            h -= gap;
        }

        context.getMatrices().pop();
        setElementWidth((int) (w * sc));
        setElementHeight((int) (h * sc));
    }

    private void drawKey(DrawContext context, Key key, int x, int y, int width, int height, float tickDelta) {
        drawMouseKey(context, key, x, y, width, height, tickDelta, key.name, null);
    }

    private void drawMouseKey(DrawContext context, Key key, int x, int y, int width, int height, float tickDelta, String buttonText, String cpsText) {
        key.updateAnimSpeed();
        boolean pressed = key.isDown();
        key.anim.setTarget(pressed ? 1f : 0f);
        float progress = key.anim.update(tickDelta);
        String animMode = pressAnimation.get();
        if ("None".equals(animMode)) {
            progress = pressed ? 1f : 0f;
        }

        int bg = backgroundColor.get();
        int pc = pressedColor.get();
        int tc = textColor.get();
        int bc = borderColor.get();
        
        if (chromaWave.get()) {
            float speed = (float) chromaSpeed.asDouble();
            // X and Y relative to screen to create a sweeping wave effect across the HUD
            float screenX = getEditorX() + x;
            float screenY = getEditorY() + y;
            int chromaBg = RenderUtils.getChromaColor(screenX, screenY, speed, 0.6f, 0.4f);
            int chromaPc = RenderUtils.getChromaColor(screenX, screenY, speed, 0.8f, 1.0f);
            int chromaBc = RenderUtils.getChromaColor(screenX, screenY, speed, 0.9f, 0.9f);
            
            // Apply original alpha values so the user's transparency preferences are kept
            bg = (chromaBg & 0x00FFFFFF) | (bg & 0xFF000000);
            pc = (chromaPc & 0x00FFFFFF) | (pc & 0xFF000000);
            bc = (chromaBc & 0x00FFFFFF) | (bc & 0xFF000000);
            
            // Make text chroma if fully opaque, else leave it white
            if ((tc >> 24 & 0xFF) > 250) {
                tc = RenderUtils.getChromaColor(screenX, screenY, speed, 0.5f, 1.0f);
            }
        }

        int bt = (int) borderThickness.asDouble();

        float renderX = x;
        float renderY = y;
        float renderW = width;
        float renderH = height;

        if (("Scale Up".equals(animMode) || "Bounce".equals(animMode)) && progress > 0) {
            float scaleAmt;
            if ("Bounce".equals(animMode)) {
                scaleAmt = 1.0f - (progress * 0.15f);
            } else {
                scaleAmt = 1.0f + (progress * 0.1f);
            }
            float addedW = (width * scaleAmt) - width;
            float addedH = (height * scaleAmt) - height;
            renderX -= addedW / 2;
            renderY -= addedH / 2;
            renderW += addedW;
            renderH += addedH;
        }

        int currentBg = ("Fade".equals(animMode) || "Scale Up".equals(animMode) || "Bounce".equals(animMode)) 
            ? RenderUtils.interpolateColor(bg, pc, progress) 
            : bg;

        context.getMatrices().push();
        
        if (bt > 0) {
            RenderUtils.drawModernBox(context, renderX, renderY, renderW, renderH, currentBg, bc);
            for (int i = 1; i < bt; i++) {
                RenderUtils.drawOutline(context, renderX + i, renderY + i, renderW - i * 2, renderH - i * 2, bc);
            }
        } else {
            RenderUtils.drawRect(context, renderX, renderY, renderW, renderH, currentBg);
        }

        if ("Fill".equals(animMode) && progress > 0) {
            float fillW = (renderW - bt * 2) * progress;
            float fillH = (renderH - bt * 2) * progress;
            float fillX = renderX + bt + ((renderW - bt * 2) / 2f) - (fillW / 2f);
            float fillY = renderY + bt + ((renderH - bt * 2) / 2f) - (fillH / 2f);
            RenderUtils.drawRect(context, fillX, fillY, fillW, fillH, pc);
        } else if ("Ripple".equals(animMode) && progress > 0) {
            // A pseudo-ripple effect using a filled scaled rect centered in the box
            float rippleProgress = (float) Math.pow(progress, 0.5); // Fast out
            float maxDim = Math.max(renderW, renderH);
            float fillW = maxDim * 1.5f * rippleProgress;
            float fillH = maxDim * 1.5f * rippleProgress;
            float fillX = renderX + (renderW / 2f) - (fillW / 2f);
            float fillY = renderY + (renderH / 2f) - (fillH / 2f);
            
            int rippleColor = (pc & 0x00FFFFFF) | ((int)((pc >> 24 & 0xFF) * (1.0f - rippleProgress)) << 24);
            
            // Scissor to keep ripple inside box bounds
            context.enableScissor(
                (int)(getEditorX() * scale.asDouble() + renderX + bt), 
                (int)(getEditorY() * scale.asDouble() + renderY + bt), 
                (int)(getEditorX() * scale.asDouble() + renderX + renderW - bt), 
                (int)(getEditorY() * scale.asDouble() + renderY + renderH - bt)
            );
            RenderUtils.drawRect(context, fillX, fillY, fillW, fillH, rippleColor);
            context.disableScissor();
        }

        if (key == spaceKey) {
            RenderUtils.drawRect(context, renderX + renderW / 4f, renderY + renderH / 2f - 1, renderW / 2f, 2, tc);
        } else {
            if (buttonText != null && cpsText != null) {
                context.getMatrices().push();
                float buttonScale = 0.65f;
                int textW = mc.textRenderer.getWidth(buttonText);
                float labelX = renderX + (renderW / 2f) - ((textW * buttonScale) / 2f);
                float labelY = renderY + 4;
                context.getMatrices().translate(Math.round(labelX), Math.round(labelY), 0);
                context.getMatrices().scale(buttonScale, buttonScale, 1f);
                context.drawText(mc.textRenderer, buttonText, 0, 0, tc, false);
                context.getMatrices().pop();
                
                int cpsW = mc.textRenderer.getWidth(cpsText);
                float cpsX = renderX + (renderW / 2f) - (cpsW / 2f);
                float cpsY = renderY + renderH - mc.textRenderer.fontHeight - 4;
                context.getMatrices().push();
                context.getMatrices().translate(Math.round(cpsX), Math.round(cpsY), 0);
                context.drawText(mc.textRenderer, cpsText, 0, 0, tc, false);
                context.getMatrices().pop();
            } else if (buttonText != null) {
                int textX = (int)(renderX + renderW / 2) - mc.textRenderer.getWidth(buttonText) / 2;
                int textY = (int)(renderY + renderH / 2) - mc.textRenderer.fontHeight / 2;
                context.drawText(mc.textRenderer, buttonText, textX, textY, tc, false);
            } else if (cpsText != null) {
                int textX = (int)(renderX + renderW / 2) - mc.textRenderer.getWidth(cpsText) / 2;
                int textY = (int)(renderY + renderH / 2) - mc.textRenderer.fontHeight / 2;
                context.drawText(mc.textRenderer, cpsText, textX, textY, tc, false);
            }
        }
        
        context.getMatrices().pop();
    }

    private class Key {
        String name;
        java.util.function.Supplier<KeyBinding> binding;
        Animation anim = new Animation(0f, 0.3f);

        Key(String name, java.util.function.Supplier<KeyBinding> binding) {
            this.name = name;
            this.binding = binding;
        }

        boolean isDown() {
            return binding.get().isPressed();
        }
        
        void updateAnimSpeed() {
            anim.setSpeed((float) animSpeed.asDouble());
        }
    }
}

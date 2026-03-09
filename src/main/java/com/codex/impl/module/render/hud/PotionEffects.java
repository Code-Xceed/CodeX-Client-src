package com.codex.impl.module.render.hud;

import com.codex.api.module.Category;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class PotionEffects extends HudElementModule {

    private final ModeValue layout = (ModeValue) new ModeValue("Layout", "Vertical", "Vertical", "Horizontal").setGroup("Layout");
    private final ModeValue alignment = (ModeValue) new ModeValue("Alignment", "Left", "Left", "Center", "Right").setGroup("Layout");
    private final ModeValue sortMode = (ModeValue) new ModeValue("Sort By", "Duration", "Duration", "Alphabetical", "None").setGroup("Layout");
    private final NumberValue scale = (NumberValue) new NumberValue("Scale", 1.0, 0.5, 3.0, 0.1).setGroup("Layout");
    private final NumberValue spacing = (NumberValue) new NumberValue("Spacing", 2.0, 0.0, 20.0, 1.0).setGroup("Layout");

    private final ModeValue backgroundStyle = (ModeValue) new ModeValue("Background Style", "Blur", "None", "Blur", "Solid Panel").setGroup("Visual");
    private final ColorValue panelColor = (ColorValue) new ColorValue("Panel Color", 0x80000000).setGroup("Visual");
    private final BoolValue showIcon = (BoolValue) new BoolValue("Show Icon", true).setGroup("Visual");
    private final BoolValue showName = (BoolValue) new BoolValue("Show Name", true).setGroup("Visual");
    
    private final BoolValue hideBeneficial = (BoolValue) new BoolValue("Hide Beneficial", false).setGroup("Filtering");
    private final BoolValue hideHarmful = (BoolValue) new BoolValue("Hide Harmful", false).setGroup("Filtering");

    private final BoolValue blinkLowDuration = (BoolValue) new BoolValue("Blink < 10s", true).setGroup("Timer");
    private final BoolValue colorLowDuration = (BoolValue) new BoolValue("Color Change < 10s", true).setGroup("Timer");
    private final ColorValue normalTextColor = (ColorValue) new ColorValue("Normal Text Color", 0xFFFFFFFF).setGroup("Timer");  
    private final ColorValue lowTextColor = (ColorValue) new ColorValue("Low Time Color", 0xFFFF5555).setGroup("Timer");        

    private final BoolValue chromaWave = (BoolValue) new BoolValue("Chroma Wave Sync", false).setGroup("Visual");

    public PotionEffects() {
        super("Potion Effects HUD", "Clean, movable potion effect display.", 100, 10, 120, 40, false);

        addValue(layout);
        addValue(alignment);
        addValue(sortMode);
        addValue(scale);
        addValue(spacing);

        addValue(backgroundStyle);
        addValue(panelColor);
        addValue(chromaWave);
        addValue(showIcon);
        addValue(showName);        
        addValue(hideBeneficial);
        addValue(hideHarmful);
        
        addValue(blinkLowDuration);
        addValue(colorLowDuration);
        addValue(normalTextColor);
        addValue(lowTextColor);
    }

    @Override
    public int defaultX() {
        return 10;
    }

    @Override
    public int defaultY() {
        return 100;
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        if (mc == null || mc.player == null) return;

        Collection<StatusEffectInstance> activeEffects = mc.player.getStatusEffects();
        List<StatusEffectInstance> effects = new ArrayList<>(activeEffects);
        
        boolean isEditing = mc.currentScreen instanceof com.codex.client.gui.navigator.CodeXNavigatorScreen;
        
        if (isEditing && effects.isEmpty()) {
            // Add preview effects while editing the HUD.
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> speedEntry = Registries.STATUS_EFFECT.getEntry(net.minecraft.registry.Registries.STATUS_EFFECT.get(Identifier.of("minecraft", "speed")));
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> strEntry = Registries.STATUS_EFFECT.getEntry(net.minecraft.registry.Registries.STATUS_EFFECT.get(Identifier.of("minecraft", "strength")));
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> poisonEntry = Registries.STATUS_EFFECT.getEntry(net.minecraft.registry.Registries.STATUS_EFFECT.get(Identifier.of("minecraft", "poison")));
            
            if (speedEntry != null) effects.add(new StatusEffectInstance(speedEntry, 3600, 1)); // 3:00 Speed II
            if (strEntry != null) effects.add(new StatusEffectInstance(strEntry, 12000, 0));    // 10:00 Strength
            if (poisonEntry != null) effects.add(new StatusEffectInstance(poisonEntry, 150, 0)); // 0:07 Poison
        }

        // Filtering
        effects.removeIf(effect -> {
            boolean isBeneficial = effect.getEffectType().value().isBeneficial();
            if (hideBeneficial.get() && isBeneficial) return true;
            if (hideHarmful.get() && !isBeneficial) return true;
            return false;
        });

        if (effects.isEmpty()) {
            setElementWidth(60);
            setElementHeight(20);
            return;
        }

        // Sorting
        String sort = sortMode.get();
        if ("Duration".equals(sort)) {
            effects.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));
        } else if ("Alphabetical".equals(sort)) {
            effects.sort(Comparator.comparing(a -> I18n.translate(a.getTranslationKey())));
        }

        context.getMatrices().push();
        float sc = (float) scale.asDouble();
        context.getMatrices().translate(Math.round(getEditorX()), Math.round(getEditorY()), 0);
        context.getMatrices().scale(sc, sc, 1.0f);

        boolean isVertical = "Vertical".equals(layout.get());
        int gap = (int) spacing.asDouble();
        String style = backgroundStyle.get();
        boolean hasIcon = showIcon.get();
        boolean hasName = showName.get();
        
        int totalW = 0;
        int totalH = 0;

        int[] effectWidths = new int[effects.size()];
        int[] effectHeights = new int[effects.size()];
        String[] displayNames = new String[effects.size()];
        String[] displayTimes = new String[effects.size()];

        // Pre-calculate dimensions
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance effect = effects.get(i);
            
            String name = I18n.translate(effect.getTranslationKey());
            if (effect.getAmplifier() > 0) {
                name += " " + (effect.getAmplifier() + 1);
            }
            
            int durationTicks = effect.getDuration();
            int seconds = durationTicks / 20;
            String time = String.format("%d:%02d", seconds / 60, seconds % 60);

            displayNames[i] = name;
            displayTimes[i] = time;

            int boxW = 4; // base padding
            int boxH = 4;
            
            if (hasIcon) {
                boxW += 20;
                boxH = Math.max(boxH, 20);
            }
            
            if (hasName) {
                int textW = Math.max(mc.textRenderer.getWidth(name), mc.textRenderer.getWidth(time));
                boxW += textW + 4;
                boxH = Math.max(boxH, mc.textRenderer.fontHeight * 2 + 4);
            } else {
                int textW = mc.textRenderer.getWidth(time);
                boxW += textW + 4;
                boxH = Math.max(boxH, mc.textRenderer.fontHeight + 4);
            }

            effectWidths[i] = boxW;
            effectHeights[i] = boxH;

            if (isVertical) {
                totalW = Math.max(totalW, boxW);
                totalH += boxH + gap;
            } else {
                totalW += boxW + gap;
                totalH = Math.max(totalH, boxH);
            }
        }

        if (isVertical) totalH -= gap;
        else totalW -= gap;

        int currentX = 0;
        int currentY = 0;
        String align = alignment.get();

        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance effect = effects.get(i);
            int w = effectWidths[i];
            int h = effectHeights[i];
            
            int drawX = currentX;
            int drawY = currentY;

            if (isVertical) {
                if ("Center".equals(align)) drawX = (totalW - w) / 2;
                else if ("Right".equals(align)) drawX = totalW - w;
            } else {
                if ("Center".equals(align)) drawY = (totalH - h) / 2;
                else if ("Right".equals(align)) drawY = totalH - h; 
            }

            // Draw Background
            if ("Blur".equals(style)) {
                RenderUtils.drawRect(context, drawX, drawY, w, h, 0x55000000);
            } else if ("Solid Panel".equals(style)) {
                int bgCol = panelColor.get();
                if (chromaWave.get()) {
                    int chroma = RenderUtils.getChromaColor(getEditorX() + drawX, getEditorY() + drawY, 1.0f);
                    bgCol = (bgCol & 0xFF000000) | (chroma & 0x00FFFFFF);
                }
                RenderUtils.drawRect(context, drawX, drawY, w, h, bgCol);
            }

            // Blink logic
            int durationTicks = effect.getDuration();
            boolean lowTime = durationTicks <= 200; // <= 10 seconds
            boolean skipRender = false;
            
            if (lowTime && blinkLowDuration.get()) {
                if (System.currentTimeMillis() % 1000 > 500) {
                    skipRender = true;
                }
            }

            if (!skipRender) {
                int contentX = drawX + 2;
                
                // Draw Icon
                if (hasIcon) {
                    net.minecraft.client.texture.Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
                    context.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured, sprite, contentX, drawY + (h - 18) / 2, 18, 18);
                    contentX += 20;
                }
                
                // Draw Text
                int color = normalTextColor.get();
                if (chromaWave.get()) {
                    color = RenderUtils.getChromaColor(getEditorX() + contentX, getEditorY() + drawY, 1.0f);
                }
                if (lowTime && colorLowDuration.get()) {
                    color = lowTextColor.get();
                }

                if (hasName) {
                    context.drawText(mc.textRenderer, displayNames[i], contentX, drawY + 2, color, true);
                    context.drawText(mc.textRenderer, displayTimes[i], contentX, drawY + 2 + mc.textRenderer.fontHeight, color, true);
                } else {
                    context.drawText(mc.textRenderer, displayTimes[i], contentX, drawY + (h - mc.textRenderer.fontHeight) / 2, color, true);
                }
            }

            if (isVertical) {
                currentY += h + gap;
            } else {
                currentX += w + gap;
            }
        }

        context.getMatrices().pop();
        
        setElementWidth((int) (totalW * sc));
        setElementHeight((int) (totalH * sc));
    }
}

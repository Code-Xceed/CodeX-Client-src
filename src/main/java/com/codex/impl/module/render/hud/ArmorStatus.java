package com.codex.impl.module.render.hud;

import com.codex.api.event.EventTarget;
import com.codex.api.event.events.UpdateEvent;
import com.codex.api.module.Category;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class ArmorStatus extends HudElementModule {

    private final ModeValue layout = new ModeValue("Layout", "Vertical", "Vertical", "Horizontal");
    private final ModeValue alignment = new ModeValue("Alignment", "Left", "Left", "Center", "Right");
    private final BoolValue reverseOrder = new BoolValue("Reverse Order", false);
    
    private final NumberValue scale = new NumberValue("Scale", 1.0, 0.5, 3.0, 0.1);
    private final NumberValue spacing = new NumberValue("Spacing", 2.0, 0.0, 20.0, 1.0);

    private final ModeValue visualStyle = new ModeValue("Visual Style", "None", "None", "Dark Box", "Outlined Box", "Compact Panel");
    private final BoolValue showHeldItem = new BoolValue("Show Held Item", true);
    private final BoolValue showOffhandItem = new BoolValue("Show Offhand Item", true);

    private final ModeValue durabilityDisplay = new ModeValue("Durability", "Percentage", "Percentage", "Numeric", "Bar Only", "Both");
    private final NumberValue criticalThreshold = new NumberValue("Critical %", 20.0, 5.0, 50.0, 1.0);
    
    private final ColorValue textColor = new ColorValue("Text Color", 0xFFFFFFFF);
    private final BoolValue chromaWave = new BoolValue("Chroma Wave Sync", false);

    private final ColorValue highDurabilityColor = new ColorValue("High Durability Color", 0xFF00FF00); // Green
    private final ColorValue medDurabilityColor = new ColorValue("Medium Durability Color", 0xFFFFFF00); // Yellow
    private final ColorValue lowDurabilityColor = new ColorValue("Low Durability Color", 0xFFFF0000); // Red

    private final BoolValue showPotions = new BoolValue("Show Potion Count", false);
    private final BoolValue showGapples = new BoolValue("Show Gapple Count", false);
    private final BoolValue showArrows = new BoolValue("Show Arrow Count", false);

    public ArmorStatus() {
        super("Armor Status", "Displays equipped armor and held items with durability info.", 10, 100, 50, 80, false);

        addValue(layout);
        addValue(alignment);
        addValue(reverseOrder);
        addValue(scale);
        addValue(spacing);

        addValue(visualStyle);
        addValue(showHeldItem);
        addValue(showOffhandItem);

        addValue(durabilityDisplay);
        addValue(criticalThreshold);

        addValue(textColor);
        addValue(chromaWave);
        addValue(highDurabilityColor);
        addValue(medDurabilityColor);
        addValue(lowDurabilityColor);

        addValue(showPotions);
        addValue(showGapples);
        addValue(showArrows);
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

        List<ItemStack> items = new ArrayList<>();
        
        // Build item list
        if (showHeldItem.get()) {
            items.add(mc.player.getMainHandStack());
        }
        
        List<ItemStack> armor = new ArrayList<>();
        for (ItemStack stack : mc.player.getArmorItems()) {
            armor.add(stack);
        }
        
        if (reverseOrder.get()) {
            for (int i = 0; i < armor.size(); i++) {
                items.add(armor.get(i));
            }
        } else {
            for (int i = armor.size() - 1; i >= 0; i--) {
                items.add(armor.get(i));
            }
        }
        
        if (showOffhandItem.get()) {
            items.add(mc.player.getOffHandStack());
        }
        
        // Add Consumables Tracking
        if (showPotions.get() || showGapples.get() || showArrows.get()) {
            int potionCount = 0;
            int gappleCount = 0;
            int arrowCount = 0;
            
            for (int i = 0; i < mc.player.getInventory().size(); i++) {
                ItemStack invStack = mc.player.getInventory().getStack(i);
                if (invStack.isEmpty()) continue;
                
                if (showPotions.get() && invStack.getItem() == Items.SPLASH_POTION) {
                    potionCount += invStack.getCount();
                }
                if (showGapples.get() && (invStack.getItem() == Items.GOLDEN_APPLE || invStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE)) {
                    gappleCount += invStack.getCount();
                }
                if (showArrows.get() && invStack.getItem() == Items.ARROW) {
                    arrowCount += invStack.getCount();
                }
            }
            
            if (showPotions.get() && potionCount > 0) {
                ItemStack dummy = new ItemStack(Items.SPLASH_POTION);
                dummy.setCount(potionCount);
                items.add(dummy);
            }
            if (showGapples.get() && gappleCount > 0) {
                ItemStack dummy = new ItemStack(Items.GOLDEN_APPLE);
                dummy.setCount(gappleCount);
                items.add(dummy);
            }
            if (showArrows.get() && arrowCount > 0) {
                ItemStack dummy = new ItemStack(Items.ARROW);
                dummy.setCount(arrowCount);
                items.add(dummy);
            }
        }

        // Mock items for GUI editor
        boolean isEditing = mc.currentScreen instanceof com.codex.client.gui.navigator.CodeXNavigatorScreen;
        if (isEditing && items.stream().allMatch(ItemStack::isEmpty)) {
            items.clear();
            items.add(new ItemStack(Items.DIAMOND_SWORD));
            items.add(new ItemStack(Items.DIAMOND_HELMET));
            items.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
            items.add(new ItemStack(Items.DIAMOND_LEGGINGS));
            items.add(new ItemStack(Items.DIAMOND_BOOTS));
            // Simulate damage
            items.get(0).setDamage(100);
            items.get(1).setDamage(300); 
            items.get(2).setDamage(50);
        }

        // Filter out completely empty items so we don't draw weird blanks
        items.removeIf(ItemStack::isEmpty);

        if (items.isEmpty()) {
            setElementWidth(40);
            setElementHeight(40);
            return;
        }

        context.getMatrices().push();
        float sc = (float) scale.asDouble();
        context.getMatrices().translate(Math.round(getEditorX()), Math.round(getEditorY()), 0);
        context.getMatrices().scale(sc, sc, 1.0f);

        boolean isVertical = "Vertical".equals(layout.get());
        int gap = (int) spacing.asDouble();
        
        int totalW = 0;
        int totalH = 0;

        // Calculate dimensions for centering/alignment
        int[] itemWidths = new int[items.size()];
        int[] itemHeights = new int[items.size()];

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int boxW = 20;
            int boxH = 20;
            
            String dMode = durabilityDisplay.get();
            boolean hasBar = stack.isDamageable() && ("Bar Only".equals(dMode) || "Both".equals(dMode));
            if (hasBar) {
                boxH += 2; // Extra height for the custom bar pushed down
            }

            String text = getDurabilityText(stack);
            if (!text.isEmpty()) {
                int tw = mc.textRenderer.getWidth(text);
                if (isVertical) {
                    boxW += tw + 6; // Icon + Text + Extra padding
                } else {
                    boxW = Math.max(boxW, tw + 4); 
                    boxH += mc.textRenderer.fontHeight + 2; 
                }
            }
            
            itemWidths[i] = boxW;
            itemHeights[i] = boxH;
            
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
        String style = visualStyle.get();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int w = itemWidths[i];
            int h = itemHeights[i];
            
            int drawX = currentX;
            int drawY = currentY;

            // Handle alignment within the row/column
            if (isVertical) {
                if ("Center".equals(align)) drawX = (totalW - w) / 2;
                else if ("Right".equals(align)) drawX = totalW - w;
            } else {
                if ("Center".equals(align)) drawY = (totalH - h) / 2;
                else if ("Right".equals(align)) drawY = totalH - h; // 'Right' means 'Bottom' in horizontal context
            }

            // Draw Background Style
            if ("Dark Box".equals(style)) {
                RenderUtils.drawRect(context, drawX - 2, drawY - 2, w + 4, h + 4, 0x70000000);
            } else if ("Outlined Box".equals(style)) {
                int outlineColor = 0x40FFFFFF;
                if (chromaWave.get()) {
                    outlineColor = RenderUtils.getChromaColor(getEditorX() + drawX, getEditorY() + drawY, 1.0f);
                    outlineColor = (0x80 << 24) | (outlineColor & 0x00FFFFFF);
                }
                RenderUtils.drawModernBox(context, drawX - 2, drawY - 2, w + 4, h + 4, 0x60000000, outlineColor);
            } else if ("Compact Panel".equals(style)) {
                RenderUtils.drawRect(context, drawX - 4, drawY - 4, w + 8, h + 8, 0x50000000);
                int panelColor = stack.isDamageable() ? getDurabilityColor(stack) : textColor.get();
                if (chromaWave.get() && !stack.isDamageable()) {
                    panelColor = RenderUtils.getChromaColor(getEditorX() + drawX, getEditorY() + drawY, 1.0f);
                }
                RenderUtils.drawRect(context, drawX - 4, drawY - 4, 2, h + 8, panelColor);
            }

            // Draw Item
            int itemDrawX = isVertical ? drawX : drawX + (w - 16) / 2;
            context.drawItem(stack, itemDrawX, drawY);

            // Draw Custom Durability Bar if requested
            String dMode = durabilityDisplay.get();
            if (stack.isDamageable() && ("Bar Only".equals(dMode) || "Both".equals(dMode))) {
                drawCustomDurabilityBar(context, stack, itemDrawX, drawY + 16); // Moved down by 2 pixels (from 14)
                // Also draw stack count manually if it's > 1, so we don't draw vanilla damage bar over our custom one
                if (stack.getCount() > 1) {
                    context.getMatrices().push();
                    context.getMatrices().translate(0, 0, 200);
                    String countStr = String.valueOf(stack.getCount());
                    context.drawText(mc.textRenderer, countStr, itemDrawX + 17 - mc.textRenderer.getWidth(countStr), drawY + 9, 0xFFFFFF, true);
                    context.getMatrices().pop();
                }
            } else if (mc.player != null) {
                // For non-damageable items or when we want the standard Vanilla bar
                // We use standard rendering without the tooltip
                if (stack.getCount() > 1 || stack.isItemBarVisible()) {
                    context.getMatrices().push();
                    context.getMatrices().translate(0, 0, 200);
                    if (stack.getCount() > 1) {
                        String countStr = String.valueOf(stack.getCount());
                        int countColor = 0xFFFFFF;
                        if (chromaWave.get() && !stack.isDamageable()) countColor = RenderUtils.getChromaColor(getEditorX() + itemDrawX, getEditorY() + drawY, 1.0f);
                        context.drawText(mc.textRenderer, countStr, itemDrawX + 17 - mc.textRenderer.getWidth(countStr), drawY + 9, countColor, true);
                    }
                    if (stack.isItemBarVisible()) {
                        int barColor = stack.getItemBarColor();
                        int barStep = stack.getItemBarStep();
                        RenderUtils.drawRect(context, itemDrawX + 2, drawY + 13, 13, 2, 0xFF000000);
                        RenderUtils.drawRect(context, itemDrawX + 2, drawY + 13, barStep, 1, barColor | 0xFF000000);
                    }
                    context.getMatrices().pop();
                }
            }

            // Draw Text
            String text = getDurabilityText(stack);
            if (!text.isEmpty()) {
                int color = getDurabilityColor(stack);
                if (chromaWave.get() && !stack.isDamageable()) {
                    color = RenderUtils.getChromaColor(getEditorX() + drawX, getEditorY() + drawY, 1.0f);
                }
                int textX, textY;
                
                boolean hasBar = stack.isDamageable() && ("Bar Only".equals(dMode) || "Both".equals(dMode));
                
                if (isVertical) {
                    textX = drawX + 24; 
                    textY = drawY + 5;
                } else {
                    textX = drawX + (w - mc.textRenderer.getWidth(text)) / 2;
                    textY = drawY + (hasBar ? 20 : 18); 
                }
                
                // Add a subtle drop shadow for readability
                context.drawText(mc.textRenderer, text, textX, textY, color, true);
            }

            if (isVertical) {
                currentY += h + gap;
            } else {
                currentX += w + gap;
            }
        }

        context.getMatrices().pop();
        
        // Add padding for the overall element bounding box based on style
        int padding = "None".equals(style) ? 0 : 4;
        setElementWidth((int) ((totalW + padding) * sc));
        setElementHeight((int) ((totalH + padding) * sc));
    }

    private String getDurabilityText(ItemStack stack) {
        if (!stack.isDamageable()) return "";
        
        String mode = durabilityDisplay.get();
        if ("Bar Only".equals(mode)) return "";

        int max = stack.getMaxDamage();
        int current = max - stack.getDamage();
        
        if ("Numeric".equals(mode)) {
            return String.valueOf(current);
        } else {
            // Percentage or Both
            int percent = (int) Math.round(((double) current / max) * 100.0);
            return percent + "%";
        }
    }

    private void drawCustomDurabilityBar(DrawContext context, ItemStack stack, int x, int y) {
        int max = stack.getMaxDamage();
        int current = max - stack.getDamage();
        float percent = (float) current / max;
        
        int barWidth = 13;
        int filledWidth = Math.round(barWidth * percent);
        int color = getDurabilityColor(stack);
        
        RenderUtils.drawRect(context, x + 2, y, barWidth, 2, 0xFF000000); // Bg
        RenderUtils.drawRect(context, x + 2, y, filledWidth, 1, color); // Fill
    }

    private int getDurabilityColor(ItemStack stack) {
        if (!stack.isDamageable()) return textColor.get();

        int max = stack.getMaxDamage();
        int current = max - stack.getDamage();
        float percent = (float) current / max;
        
        float criticalPct = (float) criticalThreshold.asDouble() / 100f;
        
        if (percent <= criticalPct) {
            // Blink if critical (using system time)
            if (System.currentTimeMillis() % 1000 < 500) {
                return lowDurabilityColor.get();
            } else {
                return 0xFFFF5555; // Lighter red blink
            }
        }
        
        if (percent > 0.6f) {
            // Interpolate Green -> Yellow
            float mapped = (percent - 0.6f) / 0.4f;
            return RenderUtils.interpolateColor(medDurabilityColor.get(), highDurabilityColor.get(), mapped);
        } else {
            // Interpolate Yellow -> Red
            float mapped = Math.max(0, (percent - criticalPct)) / (0.6f - criticalPct);
            return RenderUtils.interpolateColor(lowDurabilityColor.get(), medDurabilityColor.get(), mapped);
        }
    }
}

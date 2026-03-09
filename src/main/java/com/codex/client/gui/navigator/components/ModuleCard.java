package com.codex.client.gui.navigator.components;

import com.codex.api.module.Module;
import com.codex.client.gui.navigator.NavigatorStyle;
import com.codex.client.gui.navigator.UiInteractionFeedback;
import com.codex.client.gui.navigator.layout.UiRect;
import com.codex.client.gui.navigator.settings.GuiSettings;
import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ModuleCard {
    public final Module module;
    public int index;
    private final Animation hoverAnim = new Animation(0, 0.2f);
    private final Animation toggleAnim;
    private final Animation pressAnim = new Animation(0, 0.3f);

    public ModuleCard(Module module, int index) {
        this.module = module;
        this.index = index;
        this.toggleAnim = new Animation(module.isEnabled() ? 1f : 0f, 0.15f);
    }

    private UiRect bounds = new UiRect(0, 0, 0, 0);

    public void setBounds(UiRect bounds) {
        this.bounds = bounds;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return bounds.contains(mouseX, mouseY);
    }

    public boolean isHoveredArrow(double mouseX, double mouseY) {
        float x = bounds.x();
        float y = bounds.y();
        float width = bounds.width();
        float height = bounds.height();
        return mouseX >= x + width - 20 && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void onInteracted() {
        pressAnim.setValue(1f);
        pressAnim.setTarget(0f);
        UiInteractionFeedback.click();
    }

    public void render(
        DrawContext context,
        int mouseX,
        int mouseY,
        float partialTicks,
        boolean expanding,
        boolean isTarget,
        float expandProgress,
        int screenMiddleX,
        int screenHeight
    ) {
        float x = bounds.x();
        float y = bounds.y();
        float width = bounds.width();
        float height = bounds.height();

        if (y < 40 && !expanding) return;
        if (y > screenHeight - 20 && !expanding) return;

        if (expanding && isTarget) {
            // Animate box expanding to new Feature Screen layout
            int panelWidth = (int) (screenMiddleX * 2 * NavigatorStyle.Feature.PANEL_WIDTH_RATIO);
            int panelHeight = (int) (screenHeight * NavigatorStyle.Feature.PANEL_HEIGHT_RATIO);
            float targetX = screenMiddleX - (panelWidth / 2f);
            float targetY = (screenHeight - panelHeight) / 2f;

            float currentX = x + (targetX - x) * expandProgress;
            float currentY = y + (targetY - y) * expandProgress;
            float currentW = width + (panelWidth - width) * expandProgress;
            float currentH = height + (panelHeight - height) * expandProgress;

            RenderUtils.drawModernBox(
                context,
                currentX,
                currentY,
                currentW,
                currentH,
                GuiSettings.get().panelFillColor(),
                NavigatorStyle.Colors.FEATURE_PANEL_OUTLINE
            );
            return;
        } else if (expanding) {
            return; // Hide other cards while expanding
        }

        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean arrowHovered = isHoveredArrow(mouseX, mouseY);
        hoverAnim.setTarget(hovered ? 1.0f : 0.0f);
        float hoverProgress = hoverAnim.update(partialTicks);
        float pressProgress = pressAnim.update(partialTicks);

        toggleAnim.setTarget(module.isEnabled() ? 1.0f : 0.0f);
        float toggleProgress = toggleAnim.update(partialTicks);

        // Transparent background color transitions
        int baseColor = RenderUtils.interpolateColor(
            NavigatorStyle.Colors.MAIN_CARD_BASE_MIN,
            NavigatorStyle.Colors.MAIN_CARD_HOVER,
            hoverProgress
        );
        int color = RenderUtils.interpolateColor(baseColor, GuiSettings.get().accentColor(0x80), toggleProgress);
        int outlineColor = RenderUtils.interpolateColor(NavigatorStyle.Colors.CARD_OUTLINE, 0x55FFFFFF, hoverProgress);

        // Modern clean hover: no lift/scale, only tone and outline refinement.
        RenderUtils.drawModernBox(context, x, y, width, height, color, outlineColor);
        if (pressProgress > 0.0f) {
            int pressOverlay = RenderUtils.applyAlpha(0xFFFFFFFF, NavigatorStyle.Main.CARD_PRESS_FLASH_ALPHA * pressProgress);
            RenderUtils.drawRect(context, x + 1, y + 1, width - 2, height - 2, pressOverlay);
        }
        if (toggleProgress > 0.0f) {
            int accent = RenderUtils.applyAlpha(0xFFFFFFFF, 0.25f * toggleProgress);
            RenderUtils.drawRect(context, x + 1, y + 1, 1, height - 2, accent);
        }

        // Separator
        RenderUtils.drawRect(context, x + width - 24, y + 5, 1, height - 10, NavigatorStyle.Colors.HAIRLINE);
        int arrowColor = arrowHovered ? 0xFFFFFFFF : 0xFFAAAAAA;
        MinecraftClient mc = MinecraftClient.getInstance();
        
        // Settings Arrow
        int arrowX = (int) x + (int) width - 15;
        int arrowY = (int) y + ((int) height / 2) - 4;
        context.drawText(mc.textRenderer, ">", arrowX, arrowY, arrowColor, false);

        int textMaxWidth = (int) width - 38;
        String moduleName = trimWithEllipsis(mc, module.getName(), textMaxWidth);
        int textColor = module.isEnabled() ? 0xFFFFFFFF : 0xFFDDDDDD;
        context.drawText(mc.textRenderer, moduleName, (int) x + 10, (int) y + 8, textColor, false);

        String category = trimWithEllipsis(mc, module.getCategory().getName(), textMaxWidth);
        int categoryWidth = mc.textRenderer.getWidth(category) + 6;
        int categoryX = (int) x + 8;
        int categoryY = (int) y + (int) height - 13;
        int categoryBg = hovered ? 0x30000000 : 0x22000000;
        RenderUtils.drawModernBox(context, categoryX, categoryY, categoryWidth, 10, categoryBg, 0);
        int categoryColor = hovered ? 0xFFE0E0E0 : 0xFFC8C8C8;
        context.drawText(mc.textRenderer, category, categoryX + 3, categoryY + 1, categoryColor, false);
    }

    private String trimWithEllipsis(MinecraftClient mc, String text, int maxWidth) {
        if (mc.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = mc.textRenderer.getWidth(ellipsis);
        int baseWidth = Math.max(0, maxWidth - ellipsisWidth);
        return mc.textRenderer.trimToWidth(text, baseWidth) + ellipsis;
    }
}

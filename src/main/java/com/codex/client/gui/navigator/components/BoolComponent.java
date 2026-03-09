package com.codex.client.gui.navigator.components;

import com.codex.api.value.BoolValue;
import com.codex.client.gui.navigator.NavigatorStyle;
import com.codex.client.gui.navigator.UiInteractionFeedback;
import com.codex.client.gui.navigator.settings.GuiSettings;
import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class BoolComponent extends ValueComponent<BoolValue> {
    private final Animation toggleAnim;
    private final Animation hoverAnim;

    public BoolComponent(BoolValue value) {
        super(value);
        this.toggleAnim = new Animation(value.get() ? 1f : 0f, 0.2f);
        this.hoverAnim = new Animation(0f, 0.2f);
        this.height = 14;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        toggleAnim.setTarget(value.get() ? 1f : 0f);
        float progress = toggleAnim.update(partialTicks);
        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnim.setTarget(hovered ? 1f : 0f);
        float hoverProgress = hoverAnim.update(partialTicks);

        MinecraftClient mc = MinecraftClient.getInstance();
        int textColor = RenderUtils.interpolateColor(0xFFEEEEEE, 0xFFFFFFFF, hoverProgress);
        context.drawText(mc.textRenderer, value.getName(), x, y + 3, textColor, false);

        int switchWidth = 24;
        int switchHeight = 12;
        int switchX = x + width - switchWidth;
        int switchY = y + 1;

        // Background
        int bgColor = RenderUtils.interpolateColor(NavigatorStyle.Colors.CONTROL_IDLE, GuiSettings.get().accentColor(0x80), progress);
        bgColor = RenderUtils.interpolateColor(bgColor, NavigatorStyle.Colors.CONTROL_HOVER, hoverProgress * 0.25f);
        RenderUtils.drawModernBox(context, switchX, switchY, switchWidth, switchHeight, bgColor, NavigatorStyle.Colors.HAIRLINE);

        // Knob
        int knobWidth = 10;
        float knobX = switchX + 1 + (switchWidth - knobWidth - 2) * progress;
        RenderUtils.drawModernBox(context, knobX, switchY + 1, knobWidth, switchHeight - 2, 0xFFFFFFFF, 0);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            UiInteractionFeedback.click();
            value.toggle();
            notifyValueChanged();
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {}

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {}
}

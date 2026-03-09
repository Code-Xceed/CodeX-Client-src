package com.codex.client.gui.navigator.components;

import com.codex.api.value.NumberValue;
import com.codex.client.gui.navigator.NavigatorStyle;
import com.codex.client.gui.navigator.UiInteractionFeedback;
import com.codex.client.gui.navigator.settings.GuiSettings;
import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

public class NumberComponent extends ValueComponent<NumberValue> {
    private boolean dragging = false;
    private final Animation sliderAnim;
    private final Animation hoverAnim;

    public NumberComponent(NumberValue value) {
        super(value);
        this.sliderAnim = new Animation(getRenderPercentage(), 0.2f);
        this.hoverAnim = new Animation(0f, 0.2f);
        this.height = 18;
    }

    private float getRenderPercentage() {
        return (float) ((value.asDouble() - value.getMin().doubleValue()) / (value.getMax().doubleValue() - value.getMin().doubleValue()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        // Handle dragging
        if (dragging) {
            updateValue(mouseX);
        }

        sliderAnim.setTarget(getRenderPercentage());
        float progress = sliderAnim.update(partialTicks);
        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnim.setTarget(hovered ? 1f : 0f);
        float hoverProgress = hoverAnim.update(partialTicks);

        double current = value.get().doubleValue();
        String formatted = value.getIncrement().doubleValue() >= 1.0d
            ? Integer.toString((int) Math.round(current))
            : String.format(Locale.ROOT, "%.2f", current);
        String text = value.getName() + ": " + formatted;
        int textColor = RenderUtils.interpolateColor(0xFFEEEEEE, 0xFFFFFFFF, hoverProgress);
        context.drawText(mc.textRenderer, text, x, y, textColor, false);

        int sliderY = y + mc.textRenderer.fontHeight + 4;
        int sliderHeight = 4;
        
        // Background
        int sliderBg = RenderUtils.interpolateColor(
            NavigatorStyle.Colors.CONTROL_IDLE,
            NavigatorStyle.Colors.CONTROL_HOVER,
            hoverProgress
        );
        RenderUtils.drawModernBox(context, x, sliderY, width, sliderHeight, sliderBg, NavigatorStyle.Colors.HAIRLINE);
        
        // Fill
        RenderUtils.drawRect(context, x, sliderY, width * progress, sliderHeight, GuiSettings.get().accentColor(0x80));

        // Knob
        RenderUtils.drawModernBox(context, x + (width * progress) - 2, sliderY - 2, 4, sliderHeight + 4, 0xFFFFFFFF, 0);
    }

    private void updateValue(double mouseX) {
        double before = value.asDouble();
        float percent = (float) Math.max(0, Math.min(1, (mouseX - x) / width));
        double val = value.getMin().doubleValue() + percent * (value.getMax().doubleValue() - value.getMin().doubleValue());
        value.set(val);
        if (Double.compare(before, value.asDouble()) != 0) {
            notifyValueChanged();
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            UiInteractionFeedback.click();
            dragging = true;
            updateValue(mouseX);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            updateValue(mouseX);
        }
    }
    
    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sliderY = y + mc.textRenderer.fontHeight + 4;
        return mouseX >= x && mouseX <= x + width && mouseY >= sliderY - 4 && mouseY <= sliderY + 8;
    }
}

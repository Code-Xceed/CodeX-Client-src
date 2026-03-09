package com.codex.client.gui.navigator.components;

import com.codex.api.value.ModeValue;
import com.codex.client.gui.navigator.NavigatorStyle;
import com.codex.client.gui.navigator.UiInteractionFeedback;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ModeComponent extends ValueComponent<ModeValue> {
    private final com.codex.client.utils.Animation hoverAnim = new com.codex.client.utils.Animation(0f, 0.2f);
    
    public ModeComponent(ModeValue value) {
        super(value);
        this.height = 16;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        context.drawText(mc.textRenderer, value.getName() + ":", x, y + 4, NavigatorStyle.Feature.TEXT_PRIMARY, false);

        int boxWidth = mc.textRenderer.getWidth(value.get()) + 10;
        int boxX = x + width - boxWidth;
        
        boolean hovered = mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= y && mouseY <= y + 14;
        hoverAnim.setTarget(hovered ? 1f : 0f);
        float hoverProgress = hoverAnim.update(partialTicks);
        int boxColor = RenderUtils.interpolateColor(
            NavigatorStyle.Colors.CONTROL_IDLE,
            NavigatorStyle.Colors.CONTROL_HOVER,
            hoverProgress
        );
        int modeColor = RenderUtils.interpolateColor(NavigatorStyle.Feature.TEXT_SECONDARY, 0xFFD8D8D8, hoverProgress);
        
        RenderUtils.drawModernBox(
            context,
            boxX,
            y,
            boxWidth,
            14,
            boxColor,
            NavigatorStyle.Colors.HAIRLINE
        );
        
        context.drawText(mc.textRenderer, value.get(), boxX + 5, y + 3, modeColor, false);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int boxWidth = mc.textRenderer.getWidth(value.get()) + 10;
        int boxX = x + width - boxWidth;
        
        if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= y && mouseY <= y + 14) {
            if (button == 0) { // Left click -> cycle forward
                UiInteractionFeedback.click();
                cycle(1);
            } else if (button == 1) { // Right click -> cycle backward
                UiInteractionFeedback.click();
                cycle(-1);
            }
        }
    }
    
    private void cycle(int direction) {
        java.util.List<String> modes = value.getModes();
        int index = modes.indexOf(value.get());
        index += direction;
        if (index >= modes.size()) index = 0;
        if (index < 0) index = modes.size() - 1;
        String next = modes.get(index);
        if (!next.equals(value.get())) {
            value.set(next);
            notifyValueChanged();
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {}

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {}
}

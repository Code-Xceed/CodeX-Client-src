package com.codex.client.gui.navigator.components;

import com.codex.client.gui.navigator.NavigatorStyle;
import com.codex.client.gui.navigator.UiInteractionFeedback;
import com.codex.client.gui.navigator.layout.UiRect;
import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class NavigatorSearchBar {
    private final TextRenderer textRenderer;
    private final TextFieldWidget field;
    private final Animation activeAnim = new Animation(0f, 0.24f);
    private UiRect bounds;
    private int textY;

    public NavigatorSearchBar(TextRenderer textRenderer, int x, int y, int width, int height) {
        this.textRenderer = textRenderer;
        this.bounds = new UiRect(x, y, width, height);
        this.field = new TextFieldWidget(textRenderer, x, y, width, height, Text.literal("Search"));
        this.field.setDrawsBackground(false);
        this.field.setMaxLength(128);
        setBounds(x, y, width, height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.bounds = new UiRect(x, y, width, height);
        int centeredTextY = y + Math.max(0, (height - textRenderer.fontHeight) / 2) + NavigatorStyle.Main.SEARCH_TEXT_INSET_Y;
        this.textY = centeredTextY;
        field.setX(x + NavigatorStyle.Main.SEARCH_TEXT_INSET_X);
        field.setY(centeredTextY);
        field.setWidth(Math.max(40, width - (NavigatorStyle.Main.SEARCH_TEXT_INSET_X * 2)));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        boolean hovered = bounds.contains(mouseX, mouseY);
        boolean active = hovered || field.isFocused();
        activeAnim.setTarget(active ? 1f : 0f);
        float progress = activeAnim.update(partialTicks);

        int boxColor = RenderUtils.interpolateColor(
            NavigatorStyle.Main.SEARCH_BOX_IDLE,
            NavigatorStyle.Main.SEARCH_BOX_ACTIVE,
            progress
        );
        int outlineColor = RenderUtils.interpolateColor(
            NavigatorStyle.Main.SEARCH_OUTLINE_IDLE,
            NavigatorStyle.Main.SEARCH_OUTLINE_ACTIVE,
            progress
        );

        RenderUtils.drawModernBox(context, bounds.x(), bounds.y(), bounds.width(), bounds.height(), boxColor, outlineColor);
        context.enableScissor(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1);
        field.render(context, mouseX, mouseY, partialTicks);
        context.disableScissor();

        if (field.getText().isEmpty() && !field.isFocused()) {
            context.drawText(
                textRenderer,
                "Search",
                bounds.x() + 8,
                textY,
                NavigatorStyle.Main.SEARCH_PLACEHOLDER,
                false
            );
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!bounds.contains(mouseX, mouseY)) {
            field.setFocused(false);
            return false;
        }
        if (button == 0) {
            UiInteractionFeedback.click();
        }
        field.setFocused(true);
        return field.mouseClicked(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return field.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        return field.charTyped(chr, modifiers);
    }

    public String getText() {
        return field.getText();
    }

    public boolean isFocused() {
        return field.isFocused();
    }
}

package com.codex.client.gui.navigator;

import net.minecraft.client.gui.DrawContext;

public interface ICustomSettingsRenderer {
    int getCustomSettingsHeight(int width);

    int renderCustomSettings(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float partialTicks);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY);

    boolean mouseReleased(double mouseX, double mouseY, int button);

    boolean keyPressed(int keyCode, int scanCode, int modifiers);

    boolean charTyped(char chr, int modifiers);

    void tick();
}

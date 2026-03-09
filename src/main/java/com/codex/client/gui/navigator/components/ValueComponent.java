package com.codex.client.gui.navigator.components;

import com.codex.api.value.Value;
import net.minecraft.client.gui.DrawContext;

import java.util.Objects;

public abstract class ValueComponent<T extends Value<?>> {
    protected final T value;
    public int x, y, width, height;
    private Runnable changeListener = () -> {};

    public ValueComponent(T value) {
        this.value = value;
    }

    public abstract void render(DrawContext context, int mouseX, int mouseY, float partialTicks);
    public abstract void mouseClicked(double mouseX, double mouseY, int button);
    public abstract void mouseReleased(double mouseX, double mouseY, int button);
    public abstract void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY);
    public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    public void charTyped(char chr, int modifiers) {}

    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public boolean isVisible() {
        return value.isVisible();
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
    }

    protected final void notifyValueChanged() {
        changeListener.run();
    }

    public String getValueName() {
        return value.getName();
    }
    
    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}

package com.codex.impl.module.render.hud;

import com.codex.api.module.Category;
import com.codex.api.value.NumberValue;
import com.codex.client.module.ClientModule;

public abstract class HudElementModule extends ClientModule {
    private final NumberValue positionX;
    private final NumberValue positionY;
    private int elementWidth;
    private int elementHeight;

    protected HudElementModule(
        String name,
        String description,
        int defaultX,
        int defaultY,
        int elementWidth,
        int elementHeight,
        boolean defaultEnabled
    ) {
        super(name, description, Category.RENDER, defaultEnabled);
        this.elementWidth = Math.max(20, elementWidth);
        this.elementHeight = Math.max(12, elementHeight);
        this.positionX = new NumberValue("Position X", defaultX, 0, 5000, 1);
        this.positionY = new NumberValue("Position Y", defaultY, 0, 5000, 1);
        addValue(positionX);
        addValue(positionY);
    }

    public int getEditorX() {
        return (int) Math.round(positionX.asDouble());
    }

    public int getEditorY() {
        return (int) Math.round(positionY.asDouble());
    }

    public void setEditorPosition(double x, double y) {
        positionX.set(x);
        positionY.set(y);
    }

    public void resetPosition(int x, int y) {
        setEditorPosition(x, y);
    }

    public int getElementWidth() {
        return elementWidth;
    }

    public void setElementWidth(int elementWidth) {
        this.elementWidth = elementWidth;
    }

    public int getElementHeight() {
        return elementHeight;
    }

    public void setElementHeight(int elementHeight) {
        this.elementHeight = elementHeight;
    }

    public abstract int defaultX();
    public abstract int defaultY();
    public abstract void render(net.minecraft.client.gui.DrawContext context, float tickDelta);
}

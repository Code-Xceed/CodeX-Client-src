package com.codex.api.value;

import java.util.function.Supplier;
import java.awt.Color;

public class ColorValue extends Value<Integer> {
    
    // ARGB integer format
    public ColorValue(String name, int value) {
        super(name, value);
    }
    
    public ColorValue(String name, int value, Supplier<Boolean> visibility) {
        super(name, value, visibility);
    }
    
    public ColorValue(String name, Color color) {
        super(name, color.getRGB());
    }

    public int getRGB() {
        return get();
    }

    public Color getColor() {
        return new Color(get(), true);
    }

    public void setColor(int rgb) {
        set(rgb);
    }

    public void setColor(Color color) {
        set(color.getRGB());
    }
    
    public int getRed() {
        return (get() >> 16) & 0xFF;
    }
    
    public int getGreen() {
        return (get() >> 8) & 0xFF;
    }
    
    public int getBlue() {
        return get() & 0xFF;
    }
    
    public int getAlpha() {
        return (get() >> 24) & 0xFF;
    }
    
    // Allows setting individual components without touching the rest
    public void setAlpha(int alpha) {
        setColor((alpha << 24) | (get() & 0x00FFFFFF));
    }
    
    // We can also have HSV support
    public float[] getHSB() {
        return Color.RGBtoHSB(getRed(), getGreen(), getBlue(), null);
    }
}

package com.codex.api.render;

public interface IRenderer {
    
    // Shared drawing methods
    void drawRect(int x, int y, int width, int height, int color);
    
    void drawText(String text, int x, int y, int color, boolean shadow);
    
    int getTextWidth(String text);
    
    int getScreenHeight();
    
    int getScreenWidth();
}

package com.codex.api.render;

public class Renderer {
    private static final IRenderer NO_OP_RENDERER = new IRenderer() {
        @Override
        public void drawRect(int x, int y, int width, int height, int color) {}

        @Override
        public void drawText(String text, int x, int y, int color, boolean shadow) {}

        @Override
        public int getTextWidth(String text) {
            return 0;
        }

        @Override
        public int getScreenHeight() {
            return 0;
        }

        @Override
        public int getScreenWidth() {
            return 0;
        }
    };

    private static volatile IRenderer instance = NO_OP_RENDERER;

    public static void setInstance(IRenderer instance) {
        Renderer.instance = instance == null ? NO_OP_RENDERER : instance;
    }

    public static IRenderer get() {
        return instance;
    }

    public static boolean isConfigured() {
        return instance != NO_OP_RENDERER;
    }
}

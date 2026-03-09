package com.codex.client.gui.navigator.layout;

import com.codex.client.gui.navigator.NavigatorStyle;

public final class NavigatorChromeLayout {
    public static final int DEFAULT_SIDE_MARGIN = NavigatorLayoutTokens.Chrome.CONTENT_SIDE_MARGIN;
    public static final int DEFAULT_BOTTOM_MARGIN = NavigatorLayoutTokens.Chrome.CONTENT_BOTTOM_MARGIN;

    private static final int MIN_CONTENT_WIDTH = 320;
    private static final int MIN_CONTENT_HEIGHT = 40;
    private static final int MIN_BOTTOM_SPAN = 80;

    private NavigatorChromeLayout() {}

    public static UiRect searchRect(int screenWidth) {
        int width = NavigatorStyle.Main.SEARCH_WIDTH;
        int x = screenWidth - NavigatorStyle.Main.SEARCH_RIGHT_MARGIN - width;
        return new UiRect(x, NavigatorStyle.Main.SEARCH_Y, width, NavigatorStyle.Main.SEARCH_HEIGHT);
    }

    public static UiRect contentViewport(int screenWidth, int screenHeight, int sideMargin, int bottomMargin) {
        int top = NavigatorStyle.Main.GRID_TOP;
        int width = Math.max(MIN_CONTENT_WIDTH, screenWidth - (sideMargin * 2));
        int x = (screenWidth - width) / 2;
        int bottom = Math.max(top + MIN_BOTTOM_SPAN, screenHeight - bottomMargin);
        return new UiRect(x, top, width, Math.max(MIN_CONTENT_HEIGHT, bottom - top));
    }

    public static UiRect contentViewport(int screenWidth, int screenHeight) {
        return contentViewport(screenWidth, screenHeight, DEFAULT_SIDE_MARGIN, DEFAULT_BOTTOM_MARGIN);
    }
}

package com.codex.client.gui.navigator.layout;

import com.codex.client.gui.navigator.NavigatorStyle;

public final class MainGridLayout {
    private int columns = 1;
    private int startX;

    public void resize(int screenWidth) {
        columns = Math.max(1, (screenWidth - NavigatorStyle.Main.GRID_COLUMNS_WIDTH_BUDGET)
            / (NavigatorStyle.Main.CARD_WIDTH + NavigatorStyle.Main.CARD_GAP));

        int totalGridWidth = (columns * NavigatorStyle.Main.CARD_WIDTH)
            + ((columns - 1) * NavigatorStyle.Main.CARD_GAP);
        startX = (screenWidth - totalGridWidth) / 2;
    }

    public UiRect cardBoundsFor(int index, float scroll) {
        int column = index % columns;
        int row = index / columns;

        int x = startX + column * (NavigatorStyle.Main.CARD_WIDTH + NavigatorStyle.Main.CARD_GAP);
        int y = NavigatorStyle.Main.GRID_TOP
            + row * (NavigatorStyle.Main.CARD_HEIGHT + NavigatorStyle.Main.CARD_GAP)
            + (int) scroll;

        return new UiRect(x, y, NavigatorStyle.Main.CARD_WIDTH, NavigatorStyle.Main.CARD_HEIGHT);
    }

    public UiRect gridScissor(int screenWidth, int screenHeight) {
        int top = NavigatorStyle.Main.GRID_SCISSOR_TOP;
        int bottom = Math.max(top, screenHeight - NavigatorStyle.Main.GRID_SCISSOR_BOTTOM_MARGIN);
        return new UiRect(0, top, screenWidth, bottom - top);
    }

    public int contentHeightFor(int itemCount) {
        int rows = rowCount(itemCount);
        return (rows * (NavigatorStyle.Main.CARD_HEIGHT + NavigatorStyle.Main.CARD_GAP))
            + NavigatorStyle.Main.CONTENT_PADDING;
    }

    private int rowCount(int count) {
        if (count <= 0) {
            return 0;
        }
        return (count + columns - 1) / columns;
    }
}

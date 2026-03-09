package com.codex.client.gui.navigator.layout;

import com.codex.client.gui.navigator.NavigatorStyle;

public final class FeaturePanelLayout {
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int leftWidth;
    private int rightWidth;

    public void resize(int screenWidth, int screenHeight) {
        UiRect viewport = NavigatorChromeLayout.contentViewport(screenWidth, screenHeight);
        panelWidth = Math.min(
            viewport.width(),
            (int) (screenWidth * NavigatorStyle.Feature.PANEL_WIDTH_RATIO)
        );
        panelHeight = Math.min(
            viewport.height(),
            (int) (screenHeight * NavigatorStyle.Feature.PANEL_HEIGHT_RATIO)
        );
        panelX = (screenWidth - panelWidth) / 2;
        int centeredY = (screenHeight - panelHeight) / 2;
        int clampedY = Math.max(viewport.y(), centeredY);
        int maxY = Math.max(viewport.y(), viewport.bottom() - panelHeight);
        panelY = Math.min(clampedY, maxY);

        leftWidth = (int) (panelWidth * NavigatorStyle.Feature.LEFT_WIDTH_RATIO);
        rightWidth = panelWidth - leftWidth;
    }

    public UiRect panelRect() {
        return new UiRect(panelX, panelY, panelWidth, panelHeight);
    }

    public UiRect dividerRect() {
        return new UiRect(
            panelX + leftWidth,
            panelY + NavigatorStyle.Feature.DIVIDER_TOP,
            1,
            panelHeight - NavigatorStyle.Feature.DIVIDER_BOTTOM
        );
    }

    public UiRect keybindRect() {
        return new UiRect(
            leftColumnX(),
            panelY + NavigatorStyle.Feature.KEYBIND_Y_OFFSET,
            leftWidth - (NavigatorStyle.Feature.PANEL_PADDING * 2),
            NavigatorStyle.Feature.KEYBIND_HEIGHT
        );
    }

    public UiRect toggleRect() {
        return new UiRect(
            leftColumnX(),
            panelY + panelHeight - NavigatorStyle.Feature.TOGGLE_Y_OFFSET_FROM_BOTTOM,
            leftWidth - (NavigatorStyle.Feature.PANEL_PADDING * 2),
            NavigatorStyle.Feature.TOGGLE_HEIGHT
        );
    }

    public UiRect rightScissorRect() {
        int x = rightColumnX();
        int y = panelY + NavigatorStyle.Feature.RIGHT_SCISSOR_TOP_OFFSET;
        int width = rightContentWidth();
        int bottom = panelY + panelHeight - NavigatorStyle.Feature.RIGHT_SCISSOR_BOTTOM_OFFSET;
        return new UiRect(x, y, width, Math.max(0, bottom - y));
    }

    public UiRect previewRect() {
        int startY = leftDescriptionY() + (NavigatorStyle.Feature.DESCRIPTION_LINE_HEIGHT * 3) + 15;
        int bottomRowY = panelY + panelHeight - NavigatorStyle.Feature.TOGGLE_Y_OFFSET_FROM_BOTTOM;
        return new UiRect(
            leftColumnX(),
            startY,
            leftWidth - (NavigatorStyle.Feature.PANEL_PADDING * 2),
            bottomRowY - startY - 10
        );
    }

    public UiRect hudModuleToggleRect() {
        int y = panelY + panelHeight - NavigatorStyle.Feature.TOGGLE_Y_OFFSET_FROM_BOTTOM;
        int fullWidth = leftWidth - (NavigatorStyle.Feature.PANEL_PADDING * 2);
        int width = (fullWidth - 8) / 2;
        return new UiRect(
            leftColumnX(),
            y,
            width,
            NavigatorStyle.Feature.TOGGLE_HEIGHT
        );
    }
    
    public UiRect hudModuleHudEditorRect() {
        int y = panelY + panelHeight - NavigatorStyle.Feature.TOGGLE_Y_OFFSET_FROM_BOTTOM;
        int fullWidth = leftWidth - (NavigatorStyle.Feature.PANEL_PADDING * 2);
        int width = (fullWidth - 8) / 2;
        return new UiRect(
            leftColumnX() + width + 8,
            y,
            width,
            NavigatorStyle.Feature.TOGGLE_HEIGHT
        );
    }
    
    public UiRect hudModuleKeybindRect() {
        int y = leftTitleY() + 1;
        int fullWidth = leftWidth - (NavigatorStyle.Feature.PANEL_PADDING * 2);
        int width = 48; // Ultra compact width
        return new UiRect(
            leftColumnX() + fullWidth - width,
            y,
            width,
            12 // Compact height
        );
    }

    public int leftColumnX() {
        return panelX + NavigatorStyle.Feature.PANEL_PADDING;
    }

    public int leftTitleY() {
        return panelY + NavigatorStyle.Feature.TITLE_Y_OFFSET;
    }

    public int leftCategoryY() {
        return panelY + NavigatorStyle.Feature.CATEGORY_Y_OFFSET;
    }

    public int leftDescriptionY() {
        return panelY + NavigatorStyle.Feature.DESCRIPTION_Y_OFFSET;
    }

    public int leftDescriptionWidth() {
        return leftWidth - (NavigatorStyle.Feature.PANEL_PADDING * 2);
    }

    public int rightColumnX() {
        return panelX + leftWidth + NavigatorStyle.Feature.PANEL_PADDING;
    }

    public int rightContentWidth() {
        return rightWidth - (NavigatorStyle.Feature.PANEL_PADDING * 2);
    }

    public int rightTitleY() {
        return panelY + NavigatorStyle.Feature.RIGHT_TITLE_Y_OFFSET;
    }

    public int rightHeaderLineY() {
        return panelY + NavigatorStyle.Feature.RIGHT_HEADER_LINE_Y_OFFSET;
    }

    public int rightContentStartY() {
        return panelY + NavigatorStyle.Feature.RIGHT_CONTENT_START_Y_OFFSET;
    }

    public int keybindLabelY() {
        return keybindRect().y() - NavigatorStyle.Feature.KEYBIND_LABEL_OFFSET;
    }

    public int settingsContentHeight(int visibleComponentCount) {
        return (visibleComponentCount * NavigatorStyle.Feature.SETTINGS_HEIGHT_PER_COMPONENT)
            + NavigatorStyle.Feature.SETTINGS_HEIGHT_BASE;
    }
}

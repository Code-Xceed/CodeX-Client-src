package com.codex.client.gui.navigator.layout;

public final class NavigatorLayoutTokens {
    private NavigatorLayoutTokens() {}

    public static final class Chrome {
        public static final int CONTENT_SIDE_MARGIN = 28;
        public static final int CONTENT_BOTTOM_MARGIN = 20;

        private Chrome() {}
    }

    public static final class SettingsPanel {
        public static final int COLUMN_GAP = 14;
        public static final int ROW_GAP = 12;
        public static final int PANEL_SIDE_PADDING = 10;
        public static final int PANEL_TITLE_Y_OFFSET = 12;
        public static final int PANEL_LINE_Y_OFFSET = 24;
        public static final int CONTENT_START_Y_OFFSET = 34;
        public static final int CONTENT_BOTTOM_PADDING = 10;
        public static final int MIN_TWO_COLUMN_WIDTH = 560;

        public static final int PREVIEW_HEIGHT = 26;
        public static final int PREVIEW_GAP = 8;
        public static final int PREVIEW_SWATCH_WIDTH = 44;
        public static final int COMPONENT_GAP = 12;
        public static final int KEYBIND_HEIGHT = 20;
        public static final int RESET_HEIGHT = 18;
        public static final int STACK_GAP = 10;
        public static final int MIN_SECTION_HEIGHT = 74;

        private SettingsPanel() {}
    }

    public static final class FeatureSettings {
        public static final int SECTION_GAP = 10;
        public static final int SECTION_SIDE_PADDING = 8;
        public static final int SECTION_HEADER_Y_OFFSET = 7;
        public static final int SECTION_HEADER_LINE_Y_OFFSET = 18;
        public static final int SECTION_CONTENT_TOP = 24;
        public static final int SECTION_BOTTOM_PADDING = 8;
        public static final int SECTION_MIN_HEIGHT = 48;

        private FeatureSettings() {}
    }
}

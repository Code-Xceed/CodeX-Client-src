package com.codex.client.gui.navigator;

public final class NavigatorStyle {
    private NavigatorStyle() {}

    public static final class Animation {
        public static final float SMOOTH = 0.24f;
        public static final float MAIN_EXPAND = 0.30f;

        private Animation() {}
    }

    public static final class Scroll {
        public static final float STEP = 40.0f;
        public static final int DEFAULT_NON_SCROLLABLE_AREA = 26;
        public static final int CONTENT_BOTTOM_PADDING = 120;

        private Scroll() {}
    }

    public static final class Overlay {
        public static final int TOP_ALPHA = 0x50;
        public static final int BOTTOM_ALPHA = 0x90;

        private Overlay() {}
    }

    public static final class Main {
        public static final int TITLE_X = 20;
        public static final int TITLE_Y = 20;
        public static final float TITLE_SCALE = 1.5f;

        public static final int SEARCH_WIDTH = 172;
        public static final int SEARCH_HEIGHT = 20;
        public static final int SEARCH_RIGHT_MARGIN = 20;
        public static final int SEARCH_Y = 20;
        public static final int SEARCH_TEXT_INSET_X = 8;
        public static final int SEARCH_TEXT_INSET_Y = 0;
        public static final int SEARCH_BOX_IDLE = 0x55000000;
        public static final int SEARCH_BOX_ACTIVE = 0x78000000;
        public static final int SEARCH_OUTLINE_IDLE = 0x33FFFFFF;
        public static final int SEARCH_OUTLINE_ACTIVE = 0x55FFFFFF;
        public static final int SEARCH_PLACEHOLDER = 0x88FFFFFF;
        public static final int EMPTY_RESULTS_COLOR = 0xB0FFFFFF;
        public static final int EMPTY_RESULTS_Y_OFFSET = 14;

        public static final int GRID_COLUMNS_WIDTH_BUDGET = 100;
        public static final int CARD_WIDTH = 120;
        public static final int CARD_HEIGHT = 35;
        public static final int CARD_GAP = 15;
        public static final int GRID_TOP = 80;
        public static final int GRID_SCISSOR_TOP = 70;
        public static final int GRID_SCISSOR_BOTTOM_MARGIN = 20;
        public static final int CONTENT_PADDING = 100;
        public static final float CARD_PRESS_FLASH_ALPHA = 0.20f;

        private Main() {}
    }

    public static final class TopBar {
        public static final int Y = 16;
        public static final int HEIGHT = 26;
        public static final int TAB_HEIGHT = 20;
        public static final int TAB_PADDING_X = 12;
        public static final int TAB_GAP = 6;
        public static final int TAB_INSET_X = 6;
        public static final int TAB_INSET_Y = 3;
        public static final int ACTIVE_UNDERLINE_HEIGHT = 2;
        public static final int SCROLL_LOCK_BOTTOM = 52;

        private TopBar() {}
    }

    public static final class HudEditor {
        public static final int CONTENT_TOP = 8;
        public static final int ELEMENT_MIN_X = 0;
        public static final int ELEMENT_MIN_Y = 0;
        public static final int ELEMENT_PADDING = 8;
        public static final int TOGGLE_SIZE = 12;
        public static final int TOGGLE_MARGIN = 4;
        public static final int ELEMENT_SNAP_GAP = 0;

        public static final int CENTER_BUTTON_WIDTH = 92;
        public static final int CENTER_BUTTON_HEIGHT = 20;
        public static final int CENTER_BUTTON_GAP = 12;

        private HudEditor() {}
    }

    public static final class Feature {
        public static final float PANEL_WIDTH_RATIO = 0.70f;
        public static final float PANEL_HEIGHT_RATIO = 0.75f;
        public static final float LEFT_WIDTH_RATIO = 0.35f;

        public static final int PANEL_PADDING = 15;
        public static final int DIVIDER_TOP = 15;
        public static final int DIVIDER_BOTTOM = 30;

        public static final int TITLE_Y_OFFSET = 15;
        public static final int CATEGORY_Y_OFFSET = 30;
        public static final int DESCRIPTION_Y_OFFSET = 50;
        public static final int DESCRIPTION_LINE_HEIGHT = 12;

        public static final int KEYBIND_Y_OFFSET = 120;
        public static final int KEYBIND_HEIGHT = 20;
        public static final int KEYBIND_LABEL_OFFSET = 12;

        public static final int TOGGLE_HEIGHT = 20;
        public static final int TOGGLE_Y_OFFSET_FROM_BOTTOM = 35;

        public static final int RIGHT_TITLE_Y_OFFSET = 15;
        public static final int RIGHT_HEADER_LINE_Y_OFFSET = 28;
        public static final int RIGHT_SCISSOR_TOP_OFFSET = 30;
        public static final int RIGHT_SCISSOR_BOTTOM_OFFSET = 15;
        public static final int RIGHT_CONTENT_START_Y_OFFSET = 40;

        public static final int COMPONENT_GAP = 15;
        public static final int SETTINGS_HEIGHT_PER_COMPONENT = 30;
        public static final int SETTINGS_HEIGHT_BASE = 20;
        public static final int TEXT_PRIMARY = 0xFFFFFF;
        public static final int TEXT_SECONDARY = 0xFFAAAAAA;
        public static final int TEXT_TERTIARY = 0xFFDDDDDD;
        public static final int TEXT_MUTED = 0xFF888888;

        private Feature() {}
    }

    public static final class Colors {
        public static final int BACKGROUND_BOX_FILL = 0x80000000;
        public static final int BACKGROUND_BOX_OUTLINE = 0x44FFFFFF;
        public static final int FEATURE_PANEL_FILL = 0x95050505;
        public static final int FEATURE_PANEL_OUTLINE = 0x44FFFFFF;

        public static final int MAIN_CARD_BASE_MIN = 0x50000000;
        public static final int MAIN_CARD_BASE_MAX = 0x80000000;
        public static final int MAIN_CARD_HOVER = 0x8F000000;
        public static final int ENABLED_ACCENT = 0x80286528;
        public static final int HAIRLINE = 0x33FFFFFF;
        public static final int CONTROL_IDLE = 0x60000000;
        public static final int CONTROL_HOVER = 0x90000000;
        public static final int CARD_OUTLINE = 0x33FFFFFF;

        private Colors() {}
    }
}

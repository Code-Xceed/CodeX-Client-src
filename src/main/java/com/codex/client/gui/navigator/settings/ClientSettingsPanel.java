package com.codex.client.gui.navigator.settings;

import com.codex.api.module.Module;
import com.codex.api.module.ModuleManager;
import com.codex.api.value.Value;
import com.codex.client.CodeXClient;
import com.codex.client.gui.navigator.NavigatorStyle;
import com.codex.client.gui.navigator.UiInteractionFeedback;
import com.codex.client.gui.navigator.components.NavigatorSearchBar;
import com.codex.client.gui.navigator.components.ValueComponent;
import com.codex.client.gui.navigator.components.ValueComponentFactory;
import com.codex.client.gui.navigator.layout.NavigatorChromeLayout;
import com.codex.client.gui.navigator.layout.NavigatorLayoutTokens;
import com.codex.client.gui.navigator.layout.UiRect;
import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import com.codex.impl.module.render.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class ClientSettingsPanel {
    private static final int VIEWPORT_BOTTOM_MARGIN = NavigatorChromeLayout.DEFAULT_BOTTOM_MARGIN;
    private static final int VIEWPORT_SIDE_MARGIN = NavigatorChromeLayout.DEFAULT_SIDE_MARGIN;

    private static final int PANEL_GAP = NavigatorLayoutTokens.SettingsPanel.COLUMN_GAP;
    private static final int PANEL_ROW_GAP = NavigatorLayoutTokens.SettingsPanel.ROW_GAP;
    private static final int PANEL_SIDE_PADDING = NavigatorLayoutTokens.SettingsPanel.PANEL_SIDE_PADDING;
    private static final int PANEL_TITLE_Y_OFFSET = NavigatorLayoutTokens.SettingsPanel.PANEL_TITLE_Y_OFFSET;
    private static final int PANEL_LINE_Y_OFFSET = NavigatorLayoutTokens.SettingsPanel.PANEL_LINE_Y_OFFSET;
    private static final int CONTENT_START_Y_OFFSET = NavigatorLayoutTokens.SettingsPanel.CONTENT_START_Y_OFFSET;
    private static final int CONTENT_BOTTOM_PADDING = NavigatorLayoutTokens.SettingsPanel.CONTENT_BOTTOM_PADDING;
    private static final int MIN_TWO_COLUMN_WIDTH = NavigatorLayoutTokens.SettingsPanel.MIN_TWO_COLUMN_WIDTH;

    private static final int PREVIEW_HEIGHT = NavigatorLayoutTokens.SettingsPanel.PREVIEW_HEIGHT;
    private static final int PREVIEW_GAP = NavigatorLayoutTokens.SettingsPanel.PREVIEW_GAP;
    private static final int PREVIEW_SWATCH_WIDTH = NavigatorLayoutTokens.SettingsPanel.PREVIEW_SWATCH_WIDTH;
    private static final int COMPONENT_GAP = NavigatorLayoutTokens.SettingsPanel.COMPONENT_GAP;
    private static final int KEYBIND_HEIGHT = NavigatorLayoutTokens.SettingsPanel.KEYBIND_HEIGHT;
    private static final int RESET_HEIGHT = NavigatorLayoutTokens.SettingsPanel.RESET_HEIGHT;
    private static final int STACK_GAP = NavigatorLayoutTokens.SettingsPanel.STACK_GAP;
    private static final int MIN_SECTION_HEIGHT = NavigatorLayoutTokens.SettingsPanel.MIN_SECTION_HEIGHT;

    private static final float SCROLL_STEP = 28.0f;
    private static final List<String> SECTION_ORDER = Arrays.asList(
        "main_colors",
        "background_tint",
        "panel_surface",
        "animations",
        "feedback",
        "controls",
        "general"
    );

    private final ClientSettingsModel model = new ClientSettingsModel();
    private final List<Section> sections = new ArrayList<>();
    private final Animation scrollAnim = new Animation(0f, 0.22f);
    private final Animation keybindHoverAnim = new Animation(0f, 0.2f);
    private final Animation resetHoverAnim = new Animation(0f, 0.2f);

    private UiRect contentViewport = new UiRect(0, 0, 0, 0);
    private NavigatorSearchBar searchBar;
    private String searchQuery = "";
    private int maxScroll;
    private boolean listeningForGuiKeybind;
    private boolean layoutDirty = true;
    private int lastAppliedScroll = Integer.MIN_VALUE;

    public ClientSettingsPanel() {
        buildSections();
    }

    public void resize(int screenWidth, int screenHeight) {
        model.pullFromSettings();
        UiRect searchRect = NavigatorChromeLayout.searchRect(screenWidth);
        ensureSearchBar(searchRect.x(), searchRect.y(), searchRect.width(), searchRect.height());
        contentViewport = NavigatorChromeLayout.contentViewport(
            screenWidth,
            screenHeight,
            VIEWPORT_SIDE_MARGIN,
            VIEWPORT_BOTTOM_MARGIN
        );
        markLayoutDirty();
        ensureInteractiveLayout();
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        ensureLayoutMetrics();
        int scroll = (int) scrollAnim.update(partialTicks);
        applyLayoutForScroll(scroll);

        context.enableScissor(
            contentViewport.x(),
            contentViewport.y(),
            contentViewport.right(),
            contentViewport.bottom()
        );

        for (Section section : sections) {
            if (!sectionMatchesSearch(section)) {
                continue;
            }
            drawSection(context, mouseX, mouseY, partialTicks, section);
        }

        context.disableScissor();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!contentViewport.contains(mouseX, mouseY)) {
            return false;
        }
        ensureLayoutMetrics();
        float next = scrollAnim.getTarget() + (float) (verticalAmount * SCROLL_STEP);
        if (next > 0f) {
            next = 0f;
        } else if (next < maxScroll) {
            next = maxScroll;
        }
        scrollAnim.setTarget(next);
        return true;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        ensureInteractiveLayout();
        if (!contentViewport.contains(mouseX, mouseY)) {
            if (listeningForGuiKeybind) {
                listeningForGuiKeybind = false;
            }
            return;
        }

        for (Section section : sections) {
            if (!sectionMatchesSearch(section)) {
                continue;
            }
            if (!section.controls) {
                continue;
            }
            if (button == 0 && section.keybindRect.contains(mouseX, mouseY)) {
                UiInteractionFeedback.click();
                listeningForGuiKeybind = !listeningForGuiKeybind;
                return;
            }
            if (button == 0 && section.resetRect.contains(mouseX, mouseY)) {
                UiInteractionFeedback.click();
                model.resetVisualDefaults();
                model.applyToSettings();
                CodeXClient.queueConfigSave();
                return;
            }
        }

        if (listeningForGuiKeybind) {
            listeningForGuiKeybind = false;
        }

        for (Section section : sections) {
            if (!sectionMatchesSearch(section)) {
                continue;
            }
            if (!section.panelRect.contains(mouseX, mouseY)) {
                continue;
            }
            for (ValueComponent<?> component : section.components) {
                component.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        ensureInteractiveLayout();
        for (Section section : sections) {
            for (ValueComponent<?> component : section.components) {
                component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        ensureInteractiveLayout();
        for (Section section : sections) {
            for (ValueComponent<?> component : section.components) {
                component.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBar != null && searchBar.isFocused() && searchBar.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!listeningForGuiKeybind) {
            return false;
        }

        Module clickGui = ModuleManager.getInstance().getModule(ClickGUI.class);
        if (clickGui == null) {
            listeningForGuiKeybind = false;
            return false;
        }

        UiInteractionFeedback.click();
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            clickGui.setKey(0);
        } else {
            clickGui.setKey(keyCode);
        }
        listeningForGuiKeybind = false;
        CodeXClient.queueConfigSave();
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        return searchBar != null && searchBar.isFocused() && searchBar.charTyped(chr, modifiers);
    }

    public void renderHeader(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        if (searchBar == null) {
            return;
        }
        searchBar.render(context, mouseX, mouseY, partialTicks);
        String currentQuery = searchBar.getText().trim();
        if (!currentQuery.equals(searchQuery)) {
            searchQuery = currentQuery;
            markLayoutDirty();
            ensureLayoutMetrics();
        }
    }

    public boolean mouseClickedHeader(double mouseX, double mouseY, int button) {
        return searchBar != null && searchBar.mouseClicked(mouseX, mouseY, button);
    }

    private void buildSections() {
        sections.clear();
        LinkedHashMap<String, Section> sectionMap = new LinkedHashMap<>();
        for (String key : SECTION_ORDER) {
            sectionMap.put(key, createSection(key));
        }

        for (Value<?> value : model.allValues()) {
            String key = resolveSectionKey(value.getName());
            Section section = sectionMap.computeIfAbsent(key, this::createSection);
            ValueComponent<?> component = ValueComponentFactory.create(value);
            if (component == null) {
                continue;
            }
            component.setChangeListener(() -> {
                model.applyToSettings();
                CodeXClient.queueConfigSave();
                markLayoutDirty();
            });
            section.components.add(component);
        }

        Section controls = sectionMap.computeIfAbsent("controls", this::createSection);
        controls.controls = true;

        for (Section section : sectionMap.values()) {
            if (!section.components.isEmpty() || section.controls || section.hasPreview()) {
                sections.add(section);
            }
        }
    }

    private Section createSection(String key) {
        Section section = new Section(key, titleForKey(key));
        section.accentPreview = "main_colors".equals(key);
        section.backgroundPreview = "background_tint".equals(key);
        section.panelPreview = "panel_surface".equals(key);
        section.controls = "controls".equals(key);
        return section;
    }

    private String resolveSectionKey(String valueName) {
        if (valueName == null) {
            return "general";
        }
        if (valueName.startsWith("Main ") || valueName.startsWith("Accent ")) {
            return "main_colors";
        }
        if (valueName.startsWith("Background ")) {
            return "background_tint";
        }
        if (valueName.startsWith("Panel ")) {
            return "panel_surface";
        }
        if (valueName.startsWith("Animation")) {
            return "animations";
        }
        if (valueName.contains("Sound")) {
            return "feedback";
        }
        return "general";
    }

    private String titleForKey(String key) {
        return switch (key) {
            case "main_colors" -> "Main Colors";
            case "background_tint" -> "Background Tint";
            case "panel_surface" -> "Panel Surface";
            case "animations" -> "Animations";
            case "feedback" -> "Feedback";
            case "controls" -> "Controls";
            default -> "General";
        };
    }

    private void drawSection(DrawContext context, int mouseX, int mouseY, float partialTicks, Section section) {
        MinecraftClient mc = MinecraftClient.getInstance();
        UiRect panel = section.panelRect;
        if (panel.width() <= 0 || panel.height() <= 0) {
            return;
        }

        RenderUtils.drawModernBox(
            context,
            panel.x(),
            panel.y(),
            panel.width(),
            panel.height(),
            GuiSettings.get().panelFillColor(),
            NavigatorStyle.Colors.FEATURE_PANEL_OUTLINE
        );

        context.drawText(mc.textRenderer, section.title, panel.x() + PANEL_SIDE_PADDING, panel.y() + PANEL_TITLE_Y_OFFSET, 0xFFFFFFFF, true);
        RenderUtils.drawRect(
            context,
            panel.x() + PANEL_SIDE_PADDING,
            panel.y() + PANEL_LINE_Y_OFFSET,
            panel.width() - (PANEL_SIDE_PADDING * 2),
            1,
            NavigatorStyle.Colors.HAIRLINE
        );

        if (section.accentPreview) {
            drawColorPreviewCard(context, section.accentPreviewRect, "Main Color Preview", GuiSettings.get().accentPreviewColor());
        }
        if (section.backgroundPreview) {
            drawBackgroundPreviewCard(context, section.backgroundPreviewRect);
        }
        if (section.panelPreview) {
            drawColorPreviewCard(context, section.panelPreviewRect, "Panel Preview", GuiSettings.get().panelFillColor());
        }

        for (ValueComponent<?> component : section.components) {
            component.render(context, mouseX, mouseY, partialTicks);
        }

        if (section.controls) {
            drawKeybindControl(context, mouseX, mouseY, partialTicks, section.keybindRect);
            drawResetControl(context, mouseX, mouseY, partialTicks, section.resetRect);
        }
    }

    private void drawBackgroundPreviewCard(DrawContext context, UiRect rect) {
        RenderUtils.drawModernBox(
            context,
            rect.x(),
            rect.y(),
            rect.width(),
            rect.height(),
            NavigatorStyle.Colors.CONTROL_IDLE,
            NavigatorStyle.Colors.HAIRLINE
        );
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Background Preview",
            rect.x() + 8,
            rect.y() + 8,
            0xFFE6E6E6,
            false
        );

        GuiSettings settings = GuiSettings.get();
        int swatchX = rect.right() - PREVIEW_SWATCH_WIDTH - 4;
        int swatchY = rect.y() + 4;
        int swatchW = PREVIEW_SWATCH_WIDTH;
        int swatchH = rect.height() - 8;
        context.fillGradient(
            swatchX,
            swatchY,
            swatchX + swatchW,
            swatchY + swatchH,
            settings.backgroundTopColor(),
            settings.backgroundBottomColor()
        );
        RenderUtils.drawOutline(context, swatchX, swatchY, swatchW, swatchH, 0x77FFFFFF);
    }

    private void drawColorPreviewCard(DrawContext context, UiRect rect, String label, int swatchColor) {
        RenderUtils.drawModernBox(
            context,
            rect.x(),
            rect.y(),
            rect.width(),
            rect.height(),
            NavigatorStyle.Colors.CONTROL_IDLE,
            NavigatorStyle.Colors.HAIRLINE
        );
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            label,
            rect.x() + 8,
            rect.y() + 8,
            0xFFE6E6E6,
            false
        );

        int swatchX = rect.right() - PREVIEW_SWATCH_WIDTH - 4;
        int swatchY = rect.y() + 4;
        int swatchW = PREVIEW_SWATCH_WIDTH;
        int swatchH = rect.height() - 8;
        RenderUtils.drawModernBox(context, swatchX, swatchY, swatchW, swatchH, swatchColor, 0x77FFFFFF);
    }

    private void drawKeybindControl(DrawContext context, int mouseX, int mouseY, float partialTicks, UiRect rect) {
        boolean hovered = rect.contains(mouseX, mouseY);
        keybindHoverAnim.setTarget(hovered ? 1f : 0f);
        float hoverProgress = keybindHoverAnim.update(partialTicks);

        int bg = RenderUtils.interpolateColor(NavigatorStyle.Colors.CONTROL_IDLE, NavigatorStyle.Colors.CONTROL_HOVER, hoverProgress);
        RenderUtils.drawModernBox(context, rect.x(), rect.y(), rect.width(), rect.height(), bg, NavigatorStyle.Colors.HAIRLINE);

        String keyText = "Keybind: " + resolveGuiKeybindText();
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            keyText,
            rect.x() + 8,
            rect.y() + 6,
            0xFFEEEEEE,
            false
        );
    }

    private void drawResetControl(DrawContext context, int mouseX, int mouseY, float partialTicks, UiRect rect) {
        boolean hovered = rect.contains(mouseX, mouseY);
        resetHoverAnim.setTarget(hovered ? 1f : 0f);
        float hoverProgress = resetHoverAnim.update(partialTicks);

        int bg = RenderUtils.interpolateColor(0x44000000, 0x66000000, hoverProgress);
        RenderUtils.drawModernBox(context, rect.x(), rect.y(), rect.width(), rect.height(), bg, NavigatorStyle.Colors.HAIRLINE);
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Reset Visual Defaults",
            rect.x() + 8,
            rect.y() + 5,
            0xFFD8D8D8,
            false
        );
    }

    private void refreshLayoutMetrics() {
        int contentHeight = computeLayout(0, false);
        maxScroll = computeMaxScroll(contentHeight, contentViewport.height());
        clampScrollAnimation();
    }

    private void markLayoutDirty() {
        layoutDirty = true;
        lastAppliedScroll = Integer.MIN_VALUE;
    }

    private void ensureLayoutMetrics() {
        if (!layoutDirty) {
            return;
        }
        refreshLayoutMetrics();
    }

    private void applyLayoutForScroll(int scrollOffset) {
        if (!layoutDirty && lastAppliedScroll == scrollOffset) {
            return;
        }
        computeLayout(scrollOffset, true);
        layoutDirty = false;
        lastAppliedScroll = scrollOffset;
    }

    private void ensureInteractiveLayout() {
        ensureLayoutMetrics();
        applyLayoutForScroll((int) scrollAnim.getValue());
    }

    private int computeLayout(int scrollOffset, boolean applyBounds) {
        if (sections.isEmpty()) {
            return 0;
        }

        int columns = contentViewport.width() >= MIN_TWO_COLUMN_WIDTH ? 2 : 1;
        int panelWidth = columns == 1
            ? contentViewport.width()
            : (contentViewport.width() - PANEL_GAP) / 2;
        int[] columnY = new int[columns];
        Arrays.fill(columnY, contentViewport.y() + scrollOffset);

        for (Section section : sections) {
            if (!sectionMatchesSearch(section)) {
                continue;
            }
            int column = columns == 1 ? 0 : (columnY[0] <= columnY[1] ? 0 : 1);
            int panelX = contentViewport.x() + (column == 0 ? 0 : panelWidth + PANEL_GAP);
            int panelY = columnY[column];
            int panelHeight = sectionHeight(section);

            if (applyBounds) {
                section.panelRect = new UiRect(panelX, panelY, panelWidth, panelHeight);
                layoutSectionContent(section);
            }

            columnY[column] += panelHeight + PANEL_ROW_GAP;
        }

        int maxY = columnY[0];
        for (int y : columnY) {
            maxY = Math.max(maxY, y);
        }
        return Math.max(0, maxY - (contentViewport.y() + scrollOffset) - PANEL_ROW_GAP);
    }

    private void layoutSectionContent(Section section) {
        int x = section.panelRect.x() + PANEL_SIDE_PADDING;
        int width = section.panelRect.width() - (PANEL_SIDE_PADDING * 2);
        int y = section.panelRect.y() + CONTENT_START_Y_OFFSET;

        section.accentPreviewRect = emptyRect();
        section.backgroundPreviewRect = emptyRect();
        section.panelPreviewRect = emptyRect();
        section.keybindRect = emptyRect();
        section.resetRect = emptyRect();

        if (section.accentPreview) {
            section.accentPreviewRect = new UiRect(x, y, width, PREVIEW_HEIGHT);
            y += PREVIEW_HEIGHT + PREVIEW_GAP;
        }
        if (section.backgroundPreview) {
            section.backgroundPreviewRect = new UiRect(x, y, width, PREVIEW_HEIGHT);
            y += PREVIEW_HEIGHT + PREVIEW_GAP;
        }
        if (section.panelPreview) {
            section.panelPreviewRect = new UiRect(x, y, width, PREVIEW_HEIGHT);
            y += PREVIEW_HEIGHT + PREVIEW_GAP;
        }

        List<ValueComponent<?>> visible = new ArrayList<>();
        for (ValueComponent<?> component : section.components) {
            if (component.isVisible()) {
                visible.add(component);
            }
        }

        for (int i = 0; i < visible.size(); i++) {
            ValueComponent<?> component = visible.get(i);
            component.setBounds(x, y, width);
            y += component.height;
            if (i < visible.size() - 1 || section.controls) {
                y += COMPONENT_GAP;
            }
        }

        if (section.controls) {
            section.keybindRect = new UiRect(x, y, width, KEYBIND_HEIGHT);
            y += KEYBIND_HEIGHT + STACK_GAP;
            section.resetRect = new UiRect(x, y, width, RESET_HEIGHT);
        }
    }

    private int sectionHeight(Section section) {
        int height = CONTENT_START_Y_OFFSET + CONTENT_BOTTOM_PADDING;
        int previews = 0;
        if (section.accentPreview) {
            previews++;
        }
        if (section.backgroundPreview) {
            previews++;
        }
        if (section.panelPreview) {
            previews++;
        }
        if (previews > 0) {
            height += (previews * PREVIEW_HEIGHT) + ((previews - 1) * PREVIEW_GAP);
            if (!section.components.isEmpty() || section.controls) {
                height += PREVIEW_GAP;
            }
        }

        int visibleCount = 0;
        for (ValueComponent<?> component : section.components) {
            if (!component.isVisible()) {
                continue;
            }
            height += component.height;
            visibleCount++;
        }
        if (visibleCount > 1) {
            height += (visibleCount - 1) * COMPONENT_GAP;
        }

        if (section.controls) {
            if (visibleCount > 0 || previews > 0) {
                height += COMPONENT_GAP;
            }
            height += KEYBIND_HEIGHT + STACK_GAP + RESET_HEIGHT;
        }

        return Math.max(MIN_SECTION_HEIGHT, height);
    }

    private int computeMaxScroll(int contentHeight, int viewportHeight) {
        int overflow = contentHeight - viewportHeight;
        if (overflow <= 0) {
            return 0;
        }
        return -overflow;
    }

    private void clampScrollAnimation() {
        if (scrollAnim.getTarget() < maxScroll) {
            scrollAnim.setTarget(maxScroll);
        } else if (scrollAnim.getTarget() > 0f) {
            scrollAnim.setTarget(0f);
        }
        if (scrollAnim.getValue() < maxScroll) {
            scrollAnim.setValue(maxScroll);
        } else if (scrollAnim.getValue() > 0f) {
            scrollAnim.setValue(0f);
        }
    }

    private String resolveGuiKeybindText() {
        if (listeningForGuiKeybind) {
            return "Listening...";
        }
        Module clickGui = ModuleManager.getInstance().getModule(ClickGUI.class);
        if (clickGui == null || clickGui.getKey() <= 0) {
            return "[NONE]";
        }
        return InputUtil.fromKeyCode(clickGui.getKey(), -1).getLocalizedText().getString();
    }

    private void ensureSearchBar(int x, int y, int width, int height) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) {
            return;
        }
        if (searchBar == null) {
            searchBar = new NavigatorSearchBar(mc.textRenderer, x, y, width, height);
            return;
        }
        searchBar.setBounds(x, y, width, height);
    }

    private boolean sectionMatchesSearch(Section section) {
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }

        if (section.title.toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        if ("controls".equals(section.key)
            && ("keybind".contains(query) || "controls".contains(query) || "reset".contains(query))) {
            return true;
        }

        for (ValueComponent<?> component : section.components) {
            String name = component.getValueName();
            if (name != null && name.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private UiRect emptyRect() {
        return new UiRect(0, 0, 0, 0);
    }

    private static final class Section {
        private final String key;
        private final String title;
        private final List<ValueComponent<?>> components = new ArrayList<>();
        private boolean accentPreview;
        private boolean backgroundPreview;
        private boolean panelPreview;
        private boolean controls;

        private UiRect panelRect = new UiRect(0, 0, 0, 0);
        private UiRect accentPreviewRect = new UiRect(0, 0, 0, 0);
        private UiRect backgroundPreviewRect = new UiRect(0, 0, 0, 0);
        private UiRect panelPreviewRect = new UiRect(0, 0, 0, 0);
        private UiRect keybindRect = new UiRect(0, 0, 0, 0);
        private UiRect resetRect = new UiRect(0, 0, 0, 0);

        private Section(String key, String title) {
            this.key = key;
            this.title = title;
        }

        private boolean hasPreview() {
            return accentPreview || backgroundPreview || panelPreview;
        }
    }
}

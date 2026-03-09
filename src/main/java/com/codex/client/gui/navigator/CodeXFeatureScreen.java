package com.codex.client.gui.navigator;

import com.codex.api.module.Module;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.api.value.Value;
import com.codex.client.CodeXClient;
import com.codex.client.gui.navigator.components.ValueComponent;
import com.codex.client.gui.navigator.components.ValueComponentFactory;
import com.codex.client.gui.navigator.layout.FeaturePanelLayout;
import com.codex.client.gui.navigator.layout.NavigatorLayoutTokens;
import com.codex.client.gui.navigator.layout.UiRect;
import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import com.codex.client.gui.navigator.settings.GuiSettings;
import com.codex.impl.module.render.hud.HudElementModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class CodeXFeatureScreen extends CodeXNavigatorScreen {
    private static final int SETTINGS_SECTION_GAP = NavigatorLayoutTokens.FeatureSettings.SECTION_GAP;
    private static final int SETTINGS_SECTION_SIDE_PADDING = NavigatorLayoutTokens.FeatureSettings.SECTION_SIDE_PADDING;
    private static final int SETTINGS_SECTION_HEADER_Y_OFFSET = NavigatorLayoutTokens.FeatureSettings.SECTION_HEADER_Y_OFFSET;
    private static final int SETTINGS_SECTION_HEADER_LINE_Y_OFFSET = NavigatorLayoutTokens.FeatureSettings.SECTION_HEADER_LINE_Y_OFFSET;
    private static final int SETTINGS_SECTION_CONTENT_TOP = NavigatorLayoutTokens.FeatureSettings.SECTION_CONTENT_TOP;
    private static final int SETTINGS_SECTION_BOTTOM_PADDING = NavigatorLayoutTokens.FeatureSettings.SECTION_BOTTOM_PADDING;
    private static final int SETTINGS_SECTION_MIN_HEIGHT = NavigatorLayoutTokens.FeatureSettings.SECTION_MIN_HEIGHT;

    private final Module module;
    private final CodeXNavigatorMainScreen parent;
    private final Animation toggleAnim;
    private final Animation hudEditorHoverAnim = new Animation(0f, 0.2f);
    private final Animation keybindHoverAnim = new Animation(0f, 0.2f);
    private final Animation toggleHoverAnim = new Animation(0f, 0.2f);
    private final List<SettingSection> settingSections = new ArrayList<>();
    private final List<ValueComponent<?>> settingComponents = new ArrayList<>();
    private final FeaturePanelLayout panelLayout = new FeaturePanelLayout();
    private List<OrderedText> descriptionLines = Collections.emptyList();

    private boolean listeningForKey;
    
    private double previewZoom = 1.0;
    private double previewOffsetX = 0.0;
    private double previewOffsetY = 0.0;
    private boolean isDraggingPreview = false;

    private boolean hasPreview() {
        return module instanceof com.codex.client.gui.navigator.IPreviewable || module instanceof HudElementModule;
    }

    public CodeXFeatureScreen(Module module, CodeXNavigatorMainScreen parent) {
        super();
        this.module = module;
        this.parent = parent;
        hasBackground = false;
        toggleAnim = new Animation(module.isEnabled() ? 1f : 0f, NavigatorStyle.Animation.SMOOTH);
        buildComponents();
    }

    @Override
    protected void onResize() {
        panelLayout.resize(this.width, this.height);
        descriptionLines = this.client.textRenderer.wrapLines(
            Text.literal(resolveDescriptionText()),
            panelLayout.leftDescriptionWidth()
        );
        refreshScrollableContentHeight();
    }

    @Override
    protected void onMouseClick(double mouseX, double mouseY, int button) {
        if (button == 4) {
            UiInteractionFeedback.click();
            goBack();
            return;
        }

        UiRect panelRect = panelLayout.panelRect();
        UiRect toggleRect = hasPreview() ? panelLayout.hudModuleToggleRect() : panelLayout.toggleRect();
        
        // Only consider keybind rect if the module allows binding
        UiRect keybindRect = null;
        if (module.canBind()) {
            keybindRect = hasPreview() ? panelLayout.hudModuleKeybindRect() : panelLayout.keybindRect();
        }
        
        UiRect hudEditorRect = isHudModule() ? panelLayout.hudModuleHudEditorRect() : null;

        if (hasPreview()) {
            UiRect previewRect = panelLayout.previewRect();
            if (previewRect.contains(mouseX, mouseY)) {
                if (button == 0) {
                    isDraggingPreview = true;
                    return;
                }
            }
        }

        if (isHudModule() && hudEditorRect != null && hudEditorRect.contains(mouseX, mouseY)) {
            UiInteractionFeedback.click();
            openHudEditor();
            return;
        }

        if (toggleRect.contains(mouseX, mouseY)) {
            UiInteractionFeedback.click();
            module.toggle();
            CodeXClient.queueConfigSave();
            return;
        }

        if (module.canBind() && keybindRect != null && keybindRect.contains(mouseX, mouseY)) {
            UiInteractionFeedback.click();
            listeningForKey = !listeningForKey;
            return;
        }

        if (listeningForKey) {
            listeningForKey = false;
        }

        if (!panelRect.contains(mouseX, mouseY)) {
            UiInteractionFeedback.click();
            goBack();
            return;
        }

        if (!panelLayout.rightScissorRect().contains(mouseX, mouseY)) {
            return;
        }

        if (module instanceof ICustomSettingsRenderer custom) {
            if (custom.mouseClicked(mouseX, mouseY, button)) {
                return;
            }
        }

        int customHeight = (module instanceof ICustomSettingsRenderer c) ? c.getCustomSettingsHeight(panelLayout.rightContentWidth()) : 0;
        layoutSettingSections(panelLayout.rightContentStartY() + (int) scroll + customHeight);
        for (ValueComponent<?> component : settingComponents) {
            if (component.isVisible()) {
                component.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (module instanceof ICustomSettingsRenderer custom) {
            if (custom.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        for (ValueComponent<?> component : settingComponents) {
            if (component.isVisible()) {
                component.keyPressed(keyCode, scanCode, modifiers);
            }
        }

        if (listeningForKey) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                module.setKey(0);
            } else {
                module.setKey(keyCode);
            }
            listeningForKey = false;
            CodeXClient.queueConfigSave();
            return true;
        }

        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            UiInteractionFeedback.click();
            goBack();
            return true;
        }

        // Only go back on BACKSPACE if no text component is actively focused
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            boolean isTextFocused = false;
            for (ValueComponent<?> component : settingComponents) {
                if (component instanceof com.codex.client.gui.navigator.components.StringComponent strComp) {
                    if (strComp.isFocused()) {
                        isTextFocused = true;
                        break;
                    }
                }
            }
            if (!isTextFocused) {
                UiInteractionFeedback.click();
                goBack();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (module instanceof ICustomSettingsRenderer custom) {
            if (custom.charTyped(chr, modifiers)) {
                return true;
            }
        }

        for (ValueComponent<?> component : settingComponents) {
            if (component.isVisible()) {
                component.charTyped(chr, modifiers);
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    protected void onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingPreview && button == 0) {
            previewOffsetX += deltaX;
            previewOffsetY += deltaY;
            return;
        }
        
        if (module instanceof ICustomSettingsRenderer custom) {
            if (custom.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return;
            }
        }

        int customHeight = (module instanceof ICustomSettingsRenderer c) ? c.getCustomSettingsHeight(panelLayout.rightContentWidth()) : 0;
        layoutSettingSections(panelLayout.rightContentStartY() + (int) scroll + customHeight);
        for (ValueComponent<?> component : settingComponents) {
            if (component.isVisible()) {
                component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }
        }
    }

    @Override
    protected void onMouseRelease(double x, double y, int button) {
        if (isDraggingPreview && button == 0) {
            isDraggingPreview = false;
        }
        
        if (module instanceof ICustomSettingsRenderer custom) {
            if (custom.mouseReleased(x, y, button)) {
                return;
            }
        }

        int customHeight = (module instanceof ICustomSettingsRenderer c) ? c.getCustomSettingsHeight(panelLayout.rightContentWidth()) : 0;
        layoutSettingSections(panelLayout.rightContentStartY() + (int) scroll + customHeight);
        for (ValueComponent<?> component : settingComponents) {
            if (component.isVisible()) {
                component.mouseReleased(x, y, button);
            }
        }
    }

    @Override
    protected void onUpdate() {
        if (module instanceof ICustomSettingsRenderer custom) {
            custom.tick();
        }
        refreshScrollableContentHeight();
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        UiRect panelRect = panelLayout.panelRect();
        UiRect dividerRect = panelLayout.dividerRect();
        
        UiRect toggleRect;
        UiRect keybindRect = null;
        
        // Determine layout based on preview presence and bindability
        if (hasPreview()) {
            if (isHudModule()) {
                // HUD Module: Toggle (Left Half), Editor (Right Half), Keybind (Top Right)
                toggleRect = panelLayout.hudModuleToggleRect();
                if (module.canBind()) {
                    keybindRect = panelLayout.hudModuleKeybindRect();
                }
            } else {
                // Module with Preview but no HUD Editor
                if (module.canBind()) {
                    toggleRect = panelLayout.hudModuleToggleRect();
                    keybindRect = panelLayout.hudModuleKeybindRect();
                } else {
                    // Full width toggle at the bottom
                    toggleRect = panelLayout.toggleRect(); 
                }
            }
        } else {
            // Standard Module
            toggleRect = panelLayout.toggleRect();
            if (module.canBind()) {
                keybindRect = panelLayout.keybindRect();
            } else {
                // Shift toggle down to take up the space where keybind usually goes, for a cleaner look
                toggleRect = panelLayout.keybindRect();
            }
        }

        RenderUtils.drawModernBox(
            context,
            panelRect.x(),
            panelRect.y(),
            panelRect.width(),
            panelRect.height(),
            GuiSettings.get().panelFillColor(),
            NavigatorStyle.Colors.FEATURE_PANEL_OUTLINE
        );
        RenderUtils.drawRect(context, dividerRect.x(), dividerRect.y(), dividerRect.width(), dividerRect.height(), NavigatorStyle.Colors.HAIRLINE);

        int leftX = panelLayout.leftColumnX();
        context.drawText(this.client.textRenderer, module.getName(), leftX, panelLayout.leftTitleY(), NavigatorStyle.Feature.TEXT_PRIMARY, true);
        context.drawText(
            this.client.textRenderer,
            "Category: " + module.getCategory().getName(),
            leftX,
            panelLayout.leftCategoryY(),
            NavigatorStyle.Feature.TEXT_SECONDARY,
            false
        );

        int descriptionY = panelLayout.leftDescriptionY();
        for (OrderedText line : descriptionLines) {
            context.drawText(this.client.textRenderer, line, leftX, descriptionY, NavigatorStyle.Feature.TEXT_TERTIARY, false);
            descriptionY += NavigatorStyle.Feature.DESCRIPTION_LINE_HEIGHT;
        }

        if (hasPreview()) {
            UiRect previewRect = panelLayout.previewRect();
            context.drawText(this.client.textRenderer, "Live Preview", previewRect.x(), previewRect.y() - 12, NavigatorStyle.Feature.TEXT_SECONDARY, false);
            RenderUtils.drawModernBox(context, previewRect.x(), previewRect.y(), previewRect.width(), previewRect.height(), NavigatorStyle.Colors.CONTROL_IDLE, NavigatorStyle.Colors.HAIRLINE);
            
            if (isHudModule()) {
                HudElementModule hudModule = (HudElementModule) module;
                int prevX = hudModule.getEditorX();
                int prevY = hudModule.getEditorY();
                
                int cx = (int) (previewRect.x() + (previewRect.width() / 2) - ((hudModule.getElementWidth() * previewZoom) / 2) + previewOffsetX);
                int cy = (int) (previewRect.y() + (previewRect.height() / 2) - ((hudModule.getElementHeight() * previewZoom) / 2) + previewOffsetY);
                
                hudModule.setEditorPosition(0, 0);
                context.enableScissor(previewRect.x() + 1, previewRect.y() + 1, previewRect.right() - 1, previewRect.bottom() - 1);
                
                context.getMatrices().push();
                context.getMatrices().translate(cx, cy, 0);
                context.getMatrices().scale((float) previewZoom, (float) previewZoom, 1f);
                
                hudModule.render(context, partialTicks);
                
                context.getMatrices().pop();
                context.disableScissor();
                
                hudModule.setEditorPosition(prevX, prevY);
            } else if (module instanceof com.codex.client.gui.navigator.IPreviewable previewable) {
                context.enableScissor(previewRect.x() + 1, previewRect.y() + 1, previewRect.right() - 1, previewRect.bottom() - 1);
                previewable.renderPreview(context, partialTicks, previewRect.x() + 1, previewRect.y() + 1, previewRect.width() - 2, previewRect.height() - 2);
                context.disableScissor();
            }
        }

        if (!hasPreview() && module.canBind()) {
            context.drawText(this.client.textRenderer, "Keybind", leftX, panelLayout.keybindLabelY(), NavigatorStyle.Feature.TEXT_PRIMARY, false);
        }
        
        if (module.canBind() && keybindRect != null) {
            String keyText = listeningForKey
                ? "..."
                : (module.getKey() == 0
                ? "NONE"
                : InputUtil.fromKeyCode(module.getKey(), -1).getLocalizedText().getString());
                
            String displayStr = hasPreview() ? "[" + keyText + "]" : keyText;

            boolean keybindHovered = keybindRect.contains(mouseX, mouseY);
            keybindHoverAnim.setTarget(keybindHovered ? 1f : 0f);
            float keybindHover = keybindHoverAnim.update(partialTicks);
            int keybindBg = RenderUtils.interpolateColor(
                NavigatorStyle.Colors.CONTROL_IDLE,
                NavigatorStyle.Colors.CONTROL_HOVER,
                keybindHover
            );
            RenderUtils.drawModernBox(
                context,
                keybindRect.x(),
                keybindRect.y(),
                keybindRect.width(),
                keybindRect.height(),
                keybindBg,
                NavigatorStyle.Colors.HAIRLINE
            );
            
            if (hasPreview()) {
                int textW = this.client.textRenderer.getWidth(displayStr);
                int textX = keybindRect.x() + (keybindRect.width() / 2) - (textW / 2);
                int textY = keybindRect.y() + 2;
                context.drawText(this.client.textRenderer, displayStr, textX, textY, NavigatorStyle.Feature.TEXT_TERTIARY, false);
            } else {
                context.drawText(
                    this.client.textRenderer,
                    displayStr,
                    keybindRect.x() + (keybindRect.width() / 2) - (this.client.textRenderer.getWidth(displayStr) / 2),
                    keybindRect.y() + 6,
                    NavigatorStyle.Feature.TEXT_TERTIARY,
                    false
                );
            }
        }

        toggleAnim.setTarget(module.isEnabled() ? 1f : 0f);
        float toggleProgress = toggleAnim.update(partialTicks);
        boolean toggleHovered = toggleRect.contains(mouseX, mouseY);
        toggleHoverAnim.setTarget(toggleHovered ? 1f : 0f);
        float toggleHover = toggleHoverAnim.update(partialTicks);
        int buttonColor = RenderUtils.interpolateColor(0x60000000, GuiSettings.get().accentColor(0x80), toggleProgress);
        buttonColor = RenderUtils.interpolateColor(buttonColor, 0x90FFFFFF, 0.2f * toggleHover);
        RenderUtils.drawModernBox(
            context,
            toggleRect.x(),
            toggleRect.y(),
            toggleRect.width(),
            toggleRect.height(),
            buttonColor,
            0x44FFFFFF
        );
        String buttonText = module.isEnabled() ? "Disable" : "Enable";
        context.drawText(
            this.client.textRenderer,
            buttonText,
            toggleRect.x() + (toggleRect.width() / 2) - (this.client.textRenderer.getWidth(buttonText) / 2),
            toggleRect.y() + 6,
            NavigatorStyle.Feature.TEXT_PRIMARY,
            false
        );

        if (isHudModule()) {
            UiRect hudRect = panelLayout.hudModuleHudEditorRect();
            boolean hudHovered = hudRect.contains(mouseX, mouseY);
            hudEditorHoverAnim.setTarget(hudHovered ? 1f : 0f);
            float hudHover = hudEditorHoverAnim.update(partialTicks);
            int hudBg = RenderUtils.interpolateColor(0x56000000, GuiSettings.get().accentColor(0x66), hudHover);
            RenderUtils.drawModernBox(
                context,
                hudRect.x(),
                hudRect.y(),
                hudRect.width(),
                hudRect.height(),
                hudBg,
                0x44FFFFFF
            );
            String hudText = "HUD Editor";
            context.drawText(
                this.client.textRenderer,
                hudText,
                hudRect.x() + (hudRect.width() / 2) - (this.client.textRenderer.getWidth(hudText) / 2),
                hudRect.y() + 6,
                NavigatorStyle.Feature.TEXT_PRIMARY,
                false
            );
        }

        int rightX = panelLayout.rightColumnX();
        int rightWidth = panelLayout.rightContentWidth();
        context.drawText(this.client.textRenderer, "Module Settings", rightX, panelLayout.rightTitleY(), NavigatorStyle.Feature.TEXT_PRIMARY, true);
        RenderUtils.drawRect(context, rightX, panelLayout.rightHeaderLineY(), rightWidth, 1, NavigatorStyle.Colors.HAIRLINE);

        UiRect rightScissor = panelLayout.rightScissorRect();
        context.enableScissor(rightScissor.x(), rightScissor.y(), rightScissor.right(), rightScissor.bottom());

        int visibleSectionCount = 0;
        int contentY = panelLayout.rightContentStartY() + (int) scroll;
        
        if (module instanceof ICustomSettingsRenderer custom) {
            contentY = custom.renderCustomSettings(context, rightX, contentY, rightWidth, mouseX, mouseY, partialTicks);
            visibleSectionCount++;
        }

        layoutSettingSections(contentY);
        for (SettingSection section : settingSections) {
            if (visibleComponentCount(section) == 0) {
                continue;
            }
            visibleSectionCount++;
            renderSettingSection(context, mouseX, mouseY, partialTicks, section);
        }

        if (visibleSectionCount == 0) {
            context.drawText(
                this.client.textRenderer,
                "No settings available.",
                rightX,
                panelLayout.rightContentStartY() + (int) scroll,
                NavigatorStyle.Feature.TEXT_MUTED,
                false
            );
        }

        context.disableScissor();
    }

    @Override
    public void close() {
        parent.close();
        this.client.setScreen(parent);
    }

    private void buildComponents() {
        settingSections.clear();
        settingComponents.clear();
        LinkedHashMap<String, SettingSection> sectionsByTitle = new LinkedHashMap<>();
        for (Value<?> value : module.getValues()) {
            ValueComponent<?> component = ValueComponentFactory.create(value);
            if (component != null) {
                if (isHudModule() && isHudPositionValue(component.getValueName())) {
                    continue;
                }
                component.setChangeListener(CodeXClient::queueConfigSave);
                settingComponents.add(component);
                String title = resolveSectionTitle(value);
                SettingSection section = sectionsByTitle.computeIfAbsent(title, SettingSection::new);
                section.components.add(component);
            }
        }
        settingSections.addAll(sectionsByTitle.values());
    }

    private void refreshScrollableContentHeight() {
        int startY = panelLayout.rightContentStartY();
        int customHeight = 0;
        if (module instanceof ICustomSettingsRenderer custom) {
            customHeight = custom.getCustomSettingsHeight(panelLayout.rightContentWidth());
        }
        int endY = layoutSettingSections(startY + customHeight);
        int contentHeight = Math.max(
            0,
            (endY - startY) + NavigatorStyle.Feature.SETTINGS_HEIGHT_BASE
        );
        setContentHeight(contentHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hasPreview() && panelLayout.previewRect().contains(mouseX, mouseY)) {
            double zoomFactor = 1.1;
            if (verticalAmount > 0) {
                previewZoom *= zoomFactor;
            } else if (verticalAmount < 0) {
                previewZoom /= zoomFactor;
            }
            previewZoom = Math.max(0.2, Math.min(5.0, previewZoom)); // Clamp zoom between 0.2x and 5x
            return true;
        }
        
        if (!panelLayout.rightScissorRect().contains(mouseX, mouseY)) {
            return false;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void goBack() {
        parent.setExpanding(false);
        parent.slideAnim.setValue(1f);
        parent.slideAnim.setTarget(1f);
        this.client.setScreen(parent);
    }

    private String resolveDescriptionText() {
        String description = module.getDescription();
        return description != null ? description : "No description provided.";
    }

    private UiRect hudEditorRect(UiRect toggleRect) {
        return new UiRect(toggleRect.x(), toggleRect.y() - (NavigatorStyle.Feature.TOGGLE_HEIGHT + 8), toggleRect.width(), NavigatorStyle.Feature.TOGGLE_HEIGHT);
    }

    private boolean isHudModule() {
        return module instanceof HudElementModule;
    }

    private boolean isHudPositionValue(String name) {
        return "Position X".equalsIgnoreCase(name) || "Position Y".equalsIgnoreCase(name);
    }

    private void openHudEditor() {
        parent.setActiveSection(NavigatorSection.HUD_EDITOR);
        parent.setExpanding(false);
        parent.slideAnim.setValue(1f);
        parent.slideAnim.setTarget(1f);
        this.client.setScreen(parent);
    }

    private int layoutSettingSections(int startY) {
        int rightX = panelLayout.rightColumnX();
        int rightWidth = panelLayout.rightContentWidth();
        int y = startY;
        boolean wroteSection = false;
        for (SettingSection section : settingSections) {
            if (visibleComponentCount(section) == 0) {
                continue;
            }
            if (wroteSection) {
                y += SETTINGS_SECTION_GAP;
            }

            int sectionHeight = sectionHeight(section);
            section.bounds = new UiRect(rightX, y, rightWidth, sectionHeight);

            int componentX = rightX + SETTINGS_SECTION_SIDE_PADDING;
            int componentWidth = Math.max(10, rightWidth - (SETTINGS_SECTION_SIDE_PADDING * 2));
            int componentY = y + SETTINGS_SECTION_CONTENT_TOP;
            for (ValueComponent<?> component : section.components) {
                if (!component.isVisible()) {
                    continue;
                }
                component.setBounds(componentX, componentY, componentWidth);
                componentY += component.height + NavigatorStyle.Feature.COMPONENT_GAP;
            }

            y += sectionHeight;
            wroteSection = true;
        }
        return y;
    }

    private void renderSettingSection(DrawContext context, int mouseX, int mouseY, float partialTicks, SettingSection section) {
        UiRect rect = section.bounds;
        int fill = RenderUtils.interpolateColor(GuiSettings.get().panelFillColor(), NavigatorStyle.Colors.CONTROL_IDLE, 0.30f);
        RenderUtils.drawModernBox(context, rect.x(), rect.y(), rect.width(), rect.height(), fill, NavigatorStyle.Colors.HAIRLINE);
        context.drawText(
            this.client.textRenderer,
            section.title,
            rect.x() + SETTINGS_SECTION_SIDE_PADDING,
            rect.y() + SETTINGS_SECTION_HEADER_Y_OFFSET,
            NavigatorStyle.Feature.TEXT_PRIMARY,
            false
        );
        RenderUtils.drawRect(
            context,
            rect.x() + SETTINGS_SECTION_SIDE_PADDING,
            rect.y() + SETTINGS_SECTION_HEADER_LINE_Y_OFFSET,
            rect.width() - (SETTINGS_SECTION_SIDE_PADDING * 2),
            1,
            NavigatorStyle.Colors.HAIRLINE
        );

        for (ValueComponent<?> component : section.components) {
            if (!component.isVisible()) {
                continue;
            }
            component.render(context, mouseX, mouseY, partialTicks);
        }
    }

    private int sectionHeight(SettingSection section) {
        int visible = visibleComponentCount(section);
        if (visible == 0) {
            return 0;
        }

        int height = SETTINGS_SECTION_CONTENT_TOP + SETTINGS_SECTION_BOTTOM_PADDING;
        for (ValueComponent<?> component : section.components) {
            if (!component.isVisible()) {
                continue;
            }
            height += component.height;
        }
        if (visible > 1) {
            height += (visible - 1) * NavigatorStyle.Feature.COMPONENT_GAP;
        }
        return Math.max(SETTINGS_SECTION_MIN_HEIGHT, height);
    }

    private int visibleComponentCount(SettingSection section) {
        int count = 0;
        for (ValueComponent<?> component : section.components) {
            if (component.isVisible()) {
                count++;
            }
        }
        return count;
    }

    private String resolveSectionTitle(Value<?> value) {
        if (value.getGroup() != null) {
            return value.getGroup();
        }
        String name = value.getName();
        if (name == null) {
            return "General";
        }
        String normalized = name.toLowerCase();
        if (normalized.contains("color")
            || normalized.contains("hue")
            || normalized.contains("saturation")
            || normalized.contains("brightness")
            || normalized.contains("red")
            || normalized.contains("green")
            || normalized.contains("blue")
            || normalized.contains("alpha")
            || normalized.contains("opacity")) {
            return "Color";
        }
        if (normalized.contains("key") || normalized.contains("bind")) {
            return "Controls";
        }
        if (value instanceof ModeValue) {
            return "Modes";
        }
        if (value instanceof BoolValue) {
            return "Toggles";
        }
        if (value instanceof NumberValue) {
            return "Values";
        }
        return "General";
    }

    private static final class SettingSection {
        private final String title;
        private final List<ValueComponent<?>> components = new ArrayList<>();
        private UiRect bounds = new UiRect(0, 0, 0, 0);

        private SettingSection(String title) {
            this.title = title;
        }
    }

    @Override
    protected NavigatorSection getActiveSection() {
        return NavigatorViewState.getActiveSection();
    }

    @Override
    protected void onSectionSelected(NavigatorSection section) {
        NavigatorViewState.setActiveSection(section);
        parent.setActiveSection(section);
        parent.setExpanding(false);
        parent.slideAnim.setValue(1f);
        parent.slideAnim.setTarget(1f);
        this.client.setScreen(parent);
    }
}

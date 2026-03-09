package com.codex.client.gui.navigator.hud;

import com.codex.api.module.ModuleManager;
import com.codex.client.CodeXClient;
import com.codex.client.gui.navigator.NavigatorStyle;
import com.codex.client.gui.navigator.UiInteractionFeedback;
import com.codex.client.gui.navigator.layout.UiRect;
import com.codex.client.gui.navigator.settings.GuiSettings;
import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import com.codex.impl.module.render.hud.HudElementModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.function.Consumer;

public final class HudEditorPanel {
    private static final int CONTROL_BUTTON_GAP = 2;
    private static final int LABEL_SIDE_PADDING = 6;
    private static final int CONTROL_RAIL_SPACING = 4;

    private final Runnable doneAction;
    private final Consumer<HudElementModule> openSettingsAction;
    private final Animation resetHoverAnim = new Animation(0f, 0.2f);
    private final Animation doneHoverAnim = new Animation(0f, 0.2f);
    private final Map<String, Animation> elementHoverAnims = new HashMap<>();
    private final Map<String, Animation> toggleHoverAnims = new HashMap<>();

    private int width;
    private int height;
    private UiRect resetRect = new UiRect(0, 0, 0, 0);
    private UiRect doneRect = new UiRect(0, 0, 0, 0);

    private HudElementModule draggingModule;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean draggedSinceMouseDown;
    private List<HudElementModule> cachedHudModules = Collections.emptyList();
    private int cachedHudModuleCount = -1;
    private boolean hudCacheInitialized;

    public HudEditorPanel(Runnable doneAction, Consumer<HudElementModule> openSettingsAction) {
        this.doneAction = doneAction;
        this.openSettingsAction = openSettingsAction;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        clampAllElementsToBounds();

        int totalWidth = (NavigatorStyle.HudEditor.CENTER_BUTTON_WIDTH * 2) + NavigatorStyle.HudEditor.CENTER_BUTTON_GAP;
        int startX = (width - totalWidth) / 2;
        int y = (height / 2) - (NavigatorStyle.HudEditor.CENTER_BUTTON_HEIGHT / 2);

        resetRect = new UiRect(startX, y, NavigatorStyle.HudEditor.CENTER_BUTTON_WIDTH, NavigatorStyle.HudEditor.CENTER_BUTTON_HEIGHT);
        doneRect = new UiRect(
            startX + NavigatorStyle.HudEditor.CENTER_BUTTON_WIDTH + NavigatorStyle.HudEditor.CENTER_BUTTON_GAP,
            y,
            NavigatorStyle.HudEditor.CENTER_BUTTON_WIDTH,
            NavigatorStyle.HudEditor.CENTER_BUTTON_HEIGHT
        );
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        List<HudElementModule> modules = hudModules();
        for (HudElementModule module : modules) {
            drawHudElement(context, mouseX, mouseY, partialTicks, module);
        }
        if (modules.isEmpty()) {
            context.drawText(
                MinecraftClient.getInstance().textRenderer,
                "No HUD modules available.",
                16,
                NavigatorStyle.HudEditor.CONTENT_TOP + 14,
                0xB0FFFFFF,
                false
            );
        }

        drawButton(context, mouseX, mouseY, partialTicks, resetRect, "RESET", resetHoverAnim);
        drawButton(context, mouseX, mouseY, partialTicks, doneRect, "DONE", doneHoverAnim);
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }

        if (resetRect.contains(mouseX, mouseY)) {
            UiInteractionFeedback.click();
            resetAllElements();
            CodeXClient.queueConfigSave();
            return;
        }
        if (doneRect.contains(mouseX, mouseY)) {
            UiInteractionFeedback.click();
            doneAction.run();
            return;
        }

        List<HudElementModule> modules = hudModules();
        for (int i = modules.size() - 1; i >= 0; i--) {
            HudElementModule module = modules.get(i);
            UiRect bounds = elementBounds(module);
            UiRect toggleRect = toggleBounds(bounds);
            UiRect settingsRect = settingsBounds(bounds);

            if (toggleRect.contains(mouseX, mouseY)) {
                UiInteractionFeedback.click();
                module.toggle();
                CodeXClient.queueConfigSave();
                return;
            }
            if (settingsRect.contains(mouseX, mouseY)) {
                UiInteractionFeedback.click();
                if (openSettingsAction != null) {
                    openSettingsAction.accept(module);
                }
                return;
            }

            if (bounds.contains(mouseX, mouseY)) {
                UiInteractionFeedback.click();
                draggingModule = module;
                dragOffsetX = mouseX - bounds.x();
                dragOffsetY = mouseY - bounds.y();
                draggedSinceMouseDown = false;
                return;
            }
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || draggingModule == null) {
            return;
        }

        List<HudElementModule> modules = hudModules();
        int currentX = draggingModule.getEditorX();
        int currentY = draggingModule.getEditorY();

        int targetX = (int) Math.round(mouseX - dragOffsetX);
        int targetY = (int) Math.round(mouseY - dragOffsetY);
        targetX = clamp(targetX, NavigatorStyle.HudEditor.ELEMENT_MIN_X, maxXFor(draggingModule));
        targetY = clamp(targetY, NavigatorStyle.HudEditor.ELEMENT_MIN_Y, maxYFor(draggingModule));

        int newX = resolveAxisMove(draggingModule, modules, currentX, currentY, targetX, true);
        int newY = resolveAxisMove(draggingModule, modules, newX, currentY, targetY, false);
        if (newX == currentX && newY == currentY) {
            return;
        }
        draggingModule.setEditorPosition(newX, newY);
        draggedSinceMouseDown = true;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (draggingModule != null && draggedSinceMouseDown) {
                CodeXClient.queueConfigSave();
            }
            draggingModule = null;
            draggedSinceMouseDown = false;
        }
    }

    private void drawHudElement(DrawContext context, int mouseX, int mouseY, float partialTicks, HudElementModule module) {
        UiRect bounds = elementBounds(module);
        boolean hovered = bounds.contains(mouseX, mouseY);
        Animation hoverAnim = elementHoverAnims.computeIfAbsent(module.getName(), key -> new Animation(0f, 0.2f));
        hoverAnim.setTarget(hovered ? 1f : 0f);
        float hover = hoverAnim.update(partialTicks);

        int fill = module.isEnabled() ? GuiSettings.get().accentColor(0x40) : 0x52000000;
        fill = RenderUtils.interpolateColor(fill, 0x80000000, hover * 0.35f);
        int outline = RenderUtils.interpolateColor(0x28FFFFFF, 0x64FFFFFF, hover);
        RenderUtils.drawModernBox(context, bounds.x(), bounds.y(), bounds.width(), bounds.height(), fill, outline);
        if (module.isEnabled()) {
            RenderUtils.drawRect(context, bounds.x() + 1, bounds.y() + 1, bounds.width() - 2, 1, GuiSettings.get().accentColor(0xAA));
        }

        UiRect toggleRect = toggleBounds(bounds);
        UiRect settingsRect = settingsBounds(bounds);
        boolean isSqueezed = isSqueezed(bounds);
        boolean hasControls = bounds.width() >= (isSqueezed ? 40 : 30) && bounds.height() >= (isSqueezed ? 12 : 30);

        if (hasControls) {
            boolean toggleHovered = toggleRect.contains(mouseX, mouseY);
            Animation toggleHoverAnim = toggleHoverAnims.computeIfAbsent(module.getName(), key -> new Animation(0f, 0.22f));
            toggleHoverAnim.setTarget(toggleHovered ? 1f : 0f);
            float toggleHover = toggleHoverAnim.update(partialTicks);
            int toggleFill = module.isEnabled() ? GuiSettings.get().accentColor(0xCC) : 0x7A282828;
            toggleFill = RenderUtils.interpolateColor(toggleFill, 0xB0FFFFFF, toggleHover * 0.20f);
            int toggleOutline = RenderUtils.interpolateColor(0x50FFFFFF, 0x92FFFFFF, toggleHover);
            RenderUtils.drawModernBox(context, toggleRect.x(), toggleRect.y(), toggleRect.width(), toggleRect.height(), toggleFill, toggleOutline);

            boolean settingsHovered = settingsRect.contains(mouseX, mouseY);
            int settingsFill = RenderUtils.interpolateColor(0x6E1E1E1E, 0x7E303030, settingsHovered ? 1f : 0f);
            int settingsOutline = RenderUtils.interpolateColor(0x50FFFFFF, 0x85FFFFFF, settingsHovered ? 1f : 0f);
            RenderUtils.drawModernBox(
                context,
                settingsRect.x(),
                settingsRect.y(),
                settingsRect.width(),
                settingsRect.height(),
                settingsFill,
                settingsOutline
            );
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        String label = elementLabel(module);
        
        int labelAreaX = bounds.x() + LABEL_SIDE_PADDING;
        int labelAreaRight = bounds.right() - LABEL_SIDE_PADDING;
        
        if (hasControls) {
            if (isSqueezed) {
                labelAreaX = toggleRect.right() + 2;
                labelAreaRight = settingsRect.x() - 2;
            } else {
                labelAreaRight = controlRailX(bounds) - CONTROL_RAIL_SPACING;
            }
        }
        
        if (labelAreaRight <= labelAreaX) {
            labelAreaRight = bounds.right() - LABEL_SIDE_PADDING;
        }
        int labelAreaWidth = Math.max(1, labelAreaRight - labelAreaX);
        
        if (bounds.width() >= mc.textRenderer.getWidth(label) + 4 && bounds.height() >= mc.textRenderer.fontHeight + 4) {
            int labelX = labelAreaX + Math.max(0, (labelAreaWidth - mc.textRenderer.getWidth(label)) / 2);
            int labelY = bounds.y() + (bounds.height() - mc.textRenderer.fontHeight) / 2;
            context.enableScissor(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1);
            context.drawText(mc.textRenderer, label, labelX, labelY, 0xFFFFFFFF, false);
            context.disableScissor();
        }

        if (hasControls) {
            String settingsText = "S";
            int sx = settingsRect.x() + (settingsRect.width() - mc.textRenderer.getWidth(settingsText)) / 2;
            int sy = settingsRect.y() + 2;
            context.drawText(mc.textRenderer, settingsText, sx, sy, 0xFFEDEDED, false);
        }
    }

    private void drawButton(DrawContext context, int mouseX, int mouseY, float partialTicks, UiRect rect, String label, Animation hoverAnim) {
        boolean hovered = rect.contains(mouseX, mouseY);
        hoverAnim.setTarget(hovered ? 1f : 0f);
        float hover = hoverAnim.update(partialTicks);

        int fill = RenderUtils.interpolateColor(0x66000000, 0x7E000000, hover);
        int outline = RenderUtils.interpolateColor(0x33FFFFFF, 0x66FFFFFF, hover);
        RenderUtils.drawModernBox(context, rect.x(), rect.y(), rect.width(), rect.height(), fill, outline);

        MinecraftClient mc = MinecraftClient.getInstance();
        int textX = rect.x() + (rect.width() - mc.textRenderer.getWidth(label)) / 2;
        int textY = rect.y() + 6;
        context.drawText(mc.textRenderer, label, textX, textY, 0xFFFFFFFF, false);
    }

    private UiRect elementBounds(HudElementModule module) {
        return boundsAt(module, module.getEditorX(), module.getEditorY());
    }

    private UiRect toggleBounds(UiRect elementBounds) {
        if (isSqueezed(elementBounds)) {
            return new UiRect(
                elementBounds.x() + NavigatorStyle.HudEditor.TOGGLE_MARGIN,
                elementBounds.y() + (elementBounds.height() - NavigatorStyle.HudEditor.TOGGLE_SIZE) / 2,
                NavigatorStyle.HudEditor.TOGGLE_SIZE,
                NavigatorStyle.HudEditor.TOGGLE_SIZE
            );
        }
        return new UiRect(
            controlRailX(elementBounds),
            elementBounds.y() + NavigatorStyle.HudEditor.TOGGLE_MARGIN,
            NavigatorStyle.HudEditor.TOGGLE_SIZE,
            NavigatorStyle.HudEditor.TOGGLE_SIZE
        );
    }

    private UiRect settingsBounds(UiRect elementBounds) {
        if (isSqueezed(elementBounds)) {
            return new UiRect(
                elementBounds.right() - NavigatorStyle.HudEditor.TOGGLE_SIZE - NavigatorStyle.HudEditor.TOGGLE_MARGIN,
                elementBounds.y() + (elementBounds.height() - NavigatorStyle.HudEditor.TOGGLE_SIZE) / 2,
                NavigatorStyle.HudEditor.TOGGLE_SIZE,
                NavigatorStyle.HudEditor.TOGGLE_SIZE
            );
        }
        return new UiRect(
            controlRailX(elementBounds),
            elementBounds.bottom() - NavigatorStyle.HudEditor.TOGGLE_MARGIN - NavigatorStyle.HudEditor.TOGGLE_SIZE,
            NavigatorStyle.HudEditor.TOGGLE_SIZE,
            NavigatorStyle.HudEditor.TOGGLE_SIZE
        );
    }

    private boolean isSqueezed(UiRect elementBounds) {
        // If height is less than 30, we can't fit two buttons stacked vertically in the right rail
        return elementBounds.height() < 30;
    }

    private List<HudElementModule> hudModules() {
        ModuleManager moduleManager = ModuleManager.getInstance();
        int moduleCount = moduleManager.getModules().size();
        if (hudCacheInitialized && moduleCount == cachedHudModuleCount) {
            return cachedHudModules;
        }

        List<HudElementModule> list = new ArrayList<>();
        moduleManager.getModules().forEach(module -> {
            if (module instanceof HudElementModule hud) {
                list.add(hud);
            }
        });
        list.sort(Comparator.comparing(HudElementModule::getName));
        cachedHudModules = Collections.unmodifiableList(list);
        cachedHudModuleCount = moduleCount;
        hudCacheInitialized = true;
        return cachedHudModules;
    }

    private void resetAllElements() {
        for (HudElementModule module : hudModules()) {
            module.resetPosition(module.defaultX(), module.defaultY());
            if (!module.isEnabled()) {
                module.setEnabled(true);
            }
        }
    }

    private int resolveAxisMove(
        HudElementModule movingModule,
        List<HudElementModule> modules,
        int currentX,
        int currentY,
        int target,
        boolean horizontal
    ) {
        if (target == (horizontal ? currentX : currentY)) {
            return target;
        }

        int candidateX = horizontal ? target : currentX;
        int candidateY = horizontal ? currentY : target;
        if (!collidesWithAny(movingModule, modules, candidateX, candidateY)) {
            return target;
        }

        int current = horizontal ? currentX : currentY;
        int step = target > current ? 1 : -1;
        int resolved = current;

        while (resolved != target) {
            int next = resolved + step;
            int testX = horizontal ? next : currentX;
            int testY = horizontal ? currentY : next;
            if (collidesWithAny(movingModule, modules, testX, testY)) {
                break;
            }
            resolved = next;
        }

        return resolved;
    }

    private boolean collidesWithAny(HudElementModule movingModule, List<HudElementModule> modules, int x, int y) {
        UiRect moving = boundsAt(movingModule, x, y);
        int gap = NavigatorStyle.HudEditor.ELEMENT_SNAP_GAP;
        for (HudElementModule module : modules) {
            if (module == movingModule) {
                continue;
            }
            UiRect other = elementBounds(module);
            if (moving.x() < other.right() + gap
                && moving.right() > other.x() - gap
                && moving.y() < other.bottom() + gap
                && moving.bottom() > other.y() - gap) {
                return true;
            }
        }
        return false;
    }

    private int maxXFor(HudElementModule module) {
        return Math.max(
            NavigatorStyle.HudEditor.ELEMENT_MIN_X,
            width - elementWidth(module) - NavigatorStyle.HudEditor.ELEMENT_MIN_X
        );
    }

    private int maxYFor(HudElementModule module) {
        return Math.max(
            NavigatorStyle.HudEditor.ELEMENT_MIN_Y,
            height - elementHeight(module) - NavigatorStyle.HudEditor.ELEMENT_MIN_Y
        );
    }

    private String elementLabel(HudElementModule module) {
        String raw = module.getName();
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String label = raw.trim();
        if (label.endsWith(" HUD")) {
            label = label.substring(0, label.length() - 4);
        }
        return label;
    }

    private UiRect boundsAt(HudElementModule module, int x, int y) {
        return new UiRect(x, y, elementWidth(module), elementHeight(module));
    }

    private int elementWidth(HudElementModule module) {
        return module.getElementWidth();
    }

    private int elementHeight(HudElementModule module) {
        return module.getElementHeight();
    }

    private int controlRailX(UiRect bounds) {
        return bounds.right() - NavigatorStyle.HudEditor.TOGGLE_SIZE - NavigatorStyle.HudEditor.TOGGLE_MARGIN;
    }

    private int controlRailWidth() {
        return NavigatorStyle.HudEditor.TOGGLE_SIZE + NavigatorStyle.HudEditor.TOGGLE_MARGIN + CONTROL_RAIL_SPACING;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void clampAllElementsToBounds() {
        for (HudElementModule module : hudModules()) {
            int clampedX = clamp(module.getEditorX(), NavigatorStyle.HudEditor.ELEMENT_MIN_X, maxXFor(module));
            int clampedY = clamp(module.getEditorY(), NavigatorStyle.HudEditor.ELEMENT_MIN_Y, maxYFor(module));
            if (clampedX != module.getEditorX() || clampedY != module.getEditorY()) {
                module.setEditorPosition(clampedX, clampedY);
            }
        }
    }
}

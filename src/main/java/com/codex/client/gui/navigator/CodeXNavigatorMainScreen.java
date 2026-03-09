package com.codex.client.gui.navigator;

import com.codex.api.module.Module;
import com.codex.api.module.ModuleManager;
import com.codex.client.CodeXClient;
import com.codex.client.gui.navigator.components.ModuleCard;
import com.codex.client.gui.navigator.components.NavigatorSearchBar;
import com.codex.client.gui.navigator.hud.HudEditorPanel;
import com.codex.client.gui.navigator.layout.MainGridLayout;
import com.codex.client.gui.navigator.layout.NavigatorChromeLayout;
import com.codex.client.gui.navigator.layout.UiRect;
import com.codex.client.gui.navigator.settings.ClientSettingsPanel;
import com.codex.client.utils.Animation;
import com.codex.impl.module.render.ClickGUI;
import com.codex.impl.module.render.hud.HudElementModule;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CodeXNavigatorMainScreen extends CodeXNavigatorScreen {
    private final List<ModuleCard> moduleCards = new ArrayList<>();
    private final MainGridLayout gridLayout = new MainGridLayout();
    private final Animation expandAnim = new Animation(0, NavigatorStyle.Animation.MAIN_EXPAND);
    private final ClientSettingsPanel clientSettingsPanel = new ClientSettingsPanel();
    private final HudEditorPanel hudEditorPanel = new HudEditorPanel(
        () -> setActiveSection(NavigatorSection.MODULES),
        this::openFeatureFromHudEditor
    );

    private NavigatorSearchBar searchBar;
    private String lastSearchText = "";
    private boolean expanding;
    private Module expandingModule;
    private NavigatorSection activeSection = NavigatorViewState.getActiveSection();

    public CodeXNavigatorMainScreen() {
        super();
        hasBackground = false;
        nonScrollableArea = 0;
        loadModules("");
    }

    @Override
    protected void onResize() {
        ensureSearchBar();
        refreshGridMetrics();
        clientSettingsPanel.resize(this.width, this.height);
        hudEditorPanel.resize(this.width, this.height);
    }

    @Override
    protected void onMouseClick(double mouseX, double mouseY, int button) {
        if (expanding || closing) {
            return;
        }

        if (activeSection == NavigatorSection.CLIENT_SETTINGS) {
            double headerMouseY = contentToHeaderMouseY(mouseY);
            if (clientSettingsPanel.mouseClickedHeader(mouseX, headerMouseY, button)) {
                return;
            }
            clientSettingsPanel.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (activeSection == NavigatorSection.HUD_EDITOR) {
            hudEditorPanel.mouseClicked(mouseX, mouseY, button);
            return;
        }

        double headerMouseY = contentToHeaderMouseY(mouseY);
        if (searchBar != null && searchBar.mouseClicked(mouseX, headerMouseY, button)) {
            return;
        }

        layoutCards();
        if (button == 0) {
            for (ModuleCard card : moduleCards) {
                if (!card.isHovered(mouseX, mouseY)) {
                    continue;
                }
                card.onInteracted();
                if (card.isHoveredArrow(mouseX, mouseY)) {
                    startExpand(card.module);
                } else {
                    card.module.toggle();
                    CodeXClient.queueConfigSave();
                }
                return;
            }
            return;
        }

        if (button == 1) {
            for (ModuleCard card : moduleCards) {
                if (card.isHovered(mouseX, mouseY)) {
                    card.onInteracted();
                    startExpand(card.module);
                    return;
                }
            }
        }
    }

    @Override
    protected void onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (activeSection == NavigatorSection.CLIENT_SETTINGS) {
            clientSettingsPanel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            return;
        }
        if (activeSection == NavigatorSection.HUD_EDITOR) {
            hudEditorPanel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    @Override
    protected void onMouseRelease(double x, double y, int button) {
        if (activeSection == NavigatorSection.CLIENT_SETTINGS) {
            clientSettingsPanel.mouseReleased(x, y, button);
            return;
        }
        if (activeSection == NavigatorSection.HUD_EDITOR) {
            hudEditorPanel.mouseReleased(x, y, button);
        }
    }

    @Override
    protected void onUpdate() {
        if (activeSection == NavigatorSection.MODULES && searchBar != null) {
            String newText = searchBar.getText();
            if (!newText.equals(lastSearchText)) {
                loadModules(newText);
                lastSearchText = newText;
            }
        }

        if (expanding && expandAnim.getValue() >= 0.95f) {
            CodeXFeatureScreen featureScreen = new CodeXFeatureScreen(expandingModule, this);
            featureScreen.slideAnim.setValue(1f);
            featureScreen.slideAnim.setTarget(1f);
            this.client.setScreen(featureScreen);
            expanding = false;
            expandAnim.setValue(0);
            expandAnim.setTarget(0);
        }
    }

    @Override
    protected void onRender(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        float expandProgress = expandAnim.update(partialTicks);

        if (activeSection == NavigatorSection.CLIENT_SETTINGS) {
            clientSettingsPanel.render(context, mouseX, mouseY, partialTicks);
            return;
        }
        if (activeSection == NavigatorSection.HUD_EDITOR) {
            hudEditorPanel.render(context, mouseX, mouseY, partialTicks);
            return;
        }

        layoutCards();
        UiRect scissor = gridLayout.gridScissor(this.width, this.height);
        context.enableScissor(scissor.x(), scissor.y(), scissor.right(), scissor.bottom());
        for (ModuleCard card : moduleCards) {
            card.render(context, mouseX, mouseY, partialTicks, expanding, expandingModule == card.module, expandProgress, middleX, this.height);
        }
        if (moduleCards.isEmpty() && !expanding) {
            String emptyText = "No modules found";
            int emptyX = (this.width / 2) - (this.client.textRenderer.getWidth(emptyText) / 2);
            int emptyY = NavigatorStyle.Main.GRID_TOP + NavigatorStyle.Main.EMPTY_RESULTS_Y_OFFSET + (int) scroll;
            context.drawText(this.client.textRenderer, emptyText, emptyX, emptyY, NavigatorStyle.Main.EMPTY_RESULTS_COLOR, false);
        }
        context.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (expanding) {
            return false;
        }
        if (activeSection == NavigatorSection.CLIENT_SETTINGS) {
            return clientSettingsPanel.mouseScrolled(mouseX, mouseY, verticalAmount);
        }
        if (activeSection != NavigatorSection.MODULES) {
            return false;
        }
        UiRect scissor = gridLayout.gridScissor(this.width, this.height);
        if (!scissor.contains(mouseX, mouseY)) {
            return false;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeSection == NavigatorSection.CLIENT_SETTINGS
            && clientSettingsPanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (activeSection == NavigatorSection.HUD_EDITOR) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                setActiveSection(NavigatorSection.MODULES);
                return true;
            }
            return false;
        }
        if (activeSection == NavigatorSection.MODULES
            && searchBar != null
            && searchBar.isFocused()
            && searchBar.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (activeSection == NavigatorSection.CLIENT_SETTINGS
            && clientSettingsPanel.charTyped(chr, modifiers)) {
            return true;
        }
        if (activeSection != NavigatorSection.MODULES) {
            return false;
        }
        if (searchBar != null && searchBar.isFocused() && searchBar.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    protected NavigatorSection getActiveSection() {
        return activeSection;
    }

    @Override
    protected boolean shouldRenderTopBar() {
        return activeSection != NavigatorSection.HUD_EDITOR;
    }

    @Override
    protected boolean shouldRenderHeaderLayer() {
        return activeSection != NavigatorSection.HUD_EDITOR;
    }

    @Override
    protected void onSectionSelected(NavigatorSection section) {
        setActiveSection(section);
    }

    public void setActiveSection(NavigatorSection section) {
        this.activeSection = section == null ? NavigatorSection.MODULES : section;
        NavigatorViewState.setActiveSection(this.activeSection);
        refreshGridMetrics();
    }

    public void setExpanding(boolean expanding) {
        this.expanding = expanding;
    }

    private void loadModules(String query) {
        moduleCards.clear();
        List<Module> allModules = ModuleManager.getInstance().getModules();
        String normalized = query.toLowerCase(Locale.ROOT);
        List<Module> filtered = allModules.stream()
            .filter(module -> !(module instanceof ClickGUI))
            .filter(module -> query.isEmpty()
                || module.getName().toLowerCase(Locale.ROOT).contains(normalized)
                || module.getDescription().toLowerCase(Locale.ROOT).contains(normalized))
            .collect(Collectors.toList());

        for (int i = 0; i < filtered.size(); i++) {
            moduleCards.add(new ModuleCard(filtered.get(i), i));
        }
        refreshGridMetrics();
    }

    private void ensureSearchBar() {
        UiRect searchRect = NavigatorChromeLayout.searchRect(this.width);
        if (searchBar == null) {
            searchBar = new NavigatorSearchBar(
                this.client.textRenderer,
                searchRect.x(),
                searchRect.y(),
                searchRect.width(),
                searchRect.height()
            );
            return;
        }
        searchBar.setBounds(searchRect.x(), searchRect.y(), searchRect.width(), searchRect.height());
    }

    private void refreshGridMetrics() {
        if (this.width <= 0) {
            return;
        }
        gridLayout.resize(this.width);
        if (activeSection == NavigatorSection.MODULES) {
            setContentHeight(gridLayout.contentHeightFor(moduleCards.size()));
        } else {
            setContentHeight(0);
        }
    }

    private void layoutCards() {
        for (int i = 0; i < moduleCards.size(); i++) {
            moduleCards.get(i).setBounds(gridLayout.cardBoundsFor(i, scroll));
        }
    }

    private void startExpand(Module module) {
        expanding = true;
        expandingModule = module;
        expandAnim.setTarget(1.0f);
    }

    private void openFeatureFromHudEditor(HudElementModule module) {
        if (module == null) {
            return;
        }
        this.expanding = false;
        this.expandingModule = null;
        CodeXFeatureScreen featureScreen = new CodeXFeatureScreen(module, this);
        featureScreen.slideAnim.setValue(1f);
        featureScreen.slideAnim.setTarget(1f);
        this.client.setScreen(featureScreen);
    }

    @Override
    protected void onRenderHeader(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        if (expanding || activeSection == NavigatorSection.HUD_EDITOR) {
            return;
        }
        context.getMatrices().push();
        context.getMatrices().scale(NavigatorStyle.Main.TITLE_SCALE, NavigatorStyle.Main.TITLE_SCALE, NavigatorStyle.Main.TITLE_SCALE);
        context.drawTextWithShadow(
            this.client.textRenderer,
            "CodeX Client",
            (int) (NavigatorStyle.Main.TITLE_X / NavigatorStyle.Main.TITLE_SCALE),
            (int) (NavigatorStyle.Main.TITLE_Y / NavigatorStyle.Main.TITLE_SCALE),
            0xFFFFFF
        );
        context.getMatrices().pop();

        if (activeSection == NavigatorSection.MODULES && searchBar != null) {
            searchBar.render(context, mouseX, mouseY, partialTicks);
        } else if (activeSection == NavigatorSection.CLIENT_SETTINGS) {
            clientSettingsPanel.renderHeader(context, mouseX, mouseY, partialTicks);
        }
    }
}

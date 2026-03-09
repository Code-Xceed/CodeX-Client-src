package com.codex.client.gui.navigator;

import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import com.codex.client.gui.navigator.layout.UiRect;
import com.codex.client.gui.navigator.settings.GuiSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class CodeXNavigatorScreen extends Screen {

    protected Animation scrollAnim = new Animation(0, NavigatorStyle.Animation.SMOOTH);
    protected float scroll = 0;
    protected int maxScroll;
    
    public Animation slideAnim = new Animation(0, NavigatorStyle.Animation.SMOOTH);
    protected boolean closing = false;
    private final Animation[] topTabHoverAnims = new Animation[NavigatorSection.values().length];
    
    protected int middleX;
    protected boolean hasBackground = true;
    protected int nonScrollableArea = NavigatorStyle.Scroll.DEFAULT_NON_SCROLLABLE_AREA;

    public CodeXNavigatorScreen() {
        super(Text.literal("CodeX Navigator"));
        this.slideAnim.setValue(0);
        this.slideAnim.setTarget(1f);
        for (int i = 0; i < topTabHoverAnims.length; i++) {
            topTabHoverAnims[i] = new Animation(0f, 0.2f);
        }
    }

    @Override
    protected void init() {
        middleX = this.width / 2;
        onResize();
    }

    protected abstract void onResize();

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double headerMouseY = adjustMouseYForHeader(mouseY);
        if (shouldRenderTopBar() && handleTopBarClick(mouseX, headerMouseY, button)) {
            return true;
        }
        onMouseClick(mouseX, adjustMouseYForContent(mouseY), button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        onMouseDrag(mouseX, adjustMouseYForContent(mouseY), button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        onMouseRelease(mouseX, adjustMouseYForContent(mouseY), button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY < NavigatorStyle.TopBar.SCROLL_LOCK_BOTTOM) {
            return false;
        }
        if (maxScroll == 0) {
            return false;
        }
        scrollAnim.setTarget(scrollAnim.getTarget() + (float) (verticalAmount * NavigatorStyle.Scroll.STEP));
        if (scrollAnim.getTarget() > 0) scrollAnim.setTarget(0);
        else if (scrollAnim.getTarget() < maxScroll) scrollAnim.setTarget(maxScroll);
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void tick() {
        if (closing && slideAnim.getValue() < 0.05f) {
            this.client.setScreen(null);
            return;
        }
        onUpdate();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        scroll = scrollAnim.update(partialTicks);
        float slideProgress = slideAnim.update(partialTicks);

        renderBackground(context, mouseX, mouseY, partialTicks);
        float contentOffset = contentOffset(slideProgress);
        float headerOffset = headerOffset(slideProgress);

        context.getMatrices().push();
        context.getMatrices().translate(0, contentOffset, 0);

        int contentMouseY = (int) (mouseY - contentOffset);

        int bgx1 = middleX - 154;
        int bgx2 = middleX + 154;
        int bgy1 = 60;
        int bgy2 = this.height - 43;

        if (hasBackground) {
            drawBackgroundBox(context, bgx1, bgy1, bgx2 - bgx1, bgy2 - bgy1);
        }

        onRender(context, mouseX, contentMouseY, partialTicks);
        context.getMatrices().pop();

        if (shouldRenderTopBar() || shouldRenderHeaderLayer()) {
            context.getMatrices().push();
            context.getMatrices().translate(0, headerOffset, 0);
            int headerMouseY = (int) (mouseY - headerOffset);
            onRenderHeader(context, mouseX, headerMouseY, partialTicks);
            if (shouldRenderTopBar()) {
                renderTopBar(context, mouseX, headerMouseY, partialTicks);
            }
            context.getMatrices().pop();
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        float slideProgress = slideAnim.getValue();
        GuiSettings settings = GuiSettings.get();
        int top = scaleAlpha(settings.backgroundTopColor(), slideProgress);
        int bottom = scaleAlpha(settings.backgroundBottomColor(), slideProgress);
        context.fillGradient(0, 0, this.width, this.height, top, bottom);
    }

    @Override
    public void close() {
        closing = true;
        slideAnim.setTarget(0);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    protected abstract void onMouseClick(double mouseX, double mouseY, int button);
    protected abstract void onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY);
    protected abstract void onMouseRelease(double x, double y, int button);
    protected abstract void onUpdate();
    protected abstract void onRender(DrawContext context, int mouseX, int mouseY, float partialTicks);
    protected abstract NavigatorSection getActiveSection();
    protected abstract void onSectionSelected(NavigatorSection section);

    protected void onRenderHeader(DrawContext context, int mouseX, int mouseY, float partialTicks) {
    }

    protected boolean shouldRenderHeaderLayer() {
        return false;
    }

    protected boolean useSplitHeaderContentSlide() {
        return true;
    }

    protected boolean shouldRenderTopBar() {
        return true;
    }

    protected void setContentHeight(int contentHeight) {
        maxScroll = this.height - contentHeight - nonScrollableArea - NavigatorStyle.Scroll.CONTENT_BOTTOM_PADDING;
        if (maxScroll > 0) maxScroll = 0;

        if (scrollAnim.getTarget() < maxScroll) {
            scrollAnim.setTarget(maxScroll);
        }
        if (scrollAnim.getValue() < maxScroll) {
            scrollAnim.setValue(maxScroll);
        } else if (scrollAnim.getValue() > 0) {
            scrollAnim.setValue(0);
        }
    }

    protected void drawBackgroundBox(DrawContext context, int x, int y, int width, int height) {
        RenderUtils.drawModernBox(
            context,
            x,
            y,
            width,
            height,
            GuiSettings.get().panelFillColor(),
            NavigatorStyle.Colors.BACKGROUND_BOX_OUTLINE
        );
    }

    protected double contentToHeaderMouseY(double contentMouseY) {
        float progress = slideAnim.getValue();
        return contentMouseY + contentOffset(progress) - headerOffset(progress);
    }

    protected double adjustMouseYForHeader(double mouseY) {
        return mouseY - headerOffset(slideAnim.getValue());
    }

    protected double adjustMouseYForContent(double mouseY) {
        return mouseY - contentOffset(slideAnim.getValue());
    }

    private float slideDistance(float slideProgress) {
        return (1.0f - slideProgress) * this.height;
    }

    private float contentOffset(float slideProgress) {
        return slideDistance(slideProgress);
    }

    private float headerOffset(float slideProgress) {
        if (!useSplitHeaderContentSlide()) {
            return contentOffset(slideProgress);
        }
        return -slideDistance(slideProgress);
    }

    private void renderTopBar(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        UiRect[] tabRects = topBarRects();
        if (tabRects.length == 0) {
            return;
        }
        UiRect topBarRect = topBarRect(tabRects);
        NavigatorSection activeSection = getActiveSection();

        RenderUtils.drawModernBox(
            context,
            topBarRect.x(),
            topBarRect.y(),
            topBarRect.width(),
            topBarRect.height(),
            0x78000000,
            0x40FFFFFF
        );
        RenderUtils.drawRect(
            context,
            topBarRect.x() + 1,
            topBarRect.bottom() - 1,
            topBarRect.width() - 2,
            1,
            0x2AFFFFFF
        );

        for (int i = 0; i < tabRects.length; i++) {
            NavigatorSection section = NavigatorSection.values()[i];
            UiRect rect = tabRects[i];
            boolean active = section == activeSection;
            boolean hovered = rect.contains(mouseX, mouseY);

            topTabHoverAnims[i].setTarget(hovered ? 1f : 0f);
            float hover = topTabHoverAnims[i].update(partialTicks);

            int accentGhost = GuiSettings.get().accentColor(0x56);
            int base = active ? 0x62000000 : 0x00000000;
            int hoverColor = active ? RenderUtils.interpolateColor(0x74000000, accentGhost, 0.55f) : 0x3A000000;
            int bg = RenderUtils.interpolateColor(base, hoverColor, active ? 1.0f : hover * 0.75f);
            int outline = active ? 0x44FFFFFF : 0x00000000;
            outline = RenderUtils.interpolateColor(outline, 0x33FFFFFF, hover * (active ? 0.5f : 0.7f));

            if (active || hover > 0.05f) {
                RenderUtils.drawModernBox(context, rect.x(), rect.y(), rect.width(), rect.height(), bg, outline);
            }
            if (active) {
                RenderUtils.drawRect(
                    context,
                    rect.x() + 5,
                    rect.bottom() - NavigatorStyle.TopBar.ACTIVE_UNDERLINE_HEIGHT,
                    rect.width() - 10,
                    NavigatorStyle.TopBar.ACTIVE_UNDERLINE_HEIGHT,
                    GuiSettings.get().accentColor(0xD6)
                );
            }

            int textColor = active ? 0xFFFFFFFF : RenderUtils.interpolateColor(0xFFB4B4B4, 0xFFE5E5E5, hover);
            int textX = rect.x() + (rect.width() - this.textRenderer.getWidth(section.getTitle())) / 2;
            int textY = rect.y() + 6;
            context.drawText(this.textRenderer, section.getTitle(), textX, textY, textColor, false);
        }
    }

    private boolean handleTopBarClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        UiRect[] tabRects = topBarRects();
        for (int i = 0; i < tabRects.length; i++) {
            if (!tabRects[i].contains(mouseX, mouseY)) {
                continue;
            }
            NavigatorSection selected = NavigatorSection.values()[i];
            if (selected != getActiveSection()) {
                UiInteractionFeedback.click();
                onSectionSelected(selected);
            }
            return true;
        }
        return false;
    }

    private UiRect[] topBarRects() {
        NavigatorSection[] sections = NavigatorSection.values();
        UiRect[] rects = new UiRect[sections.length];
        if (sections.length == 0) {
            return rects;
        }
        int totalWidth = 0;
        for (NavigatorSection section : sections) {
            totalWidth += this.textRenderer.getWidth(section.getTitle()) + (NavigatorStyle.TopBar.TAB_PADDING_X * 2);
        }
        totalWidth += (sections.length - 1) * NavigatorStyle.TopBar.TAB_GAP;

        int startX = (this.width - totalWidth) / 2;
        int y = NavigatorStyle.TopBar.Y + NavigatorStyle.TopBar.TAB_INSET_Y;
        for (int i = 0; i < sections.length; i++) {
            int width = this.textRenderer.getWidth(sections[i].getTitle()) + (NavigatorStyle.TopBar.TAB_PADDING_X * 2);
            rects[i] = new UiRect(startX, y, width, NavigatorStyle.TopBar.TAB_HEIGHT);
            startX += width + NavigatorStyle.TopBar.TAB_GAP;
        }
        return rects;
    }

    private UiRect topBarRect(UiRect[] tabs) {
        UiRect first = tabs[0];
        UiRect last = tabs[tabs.length - 1];
        int x = first.x() - NavigatorStyle.TopBar.TAB_INSET_X;
        int width = (last.right() - first.x()) + (NavigatorStyle.TopBar.TAB_INSET_X * 2);
        return new UiRect(x, NavigatorStyle.TopBar.Y, width, NavigatorStyle.TopBar.HEIGHT);
    }

    private int scaleAlpha(int color, float factor) {
        int rgb = color & 0x00FFFFFF;
        int alpha = (color >>> 24) & 0xFF;
        int scaledAlpha = Math.max(0, Math.min(255, (int) (alpha * factor)));
        return (scaledAlpha << 24) | rgb;
    }
}

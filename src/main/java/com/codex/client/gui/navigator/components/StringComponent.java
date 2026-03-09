package com.codex.client.gui.navigator.components;

import com.codex.api.value.StringValue;
import com.codex.client.gui.navigator.NavigatorStyle;
import com.codex.client.gui.navigator.UiInteractionFeedback;
import com.codex.client.gui.navigator.settings.GuiSettings;
import com.codex.client.utils.Animation;
import com.codex.client.utils.RenderUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class StringComponent extends ValueComponent<StringValue> {
    private boolean focused = false;
    private final Animation focusAnim = new Animation(0f, 0.2f);
    private int cursorTick = 0;
    private int cursorPos = 0;

    public boolean isFocused() {
        return focused;
    }

    public StringComponent(StringValue value) {
        super(value);
        this.height = 30; // Label + Text Box
        this.cursorPos = value.get().length();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        cursorTick++;

        context.drawText(mc.textRenderer, value.getName(), x, y, NavigatorStyle.Feature.TEXT_PRIMARY, false);

        int boxY = y + mc.textRenderer.fontHeight + 4;
        int boxHeight = 16;
        
        focusAnim.setTarget(focused ? 1f : (isHovered(mouseX, mouseY) ? 0.3f : 0f));
        float focusProgress = focusAnim.update(partialTicks);

        int bgColor = RenderUtils.interpolateColor(NavigatorStyle.Colors.CONTROL_IDLE, GuiSettings.get().accentColor(0x40), focusProgress);
        int outlineColor = RenderUtils.interpolateColor(NavigatorStyle.Colors.HAIRLINE, GuiSettings.get().accentColor(0xFF), focusProgress);
        
        RenderUtils.drawModernBox(context, x, boxY, width, boxHeight, bgColor, outlineColor);

        String text = value.get();
        if (cursorPos > text.length()) cursorPos = text.length();

        // Simple scissor for text rendering
        int textX = x + 4;
        context.enableScissor(x + 2, boxY + 2, x + width - 2, boxY + boxHeight - 2);
        
        String beforeCursor = text.substring(0, cursorPos);
        int cursorOffset = mc.textRenderer.getWidth(beforeCursor);
        
        // Scroll adjustment if text is too long
        int scrollX = 0;
        if (cursorOffset > width - 12) {
            scrollX = (width - 12) - cursorOffset;
        }
        
        context.drawText(mc.textRenderer, text, textX + scrollX, boxY + 4, 0xFFFFFFFF, false);
        
        if (focused && (cursorTick / 10) % 2 == 0) {
            context.drawText(mc.textRenderer, "_", textX + scrollX + cursorOffset, boxY + 4, 0xFFFFFFFF, false);
        }
        
        context.disableScissor();
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int boxY = y + mc.textRenderer.fontHeight + 4;
        boolean overBox = mouseX >= x && mouseX <= x + width && mouseY >= boxY && mouseY <= boxY + 16;
        
        if (button == 0) {
            if (overBox) {
                focused = true;
                cursorPos = value.get().length();
                UiInteractionFeedback.click();
            } else {
                focused = false;
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {}

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {}

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return;
        
        String current = value.get();
        
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorPos > 0) cursorPos--;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorPos < current.length()) cursorPos++;
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursorPos > 0 && !current.isEmpty()) {
                String newVal = current.substring(0, cursorPos - 1) + current.substring(cursorPos);
                value.set(newVal);
                cursorPos--;
                notifyValueChanged();
            }
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorPos < current.length()) {
                String newVal = current.substring(0, cursorPos) + current.substring(cursorPos + 1);
                value.set(newVal);
                notifyValueChanged();
            }
        } else if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                String newVal = current.substring(0, cursorPos) + clipboard + current.substring(cursorPos);
                value.set(newVal);
                cursorPos += clipboard.length();
                notifyValueChanged();
            }
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            focused = false;
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (!focused) return;
        if (chr >= 32 && chr != 127) {
            String current = value.get();
            String newVal = current.substring(0, cursorPos) + chr + current.substring(cursorPos);
            value.set(newVal);
            cursorPos++;
            notifyValueChanged();
        }
    }
}

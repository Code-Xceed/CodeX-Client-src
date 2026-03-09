package com.codex.impl.module.movement;

import com.codex.api.event.EventTarget;
import com.codex.api.event.events.UpdateEvent;
import com.codex.api.module.Category;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.api.value.StringValue;
import com.codex.client.module.ClientModule;
import com.codex.impl.module.render.hud.HudElementModule;
import com.codex.client.utils.RenderUtils;
import net.minecraft.client.gui.DrawContext;

public class ToggleSprint extends HudElementModule {
    private final BoolValue enableSprint = new BoolValue("Toggle Sprint", true);
    private final BoolValue enableSneak = new BoolValue("Toggle Sneak", false);
    private final BoolValue sprintOverride = new BoolValue("Double Tap Override", true);

    private final BoolValue showIndicator = new BoolValue("Show Indicator", true);
    private final StringValue sprintText = new StringValue("Sprint Text", "[Sprinting]");
    private final StringValue sneakText = new StringValue("Sneak Text", "[Sneaking]");
    private final NumberValue fontScale = new NumberValue("Font Scale", 1.0, 0.5, 3.0, 0.1);
    private final ColorValue textColor = new ColorValue("Text Color", 0xFFFFFFFF);
    private final BoolValue backgroundBlur = new BoolValue("Background Shadow", true);

    private final BoolValue autoSprint = new BoolValue("Auto Sprint (W)", false);
    private final ModeValue sneakMode = new ModeValue("Sneak Mode", "Standard", "Standard", "Safe Bridge");

    private boolean sprintToggled = false;
    private boolean sneakToggled = false;
    private boolean wasSprintKeyPressed = false;
    private boolean wasSneakKeyPressed = false;
    
    private String currentDisplayState = "";

    public ToggleSprint() {
        super("Toggle Sprint", "Allows sprint and sneak to function as toggle.", 10, 80, 50, 15, false);

        addValue(enableSprint);
        addValue(enableSneak);
        addValue(sprintOverride);

        addValue(showIndicator);
        addValue(sprintText);
        addValue(sneakText);
        addValue(fontScale);
        addValue(textColor);
        addValue(backgroundBlur);

        addValue(autoSprint);
        addValue(sneakMode);
    }

    @Override
    public int defaultX() {
        return 10;
    }

    @Override
    public int defaultY() {
        return 80;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        boolean sprintKeyNow = mc.options.sprintKey.isPressed();
        boolean sneakKeyNow = mc.options.sneakKey.isPressed();

        if (enableSprint.get()) {
            if (sprintKeyNow && !wasSprintKeyPressed) {
                sprintToggled = !sprintToggled;
            }
            if (autoSprint.get() && mc.player.forwardSpeed > 0) {
                sprintToggled = true;
            }
        } else {
            sprintToggled = false;
        }

        if (enableSneak.get()) {
            if (sneakKeyNow && !wasSneakKeyPressed) {
                sneakToggled = !sneakToggled;
            }
        } else {
            sneakToggled = false;
        }

        wasSprintKeyPressed = sprintKeyNow;
        wasSneakKeyPressed = sneakKeyNow;

        // Apply States
        if (sprintToggled && !mc.player.isSneaking() && mc.player.forwardSpeed > 0) {
            mc.player.setSprinting(true);
            currentDisplayState = sprintText.get();
        } else if (sneakToggled) {
            // Keep sneak state synchronized with the current client input path.
            mc.options.sneakKey.setPressed(true); 
            currentDisplayState = sneakText.get();
        } else {
            if (mc.options.sneakKey.isPressed() && !sneakKeyNow) {
                mc.options.sneakKey.setPressed(false);
            }
            currentDisplayState = "";
        }
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        if (!showIndicator.get()) {
            setElementWidth(50);
            setElementHeight(15);
            return;
        }
        
        // Keep the indicator visible while editing HUD placement.
        boolean isEditing = mc.currentScreen instanceof com.codex.client.gui.navigator.CodeXNavigatorScreen;
        String displayToRender = currentDisplayState;
        
        if (isEditing && currentDisplayState.isEmpty()) {
            displayToRender = sprintText.get();
        }
        
        if (displayToRender.isEmpty()) {
            setElementWidth(50);
            setElementHeight(15);
            return;
        }

        context.getMatrices().push();
        float scale = (float) fontScale.asDouble();
        context.getMatrices().translate(Math.round(getEditorX()), Math.round(getEditorY()), 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        int textWidth = mc.textRenderer.getWidth(displayToRender);
        int textHeight = mc.textRenderer.fontHeight;

        if (backgroundBlur.get()) {
            RenderUtils.drawRect(context, -3, -3, textWidth + 6, textHeight + 5, 0x55000000);
        }
        
        context.drawText(mc.textRenderer, displayToRender, 0, 0, textColor.get(), backgroundBlur.get());

        context.getMatrices().pop();
        
        setElementWidth((int) ((textWidth + 6) * scale));
        setElementHeight((int) ((textHeight + 5) * scale));
    }
}

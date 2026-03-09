package com.codex.impl.module.render;

import com.codex.api.event.EventTarget;
import com.codex.api.event.events.MouseEvent;
import com.codex.api.event.events.UpdateEvent;
import com.codex.api.module.Category;
import com.codex.api.module.Module;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class Zoom extends Module {

    private final NumberValue baseZoomMultiplier = (NumberValue) new NumberValue("Base Zoom Multiplier", 3.0, 1.1, 10.0, 0.1).setGroup("Values");
    private final NumberValue minZoom = (NumberValue) new NumberValue("Min Zoom", 1.1, 1.1, 5.0, 0.1).setGroup("Values");
    private final NumberValue maxZoom = (NumberValue) new NumberValue("Max Zoom", 20.0, 5.0, 50.0, 0.5).setGroup("Values");
    private final NumberValue scrollSensitivity = (NumberValue) new NumberValue("Scroll Sensitivity", 1.0, 0.1, 5.0, 0.1).setGroup("Values");

    private final ModeValue animationMode = (ModeValue) new ModeValue("Animation", "Smooth", "Smooth", "Linear", "Instant").setGroup("Mode");
    private final NumberValue animationSpeed = (NumberValue) new NumberValue("Animation Speed", 0.3, 0.05, 1.0, 0.05).setGroup("Values");

    private final BoolValue cinematicCamera = (BoolValue) new BoolValue("Cinematic Camera", true).setGroup("Toggles");
    private final BoolValue reduceSensitivity = (BoolValue) new BoolValue("Reduce Sensitivity", true).setGroup("Toggles");
    private final NumberValue sensitivityMultiplier = (NumberValue) new NumberValue("Sensitivity Multiplier", 0.5, 0.1, 1.0, 0.05).setGroup("Values");

    private final BoolValue hideHand = (BoolValue) new BoolValue("Hide Hand", false).setGroup("Toggles");
    private final BoolValue hideHud = (BoolValue) new BoolValue("Hide HUD", false).setGroup("Toggles");

    private double currentScrollZoom = 0.0;
    private double targetFovMultiplier = 1.0;
    private double currentFovMultiplier = 1.0;
    private long lastFrameTime = 0;

    private boolean isZooming = false;

    private final ModeValue keyMode = (ModeValue) new ModeValue("Keybind Mode", "Hold", "Hold", "Toggle").setGroup("Mode");

    public Zoom() {
        super("Zoom", "Smooth, scroll-adjustable zoom system.", Category.RENDER, false);

        baseZoomMultiplier.setGroup("Values");
        minZoom.setGroup("Values");
        maxZoom.setGroup("Values");
        scrollSensitivity.setGroup("Values");
        animationSpeed.setGroup("Values");
        sensitivityMultiplier.setGroup("Values");
        
        animationMode.setGroup("Mode");
        keyMode.setGroup("Mode");

        cinematicCamera.setGroup("Toggles");
        reduceSensitivity.setGroup("Toggles");
        hideHand.setGroup("Toggles");
        hideHud.setGroup("Toggles");

        addValue(baseZoomMultiplier);
        addValue(minZoom);
        addValue(maxZoom);
        addValue(scrollSensitivity);
        addValue(animationMode);
        addValue(animationSpeed);
        
        addValue(cinematicCamera);
        addValue(reduceSensitivity);
        addValue(sensitivityMultiplier);
        
        addValue(hideHand);
        addValue(hideHud);
        
        addValue(keyMode);
    }

    @Override
    public void onEnable() {
        currentScrollZoom = 0;
        isZooming = false;
        targetFovMultiplier = 1.0;
        currentFovMultiplier = 1.0;
        lastFrameTime = 0;
    }

    @Override
    public void onDisable() {
        setZooming(false);
        targetFovMultiplier = 1.0;
    }

    @Override
    public void onKeyPressed() {
        if (!isEnabled()) return;
        
        if ("Toggle".equals(keyMode.get())) {
            setZooming(!isZooming);
        } else {
            // Hold mode
            setZooming(true);
        }
    }

    @Override
    public void onKeyReleased() {
        if (!isEnabled()) return;
        
        if ("Hold".equals(keyMode.get())) {
            setZooming(false);
        }
    }

    public boolean isZooming() {
        return isEnabled() && isZooming;
    }

    public void setZooming(boolean zooming) {
        if (!isEnabled()) return;
        
        if (zooming && !this.isZooming) {
            // Just started zooming
            currentScrollZoom = baseZoomMultiplier.asDouble();
            if (cinematicCamera.get()) {
                MinecraftClient.getInstance().options.smoothCameraEnabled = true;
            }
        } else if (!zooming && this.isZooming) {
            // Just stopped zooming
            if (cinematicCamera.get()) {
                MinecraftClient.getInstance().options.smoothCameraEnabled = false;
            }
        }
        
        this.isZooming = zooming;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        // We move FOV interpolation strictly to render tick for flawless refresh-rate bound smoothness
    }

    public double getRenderFovMultiplier(float tickDelta) {
        long timeNow = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = timeNow;
        float frameDelta = (timeNow - lastFrameTime) / 50.0f;
        lastFrameTime = timeNow;

        // Calculate target FOV
        if (isZooming) {
            targetFovMultiplier = 1.0 / currentScrollZoom;
        } else {
            targetFovMultiplier = 1.0;
        }

        String anim = animationMode.get();
        if ("Instant".equals(anim)) {
            currentFovMultiplier = targetFovMultiplier;
        } else if ("Linear".equals(anim)) {
            double speed = animationSpeed.asDouble() * 0.1 * frameDelta; 
            if (currentFovMultiplier < targetFovMultiplier) {
                currentFovMultiplier = Math.min(targetFovMultiplier, currentFovMultiplier + speed);
            } else if (currentFovMultiplier > targetFovMultiplier) {
                currentFovMultiplier = Math.max(targetFovMultiplier, currentFovMultiplier - speed);
            }
        } else {
            // Smooth (Exponential)
            double speed = animationSpeed.asDouble();
            double factor = 1.0 - Math.pow(1.0 - (speed * 0.5), frameDelta);
            currentFovMultiplier += (targetFovMultiplier - currentFovMultiplier) * Math.clamp(factor, 0.01, 1.0);
        }
        
        if (Math.abs(currentFovMultiplier - targetFovMultiplier) < 0.0001) {
            currentFovMultiplier = targetFovMultiplier;
        }

        return currentFovMultiplier;
    }

    public void onScroll(double amount) {
        if (isZooming) {
            double sens = scrollSensitivity.asDouble();
            if (amount > 0) {
                currentScrollZoom *= (1.0 + (0.1 * sens));
            } else if (amount < 0) {
                currentScrollZoom /= (1.0 + (0.1 * sens));
            }
            currentScrollZoom = Math.clamp(currentScrollZoom, minZoom.asDouble(), maxZoom.asDouble());
        }
    }

    public boolean shouldHideHand() {
        return isZooming && hideHand.get() && currentFovMultiplier < 0.99;
    }

    public boolean shouldHideHud() {
        return isZooming && hideHud.get() && currentFovMultiplier < 0.99;
    }

    public double getMouseSensitivityMultiplier() {
        if (isZooming && reduceSensitivity.get()) {
            return currentFovMultiplier * sensitivityMultiplier.asDouble();
        }
        return 1.0;
    }
}

package com.codex.impl.module.world;

import com.codex.api.event.EventTarget;
import com.codex.api.event.events.UpdateEvent;
import com.codex.api.module.Category;
import com.codex.api.module.Module;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import net.minecraft.client.MinecraftClient;

public class TimeChanger extends Module {

    private final ModeValue mode = (ModeValue) new ModeValue("Mode", "Preset", "Preset", "Custom Slider").setGroup("General");
    private final ModeValue preset = (ModeValue) new ModeValue("Preset", "Day", "Day", "Noon", "Sunset", "Night", "Midnight").setGroup("General");
    private final NumberValue customTime = (NumberValue) new NumberValue("Custom Time", 6000.0, 0.0, 24000.0, 100.0).setGroup("General");
    
    private final BoolValue smoothTransition = (BoolValue) new BoolValue("Smooth Transition", true).setGroup("Advanced");
    private final BoolValue resetOnWorldChange = (BoolValue) new BoolValue("Reset on World Change", true).setGroup("Advanced");
    private final BoolValue disableInMultiplayer = (BoolValue) new BoolValue("Disable in Multiplayer", false).setGroup("Advanced");

    private double smoothTime = -1;
    private long lastFrameTime = 0;

    public TimeChanger() {
        super("Time Changer", "Client-side time override.", Category.WORLD, false);

        addValue(mode);
        addValue(preset);
        addValue(customTime);
        addValue(smoothTransition);
        addValue(resetOnWorldChange);
        addValue(disableInMultiplayer);
    }
    
    @Override
    public void onEnable() {
        smoothTime = -1;
        lastFrameTime = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        // Logic moved to render to ensure frame-independent smoothing
    }

    public long getTargetTime() {
        if ("Preset".equals(mode.get())) {
            switch (preset.get()) {
                case "Day": return 1000;
                case "Noon": return 6000;
                case "Sunset": return 12000;
                case "Night": return 13000;
                case "Midnight": return 18000;
                default: return 1000;
            }
        } else {
            return (long) customTime.asDouble();
        }
    }
    
    public long getRenderTime(long defaultTime) {
        if (!isEnabled()) return defaultTime;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (disableInMultiplayer.get() && mc != null && !mc.isInSingleplayer()) {
            return defaultTime;
        }

        long targetTime = getTargetTime();

        if (smoothTransition.get()) {
            long timeNow = System.currentTimeMillis();
            if (lastFrameTime == 0 || smoothTime == -1) {
                lastFrameTime = timeNow;
                smoothTime = targetTime;
            }
            float frameDelta = (timeNow - lastFrameTime) / 50.0f; // Normalize to 20tps basis
            lastFrameTime = timeNow;

            double diff = targetTime - smoothTime;
            
            // Handle day wrap around logic smoothly
            if (diff < -12000) diff += 24000;
            else if (diff > 12000) diff -= 24000;
            
            // Frame-independent eased interpolation
            float smoothFactor = 1.0f - (float)Math.pow(1.0 - 0.05, frameDelta);
            smoothTime += diff * Math.clamp(smoothFactor, 0.01f, 1.0f);
            
            if (smoothTime < 0) smoothTime += 24000;
            if (smoothTime > 24000) smoothTime -= 24000;

            return (long) smoothTime;
        }
        
        return targetTime;
    }
}

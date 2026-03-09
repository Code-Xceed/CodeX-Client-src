package com.codex.impl.module.world;

import com.codex.api.event.EventTarget;
import com.codex.api.event.events.UpdateEvent;
import com.codex.api.module.Category;
import com.codex.api.module.Module;
import com.codex.api.value.BoolValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.compat.ISimpleOption;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {

    private final ModeValue mode = (ModeValue) new ModeValue("Mode", "Gamma", "Gamma", "Lightmap", "Night Vision").setGroup("General");
    private final NumberValue brightness = (NumberValue) new NumberValue("Brightness (Gamma)", 10.0, 1.0, 20.0, 0.5).setGroup("General");
    private final BoolValue disableInMultiplayer = (BoolValue) new BoolValue("Disable in Multiplayer", false).setGroup("Advanced");
    
    // For Night Vision mode
    private final BoolValue hideNvEffect = (BoolValue) new BoolValue("Hide Effect Icon", true).setGroup("Advanced");

    private Double originalGamma = null;

    public Fullbright() {
        super("Fullbright", "Maximum brightness without torches.", Category.WORLD, false);

        addValue(mode);
        addValue(brightness);
        addValue(disableInMultiplayer);
        addValue(hideNvEffect);
    }

    @SuppressWarnings("unchecked")
    private void setGamma(double value) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            ((ISimpleOption<Double>) (Object) mc.options.getGamma()).forceSetValue(value);
        }
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null && "Gamma".equals(mode.get())) {
            originalGamma = mc.options.getGamma().getValue();
            setGamma(brightness.asDouble());
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null && originalGamma != null) {
            setGamma(originalGamma);
            originalGamma = null;
        }
        
        if (mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        
        if (disableInMultiplayer.get() && !mc.isInSingleplayer()) {
            return;
        }

        String currentMode = mode.get();
        if ("Night Vision".equals(currentMode)) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, !hideNvEffect.get(), !hideNvEffect.get()));
        } else {
            if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                StatusEffectInstance effect = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (effect != null && effect.getDuration() > 600) {
                    mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
            }
        }
        
        if ("Gamma".equals(currentMode)) {
            if (originalGamma == null) {
                originalGamma = mc.options.getGamma().getValue();
            }
            // Constantly enforce the custom brightness value in case the game tries to reset it
            setGamma(brightness.asDouble());
        } else if (originalGamma != null) {
            setGamma(originalGamma);
            originalGamma = null;
        }
    }

    public boolean isLightmapMode() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (disableInMultiplayer.get() && mc != null && !mc.isInSingleplayer()) {
            return false;
        }
        return isEnabled() && "Lightmap".equals(mode.get());
    }
}

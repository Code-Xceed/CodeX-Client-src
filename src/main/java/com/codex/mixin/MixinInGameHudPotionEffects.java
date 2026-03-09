package com.codex.mixin;

import com.codex.api.module.ModuleManager;
import com.codex.impl.module.render.hud.PotionEffects;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHudPotionEffects {

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderStatusEffectOverlay(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {
        PotionEffects module = ModuleManager.getInstance().getModule(PotionEffects.class);
        if (module != null && module.isEnabled()) {
            ci.cancel(); // Cancel vanilla potion rendering so our beautiful HUD takes over fully
        }
    }
}

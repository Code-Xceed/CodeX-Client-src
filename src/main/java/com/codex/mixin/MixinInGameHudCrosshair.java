package com.codex.mixin;

import com.codex.api.module.ModuleManager;
import com.codex.impl.module.render.CustomCrosshair;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHudCrosshair {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {
        CustomCrosshair module = ModuleManager.getInstance().getModule(CustomCrosshair.class);
        if (module != null && module.shouldCancelVanillaCrosshair()) {
            module.render(context, tickCounter.getTickDelta(true));
            ci.cancel();
        }
    }
}

package com.codex.mixin;

import com.codex.api.module.ModuleManager;
import com.codex.impl.module.render.Zoom;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {
        Zoom zoom = ModuleManager.getInstance().getModule(Zoom.class);
        if (zoom != null && zoom.shouldHideHud()) {
            ci.cancel();
        }
    }
}

package com.codex.mixin;

import com.codex.api.module.ModuleManager;
import com.codex.impl.module.render.Zoom;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(net.minecraft.client.render.Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        Zoom zoom = ModuleManager.getInstance().getModule(Zoom.class);
        if (zoom != null && zoom.isEnabled()) {
            double fovMultiplier = zoom.getRenderFovMultiplier(tickDelta);
            if (fovMultiplier != 1.0) {
                float currentFov = cir.getReturnValue();
                cir.setReturnValue((float) (currentFov * fovMultiplier));
            }
        }
    }
}

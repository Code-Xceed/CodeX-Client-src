package com.codex.mixin;

import com.codex.api.module.ModuleManager;
import com.codex.impl.module.world.Fullbright;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public class MixinLightmapTextureManager {

    @Inject(method = "getBrightness(Lnet/minecraft/world/dimension/DimensionType;I)F", at = @At("HEAD"), cancellable = true)
    private static void onGetBrightness(net.minecraft.world.dimension.DimensionType type, int lightLevel, CallbackInfoReturnable<Float> cir) {
        Fullbright fb = ModuleManager.getInstance().getModule(Fullbright.class);
        if (fb != null && fb.isLightmapMode()) {
            cir.setReturnValue(1.0f);
        }
    }
}

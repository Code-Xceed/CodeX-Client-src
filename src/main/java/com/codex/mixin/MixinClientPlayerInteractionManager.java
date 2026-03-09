package com.codex.mixin;

import com.codex.api.module.ModuleManager;
import com.codex.impl.module.render.CustomCrosshair;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        CustomCrosshair crosshair = ModuleManager.getInstance().getModule(CustomCrosshair.class);
        if (crosshair != null && crosshair.isEnabled()) {
            crosshair.onAttackEntity(target);
        }
    }
}

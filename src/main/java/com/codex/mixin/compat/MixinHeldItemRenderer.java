package com.codex.mixin.compat;

import com.codex.api.module.ModuleManager;
import com.codex.impl.module.render.Zoom;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {
    
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(float tickDelta, net.minecraft.client.util.math.MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider.Immediate vertexConsumers, net.minecraft.client.network.ClientPlayerEntity player, int light, CallbackInfo ci) {
        Zoom zoom = ModuleManager.getInstance().getModule(Zoom.class);
        if (zoom != null && zoom.shouldHideHand()) {
            ci.cancel();
        }
    }
}

package com.codex.mixin;

import com.codex.api.module.ModuleManager;
import com.codex.impl.module.world.TimeChanger;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.Properties.class)
public class MixinClientWorld {

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        TimeChanger timeChanger = ModuleManager.getInstance().getModule(TimeChanger.class);
        if (timeChanger != null && timeChanger.isEnabled()) {
            // ClientWorld.getTimeOfDay usually returns the raw time.
            // We pass it in case the module is off (but it won't be if we are in this block, except for the multiplayer check)
            // But we don't have the original value easily without calling super, which we can't do in an inject HEAD without extra logic.
            // Actually, we can just intercept the return value.
        }
    }
    
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void onGetTimeOfDayReturn(CallbackInfoReturnable<Long> cir) {
        TimeChanger timeChanger = ModuleManager.getInstance().getModule(TimeChanger.class);
        if (timeChanger != null && timeChanger.isEnabled()) {
            long modifiedTime = timeChanger.getRenderTime(cir.getReturnValue());
            if (modifiedTime != cir.getReturnValue()) {
                cir.setReturnValue(modifiedTime);
            }
        }
    }
}

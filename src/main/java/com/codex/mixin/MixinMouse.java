package com.codex.mixin;

import com.codex.api.event.EventManager;
import com.codex.api.event.events.MouseEvent;
import com.codex.api.event.events.MouseUpdateEvent;
import com.codex.api.module.ModuleManager;
import com.codex.impl.module.render.Zoom;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void onUpdateMouse(CallbackInfo ci) {
        MouseUpdateEvent event = new MouseUpdateEvent(this.cursorDeltaX, this.cursorDeltaY);
        EventManager.call(event);
        this.cursorDeltaX = event.getDeltaX();
        this.cursorDeltaY = event.getDeltaY();
    }

    @Inject(method = "onMouseButton", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 0))
    private void onMouseButtonHook(long window, int button, int action, int mods, CallbackInfo ci) {
        EventManager.call(new MouseEvent(button, action));
    }

    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSpectator()Z"), cancellable = true)
    private void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        Zoom zoom = ModuleManager.getInstance().getModule(Zoom.class);
        if (zoom != null && zoom.isZooming()) {
            zoom.onScroll(vertical);
            ci.cancel();
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object redirectMouseSensitivity(net.minecraft.client.option.SimpleOption<?> option) {
        Object value = option.getValue();
        if (value instanceof Double d) {
            Zoom zoom = ModuleManager.getInstance().getModule(Zoom.class);
            if (zoom != null && zoom.isEnabled() && zoom.getMouseSensitivityMultiplier() != 1.0) {
                return d * zoom.getMouseSensitivityMultiplier();
            }
        }
        return value;
    }
}

package com.codex.mixin;

import com.codex.api.event.EventManager;
import com.codex.api.event.events.KeyEvent;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Inject(method = "onKey", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 0))
    private void onKeyHook(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (key != -1) {
            EventManager.call(new KeyEvent(key, action));
        }
    }
}

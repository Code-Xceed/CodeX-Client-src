package com.codex.client.gui.navigator;

import com.codex.client.gui.navigator.settings.GuiSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public final class UiInteractionFeedback {
    private UiInteractionFeedback() {}

    public static void click() {
        if (!GuiSettings.get().isClickSoundsEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSoundManager() == null) {
            return;
        }
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 0.85f));
    }
}

package com.codex.client.module;

import com.codex.api.module.Category;
import com.codex.api.module.Module;
import net.minecraft.client.MinecraftClient;

public abstract class ClientModule extends Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    public ClientModule(String name, String description, Category category) {
        super(name, description, category);
    }

    public ClientModule(String name, String description, Category category, boolean defaultEnabled) {
        super(name, description, category, defaultEnabled);
    }
}

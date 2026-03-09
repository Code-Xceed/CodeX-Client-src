package com.codex.impl.module.render;

import com.codex.api.module.Category;
import com.codex.client.CodeXClient;
import com.codex.client.module.ClientModule;
import com.codex.client.gui.navigator.CodeXNavigatorMainScreen;
import org.lwjgl.glfw.GLFW;

public class ClickGUI extends ClientModule {

    public ClickGUI() {
        super("ClickGUI", "Opens the sleek and modern CodeX interface", Category.RENDER, false);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT); // Default keybind for GUI
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc != null) {
            mc.setScreen(new CodeXNavigatorMainScreen());
        }
        // Immediately disable so it doesn't stay "toggled on" permanently
        this.setEnabled(false);
        CodeXClient.queueConfigSave();
    }
}

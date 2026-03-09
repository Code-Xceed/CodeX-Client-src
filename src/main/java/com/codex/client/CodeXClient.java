package com.codex.client;

import com.codex.api.module.ModuleConfigStore;
import com.codex.api.module.Module;
import com.codex.api.module.ModuleManager;
import com.codex.client.gui.navigator.settings.GuiSettingsStore;
import com.codex.impl.module.render.ClickGUI;
import com.codex.impl.module.movement.ToggleSprint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.codex.impl.module.render.hud.ArmorStatus;
import com.codex.impl.module.render.hud.FpsPingDisplay;
import com.codex.impl.module.render.hud.HudElementModule;
import com.codex.impl.module.render.Zoom;

import com.codex.impl.module.render.hud.PotionEffects;
import com.codex.impl.module.world.TimeChanger;
import com.codex.impl.module.render.CustomCrosshair;
import com.codex.impl.module.render.BlockOverlay;
import com.codex.impl.module.world.Fullbright;
import com.codex.impl.module.combat.AimAssist;

public class CodeXClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("CodeX-Client");
    private static final String CONFIG_FILE_NAME = "codex-client.properties";
    private static final String GUI_CONFIG_FILE_NAME = "codex-gui.properties";
    private static final long CONFIG_SAVE_DEBOUNCE_MS = 220L;
    private static final long CONFIG_SAVE_WAIT_MS = 2000L;
    private final Set<Integer> pressedModuleKeys = new HashSet<Integer>();
    private final Set<Integer> activeBoundKeys = new HashSet<Integer>();
    private final ScheduledExecutorService configSaveExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CodeX-Config-Writer");
        thread.setDaemon(true);
        return thread;
    });
    private final Object saveScheduleLock = new Object();
    private ScheduledFuture<?> pendingConfigSave;
    private static Path configPath;
    private static Path guiConfigPath;
    private static volatile CodeXClient instance;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Starting CodeX Client for Minecraft 1.21.4...");
        instance = this;

        ModuleManager.init();
        ModuleManager moduleManager = ModuleManager.getInstance();
        registerModules(moduleManager);

        configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        guiConfigPath = FabricLoader.getInstance().getConfigDir().resolve(GUI_CONFIG_FILE_NAME);
        if (!ModuleConfigStore.load(moduleManager, configPath)) {
            moduleManager.enableDefaultModules();
            ModuleConfigStore.save(moduleManager, configPath);
            LOGGER.info("No existing module config found; defaults applied.");
        } else {
            LOGGER.info("Module config loaded from {}", configPath);
        }
        if (!GuiSettingsStore.load(guiConfigPath)) {
            GuiSettingsStore.save(guiConfigPath);
            LOGGER.info("No existing GUI config found; defaults applied.");
        } else {
            LOGGER.info("GUI config loaded from {}", guiConfigPath);
        }
        ensureEssentialKeybinds(moduleManager);

        setupClientHooks();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            flushPendingConfigSave();
            saveModuleConfig();
            saveGuiConfig();
            shutdownConfigSaveExecutor();
            moduleManager.disableAll();
        }, "CodeX-Config-Save"));

        LOGGER.info("CodeX Client initialized ({} modules loaded).",
            moduleManager.getModules().size());
    }

    private void registerModules(ModuleManager mgr) {
        registerIfMissing(mgr, new ToggleSprint());
        registerIfMissing(mgr, new ClickGUI());
        registerIfMissing(mgr, new com.codex.impl.module.render.hud.Keystrokes());
        registerIfMissing(mgr, new ArmorStatus());
        registerIfMissing(mgr, new FpsPingDisplay());
        registerIfMissing(mgr, new Zoom());
        registerIfMissing(mgr, new PotionEffects());
        registerIfMissing(mgr, new TimeChanger());
        registerIfMissing(mgr, new Fullbright());
        registerIfMissing(mgr, new CustomCrosshair());
        registerIfMissing(mgr, new BlockOverlay());
        registerIfMissing(mgr, new AimAssist());
    }

    private void registerIfMissing(ModuleManager mgr, Module module) {
        if (mgr.getModule(module.getClass()) == null) {
            mgr.register(module);
        }
    }

    private void setupClientHooks() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            processModuleHotkeys(client);
        });

        HudRenderCallback.EVENT.register((context, tickDeltaManager) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.currentScreen instanceof com.codex.client.gui.navigator.CodeXNavigatorScreen) {
                return;
            }
            float tickDelta = tickDeltaManager.getTickDelta(true);
            for (Module module : ModuleManager.getInstance().getModules()) {
                if (module.isEnabled() && module instanceof HudElementModule hudModule) {
                    hudModule.render(context, tickDelta);
                }
            }
        });

        LOGGER.info("Client hooks registered.");
    }

    private void processModuleHotkeys(MinecraftClient client) {
        if (client == null || client.getWindow() == null || client.currentScreen != null) {
            pressedModuleKeys.clear();
            activeBoundKeys.clear();
            return;
        }

        long windowHandle = client.getWindow().getHandle();
        activeBoundKeys.clear();

        for (Module module : ModuleManager.getInstance().getModules()) {
            int key = module.getKey();
            if (key <= 0 || key == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
                continue;
            }

            Integer keyObj = Integer.valueOf(key);
            activeBoundKeys.add(keyObj);

            boolean down = InputUtil.isKeyPressed(windowHandle, key);
            boolean alreadyDown = pressedModuleKeys.contains(keyObj);
            if (down) {
                if (!alreadyDown) {
                    try {
                        module.onKeyPressed();
                    } catch (RuntimeException ex) {
                        LOGGER.error("Module hotkey press failed for {}", module.getName(), ex);
                        continue;
                    }
                    queueConfigSave();
                }
                try {
                    module.onKeyHold();
                } catch (RuntimeException ex) {
                    // Ignore spammy hold errors
                }
                pressedModuleKeys.add(keyObj);
            } else {
                if (alreadyDown) {
                    try {
                        module.onKeyReleased();
                    } catch (RuntimeException ex) {
                        LOGGER.error("Module hotkey release failed for {}", module.getName(), ex);
                    }
                    pressedModuleKeys.remove(keyObj);
                }
            }
        }

        pressedModuleKeys.retainAll(activeBoundKeys);
    }

    private void requestConfigSave() {
        final Path path = configPath;
        if (path == null) {
            return;
        }

        synchronized (saveScheduleLock) {
            if (pendingConfigSave != null) {
                pendingConfigSave.cancel(false);
            }
            pendingConfigSave = configSaveExecutor.schedule(() -> {
                boolean modulesSaved = ModuleConfigStore.save(ModuleManager.getInstance(), path);
                boolean guiSaved = saveGuiConfig();
                if (!modulesSaved) {
                    LOGGER.warn("Failed to persist module config to {}", path);
                }
                if (!guiSaved && guiConfigPath != null) {
                    LOGGER.warn("Failed to persist GUI config to {}", guiConfigPath);
                }
                synchronized (saveScheduleLock) {
                    pendingConfigSave = null;
                }
            }, CONFIG_SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void flushPendingConfigSave() {
        ScheduledFuture<?> pending;
        synchronized (saveScheduleLock) {
            pending = pendingConfigSave;
        }
        if (pending == null) {
            return;
        }

        try {
            pending.get(CONFIG_SAVE_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (CancellationException ignored) {
            // Cancelled by a newer save schedule.
        } catch (Exception exception) {
            LOGGER.debug("Config writer flush interrupted", exception);
        }
    }

    private void shutdownConfigSaveExecutor() {
        configSaveExecutor.shutdown();
        try {
            if (!configSaveExecutor.awaitTermination(CONFIG_SAVE_WAIT_MS, TimeUnit.MILLISECONDS)) {
                configSaveExecutor.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            configSaveExecutor.shutdownNow();
        }
    }

    public static boolean saveModuleConfig() {
        Path path = configPath;
        if (path == null) {
            return false;
        }
        return ModuleConfigStore.save(ModuleManager.getInstance(), path);
    }

    public static Path getConfigPath() {
        return configPath;
    }

    public static boolean saveGuiConfig() {
        return GuiSettingsStore.save(guiConfigPath);
    }

    public static void queueConfigSave() {
        CodeXClient current = instance;
        if (current != null) {
            current.requestConfigSave();
        }
    }

    private void ensureEssentialKeybinds(ModuleManager moduleManager) {
        ClickGUI clickGUI = moduleManager.getModule(ClickGUI.class);
        if (clickGUI == null) {
            return;
        }

        int key = clickGUI.getKey();
        if (key <= 0 || key == GLFW.GLFW_KEY_UNKNOWN) {
            clickGUI.setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
            queueConfigSave();
            LOGGER.info("Assigned default Right Shift keybind to ClickGUI.");
        }
    }
}

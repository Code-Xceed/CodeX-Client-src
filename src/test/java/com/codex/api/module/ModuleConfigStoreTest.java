package com.codex.api.module;

import com.codex.api.event.EventManager;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleConfigStoreTest {

    private Path tempConfig;

    @AfterEach
    void tearDown() throws IOException {
        ModuleManager.getInstance().reset();
        EventManager.clear();
        if (tempConfig != null) {
            Files.deleteIfExists(tempConfig);
        }
    }

    @Test
    void saveLoadRoundTripUsesAtomicWriteAndStableModuleKey() throws IOException {
        ModuleManager manager = ModuleManager.getInstance();
        PersistedModule module = new PersistedModule();
        manager.register(module);

        module.setKey(71);
        module.setEnabled(true);
        module.mode.set("B");
        module.speed.set(4.24D);
        ModuleConfigStore.setFavorite(module, true);

        tempConfig = Files.createTempFile("codex-config-", ".properties");
        assertTrue(ModuleConfigStore.save(manager, tempConfig));
        Path tempWrite = tempConfig.resolveSibling(tempConfig.getFileName().toString() + ".tmp");
        assertTrue(Files.notExists(tempWrite));

        manager.reset();
        EventManager.clear();
        PersistedModule reloaded = new PersistedModule();
        manager.register(reloaded);

        assertTrue(ModuleConfigStore.load(manager, tempConfig));
        assertTrue(reloaded.isEnabled());
        assertEquals(71, reloaded.getKey());
        assertEquals("B", reloaded.mode.get());
        assertEquals(4.0D, reloaded.speed.asDouble(), 0.0001D);
        assertTrue(ModuleConfigStore.isFavorite(reloaded));
    }

    @Test
    void legacyNameBasedConfigStillLoads() throws IOException {
        ModuleManager manager = ModuleManager.getInstance();
        LegacyNamedModule module = new LegacyNamedModule();
        manager.register(module);

        tempConfig = Files.createTempFile("codex-config-legacy-", ".properties");
        Properties properties = new Properties();
        properties.setProperty("module.legacy_module.enabled", "true");
        properties.setProperty("module.legacy_module.key", "66");
        properties.setProperty("module.legacy_module.favorite", "true");
        properties.setProperty("module.legacy_module.value.mode_setting", "B");
        try (OutputStream outputStream = Files.newOutputStream(tempConfig)) {
            properties.store(outputStream, "legacy");
        }

        assertTrue(ModuleConfigStore.load(manager, tempConfig));
        assertTrue(module.isEnabled());
        assertEquals(66, module.getKey());
        assertEquals("B", module.mode.get());
        assertTrue(ModuleConfigStore.isFavorite(module));
    }

    private static final class PersistedModule extends Module {
        private final ModeValue mode = new ModeValue("Mode", "A", "A", "B");
        private final NumberValue speed = new NumberValue("Speed", 0.0D, 0.0D, 10.0D, 0.5D);

        private PersistedModule() {
            super("Persisted Module", "test", Category.MISC, false);
            addValue(mode);
            addValue(speed);
        }
    }

    private static final class LegacyNamedModule extends Module {
        private final ModeValue mode = new ModeValue("Mode Setting", "A", "A", "B");

        private LegacyNamedModule() {
            super("Legacy Module", "test", Category.MISC, false);
            addValue(mode);
        }
    }
}

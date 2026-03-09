package com.codex.api.module;

import com.codex.api.event.EventManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleManagerTest {

    @AfterEach
    void tearDown() {
        ModuleManager.getInstance().reset();
        EventManager.clear();
    }

    @Test
    void registerAndLookupModule() {
        ModuleManager manager = ModuleManager.getInstance();
        DummyModule module = new DummyModule("SprintLike", false);

        manager.register(module);

        assertEquals(module, manager.getModule(DummyModule.class));
        assertEquals(module, manager.getModule("sprintlike"));
        assertNotNull(manager.getModules());
        assertEquals(1, manager.getModules().size());
    }

    @Test
    void duplicateRegistrationIsRejected() {
        ModuleManager manager = ModuleManager.getInstance();
        manager.register(new DummyModule("One", false));

        assertThrows(IllegalArgumentException.class, () -> manager.register(new DummyModule("One", false)));
    }

    @Test
    void defaultModulesCanBeEnabledInBulk() {
        ModuleManager manager = ModuleManager.getInstance();
        DefaultOnModule alwaysOn = new DefaultOnModule();
        OptionalModule optional = new OptionalModule();
        manager.register(alwaysOn);
        manager.register(optional);

        manager.enableDefaultModules();

        assertTrue(alwaysOn.isEnabled());
        assertFalse(optional.isEnabled());
    }

    private static final class DummyModule extends Module {
        DummyModule(String name, boolean defaultEnabled) {
            super(name, "test", Category.MISC, defaultEnabled);
        }
    }

    private static final class DefaultOnModule extends Module {
        DefaultOnModule() {
            super("AlwaysOn", "test", Category.MISC, true);
        }
    }

    private static final class OptionalModule extends Module {
        OptionalModule() {
            super("Optional", "test", Category.MISC, false);
        }
    }
}

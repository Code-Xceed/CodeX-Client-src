package com.codex.api.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry for all modules used by the client runtime.
 */
public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new CopyOnWriteArrayList<>();
    private final Map<Class<? extends Module>, Module> modulesByClass = new ConcurrentHashMap<>();
    private final Map<String, Module> modulesByName = new ConcurrentHashMap<>();

    private ModuleManager() {}

    /**
     * Initializes the singleton. Kept for backward compatibility.
     */
    public static void init() {
        // No-op: singleton is eagerly initialized.
    }

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    public void register(Module module) {
        Objects.requireNonNull(module, "module");

        if (modulesByClass.putIfAbsent(module.getClass(), module) != null) {
            throw new IllegalArgumentException("Module already registered by class: " + module.getClass().getName());
        }

        String normalizedName = normalizeName(module.getName());
        if (modulesByName.putIfAbsent(normalizedName, module) != null) {
            modulesByClass.remove(module.getClass());
            throw new IllegalArgumentException("Module already registered by name: " + module.getName());
        }

        modules.add(module);
    }

    /**
     * Returns an unmodifiable view of all registered modules.
     */
    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * Returns all modules in the given category.
     */
    public List<Module> getModulesByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) list.add(m);
        }
        return list;
    }

    /**
     * Finds a module by its implementation class.
     */
    public <T extends Module> T getModule(Class<T> clazz) {
        Module module = modulesByClass.get(clazz);
        if (module == null) {
            return null;
        }
        return clazz.cast(module);
    }

    /**
     * Finds a module by name (case-insensitive).
     */
    public Module getModule(String name) {
        if (name == null) {
            return null;
        }
        return modulesByName.get(normalizeName(name));
    }

    public void enableDefaultModules() {
        for (Module module : modules) {
            if (module.isDefaultEnabled() && !module.isEnabled()) {
                module.setEnabled(true);
            }
        }
    }

    public void disableAll() {
        List<Module> reverse = new ArrayList<Module>(modules);
        Collections.reverse(reverse);
        for (Module module : reverse) {
            if (module.isEnabled()) {
                module.setEnabled(false);
            }
        }
    }

    public void reset() {
        disableAll();
        modules.clear();
        modulesByClass.clear();
        modulesByName.clear();
    }

    private String normalizeName(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}

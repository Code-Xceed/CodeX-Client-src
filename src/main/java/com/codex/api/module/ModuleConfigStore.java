package com.codex.api.module;

import com.codex.api.value.BoolValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.api.value.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Lightweight properties-based module persistence to keep module state stable
 * across client restarts without introducing extra runtime dependencies.
 */
public final class ModuleConfigStore {
    private static final String MODULE_PREFIX = "module.";
    private static final Set<String> FAVORITE_MODULE_KEYS = Collections.synchronizedSet(new HashSet<String>());

    private ModuleConfigStore() {}

    public static boolean load(ModuleManager manager, Path configPath) {
        if (configPath == null || !Files.exists(configPath)) {
            return false;
        }

        FAVORITE_MODULE_KEYS.clear();
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException ioException) {
            return false;
        }

        for (Module module : manager.getModules()) {
            String modulePrefix = resolveModulePrefix(properties, module);
            String enabledRaw = properties.getProperty(modulePrefix + ".enabled");
            String keyRaw = properties.getProperty(modulePrefix + ".key");
            String favoriteRaw = properties.getProperty(modulePrefix + ".favorite");

            if (keyRaw != null) {
                try {
                    module.setKey(Integer.parseInt(keyRaw));
                } catch (NumberFormatException ignored) {}
            }
            if (favoriteRaw != null) {
                if (Boolean.parseBoolean(favoriteRaw)) {
                    FAVORITE_MODULE_KEYS.add(favoriteKey(module));
                } else {
                    FAVORITE_MODULE_KEYS.remove(favoriteKey(module));
                }
            }

            for (Value<?> value : module.getValues()) {
                String valueRaw = properties.getProperty(modulePrefix + ".value." + key(value.getName()));
                if (valueRaw != null) {
                    applyValue(value, valueRaw);
                }
            }

            if (enabledRaw != null) {
                module.setEnabled(Boolean.parseBoolean(enabledRaw));
            } else if (module.isDefaultEnabled()) {
                module.setEnabled(true);
            }
        }

        return true;
    }

    public static boolean save(ModuleManager manager, Path configPath) {
        if (configPath == null) {
            return false;
        }

        Path parent = configPath.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ignored) {
            return false;
        }

        Properties properties = new Properties();
        for (Module module : manager.getModules()) {
            String modulePrefix = moduleKey(module);
            properties.setProperty(modulePrefix + ".enabled", Boolean.toString(module.isEnabled()));
            properties.setProperty(modulePrefix + ".key", Integer.toString(module.getKey()));
            properties.setProperty(modulePrefix + ".favorite", Boolean.toString(isFavorite(module)));

            for (Value<?> value : module.getValues()) {
                properties.setProperty(modulePrefix + ".value." + key(value.getName()), serialize(value.get()));
            }
        }

        Path tempPath = configPath.resolveSibling(configPath.getFileName().toString() + ".tmp");
        try (OutputStream outputStream = Files.newOutputStream(tempPath)) {
            properties.store(outputStream, "CodeX module settings");
        } catch (IOException ioException) {
            return false;
        }

        try {
            Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (AtomicMoveNotSupportedException notSupported) {
            try {
                Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException ioException) {
                safeDelete(tempPath);
                return false;
            }
        } catch (IOException ioException) {
            safeDelete(tempPath);
            return false;
        }
    }

    private static void applyValue(Value<?> value, String rawValue) {
        try {
            if (value instanceof BoolValue) {
                if ("true".equalsIgnoreCase(rawValue) || "false".equalsIgnoreCase(rawValue)) {
                    ((BoolValue) value).set(Boolean.parseBoolean(rawValue));
                }
                return;
            }

            if (value instanceof NumberValue) {
                ((NumberValue) value).set(Double.parseDouble(rawValue));
                return;
            }

            if (value instanceof ModeValue) {
                ((ModeValue) value).set(rawValue);
                return;
            }

            if (value.get() instanceof String) {
                @SuppressWarnings("unchecked")
                Value<String> stringValue = (Value<String>) value;
                stringValue.set(rawValue);
            }
        } catch (RuntimeException ignored) {
            // Keep loading resilient even if one value is malformed.
        }
    }

    private static String serialize(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String moduleKey(Module module) {
        return MODULE_PREFIX + key(moduleIdentifier(module));
    }

    private static String legacyModuleKey(Module module) {
        return MODULE_PREFIX + key(module.getName());
    }

    private static String resolveModulePrefix(Properties properties, Module module) {
        String current = moduleKey(module);
        if (hasModuleData(properties, current)) {
            return current;
        }

        String legacy = legacyModuleKey(module);
        if (hasModuleData(properties, legacy)) {
            return legacy;
        }

        return current;
    }

    private static boolean hasModuleData(Properties properties, String modulePrefix) {
        if (properties.containsKey(modulePrefix + ".enabled")
            || properties.containsKey(modulePrefix + ".key")
            || properties.containsKey(modulePrefix + ".favorite")) {
            return true;
        }

        String valuePrefix = modulePrefix + ".value.";
        for (String propertyKey : properties.stringPropertyNames()) {
            if (propertyKey.startsWith(valuePrefix)) {
                return true;
            }
        }

        return false;
    }

    private static String moduleIdentifier(Module module) {
        String className = module.getClass().getName();
        if (className == null || className.trim().isEmpty()) {
            return module.getName();
        }
        return className;
    }

    private static String favoriteKey(Module module) {
        return key(moduleIdentifier(module));
    }

    private static String legacyFavoriteKey(Module module) {
        return key(module.getName());
    }

    public static boolean isFavorite(Module module) {
        if (module == null) {
            return false;
        }
        String current = favoriteKey(module);
        if (FAVORITE_MODULE_KEYS.contains(current)) {
            return true;
        }
        return FAVORITE_MODULE_KEYS.contains(legacyFavoriteKey(module));
    }

    public static void setFavorite(Module module, boolean favorite) {
        if (module == null) {
            return;
        }
        String moduleKey = favoriteKey(module);
        String legacyKey = legacyFavoriteKey(module);
        if (favorite) {
            FAVORITE_MODULE_KEYS.add(moduleKey);
            FAVORITE_MODULE_KEYS.remove(legacyKey);
        } else {
            FAVORITE_MODULE_KEYS.remove(moduleKey);
            FAVORITE_MODULE_KEYS.remove(legacyKey);
        }
    }

    public static boolean toggleFavorite(Module module) {
        boolean next = !isFavorite(module);
        setFavorite(module, next);
        return next;
    }

    private static String key(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
        return normalized.replaceAll("[^a-z0-9_\\-]", "");
    }

    private static void safeDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {}
    }
}

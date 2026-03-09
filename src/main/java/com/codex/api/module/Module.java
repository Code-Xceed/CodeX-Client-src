package com.codex.api.module;

import com.codex.api.event.EventManager;
import com.codex.api.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final boolean defaultEnabled;
    private final boolean canBind;
    private int key;
    private boolean enabled;
    private final List<Value<?>> values = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this(name, description, category, false, true);
    }

    public Module(String name, String description, Category category, boolean defaultEnabled) {
        this(name, description, category, defaultEnabled, true);
    }

    public Module(String name, String description, Category category, boolean defaultEnabled, boolean canBind) {
        this.name = validate(name, "name");
        this.description = validate(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.defaultEnabled = defaultEnabled;
        this.canBind = canBind;
    }

    private static String validate(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public void onKeyPressed() {
        toggle();
    }
    
    public void onKeyReleased() {
        // Optional override for hold-based modules
    }
    
    public void onKeyHold() {
        // Optional override for hold-based modules
    }

    public final synchronized void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        if (enabled) {
            this.enabled = true;
            EventManager.register(this);
            try {
                onEnable();
            } catch (RuntimeException ex) {
                this.enabled = false;
                EventManager.unregister(this);
                throw ex;
            }
        } else {
            RuntimeException thrown = null;
            try {
                onDisable();
            } catch (RuntimeException ex) {
                thrown = ex;
            } finally {
                this.enabled = false;
                EventManager.unregister(this);
            }
            if (thrown != null) {
                throw thrown;
            }
        }
    }

    protected void onEnable() {}
    protected void onDisable() {}

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        if (key < 0) {
            throw new IllegalArgumentException("key must be non-negative");
        }
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public boolean canBind() {
        return canBind;
    }

    protected final void addValue(Value<?> value) {
        values.add(Objects.requireNonNull(value, "value"));
    }

    public List<Value<?>> getValues() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public String toString() {
        return "Module{" +
            "name='" + name + '\'' +
            ", category=" + category +
            ", enabled=" + enabled +
            '}';
    }
}

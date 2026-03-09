package com.codex.api.value;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class Value<T> {
    protected final String name;
    protected T value;
    protected final Supplier<Boolean> visibility;
    private String group = null;

    public Value(String name, T value, Supplier<Boolean> visibility) {
        Objects.requireNonNull(name, "name");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        this.name = name;
        this.value = Objects.requireNonNull(value, "value");
        this.visibility = Objects.requireNonNull(visibility, "visibility");
    }

    public Value(String name, T value) {
        this(name, value, () -> true);
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return visibility.get();
    }

    public String getGroup() {
        return group;
    }

    public Value<T> setGroup(String group) {
        this.group = group;
        return this;
    }
}

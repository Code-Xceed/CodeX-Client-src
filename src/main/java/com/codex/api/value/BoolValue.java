package com.codex.api.value;

import java.util.function.Supplier;

public class BoolValue extends Value<Boolean> {
    public BoolValue(String name, boolean value) {
        super(name, value);
    }
    
    public BoolValue(String name, boolean value, Supplier<Boolean> visibility) {
        super(name, value, visibility);
    }

    public void toggle() {
        this.value = !this.value;
    }
}

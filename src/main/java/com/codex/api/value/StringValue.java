package com.codex.api.value;

import java.util.function.Supplier;

public class StringValue extends Value<String> {
    public StringValue(String name, String value) {
        super(name, value);
    }
    
    public StringValue(String name, String value, Supplier<Boolean> visibility) {
        super(name, value, visibility);
    }
}

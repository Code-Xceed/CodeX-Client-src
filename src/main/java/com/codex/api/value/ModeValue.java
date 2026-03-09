package com.codex.api.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ModeValue extends Value<String> {
    private final List<String> modes;

    public ModeValue(String name, String value, String... modes) {
        super(name, value);
        if (modes == null || modes.length == 0) {
            throw new IllegalArgumentException("modes cannot be empty");
        }

        List<String> safeModes = new ArrayList<String>(modes.length);
        for (String mode : modes) {
            if (mode == null || mode.trim().isEmpty()) {
                throw new IllegalArgumentException("mode cannot be blank");
            }
            safeModes.add(mode);
        }
        this.modes = Collections.unmodifiableList(safeModes);
        set(value);
    }

    @Override
    public void set(String value) {
        String canonical = canonicalMode(value);
        if (canonical == null) {
            throw new IllegalArgumentException("Unsupported mode: " + value);
        }
        super.set(canonical);
    }

    private String canonicalMode(String value) {
        if (value == null) {
            return null;
        }
        for (String mode : modes) {
            if (mode.equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return null;
    }

    public List<String> getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return mode != null && this.value.toLowerCase(Locale.ROOT).equals(mode.toLowerCase(Locale.ROOT));
    }
}

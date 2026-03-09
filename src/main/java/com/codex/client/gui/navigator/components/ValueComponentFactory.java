package com.codex.client.gui.navigator.components;

import com.codex.api.value.BoolValue;
import com.codex.api.value.ColorValue;
import com.codex.api.value.ModeValue;
import com.codex.api.value.NumberValue;
import com.codex.api.value.StringValue;
import com.codex.api.value.Value;

public final class ValueComponentFactory {
    private ValueComponentFactory() {}

    public static ValueComponent<?> create(Value<?> value) {
        if (value instanceof BoolValue boolValue) {
            return new BoolComponent(boolValue);
        }
        if (value instanceof NumberValue numberValue) {
            return new NumberComponent(numberValue);
        }
        if (value instanceof ModeValue modeValue) {
            return new ModeComponent(modeValue);
        }
        if (value instanceof StringValue stringValue) {
            return new StringComponent(stringValue);
        }
        if (value instanceof ColorValue colorValue) {
            return new ColorComponent(colorValue);
        }
        return null;
    }
}

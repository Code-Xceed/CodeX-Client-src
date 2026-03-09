package com.codex.api.value;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueModelTest {

    @Test
    void numberValueClampsAndSnapsToIncrement() {
        NumberValue value = new NumberValue("Speed", 0.0D, 0.0D, 10.0D, 0.5D);
        value.set(1.26D);
        assertEquals(1.5D, value.asDouble(), 0.0001D);

        value.set(12.0D);
        assertEquals(10.0D, value.asDouble(), 0.0001D);
    }

    @Test
    void modeValueRejectsInvalidMode() {
        ModeValue modeValue = new ModeValue("Mode", "A", "A", "B");
        modeValue.set("b");
        assertEquals("B", modeValue.get());

        assertThrows(IllegalArgumentException.class, () -> modeValue.set("C"));
    }
}

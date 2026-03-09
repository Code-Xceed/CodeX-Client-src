package com.codex.api.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventManagerTest {

    @AfterEach
    void tearDown() {
        EventManager.clear();
    }

    @Test
    void listenersFireByPriority() {
        List<String> calls = new ArrayList<String>();
        Listener listener = new Listener(calls);
        EventManager.register(listener);

        EventManager.call(new TestEvent());

        assertEquals(2, calls.size());
        assertEquals("high", calls.get(0));
        assertEquals("low", calls.get(1));
    }

    @Test
    void unregisterStopsEvents() {
        List<String> calls = new ArrayList<String>();
        Listener listener = new Listener(calls);
        EventManager.register(listener);
        EventManager.unregister(listener);

        EventManager.call(new TestEvent());

        assertEquals(0, calls.size());
    }

    private static final class Listener {
        private final List<String> calls;

        private Listener(List<String> calls) {
            this.calls = calls;
        }

        @EventTarget(1)
        public void onHigh(TestEvent event) {
            calls.add("high");
        }

        @EventTarget(5)
        public void onLow(TestEvent event) {
            calls.add("low");
        }
    }

    private static final class TestEvent extends Event {}
}

package com.codex.api.event.events;

import com.codex.api.event.Event;

public class KeyEvent extends Event {
    private final int key;
    private final int action;

    public KeyEvent(int key, int action) {
        this.key = key;
        this.action = action;
    }

    public int getKey() {
        return key;
    }

    public int getAction() {
        return action;
    }

    public boolean isPressed() {
        return action == 1;
    }

    public boolean isReleased() {
        return action == 0;
    }
}

package com.codex.api.event.events;

import com.codex.api.event.Event;

public class MouseEvent extends Event {
    private final int button;
    private final int action;

    public MouseEvent(int button, int action) {
        this.button = button;
        this.action = action;
    }

    public int getButton() {
        return button;
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

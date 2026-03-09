package com.codex.api.event.events;

import com.codex.api.event.Event;

public class Render2DEvent extends Event {
    private final float tickDelta;

    public Render2DEvent(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}

package com.codex.api.event.events;

import com.codex.api.event.Event;

public class MouseUpdateEvent extends Event {
    private double deltaX;
    private double deltaY;

    public MouseUpdateEvent(double deltaX, double deltaY) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public double getDeltaX() {
        return deltaX;
    }

    public void setDeltaX(double deltaX) {
        this.deltaX = deltaX;
    }

    public double getDeltaY() {
        return deltaY;
    }

    public void setDeltaY(double deltaY) {
        this.deltaY = deltaY;
    }
}

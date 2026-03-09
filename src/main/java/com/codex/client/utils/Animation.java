package com.codex.client.utils;

import com.codex.client.gui.navigator.settings.GuiSettings;

public class Animation {
    private float currentValue;
    private float targetValue;
    private float speed;

    public Animation(float initialValue, float speed) {
        this.currentValue = initialValue;
        this.targetValue = initialValue;
        this.speed = speed;
    }

    public void setTarget(float targetValue) {
        this.targetValue = targetValue;
    }

    public float getTarget() {
        return targetValue;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getValue() {
        return currentValue;
    }

    public void setValue(float value) {
        this.currentValue = value;
    }

    public float update(float partialTicks) {
        GuiSettings settings = GuiSettings.get();
        if (!settings.isAnimationsEnabled()) {
            currentValue = targetValue;
            return currentValue;
        }

        if (Math.abs(targetValue - currentValue) < 0.001f) {
            currentValue = targetValue;
            return currentValue;
        }
        float effectiveSpeed = (float) Math.max(0.001f, Math.min(0.99f, speed * settings.getAnimationSpeedMultiplier()));
        float scaledStep = 1.0f - (float) Math.pow(1.0f - effectiveSpeed, Math.max(1.0f, partialTicks + 1.0f));
        currentValue += (targetValue - currentValue) * scaledStep;
        return currentValue;
    }
}

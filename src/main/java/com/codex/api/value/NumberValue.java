package com.codex.api.value;

public class NumberValue extends Value<Number> {
    private final double min;
    private final double max;
    private final double increment;

    public NumberValue(String name, Number value, Number min, Number max, Number increment) {
        super(name, value);
        this.min = min.doubleValue();
        this.max = max.doubleValue();
        this.increment = increment.doubleValue();

        if (this.min > this.max) {
            throw new IllegalArgumentException("min cannot be greater than max");
        }
        if (this.increment <= 0.0D) {
            throw new IllegalArgumentException("increment must be > 0");
        }

        set(value);
    }

    @Override
    public void set(Number value) {
        super.set(normalize(value.doubleValue()));
    }

    private Number normalize(double input) {
        double clamped = Math.max(min, Math.min(max, input));
        double steps = Math.round((clamped - min) / increment);
        double snapped = min + (steps * increment);
        return Math.max(min, Math.min(max, snapped));
    }

    public Number getMin() {
        return min;
    }

    public Number getMax() {
        return max;
    }

    public Number getIncrement() {
        return increment;
    }

    public double asDouble() {
        return get().doubleValue();
    }
}

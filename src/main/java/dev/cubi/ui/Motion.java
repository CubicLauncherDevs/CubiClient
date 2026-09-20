package dev.cubi.ui;

/** Time-based easing, independent of the display's refresh rate. */
public final class Motion {
    private float value;
    private long previous;
    public float to(float target) {
        long now = System.nanoTime();
        float elapsed = previous == 0 ? 1 : Math.min(0.1f, (now - previous) / 1_000_000_000f);
        previous = now;
        value += (target - value) * (1 - (float) Math.exp(-elapsed * 22));
        return value;
    }
}

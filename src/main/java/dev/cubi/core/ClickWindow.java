package dev.cubi.core;

/** Bounded, allocation-free rolling second. Called on Minecraft's main thread. */
public final class ClickWindow {
    private final long[] times = new long[256];
    private int start, count;

    public void press(long now) {
        expire(now);
        if (count == times.length) { start = (start + 1) & 255; count--; }
        times[(start + count) & 255] = now;
        count++;
    }

    public int count(long now) { expire(now); return count; }
    public void clear() { start = 0; count = 0; }

    private void expire(long now) {
        while (count > 0 && now - times[start] >= 1_000_000_000L) {
            start = (start + 1) & 255;
            count--;
        }
    }
}

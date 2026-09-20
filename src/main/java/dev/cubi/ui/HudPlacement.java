package dev.cubi.ui;

/** GUI-coordinate placement shared by the editor and its boundary tests. */
public final class HudPlacement {
    private HudPlacement() { }
    public static float snap(float position, float size, float screen, float threshold) {
        float best = position, distance = threshold;
        for (int i = 0; i < 3; i++) {
            float candidate = i == 0 ? 12 : i == 1 ? (screen - size) / 2 : screen - size - 12;
            float delta = Math.abs(position - candidate);
            if (delta <= distance) { best = candidate; distance = delta; }
        }
        return Math.max(0, Math.min(Math.max(0, screen - size), best));
    }
    public static float guide(float position, float size, float screen) {
        if (Math.abs(position - 12) < 0.6f) return 12;
        if (Math.abs(position + size / 2 - screen / 2) < 0.6f) return screen / 2;
        if (Math.abs(position + size - (screen - 12)) < 0.6f) return screen - 12;
        return -1;
    }

    /** Lift the watermark above the hotbar when its full name cannot fit to the left. */
    public static int watermarkY(int width, int height, float rightEdge) {
        return Math.max(0, height - (rightEdge + 6 >= width / 2f - 91 ? 38 : 23));
    }
}

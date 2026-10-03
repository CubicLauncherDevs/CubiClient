package dev.cubi.camera;

/** One update per rendered frame; FOV and both mouse axes consume the same snapshot. */
public final class ZoomController {
    private Object world;
    private int key;
    private long previous;
    private boolean hasTime, blocked;
    private float scale = 1, mouseScale = 1;

    public void update(ZoomSettings settings, Object currentWorld, boolean eligible, boolean down, long now) {
        double seconds = hasTime ? Math.max(0, Math.min(0.25, (now - previous) / 1_000_000_000.0)) : 0;
        previous = now; hasTime = true;
        if (world != currentWorld || key != settings.key) {
            world = currentWorld; key = settings.key;
            scale = 1; blocked = down;
        }
        if (!eligible || currentWorld == null || !settings.enabled || settings.key == 0) {
            scale = mouseScale = 1;
            blocked = down;
            return;
        }
        if (!down) blocked = false;
        float target = down && !blocked ? 1 / settings.magnification : 1;
        if (settings.smooth) {
            scale += (target - scale) * (float) (1 - Math.exp(-seconds * 22));
            if (Math.abs(scale - target) < 0.0001f) scale = target;
        } else scale = target;
        mouseScale = settings.adaptSensitivity ? scale : 1;
    }

    public void cancel() { scale = mouseScale = 1; blocked = true; }

    /** Optical magnification, applied only to the world projection, never the hand or HUD. */
    public float fov(float vanilla, boolean worldProjection) {
        if (!worldProjection || scale == 1 || !Float.isFinite(vanilla) || vanilla <= 0 || vanilla >= 180) return vanilla;
        return (float) Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(vanilla) / 2) * scale));
    }
    public float mouse(float delta) { return delta * mouseScale; }
}

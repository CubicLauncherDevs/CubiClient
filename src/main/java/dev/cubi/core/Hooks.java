package dev.cubi.core;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Main-thread hooks. A failure is logged once and disables the overlay, not a render-loop log flood. */
public final class Hooks {
    private static CubiClient client;
    private static boolean failed;
    private static long consumedKey = Long.MIN_VALUE;
    private static boolean particlePass, particleFailed;
    private static boolean zoomFailed;
    private Hooks() { }

    private static CubiClient client() throws Throwable {
        if (failed) return null;
        if (client == null) client = new CubiClient();
        return client;
    }

    public static void fail(Throwable error) {
        if (!failed) {
            System.err.println("[Cubi] Error de integración. HUD desactivado; consulta este registro:");
            error.printStackTrace();
        }
        failed = true;
    }

    public static void tick() {
        try { CubiClient c = client(); if (c != null) c.tick(); } catch (Throwable error) { fail(error); }
    }

    public static void key() {
        try {
            if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) return;
            if (consumedKey == Keyboard.getEventNanoseconds()) return;
            consumeKey();
            CubiClient c = client();
            if (c != null) c.key(Keyboard.getEventKey());
        } catch (Throwable error) { fail(error); }
    }

    public static void consumeKey() { consumedKey = Keyboard.getEventNanoseconds(); }

    /** Called before vanilla mouse processing, not from the FOV getter (which runs multiple times). */
    public static void zoomFrame() {
        if (client == null || failed || zoomFailed) return;
        try {
            int key = client.config.zoom.key;
            boolean down = key > 1 && key < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(key);
            client.cameraZoom.update(client.config.zoom, client.game.worldIdentity(),
                    client.game.cameraInputActive() && client.config.zoomKeyAvailable(key), down, System.nanoTime());
        } catch (Throwable error) {
            client.cameraZoom.cancel(); zoomFailed = true;
            System.err.println("[Cubi] Zoom desactivado por un error de integración.");
            error.printStackTrace();
        }
    }
    public static float zoomFov(float vanilla, boolean worldProjection) {
        return client == null || failed || zoomFailed ? vanilla : client.cameraZoom.fov(vanilla, worldProjection);
    }
    public static float zoomMouse(float delta) {
        return client == null || failed || zoomFailed ? delta : client.cameraZoom.mouse(delta);
    }

    public static boolean mouse(boolean hasEvent) {
        try {
            if (hasEvent && !failed && client != null && client.modules.keys.state.enabled
                    && Mouse.getEventButton() >= 0 && Mouse.getEventButton() <= 1 && Mouse.getEventButtonState()
                    && client.game.screen() == null && client.game.inWorld()) {
                int button = Mouse.getEventButton();
                if (button == 0) client.left.press(System.nanoTime());
                if (button == 1) client.right.press(System.nanoTime());
            }
        } catch (Throwable error) { fail(error); }
        return hasEvent;
    }

    public static void hud() {
        try {
            CubiClient c = client();
            if (c == null || !c.modules.visible(c) || !c.game.inWorld() || !c.game.hudVisible() || c.game.screen() != null) return;
            long start = System.nanoTime();
            try { c.modules.render(c); } finally { c.game.white(); }
            c.recordRender(System.nanoTime() - start);
        } catch (Throwable error) { fail(error); }
    }

    public static void frameBegin() {
        particlePass = false;
        if (client == null || failed || !client.capture.active()) return;
        try {
            boolean eligible = client.captureEligible();
            client.capture.begin(System.nanoTime(), eligible);
        } catch (Throwable error) { captureFailure(error); }
    }
    public static void frameEnd() {
        particlePass = false;
        if (client == null || failed || !client.capture.active()) return;
        try {
            long now = System.nanoTime();
            client.capture.end(now, client.captureEligible());
        } catch (Throwable error) { captureFailure(error); }
    }
    public static void stageBegin(int stage) {
        if (client != null && !failed && client.capture.active()) client.capture.stageBegin(stage, System.nanoTime());
    }
    public static void stageEnd(int stage) {
        if (client != null && !failed && client.capture.active()) client.capture.stageEnd(stage, System.nanoTime());
    }
    private static void captureFailure(Throwable error) {
        client.capture.stop("Error de diagnóstico");
        System.err.println("[Cubi] Captura detenida: " + error);
    }

    public static boolean particleCullingAvailable() { return !particleFailed; }
    public static void particlesBegin() {
        particlePass = false;
        if (client == null || failed || particleFailed || !client.config.performance.particleCulling) return;
        try { client.game.beginParticles(); particlePass = true; }
        catch (Throwable error) { particleFailure(error); }
    }
    public static void particlesEnd() { particlePass = false; }
    public static boolean particleVisible(Object particle, float partial, float rx, float rxz, float rz, float ryz, float rxy) {
        if (!particlePass) return true;
        try { return client.game.particleVisible(particle, partial, rx, rxz, rz, ryz, rxy); }
        catch (Throwable error) { particleFailure(error); return true; }
    }
    private static void particleFailure(Throwable error) {
        particlePass = false; particleFailed = true;
        System.err.println("[Cubi] Culling de partículas desactivado; se conserva el render vanilla.");
        error.printStackTrace();
    }

    public static boolean homeDraw(Object screen, int x, int y, float partial) {
        try {
            CubiClient c = client();
            if (c == null) return false;
            c.home.draw(screen, x, y, partial);
            return true;
        } catch (Throwable error) { fail(error); return false; }
    }

    public static boolean homeClick(Object screen, int x, int y, int button) {
        try {
            CubiClient c = client();
            if (c == null) return false;
            c.home.click(screen, x, y, button);
        } catch (Throwable error) { fail(error); }
        return true; // Never pass the failing click through to hidden vanilla buttons.
    }

    public static boolean homeType(Object screen, char character, int key) {
        try {
            CubiClient c = client();
            if (c == null) return false;
            c.home.type(screen, key);
        } catch (Throwable error) { fail(error); }
        return true;
    }
}

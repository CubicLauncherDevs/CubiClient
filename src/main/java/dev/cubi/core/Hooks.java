package dev.cubi.core;

import dev.cubi.ui.Ink;
import dev.cubi.ui.Theme;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Main-thread hooks. A failure is logged once and disables the overlay, not a render-loop log flood. */
public final class Hooks {
    private static CubiClient client;
    private static boolean failed;
    private static long consumedKey = Long.MIN_VALUE;
    private static boolean particlePass, particleFailed;
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

    public static void brand() {
        try {
            CubiClient c = client();
            if (c == null) return;
            c.ink.round(10, 10, 154, 37, Theme.CARD_RADIUS, Ink.alpha(Theme.BACKGROUND, 0.75f));
            c.ink.border(10, 10, 154, 37, Theme.CARD_RADIUS, Theme.BORDER);
            c.ink.icon(0, 19, 19, 19, c.accent());
            c.ink.text(ClientIdentity.NAME, 46, 17, 14, true, Ink.WHITE);
            c.ink.small(Keyboard.getKeyName(c.config.menuKey) + "  /  Personalizar", 46, 34, Ink.MUTED);
            c.game.white();
        } catch (Throwable error) { fail(error); }
    }
}

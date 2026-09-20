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
            if (hasEvent && !failed && client != null && Mouse.getEventButtonState()
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
            if (c == null || !c.game.inWorld() || !c.game.hudVisible() || c.game.screen() != null) return;
            long start = System.nanoTime();
            try { c.modules.render(c); } finally { c.game.white(); }
            c.recordRender(System.nanoTime() - start);
        } catch (Throwable error) { fail(error); }
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

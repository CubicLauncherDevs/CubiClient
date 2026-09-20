package dev.cubi.module;

import dev.cubi.core.CubiClient;
import dev.cubi.config.ClientConfig;
import dev.cubi.ui.Ink;
import dev.cubi.ui.Motion;
import dev.cubi.ui.Theme;
import org.lwjgl.input.Mouse;

public final class KeysModule extends HudModule {
    private final String[] labels = {"W", "A", "S", "D", "SPACE"};
    private int ticks;
    private final Motion[] presses = {new Motion(), new Motion(), new Motion(), new Motion(), new Motion(), new Motion(), new Motion()};
    public KeysModule(ClientConfig config) { super(config, "keys", "Keystrokes", "Teclas, ratón y CPS", "02", 82, 94); }

    @Override
    public void tick(CubiClient client) throws Throwable {
        if (ticks++ % 20 == 0) {
            for (int i = 0; i < labels.length; i++) {
                String name = client.game.movementName(i);
                labels[i] = name.length() > (i == 4 ? 12 : 4) ? name.substring(0, i == 4 ? 12 : 4) : name;
            }
        }
    }

    @Override
    public void paint(CubiClient client, Ink ink, boolean preview) throws Throwable {
        key(client, ink, 0, labels[0], 28, 0, 26, 26, preview || client.game.movementDown(0));
        for (int i = 1; i <= 3; i++) key(client, ink, i, labels[i], (i - 1) * 28, 28, 26, 26,
                preview ? i == 3 : client.game.movementDown(i));
        key(client, ink, 5, "LMB", 0, 56, 40, 19, !preview && Mouse.isButtonDown(0), preview ? "8 CPS" : client.leftLabel);
        key(client, ink, 6, "RMB", 42, 56, 40, 19, !preview && Mouse.isButtonDown(1), preview ? "6 CPS" : client.rightLabel);
        key(client, ink, 4, labels[4], 0, 77, 82, 17, !preview && client.game.movementDown(4));
    }

    private void key(CubiClient client, Ink ink, int index, String label, int x, int y, int width, int height, boolean pressed) throws Throwable {
        key(client, ink, index, label, x, y, width, height, pressed, null);
    }

    private void key(CubiClient client, Ink ink, int index, String label, int x, int y, int width, int height, boolean pressed, String cps) throws Throwable {
        float amount = presses[index].to(pressed ? 1 : 0);
        int idle = Ink.alpha(Theme.BACKGROUND, state.background ? state.opacity : 0);
        if (state.shadow && state.background && state.opacity > 0) ink.round(x, y + 1, width, height, Theme.HUD_RADIUS,
                Ink.alpha(Theme.HUD_SHADOW, (Theme.HUD_SHADOW >>> 24) / 255f * state.opacity / 0.48f));
        ink.round(x, y, width, height, Theme.HUD_RADIUS, Ink.mix(idle, client.accent(), amount));
        int idleBorder = Ink.alpha(Theme.BORDER_HOVER, state.background ? state.opacity : 0);
        ink.border(x, y, width, height, Theme.HUD_RADIUS, Ink.mix(idleBorder, client.accent(), amount));
        int text = Ink.mix(Ink.WHITE, Ink.DARK, amount);
        if (cps != null) {
            keyText(ink, label, x, y + 1.5f, width, 7, text, amount);
            keyText(ink, cps, x, y + 10, width, 7, text, amount);
        } else if (index == 4 && label.equals("SPACE")) {
            ink.round(x + 28, y + 8, 26, 1.5f, 0.75f, text);
        } else {
            float size = index < 4 ? 11 : 8;
            keyText(ink, label, x, y + (height - size) / 2, width, size, text, amount);
        }
    }

    private void keyText(Ink ink, String label, float x, float y, int width, float size, int color, float amount) throws Throwable {
        float labelWidth = ink.width(label, size, true);
        if (labelWidth > width - 5) size *= (width - 5) / labelWidth;
        float left = x + (width - ink.width(label, size, true)) / 2;
        if (state.shadow && amount < 0.1f) ink.text(label, left + 0.5f, y + 0.6f, size, true, Theme.TEXT_SHADOW);
        ink.text(label, left, y, size, true, color);
    }
}

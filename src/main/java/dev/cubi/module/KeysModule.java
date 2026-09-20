package dev.cubi.module;

import dev.cubi.core.CubiClient;
import dev.cubi.ui.Ink;
import dev.cubi.ui.Motion;
import org.lwjgl.input.Mouse;

public final class KeysModule extends HudModule {
    private final String[] labels = {"W", "A", "S", "D", "SPACE"};
    private int ticks;
    private final Motion[] presses = {new Motion(), new Motion(), new Motion(), new Motion(), new Motion(), new Motion(), new Motion()};
    public KeysModule(CubiClient client) { super(client, "keys", "Keystrokes", "Tu movimiento, visible", "03", 82, 94); }

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
        key(client, ink, 5, "LMB", 0, 56, 40, 19, !preview && Mouse.isButtonDown(0));
        key(client, ink, 6, "RMB", 42, 56, 40, 19, !preview && Mouse.isButtonDown(1));
        key(client, ink, 4, labels[4], 0, 77, 82, 17, !preview && client.game.movementDown(4));
    }

    private void key(CubiClient client, Ink ink, int index, String label, int x, int y, int width, int height, boolean pressed) throws Throwable {
        float amount = presses[index].to(pressed ? 1 : 0);
        int idle = Ink.alpha(0xFF0C0E13, state.background ? state.opacity : 0);
        if (state.shadow && state.background) ink.round(x, y + 1, width, height, 4, 0x26000000);
        ink.round(x, y, width, height, 4, Ink.mix(idle, client.accent(), amount));
        int text = Ink.mix(Ink.WHITE, Ink.DARK, amount);
        if (index == 4 && label.equals("SPACE")) {
            ink.round(x + 28, y + 8, 26, 1.5f, 0.75f, text);
        } else {
            float size = index < 4 ? 11 : 8;
            float labelWidth = ink.width(label, size, true);
            if (labelWidth > width - 5) size *= (width - 5) / labelWidth;
            float left = x + (width - ink.width(label, size, true)) / 2;
            float top = y + (height - size) / 2;
            if (state.shadow && amount < 0.1f) ink.text(label, left + 0.5f, top + 0.6f, size, true, 0x77000000);
            ink.text(label, left, top, size, true, text);
        }
    }
}

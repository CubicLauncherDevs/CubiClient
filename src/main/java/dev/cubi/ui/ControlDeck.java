package dev.cubi.ui;

import dev.cubi.config.ClientConfig;
import dev.cubi.core.CubiClient;
import dev.cubi.core.Hooks;
import dev.cubi.module.HudModule;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Minimal module browser and on-world editor. All interaction stays on the game thread. */
public final class ControlDeck {
    private static final int WIDTH = 520, HEIGHT = 352;
    private static final Motion[] hover = {new Motion(), new Motion(), new Motion()};
    private static final Motion[] enabled = {new Motion(), new Motion(), new Motion()};
    private static final Motion backgroundSwitch = new Motion(), shadowSwitch = new Motion();
    private static CubiClient client;
    private static Object previous;
    private static boolean editor, dragging, opacityDrag;
    private static int selected, capture = -1, offsetX, offsetY;
    private static float zoom, originX, originY;
    private static long opened;

    private ControlDeck() { }
    public static void open(CubiClient instance, Object parent) {
        client = instance; previous = parent;
        editor = false; dragging = false; opacityDrag = false; capture = -1;
        selected = Math.min(selected, client.modules.all.length - 1);
        opened = System.nanoTime();
    }

    public static void draw(int mouseX, int mouseY, float partialTicks) {
        if (client == null) return;
        try {
            client.game.resize();
            if (editor) drawEditor(mouseX, mouseY); else drawDeck(mouseX, mouseY);
        } catch (Throwable error) { recover(error); }
        finally {
            client.ink.opacity = 1;
            try { client.game.white(); } catch (Throwable error) { Hooks.fail(error); }
        }
    }

    private static void layout() {
        zoom = Math.max(0.1f, Math.min(1, Math.min((client.game.width - 16f) / WIDTH, (client.game.height - 16f) / HEIGHT)));
        originX = (client.game.width - WIDTH * zoom) / 2;
        originY = (client.game.height - HEIGHT * zoom) / 2;
    }

    private static void drawDeck(int mouseX, int mouseY) throws Throwable {
        layout();
        int mx = (int) ((mouseX - originX) / zoom), my = (int) ((mouseY - originY) / zoom);
        if (opacityDrag) {
            if (Mouse.isButtonDown(0)) opacityAt(mx);
            else { opacityDrag = false; client.config.save(); }
        }
        Ink ink = client.ink;
        ink.rect(0, 0, client.game.width, client.game.height, client.game.inWorld() ? 0x86060910 : 0xFF0A0C11);
        ink.opacity = Math.min(1, (System.nanoTime() - opened) / 160_000_000f);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(originX, originY, 0); GL11.glScalef(zoom, zoom, 1);
            ink.round(-2, 5, WIDTH + 4, HEIGHT, 14, 0x4A000000);
            ink.round(0, 0, WIDTH, HEIGHT, 12, Ink.LINE);
            ink.round(0.6f, 0.6f, WIDTH - 1.2f, HEIGHT - 1.2f, 11.5f, Ink.BACKGROUND);
            ink.icon(0, 22, 21, 23, client.accent());
            ink.text("cubi", 52, 22, 20, true, Ink.WHITE);
            ink.round(94, 25, 36, 16, 4, Ink.CARD);
            ink.center("1.8.9", 94, 29, 36, 8, true, Ink.MUTED);
            action("Editar HUD", 356, 20, 112, 28, mx, my, true);
            action("×", 476, 20, 26, 28, mx, my, false);
            ink.text("Hazlo tuyo.", 22, 67, 23, true, Ink.WHITE);
            ink.text("Los detalles hacen la diferencia.", 176, 77, 10, false, Ink.MUTED);

            for (int i = 0; i < client.modules.all.length; i++) {
                HudModule module = client.modules.all[i];
                int x = 22 + i * 164;
                float over = hover[i].to(Ink.inside(mx, my, x, 112, 148, 112) ? 1 : 0);
                int border = selected == i ? Ink.alpha(client.accent(), 0.65f) : Ink.mix(Ink.LINE, Ink.MUTED, over * 0.35f);
                ink.round(x, 112, 148, 112, 8, border);
                ink.round(x + 0.7f, 112.7f, 146.6f, 110.6f, 7.3f, Ink.mix(Ink.PANEL, Ink.CARD, over * 0.6f));
                ink.icon(i + 1, x + 12, 125, 16, selected == i ? client.accent() : Ink.MUTED);
                ink.text(module.title, x + 35, 128, 11, true, Ink.WHITE);
                ink.toggle(x + 110, 125, enabled[i].to(module.state.enabled ? 1 : 0), client.accent());
                float previewScale = i == 2 ? 0.55f : 1.05f;
                module.renderAt(client, x + (148 - module.width * previewScale) / 2, 150 + (52 - module.height * previewScale) / 2, previewScale, true);
                ink.round(x + 13, 211, 4, 4, 2, module.state.enabled ? client.accent() : Ink.MUTED);
                ink.small(module.state.enabled ? "Activado" : "Desactivado", x + 22, 209, Ink.MUTED);
                ink.small(selected == i ? "Editando" : "Ajustes", x + 106, 209, selected == i ? client.accent() : Ink.MUTED);
            }
            HudModule active = client.modules.all[selected];
            ink.round(22, 238, 476, 89, 8, Ink.PANEL);
            ink.text(active.title, 36, 249, 12, true, Ink.WHITE);
            ink.small(active.description, 120, 252, Ink.MUTED);
            action(capture == selected ? "Pulsa una tecla..." : "Tecla: " + keyName(active.state.key), 379, 243, 105, 22, mx, my, false);
            ink.small("Escala", 36, 276, Ink.MUTED);
            action("-", 36, 292, 22, 22, mx, my, false);
            ink.center(Math.round(active.state.scale * 100) + "%", 59, 299, 43, 9, true, Ink.WHITE);
            action("+", 103, 292, 22, 22, mx, my, false);
            ink.small("Opacidad", 152, 276, Ink.MUTED);
            ink.small(Math.round(active.state.opacity * 100) + "%", 244, 276, Ink.WHITE);
            float fill = (active.state.opacity - 0.1f) / 0.75f * 116;
            ink.round(152, 301, 116, 3, 1.5f, Ink.LINE);
            ink.round(152, 301, Math.max(3, fill), 3, 1.5f, client.accent());
            ink.round(148 + fill, 298, 9, 9, 4.5f, Ink.WHITE);
            ink.small("Fondo", 293, 276, Ink.MUTED);
            ink.toggle(294, 295, backgroundSwitch.to(active.state.background ? 1 : 0), client.accent());
            ink.small("Sombra", 346, 276, Ink.MUTED);
            ink.toggle(349, 295, shadowSwitch.to(active.state.shadow ? 1 : 0), client.accent());
            ink.small("Acento", 412, 276, Ink.MUTED);
            for (int i = 0; i < Ink.ACCENTS.length; i++) {
                int x = 412 + i * 18;
                if (client.config.accent == i) ink.round(x - 2, 294, 16, 16, 8, Ink.WHITE);
                ink.round(x, 296, 12, 12, 6, Ink.ACCENTS[i]);
            }
            if (capture != -1) ink.small("Pulsa una tecla · Esc cancela · Supr elimina el atajo", 22, 339, client.accent());
            else {
                ink.small(keyName(client.config.menuKey) + "  /  Abrir menú", 22, 339, Ink.MUTED);
                ink.round(218, 338, 9, 9, 3, client.config.watermark ? client.accent() : Ink.LINE);
                if (client.config.watermark) ink.round(221, 341, 3, 3, 1, Ink.DARK);
                ink.small("Firma Cubi", 233, 339, Ink.MUTED);
                ink.small(client.config.status, 413, 339, Ink.MUTED);
            }
        } finally { GL11.glPopMatrix(); ink.opacity = 1; }
    }

    private static void drawEditor(int mouseX, int mouseY) throws Throwable {
        Ink ink = client.ink;
        HudModule active = client.modules.all[selected];
        if (dragging) {
            if (!Mouse.isButtonDown(0)) { dragging = false; client.config.save(); }
            else {
                float threshold = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 0 : 4;
                active.position(HudPlacement.snap(mouseX - offsetX, active.pixelWidth(), client.game.width, threshold),
                        HudPlacement.snap(mouseY - offsetY, active.pixelHeight(), client.game.height, threshold), client.game.width, client.game.height);
                client.config.changed();
            }
        }
        ink.rect(0, 0, client.game.width, client.game.height, client.game.inWorld() ? 0x25060910 : 0xFF0D1017);
        if (dragging) {
            float gx = HudPlacement.guide(active.x(client.game.width), active.pixelWidth(), client.game.width);
            float gy = HudPlacement.guide(active.y(client.game.height), active.pixelHeight(), client.game.height);
            if (gx >= 0) ink.rect(Math.round(gx), 0, 1, client.game.height, Ink.alpha(client.accent(), 0.5f));
            if (gy >= 0) ink.rect(0, Math.round(gy), client.game.width, 1, Ink.alpha(client.accent(), 0.5f));
        }
        for (int i = 0; i < client.modules.all.length; i++) {
            HudModule module = client.modules.all[i];
            ink.opacity = module.state.enabled ? 1 : 0.45f;
            module.render(client, true);
            ink.opacity = 1;
            int x = module.x(client.game.width), y = module.y(client.game.height);
            if (i == selected || Ink.inside(mouseX, mouseY, x, y, module.pixelWidth(), module.pixelHeight())) {
                int w = module.pixelWidth() + 6, h = module.pixelHeight() + 6;
                int color = i == selected ? client.accent() : 0x88FFFFFF;
                for (int corner = 0; corner < 4; corner++) {
                    int cx = x - 3 + (corner % 2 == 0 ? 0 : w - 1), cy = y - 3 + (corner < 2 ? 0 : h - 1);
                    ink.rect(cx - (corner % 2 == 0 ? 0 : 4), cy, 5, 1, color);
                    ink.rect(cx, cy - (corner < 2 ? 0 : 4), 1, 5, color);
                }
            }
        }
        int barX = client.game.width / 2 - 150, barY = client.game.height - 43;
        ink.round(barX, barY + 2, 300, 32, 8, 0x44000000);
        ink.round(barX, barY, 300, 32, 8, 0xF2191C24);
        ink.text(active.title, barX + 10, barY + 11, 10, true, Ink.WHITE);
        action("-", barX + 89, barY + 6, 20, 20, mouseX, mouseY, false);
        ink.center(Math.round(active.state.scale * 100) + "%", barX + 110, barY + 12, 35, 8, true, Ink.WHITE);
        action("+", barX + 146, barY + 6, 20, 20, mouseX, mouseY, false);
        action("Restablecer", barX + 173, barY + 6, 71, 20, mouseX, mouseY, false);
        action("Listo", barX + 251, barY + 6, 40, 20, mouseX, mouseY, true);
        ink.center("Arrastra para colocar · Shift: precisión · Tab: seleccionar", 0, client.game.height - 58,
                client.game.width, 8, false, Ink.MUTED);
    }

    private static void action(String label, int x, int y, int w, int h, int mx, int my, boolean primary) throws Throwable {
        boolean over = Ink.inside(mx, my, x, y, w, h);
        int color = primary ? Ink.mix(client.accent(), Ink.WHITE, over ? 0.14f : 0) : over ? Ink.LINE : Ink.CARD;
        client.ink.round(x, y, w, h, 5, color);
        client.ink.center(label, x, y + (h - 9) / 2f, w, 9, true, primary ? Ink.DARK : Ink.WHITE);
    }

    public static void click(int mouseX, int mouseY, int button) {
        if (client == null || button != 0) return;
        try {
            if (editor) {
                int x = client.game.width / 2 - 150, y = client.game.height - 43;
                if (Ink.inside(mouseX, mouseY, x, y, 300, 32)) {
                    if (Ink.inside(mouseX, mouseY, x + 89, y + 6, 20, 20)) scale(-0.05f);
                    if (Ink.inside(mouseX, mouseY, x + 146, y + 6, 20, 20)) scale(0.05f);
                    if (Ink.inside(mouseX, mouseY, x + 173, y + 6, 71, 20)) { client.modules.resetLayout(client); client.config.save(); }
                    if (Ink.inside(mouseX, mouseY, x + 251, y + 6, 40, 20)) { editor = false; dragging = false; client.config.save(); }
                    return;
                }
                for (int i = client.modules.all.length - 1; i >= 0; i--) {
                    HudModule module = client.modules.all[i];
                    int px = module.x(client.game.width), py = module.y(client.game.height);
                    if (Ink.inside(mouseX, mouseY, px, py, module.pixelWidth(), module.pixelHeight())) {
                        selected = i; dragging = true; offsetX = mouseX - px; offsetY = mouseY - py; return;
                    }
                }
                return;
            }
            if (capture != -1) return;
            layout();
            int mx = (int) ((mouseX - originX) / zoom), my = (int) ((mouseY - originY) / zoom);
            if (Ink.inside(mx, my, 476, 20, 26, 28)) { close(); return; }
            if (Ink.inside(mx, my, 356, 20, 112, 28)) { editor = true; return; }
            for (int i = 0; i < client.modules.all.length; i++) {
                int x = 22 + i * 164;
                if (Ink.inside(mx, my, x, 112, 148, 112)) {
                    selected = i;
                    if (Ink.inside(mx, my, x + 106, 120, 36, 25)) {
                        client.modules.all[i].state.enabled = !client.modules.all[i].state.enabled;
                        changed();
                    }
                    return;
                }
            }
            HudModule active = client.modules.all[selected];
            if (Ink.inside(mx, my, 379, 243, 105, 22)) capture = selected;
            if (Ink.inside(mx, my, 22, 333, 178, 16)) capture = -2;
            if (Ink.inside(mx, my, 36, 292, 22, 22)) scale(-0.05f);
            if (Ink.inside(mx, my, 103, 292, 22, 22)) scale(0.05f);
            if (Ink.inside(mx, my, 148, 291, 124, 24)) { opacityDrag = true; opacityAt(mx); }
            if (Ink.inside(mx, my, 288, 290, 40, 24)) { active.state.background = !active.state.background; changed(); }
            if (Ink.inside(mx, my, 343, 290, 40, 24)) { active.state.shadow = !active.state.shadow; changed(); }
            for (int i = 0; i < Ink.ACCENTS.length; i++) if (Ink.inside(mx, my, 410 + 18 * i, 292, 16, 20)) { client.config.accent = i; changed(); }
            if (Ink.inside(mx, my, 212, 333, 88, 16)) { client.config.watermark = !client.config.watermark; changed(); }
        } catch (Throwable error) { recover(error); }
    }

    private static void opacityAt(int mx) {
        client.modules.all[selected].state.opacity = ClientConfig.finiteClamp(0.1f + (mx - 152) / 116f * 0.75f, 0.1f, 0.85f, 0.48f);
        client.config.changed();
    }
    private static void changed() { client.config.changed(); client.config.save(); }
    public static void release(int mouseX, int mouseY, int button) {
        if (client != null && button == 0) { dragging = false; opacityDrag = false; client.config.save(); }
    }

    public static void type(char character, int key) {
        if (client == null) return;
        // A held shortcut must not close a screen opened at the end of the last tick.
        // Keep repeat enabled for arrow-key nudging and other ordinary editor input.
        if (Keyboard.isRepeatEvent() && (capture != -1 || key == client.config.menuKey || key == Keyboard.KEY_ESCAPE)) return;
        Hooks.consumeKey();
        try {
            if (capture != -1) {
                if (key == Keyboard.KEY_ESCAPE) { capture = -1; return; }
                if (key == 0 || key >= Keyboard.KEYBOARD_SIZE) return;
                boolean clear = key == Keyboard.KEY_BACK || key == Keyboard.KEY_DELETE;
                if (capture == -2) {
                    if (clear) { capture = -1; return; }
                    client.config.menuKey = key;
                    for (HudModule module : client.modules.all) if (module.state.key == key) module.state.key = 0;
                } else {
                    if (!clear && key == client.config.menuKey) return;
                    for (HudModule module : client.modules.all) if (module.state.key == key) module.state.key = 0;
                    client.modules.all[capture].state.key = clear ? 0 : key;
                }
                capture = -1; changed(); return;
            }
            if (key == Keyboard.KEY_ESCAPE || key == client.config.menuKey) {
                if (editor) { editor = false; dragging = false; client.config.save(); }
                else close();
                return;
            }
            if (!editor) return;
            if (key == Keyboard.KEY_TAB) { selected = (selected + 1) % client.modules.all.length; return; }
            if (character == '+' || character == '=' || key == Keyboard.KEY_ADD) { scale(0.05f); return; }
            if (character == '-' || key == Keyboard.KEY_SUBTRACT) { scale(-0.05f); return; }
            HudModule module = client.modules.all[selected];
            int step = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 1 : 4;
            int dx = key == Keyboard.KEY_LEFT ? -step : key == Keyboard.KEY_RIGHT ? step : 0;
            int dy = key == Keyboard.KEY_UP ? -step : key == Keyboard.KEY_DOWN ? step : 0;
            if (dx != 0 || dy != 0) {
                module.position(module.x(client.game.width) + dx, module.y(client.game.height) + dy, client.game.width, client.game.height);
                client.config.changed();
            }
        } catch (Throwable error) { recover(error); }
    }

    private static void scale(float amount) {
        HudModule module = client.modules.all[selected];
        module.state.scale = ClientConfig.finiteClamp(Math.round((module.state.scale + amount) * 100) / 100f, 0.75f, 2, 1);
        client.config.changed();
        if (!editor) client.config.save();
    }
    private static String keyName(int key) {
        if (key == 0) return "Sin asignar";
        String name = Keyboard.getKeyName(key);
        return name == null ? "?" : name;
    }
    private static void close() throws Throwable { client.config.save(); client.game.show(previous); }
    public static void closed() {
        if (client != null) {
            dragging = false; opacityDrag = false; capture = -1;
            client.config.save(); client.controlScreen = null;
        }
    }
    private static void recover(Throwable error) {
        Hooks.fail(error);
        try { client.game.show(previous); } catch (Throwable ignored) { /* Original failure already logged. */ }
    }
}

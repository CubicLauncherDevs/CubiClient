package dev.cubi.ui;

import dev.cubi.config.ClientConfig;
import dev.cubi.core.ClientIdentity;
import dev.cubi.core.CubiClient;
import dev.cubi.core.Hooks;
import dev.cubi.module.HudModule;
import dev.cubi.ui.DeckLayout.Rect;
import dev.cubi.ui.DeckState.Page;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import static dev.cubi.ui.DeckLayout.*;

/** One Minecraft screen with separate module browser, module settings and appearance views. */
public final class ControlDeck {
    private static final String[] ACCENT_NAMES = {"Blanco · Predeterminado", "Azul", "Lavanda", "Melocotón"};
    private static final Motion backgroundSwitch = new Motion(), shadowSwitch = new Motion(), watermarkSwitch = new Motion();
    private static CubiClient client;
    private static DeckState nav;
    private static PerformancePanel performance;
    private static Motion[] hover, enabled;
    private static Object previous;
    private static boolean dragging, opacityDrag;
    private static int offsetX, offsetY;
    private static float zoom, originX, originY;
    private static long opened;

    private ControlDeck() { }

    public static void open(CubiClient instance, Object parent) throws Throwable {
        client = instance; previous = parent;
        int selected = nav == null ? 0 : Math.min(nav.selected(), instance.modules.all.length - 1);
        nav = new DeckState(instance.modules.all.length);
        nav.select(selected);
        performance = new PerformancePanel(instance);
        hover = new Motion[instance.modules.all.length];
        enabled = new Motion[instance.modules.all.length];
        for (int i = 0; i < hover.length; i++) { hover[i] = new Motion(); enabled[i] = new Motion(); }
        dragging = false; opacityDrag = false;
        opened = System.nanoTime();
    }

    public static void draw(int mouseX, int mouseY, float partialTicks) {
        if (client == null) return;
        try {
            client.game.resize();
            if (nav.page() == Page.EDITOR) drawEditor(mouseX, mouseY); else drawDeck(mouseX, mouseY);
        } catch (Throwable error) { recover(error); }
        finally {
            client.ink.opacity = 1;
            try { client.game.white(); } catch (Throwable error) { Hooks.fail(error); }
        }
    }

    private static void layout() {
        zoom = DeckLayout.zoom(client.game.width, client.game.height);
        originX = DeckLayout.originX(client.game.width, zoom);
        originY = DeckLayout.originY(client.game.height, zoom);
    }

    private static void drawDeck(int mouseX, int mouseY) throws Throwable {
        layout();
        int mx = (int) ((mouseX - originX) / zoom), my = (int) ((mouseY - originY) / zoom);
        if (opacityDrag) {
            if (Mouse.isButtonDown(0) && nav.page() == Page.SETTINGS) opacityAt(mx);
            else { opacityDrag = false; client.config.save(); }
        }
        Ink ink = client.ink;
        ink.rect(0, 0, client.game.width, client.game.height, client.game.inWorld() ? Theme.OVERLAY : Theme.BACKGROUND);
        ink.opacity = Math.min(1, (System.nanoTime() - opened) / 160_000_000f);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(originX, originY, 0); GL11.glScalef(zoom, zoom, 1);
            ink.round(-2, 5, WIDTH + 4, HEIGHT, Theme.WINDOW_RADIUS + 2, Theme.WINDOW_SHADOW);
            ink.surface(0, 0, WIDTH, HEIGHT, Theme.WINDOW_RADIUS, Theme.BACKGROUND, Theme.BORDER);
            ink.icon(0, 20, 20, 24, client.accent());
            ink.text(ClientIdentity.NAME, 54, 19, 17, true, Theme.TEXT);
            ink.small("Minecraft " + ClientIdentity.MINECRAFT, 54, 42, Theme.SECONDARY);
            action("Editar HUD", EDIT, mx, my, true);
            action("×", CLOSE, mx, my, false);
            ink.rect(20, 61, WIDTH - 40, 1, Theme.BORDER);
            tab("Módulos", MODULES_TAB, nav.page() == Page.MODULES || nav.page() == Page.SETTINGS, mx, my);
            tab("Apariencia", APPEARANCE_TAB, nav.page() == Page.APPEARANCE, mx, my);
            tab("Rendimiento", PERFORMANCE_TAB, nav.page() == Page.PERFORMANCE || nav.page() == Page.DIAGNOSTICS, mx, my);

            if (nav.page() == Page.MODULES) drawModules(mx, my);
            else if (nav.page() == Page.SETTINGS) drawSettings(mx, my);
            else if (nav.page() == Page.APPEARANCE) drawAppearance(mx, my);
            else performance.draw(nav.page() == Page.DIAGNOSTICS, mx, my);

            ink.rect(20, 333, WIDTH - 40, 1, Theme.BORDER);
            if (nav.capturing()) {
                ink.small(nav.binding() == DeckState.MENU_BINDING
                        ? "Pulsa una tecla para abrir el menú · Esc cancela"
                        : "Pulsa una tecla · Esc cancela · Supr elimina el atajo", 20, 346, client.accent());
            } else {
                ink.small(client.config.status, 20, 346, Theme.MUTED);
                ink.center(nav.page() == Page.SETTINGS || nav.page() == Page.DIAGNOSTICS ? "Esc: volver" : "Esc: cerrar", 185, 346, 190, 8, false, Theme.SECONDARY);
                String shortcut = keyName(client.config.menuKey) + " · Cerrar";
                ink.small(shortcut, WIDTH - 20 - ink.width(shortcut, 8, false), 346, Theme.SECONDARY);
            }
        } finally { GL11.glPopMatrix(); ink.opacity = 1; }
    }

    private static void tab(String title, Rect rect, boolean active, int mx, int my) throws Throwable {
        if (active || rect.contains(mx, my)) client.ink.round(rect.x, rect.y, rect.w, rect.h, Theme.CONTROL_RADIUS, Theme.CARD);
        client.ink.center(title, rect.x, rect.y + 9, rect.w, 11, true, active ? Theme.TEXT : Theme.SECONDARY);
        if (active) client.ink.rect(rect.x + 12, rect.y + rect.h - 1, rect.w - 24, 1, client.accent());
    }

    private static void drawModules(int mx, int my) throws Throwable {
        Ink ink = client.ink;
        int count = 0;
        for (HudModule module : client.modules.all) if (module.state.enabled) count++;
        String summary = count + " / " + client.modules.all.length + " activos";
        ink.text("Módulos · " + summary, 20, 123, 10, false, Theme.SECONDARY);
        if (nav.modulePages() > 1) {
            if (nav.modulePage() > 0) action("<", MODULE_PREVIOUS, mx, my, false);
            if (nav.modulePage() + 1 < nav.modulePages()) action(">", MODULE_NEXT, mx, my, false);
            ink.center((nav.modulePage() + 1) + " / " + nav.modulePages(), 424, 123, 82, 9, true, Theme.SECONDARY);
        }
        for (int slot = 0; slot < CARDS.length; slot++) {
            int i = nav.visibleModule(slot);
            if (i < 0) continue;
            HudModule module = client.modules.all[i];
            Rect card = CARDS[slot], gear = SETTINGS[slot], preview = CARD_PREVIEWS[slot];
            float over = hover[i].to(card.contains(mx, my) ? 1 : 0);
            ink.surface(card.x, card.y, card.w, card.h, Theme.CARD_RADIUS,
                    Ink.mix(Theme.CARD, Theme.SELECTED, over * 0.5f), Ink.mix(Theme.BORDER, Theme.BORDER_HOVER, over));
            ink.icon(module.icon(), card.x + 10, card.y + 10, 14, Theme.SECONDARY);
            ink.text(module.title, card.x + 31, card.y + 12, 11, true, Theme.TEXT);
            float descriptionSize = Math.min(8, 8 * 142 / Math.max(1, ink.width(module.description, 8, false)));
            ink.text(module.description, card.x + 10, card.y + 32, descriptionSize, false, Theme.SECONDARY);
            float previewScale = Math.min(1.1f, Math.min((float) preview.w / module.width, (float) preview.h / module.height));
            float alpha = ink.opacity;
            if (!module.state.enabled) ink.opacity *= 0.55f;
            try {
                module.renderAt(client, preview.x + (preview.w - module.width * previewScale) / 2,
                        preview.y + (preview.h - module.height * previewScale) / 2, previewScale, true);
            } finally { ink.opacity = alpha; }
            toggleButton(TOGGLES[slot], module.state.enabled, enabled[i].to(module.state.enabled ? 1 : 0), mx, my);
            ink.surface(gear.x, gear.y, gear.w, gear.h, Theme.CONTROL_RADIUS,
                    gear.contains(mx, my) ? Theme.SELECTED : Theme.CARD, gear.contains(mx, my) ? Theme.BORDER_HOVER : Theme.BORDER);
            ink.icon(6, gear.x + (gear.w - 15) / 2f, gear.y + (gear.h - 15) / 2f, 15, Theme.TEXT);
        }
    }

    private static void drawSettings(int mx, int my) throws Throwable {
        HudModule active = client.modules.all[nav.selected()];
        Ink ink = client.ink;
        action("Volver", BACK, mx, my, false);
        ink.text("Módulos / " + active.title, 102, 118, 14, true, Theme.TEXT);
        ink.small(active.description, 102, 139, Theme.SECONDARY);
        toggleButton(ENABLE, active.state.enabled, enabled[nav.selected()].to(active.state.enabled ? 1 : 0), mx, my);
        panel(PREVIEW); panel(OPTIONS);
        ink.label("VISTA PREVIA", PREVIEW.x + 14, PREVIEW.y + 14);
        float scale = Math.min(active.state.scale, Math.min((PREVIEW.w - 28f) / active.width, (PREVIEW.h - 56f) / active.height));
        active.renderAt(client, PREVIEW.x + (PREVIEW.w - active.width * scale) / 2,
                PREVIEW.y + 33 + (PREVIEW.h - 56 - active.height * scale) / 2, scale, true);
        ink.center("Datos de ejemplo", PREVIEW.x, PREVIEW.y + PREVIEW.h - 15, PREVIEW.w, 8, false, Theme.MUTED);

        ink.text("Escala", 226, 174, Theme.TEXT);
        action("-", SCALE_MINUS, mx, my, false);
        ink.center(Math.round(active.state.scale * 100) + "%", 450, 174, 46, 9, true, Theme.TEXT);
        action("+", SCALE_PLUS, mx, my, false);
        ink.text("Opacidad", 226, 201, Theme.TEXT);
        ink.small(Math.round(active.state.opacity * 100) + "%", 331, 203, Theme.SECONDARY);
        float fill = active.state.opacity / 0.85f * (OPACITY.w - 10);
        float start = OPACITY.x + 5;
        ink.round(start, OPACITY.y + 9, OPACITY.w - 10, 4, 1.5f, Theme.INPUT);
        if (fill > 0) ink.round(start, OPACITY.y + 9, Math.max(3, fill), 4, 1.5f, client.accent());
        ink.round(start + fill - 4, OPACITY.y + 6.5f, 8, 9, 2, client.accent());
        ink.text("Fondo", 226, 231, Theme.TEXT);
        checkbox(BACKGROUND, backgroundSwitch.to(active.state.background ? 1 : 0));
        ink.text("Sombra del texto", 226, 260, Theme.TEXT);
        checkbox(SHADOW, shadowSwitch.to(active.state.shadow ? 1 : 0));
        ink.text("Atajo", 226, 290, Theme.TEXT);
        action(nav.capturing() ? "Pulsa una tecla..." : keyName(active.state.key), MODULE_BIND, mx, my, false);
    }

    private static void drawAppearance(int mx, int my) throws Throwable {
        Ink ink = client.ink;
        ink.text("Ajustes generales de " + ClientIdentity.NAME, 20, 123, 10, false, Theme.SECONDARY);
        panel(THEME); panel(GENERAL);
        ink.label("TEMA", 34, 163);
        ink.text(Theme.NAME, 34, 181, 14, true, Theme.TEXT);
        ink.small("Cantarell · Superficies oscuras", 34, 202, Theme.SECONDARY);
        ink.text("Color de acento", 34, 221, 9, true, Theme.TEXT);
        for (int i = 0; i < ACCENTS.length; i++) {
            Rect swatch = ACCENTS[i];
            ink.surface(swatch.x, swatch.y, swatch.w, swatch.h, Theme.CONTROL_RADIUS, Theme.CARD,
                    client.config.accent == i ? Theme.BORDER_FOCUS : swatch.contains(mx, my) ? Theme.BORDER_HOVER : Theme.BORDER);
            ink.round(swatch.x + 7, swatch.y + 7, swatch.w - 14, swatch.h - 14, 2, Ink.ACCENTS[i]);
        }
        ink.small(ACCENT_NAMES[client.config.accent], 34, 289, Theme.SECONDARY);
        ink.text("Mostrar " + ClientIdentity.NAME, 262, 165, 11, true, Theme.TEXT);
        ink.small("Marca discreta durante la partida", 262, 184, Theme.SECONDARY);
        checkbox(WATERMARK, watermarkSwitch.to(client.config.watermark ? 1 : 0));
        ink.rect(262, 201, 262, 1, Theme.BORDER);
        ink.text("Tecla del menú", 262, 214, 10, true, Theme.TEXT);
        ink.small("Abrir y cerrar " + ClientIdentity.NAME, 262, 235, Theme.SECONDARY);
        action(nav.capturing() ? "Pulsa una tecla..." : keyName(client.config.menuKey), MENU_BIND, mx, my, false);
        ink.rect(262, 254, 262, 1, Theme.BORDER);
        ink.text("Guardado automático", 262, 269, 10, true, Theme.TEXT);
        ink.small("Se guarda al cambiar o cerrar", 262, 289, Theme.SECONDARY);
    }

    private static void panel(Rect rect) throws Throwable {
        client.ink.surface(rect.x, rect.y, rect.w, rect.h, Theme.CARD_RADIUS, Theme.CARD, Theme.BORDER);
    }
    private static void checkbox(Rect rect, float amount) throws Throwable {
        client.ink.checkbox(rect.x + (rect.w - 15) / 2f, rect.y + (rect.h - 15) / 2f, 15, amount, client.accent());
    }
    private static void toggleButton(Rect rect, boolean active, float amount, int mx, int my) throws Throwable {
        Ink ink = client.ink;
        ink.surface(rect.x, rect.y, rect.w, rect.h, Theme.CONTROL_RADIUS, Theme.CARD,
                rect.contains(mx, my) ? Theme.BORDER_HOVER : Theme.BORDER);
        ink.checkbox(rect.x + 8, rect.y + (rect.h - 13) / 2f, 13, amount, client.accent());
        ink.text(active ? "Activado" : "Desactivado", rect.x + 28, rect.y + (rect.h - 9) / 2f, 9, true, active ? Theme.TEXT : Theme.SECONDARY);
    }
    private static void action(String label, Rect rect, int mx, int my, boolean primary) throws Throwable {
        client.ink.button(label, rect.x, rect.y, rect.w, rect.h, rect.contains(mx, my), primary, client.accent());
    }

    private static void drawEditor(int mouseX, int mouseY) throws Throwable {
        Ink ink = client.ink;
        HudModule active = client.modules.all[nav.selected()];
        if (dragging) {
            if (!Mouse.isButtonDown(0)) { dragging = false; client.config.save(); }
            else {
                float threshold = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 0 : 4;
                active.position(HudPlacement.snap(mouseX - offsetX, active.pixelWidth(), client.game.width, threshold),
                        HudPlacement.snap(mouseY - offsetY, active.pixelHeight(), client.game.height, threshold), client.game.width, client.game.height);
                client.config.changed();
            }
        }
        ink.rect(0, 0, client.game.width, client.game.height, client.game.inWorld() ? Theme.EDITOR_OVERLAY : Theme.BACKGROUND);
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
            if (i == nav.selected() || Ink.inside(mouseX, mouseY, x, y, module.pixelWidth(), module.pixelHeight())) {
                int w = module.pixelWidth() + 6, h = module.pixelHeight() + 6;
                int color = i == nav.selected() ? client.accent() : Ink.alpha(Theme.SECONDARY, 0.6f);
                for (int corner = 0; corner < 4; corner++) {
                    int cx = x - 3 + (corner % 2 == 0 ? 0 : w - 1), cy = y - 3 + (corner < 2 ? 0 : h - 1);
                    ink.rect(cx - (corner % 2 == 0 ? 0 : 4), cy, 5, 1, color);
                    ink.rect(cx, cy - (corner < 2 ? 0 : 4), 1, 5, color);
                }
            }
        }
        int x = editorX(client.game.width), y = editorY(client.game.height);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0);
            ink.round(0, 2, EDITOR_WIDTH, EDITOR_HEIGHT, Theme.CARD_RADIUS, Theme.WINDOW_SHADOW);
            ink.surface(0, 0, EDITOR_WIDTH, EDITOR_HEIGHT, Theme.CARD_RADIUS, Theme.SIDEBAR, Theme.BORDER);
            ink.label("SELECCIÓN", 12, 8);
            float titleSize = Math.min(10, 10 * (EDITOR_MINUS.x - 18) / ink.width(active.title, 10, true));
            ink.text(active.title, 12, 24, titleSize, true, Theme.TEXT);
            ink.label("ESCALA", 116, 7);
            action("-", EDITOR_MINUS, mouseX - x, mouseY - y, false);
            ink.center(Math.round(active.state.scale * 100) + "%", 119, 24, 37, 8, true, Theme.TEXT);
            action("+", EDITOR_PLUS, mouseX - x, mouseY - y, false);
            action("Restablecer", EDITOR_RESET, mouseX - x, mouseY - y, false);
            action("Listo", EDITOR_DONE, mouseX - x, mouseY - y, true);
        } finally { GL11.glPopMatrix(); }
        ink.center("Arrastra · Shift: precisión · Tab: selección · Esc: volver", 0, y - 14, client.game.width, 8, false, Theme.SECONDARY);
    }

    public static void click(int mouseX, int mouseY, int button) {
        if (client == null || button != 0) return;
        try {
            if (nav.page() == Page.EDITOR) { editorClick(mouseX, mouseY); return; }
            layout();
            int mx = (int) ((mouseX - originX) / zoom), my = (int) ((mouseY - originY) / zoom);
            if (CLOSE.contains(mx, my)) { close(); return; }
            if (nav.capturing()) return;
            if (EDIT.contains(mx, my)) { finishInteraction(); nav.edit(); return; }
            if (MODULES_TAB.contains(mx, my)) { finishInteraction(); nav.tab(Page.MODULES); return; }
            if (APPEARANCE_TAB.contains(mx, my)) { finishInteraction(); nav.tab(Page.APPEARANCE); return; }
            if (PERFORMANCE_TAB.contains(mx, my)) { finishInteraction(); nav.tab(Page.PERFORMANCE); return; }
            if (nav.page() == Page.PERFORMANCE || nav.page() == Page.DIAGNOSTICS) { performance.click(nav, mx, my); return; }
            if (nav.page() == Page.MODULES) {
                if (MODULE_PREVIOUS.contains(mx, my)) { nav.turnModules(-1); return; }
                if (MODULE_NEXT.contains(mx, my)) { nav.turnModules(1); return; }
                for (int slot = 0; slot < CARDS.length; slot++) {
                    int i = nav.visibleModule(slot);
                    if (i < 0) continue;
                    if (TOGGLES[slot].contains(mx, my)) { toggleModule(i); return; }
                    if (CARDS[slot].contains(mx, my)) { nav.settings(i); return; }
                }
            } else if (nav.page() == Page.SETTINGS) {
                HudModule active = client.modules.all[nav.selected()];
                if (BACK.contains(mx, my)) { back(); return; }
                if (ENABLE.contains(mx, my)) toggleModule(nav.selected());
                else if (SCALE_MINUS.contains(mx, my)) scale(-0.05f);
                else if (SCALE_PLUS.contains(mx, my)) scale(0.05f);
                else if (OPACITY.contains(mx, my)) { opacityDrag = true; opacityAt(mx); }
                else if (BACKGROUND.contains(mx, my)) { active.state.background = !active.state.background; changed(); }
                else if (SHADOW.contains(mx, my)) { active.state.shadow = !active.state.shadow; changed(); }
                else if (MODULE_BIND.contains(mx, my)) nav.captureModule();
            } else {
                if (WATERMARK.contains(mx, my)) { client.config.watermark = !client.config.watermark; changed(); }
                else if (MENU_BIND.contains(mx, my)) nav.captureMenu();
                else for (int i = 0; i < ACCENTS.length; i++) if (ACCENTS[i].contains(mx, my)) { client.config.accent = i; changed(); break; }
            }
        } catch (Throwable error) { recover(error); }
    }

    private static void editorClick(int mouseX, int mouseY) throws Throwable {
        int x = mouseX - editorX(client.game.width), y = mouseY - editorY(client.game.height);
        if (Ink.inside(x, y, 0, 0, EDITOR_WIDTH, EDITOR_HEIGHT)) {
            if (EDITOR_MINUS.contains(x, y)) scale(-0.05f);
            else if (EDITOR_PLUS.contains(x, y)) scale(0.05f);
            else if (EDITOR_RESET.contains(x, y)) { client.modules.resetLayout(client); client.config.save(); }
            else if (EDITOR_DONE.contains(x, y)) back();
            return;
        }
        for (int i = client.modules.all.length - 1; i >= 0; i--) {
            HudModule module = client.modules.all[i];
            int px = module.x(client.game.width), py = module.y(client.game.height);
            if (Ink.inside(mouseX, mouseY, px, py, module.pixelWidth(), module.pixelHeight())) {
                nav.select(i); dragging = true; offsetX = mouseX - px; offsetY = mouseY - py; return;
            }
        }
    }
    private static void toggleModule(int index) {
        HudModule module = client.modules.all[index];
        module.state.enabled = !module.state.enabled; changed();
    }
    private static void opacityAt(int mx) {
        client.modules.all[nav.selected()].state.opacity = DeckLayout.opacityAt(mx);
        client.config.changed();
    }
    private static void changed() { client.capture.stop("HUD/apariencia modificados"); client.config.changed(); client.config.save(); }
    private static void finishInteraction() { dragging = false; opacityDrag = false; client.config.save(); }
    public static void release(int mouseX, int mouseY, int button) { if (client != null && button == 0) finishInteraction(); }

    public static void type(char character, int key) {
        if (client == null) return;
        if (Keyboard.isRepeatEvent() && (nav.capturing() || key == client.config.menuKey || key == Keyboard.KEY_ESCAPE)) return;
        Hooks.consumeKey();
        try {
            if (nav.capturing()) {
                if (key == Keyboard.KEY_ESCAPE) { nav.cancelCapture(); return; }
                if (key == 0 || key >= Keyboard.KEYBOARD_SIZE) return;
                boolean clear = key == Keyboard.KEY_BACK || key == Keyboard.KEY_DELETE;
                if (nav.binding() == DeckState.MENU_BINDING) {
                    if (clear) { nav.cancelCapture(); return; }
                    client.config.menuKey = key;
                    for (HudModule module : client.modules.all) if (module.state.key == key) module.state.key = 0;
                } else {
                    if (!clear && key == client.config.menuKey) return;
                    for (HudModule module : client.modules.all) if (module.state.key == key) module.state.key = 0;
                    client.modules.all[nav.binding()].state.key = clear ? 0 : key;
                }
                nav.cancelCapture(); changed(); return;
            }
            if (key == client.config.menuKey) { close(); return; }
            if (key == Keyboard.KEY_ESCAPE) { back(); return; }
            if (nav.page() != Page.EDITOR) return;
            if (key == Keyboard.KEY_TAB) { nav.nextModule(); return; }
            if (character == '+' || character == '=' || key == Keyboard.KEY_ADD) { scale(0.05f); return; }
            if (character == '-' || key == Keyboard.KEY_SUBTRACT) { scale(-0.05f); return; }
            HudModule module = client.modules.all[nav.selected()];
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
        client.capture.stop("Escala del HUD modificada");
        HudModule module = client.modules.all[nav.selected()];
        module.state.scale = ClientConfig.finiteClamp(Math.round((module.state.scale + amount) * 100) / 100f, 0.75f, 2, 1);
        client.config.changed();
        if (nav.page() != Page.EDITOR) client.config.save();
    }
    private static String keyName(int key) {
        if (key == 0) return "Sin asignar";
        String name = Keyboard.getKeyName(key); return name == null ? "?" : name;
    }
    private static void back() throws Throwable { finishInteraction(); if (nav.back()) close(); }
    private static void close() throws Throwable { finishInteraction(); client.game.show(previous); }
    public static void closed() {
        if (client != null) { finishInteraction(); nav.cancelCapture(); client.controlScreen = null; }
    }
    private static void recover(Throwable error) {
        Hooks.fail(error);
        try { client.game.show(previous); } catch (Throwable ignored) { /* Original failure already logged. */ }
    }
}

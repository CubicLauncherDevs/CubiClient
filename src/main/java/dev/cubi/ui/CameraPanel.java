package dev.cubi.ui;

import dev.cubi.camera.ZoomSettings;
import dev.cubi.core.CubiClient;
import dev.cubi.ui.DeckLayout.Rect;
import org.lwjgl.input.Keyboard;

import static dev.cubi.ui.DeckLayout.*;

/** Camera controls are independent from the six movable HUD widgets. */
public final class CameraPanel {
    private final CubiClient client;
    private final Motion enabled = new Motion(), smooth = new Motion(), sensitivity = new Motion();

    public CameraPanel(CubiClient client) { this.client = client; }

    public void draw(DeckState nav, int mx, int my) throws Throwable {
        ZoomSettings settings = client.config.zoom;
        Ink ink = client.ink;
        ink.text("Zoom", 20, 118, 14, true, Theme.TEXT);
        ink.small("Mantén la tecla para acercar; suéltala para volver.", 20, 137, Theme.SECONDARY);
        Rect toggle = ZOOM_ENABLE;
        ink.surface(toggle.x, toggle.y, toggle.w, toggle.h, Theme.CONTROL_RADIUS, Theme.CARD,
                toggle.contains(mx, my) ? Theme.BORDER_HOVER : Theme.BORDER);
        ink.checkbox(toggle.x + 8, toggle.y + 6, 13, enabled.to(settings.enabled ? 1 : 0), client.accent());
        ink.text(settings.enabled ? "Activado" : "Desactivado", toggle.x + 28, toggle.y + 9, 9, true, Theme.TEXT);
        ink.surface(ZOOM_PANEL.x, ZOOM_PANEL.y, ZOOM_PANEL.w, ZOOM_PANEL.h, Theme.CARD_RADIUS, Theme.CARD, Theme.BORDER);
        ink.text("Tecla de zoom", 36, 163, 10, true, Theme.TEXT);
        boolean conflict = settings.key != 0 && !client.config.zoomKeyAvailable(settings.key);
        ink.small(conflict ? "Tecla compartida con otro atajo de Cubi; elige otra." : "Solo mientras la mantienes pulsada", 36, 181,
                conflict ? Theme.WARNING : Theme.SECONDARY);
        String name = settings.key == 0 ? "Sin asignar" : Keyboard.getKeyName(settings.key);
        action(nav.binding() == DeckState.ZOOM_BINDING ? "Pulsa una tecla..." : name == null ? "?" : name, ZOOM_BIND, mx, my);
        ink.rect(36, 194, 488, 1, Theme.BORDER);
        ink.text("Acercamiento", 36, 203, 10, true, Theme.TEXT);
        ink.small("De 2× a 8×", 36, 219, Theme.SECONDARY);
        action("-", ZOOM_MINUS, mx, my); action("+", ZOOM_PLUS, mx, my);
        ink.center(settings.magnification + "×", 450, 204, 46, 9, true, Theme.TEXT);
        ink.rect(36, 234, 488, 1, Theme.BORDER);
        ink.text("Transición suave", 36, 245, 10, true, Theme.TEXT);
        checkbox(ZOOM_SMOOTH, smooth.to(settings.smooth ? 1 : 0));
        ink.rect(36, 271, 488, 1, Theme.BORDER);
        ink.text("Adaptar sensibilidad", 36, 281, 10, true, Theme.TEXT);
        ink.small("Reduce el movimiento del ratón durante el zoom", 36, 301, Theme.SECONDARY);
        checkbox(ZOOM_SENSITIVITY, sensitivity.to(settings.adaptSensitivity ? 1 : 0));
    }
    private void action(String label, Rect rect, int mx, int my) throws Throwable {
        client.ink.button(label, rect.x, rect.y, rect.w, rect.h, rect.contains(mx, my), false);
    }
    private void checkbox(Rect rect, float value) throws Throwable {
        client.ink.checkbox(rect.x + (rect.w - 15) / 2f, rect.y + (rect.h - 15) / 2f, 15, value, client.accent());
    }
    public void click(DeckState nav, int mx, int my) {
        ZoomSettings settings = client.config.zoom;
        if (ZOOM_BIND.contains(mx, my)) { nav.captureZoom(); return; }
        if (ZOOM_ENABLE.contains(mx, my)) settings.enabled = !settings.enabled;
        else if (ZOOM_MINUS.contains(mx, my)) settings.magnification = Math.max(2, settings.magnification - 0.5f);
        else if (ZOOM_PLUS.contains(mx, my)) settings.magnification = Math.min(8, settings.magnification + 0.5f);
        else if (ZOOM_SMOOTH.contains(mx, my)) settings.smooth = !settings.smooth;
        else if (ZOOM_SENSITIVITY.contains(mx, my)) settings.adaptSensitivity = !settings.adaptSensitivity;
        else return;
        client.cameraZoom.cancel();
        client.capture.stop("Zoom modificado");
        client.config.changed(); client.config.save();
    }
}

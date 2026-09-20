package dev.cubi.module;

import dev.cubi.config.ClientConfig.ModuleState;
import dev.cubi.config.ClientConfig;
import dev.cubi.core.CubiClient;
import dev.cubi.ui.Ink;
import dev.cubi.ui.Theme;
import org.lwjgl.opengl.GL11;

public abstract class HudModule {
    public final String id, title, description, number;
    public final int width, height;
    public final ModuleState state;
    boolean newState;

    protected HudModule(ClientConfig config, String id, String title, String description, String number, int width, int height) {
        this.id = id; this.title = title; this.description = description; this.number = number;
        this.width = width; this.height = height;
        newState = !config.modules.containsKey(id) || config.modules.get(id) == null;
        state = config.state(id);
    }

    public void tick(CubiClient client) throws Throwable { }
    public void clearData() throws Throwable { }
    public int icon() { return id.equals("keys") ? 3 : 1; }
    public abstract void paint(CubiClient client, Ink ink, boolean preview) throws Throwable;
    public int pixelWidth() { return (int) Math.ceil(width * state.scale); }
    public int pixelHeight() { return (int) Math.ceil(height * state.scale); }
    public int x(int screenWidth) { return Math.round(state.x * Math.max(0, screenWidth - pixelWidth())); }
    public int y(int screenHeight) { return Math.round(state.y * Math.max(0, screenHeight - pixelHeight())); }
    public void position(float x, float y, int screenWidth, int screenHeight) {
        state.x = Math.max(0, Math.min(1, x / Math.max(1, screenWidth - pixelWidth())));
        state.y = Math.max(0, Math.min(1, y / Math.max(1, screenHeight - pixelHeight())));
    }
    public void render(CubiClient client, boolean preview) throws Throwable {
        renderAt(client, x(client.game.width), y(client.game.height), state.scale, preview);
    }
    public void renderAt(CubiClient client, float x, float y, float scale, boolean preview) throws Throwable {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0);
            GL11.glScalef(scale, scale, 1);
            boolean batch = client.config.performance.hudBatching;
            if (batch) client.ink.beginBatch();
            try { paint(client, client.ink, preview); }
            finally { if (batch) client.ink.endBatch(); }
        } finally { GL11.glPopMatrix(); }
    }
    protected void plate(Ink ink) throws Throwable {
        if (state.background && state.opacity > 0) {
            if (state.shadow) ink.round(0, 1, width, height, Theme.HUD_RADIUS,
                    Ink.alpha(Theme.HUD_SHADOW, (Theme.HUD_SHADOW >>> 24) / 255f * state.opacity / 0.48f));
            ink.round(0, 0, width, height, Theme.HUD_RADIUS, Ink.alpha(Theme.BACKGROUND, state.opacity));
            ink.border(0, 0, width, height, Theme.HUD_RADIUS, Ink.alpha(Theme.BORDER_HOVER, state.opacity));
        }
    }
    protected void metric(Ink ink, String value, String unit) throws Throwable {
        plate(ink);
        float size = Math.min(11, 11 * (width - 21 - ink.width(unit, 9, false)) / Math.max(1, ink.width(value, 11, true)));
        float valueWidth = ink.width(value, size, true);
        float x = (width - valueWidth - 5 - ink.width(unit, 9, false)) / 2;
        if (state.shadow) ink.text(value, x + 0.6f, 7.6f, size, true, Theme.TEXT_SHADOW);
        ink.text(value, x, 7, size, true, Ink.WHITE);
        if (state.shadow) ink.text(unit, x + valueWidth + 5.6f, 8.6f, 9, false, Theme.TEXT_SHADOW);
        ink.text(unit, x + valueWidth + 5, 8, 9, false, Theme.SECONDARY);
    }
    protected void fittedText(Ink ink, String value, float x, float y, float available, float size, int color) throws Throwable {
        float measured = ink.width(value, size, true);
        if (measured > available) size *= available / measured;
        if (state.shadow) ink.text(value, x + 0.6f, y + 0.6f, size, true, Theme.TEXT_SHADOW);
        ink.text(value, x, y, size, true, color);
    }
}

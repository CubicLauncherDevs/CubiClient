package dev.cubi.ui;

import dev.cubi.bridge.Game189;

/** Shared visual language for the HUD, module browser and editor. */
public final class Ink {
    public static final int BACKGROUND = Theme.BACKGROUND, PANEL = Theme.CARD, CARD = Theme.INPUT;
    public static final int LINE = Theme.BORDER, WHITE = Theme.TEXT, MUTED = Theme.SECONDARY;
    public static final int DARK = Theme.ON_ACCENT;
    public static final int[] ACCENTS = {Theme.ACCENT, 0xFF9BC8FF, 0xFFC7B4FF, 0xFFFFC4A3};
    private final Game189 game;
    private final UiAtlas atlas;
    public float opacity = 1;

    public Ink(Game189 game) throws Throwable { this.game = game; atlas = new UiAtlas(game); }
    private int tint(int color) { return alpha(color, ((color >>> 24) / 255f) * opacity); }
    public void rect(int x, int y, int w, int h, int color) throws Throwable {
        if (w > 0 && h > 0) game.rect(x, y, x + w, y + h, tint(color));
    }
    public void round(float x, float y, float w, float h, float radius, int color) throws Throwable { atlas.rounded(x, y, w, h, radius, tint(color)); }
    public void border(float x, float y, float w, float h, float radius, int color) throws Throwable { atlas.border(x, y, w, h, radius, tint(color)); }
    public void surface(float x, float y, float w, float h, float radius, int fill, int border) throws Throwable {
        round(x, y, w, h, radius, border);
        round(x + Theme.STROKE, y + Theme.STROKE, w - 2 * Theme.STROKE, h - 2 * Theme.STROKE,
                Math.max(0, radius - Theme.STROKE), fill);
    }
    public void text(String value, float x, float y, int color) throws Throwable { text(value, x, y, 10, false, color); }
    public void text(String value, float x, float y, float size, boolean bold, int color) throws Throwable { atlas.text(value, x, y, size, bold, tint(color)); }
    public float width(String value, float size, boolean bold) { return atlas.width(value, size, bold); }
    public void center(String value, float x, float y, float width, float size, boolean bold, int color) throws Throwable {
        text(value, x + (width - width(value, size, bold)) / 2, y, size, bold, color);
    }
    public void center(String value, int x, int y, int width, int color) throws Throwable { center(value, x, y, width, 10, false, color); }
    public void small(String value, float x, float y, int color) throws Throwable { text(value, x, y, 8, false, color); }
    public void label(String value, float x, float y) throws Throwable {
        atlas.text(value, x, y, 7.5f, true, tint(Theme.SECONDARY), 0.65f);
    }
    public void outline(int x, int y, int w, int h, int color) throws Throwable {
        rect(x, y, w, 1, color); rect(x, y + h - 1, w, 1, color);
        rect(x, y, 1, h, color); rect(x + w - 1, y, 1, h, color);
    }
    public void logo(int x, int y, int size) throws Throwable { icon(0, x, y, size, Theme.ACCENT); }
    public void icon(int id, float x, float y, float size, int color) throws Throwable { atlas.icon(id, x, y, size, tint(color)); }
    public void button(String label, int x, int y, int w, int h, boolean hover, boolean primary) throws Throwable {
        button(label, x, y, w, h, hover, primary, Theme.ACCENT);
    }
    public void button(String label, int x, int y, int w, int h, boolean hover, boolean primary, int accent) throws Throwable {
        if (primary) round(x, y, w, h, Theme.CONTROL_RADIUS, hover ? mix(accent, DARK, 0.1f) : accent);
        else surface(x, y, w, h, Theme.CONTROL_RADIUS, hover ? Theme.SELECTED : Theme.CARD,
                hover ? Theme.BORDER_HOVER : Theme.BORDER);
        float size = 9;
        float labelWidth = width(label, size, true);
        if (labelWidth > w - 10) size *= (w - 10) / labelWidth;
        center(label, x, y + (h - size) / 2f, w, size, true, primary ? DARK : hover ? WHITE : MUTED);
    }
    public void toggle(float x, float y, float amount, int accent) throws Throwable {
        checkbox(x + 6, y, 15, amount, accent);
    }
    public void checkbox(float x, float y, float size, float amount, int accent) throws Throwable {
        surface(x, y, size, size, Theme.CONTROL_RADIUS * size / 18,
                mix(Theme.INPUT, accent, amount), mix(Theme.BORDER_HOVER, accent, amount));
        if (amount > 0.01f) icon(4, x + 1, y + 1, size - 2, alpha(Theme.ON_ACCENT, amount));
    }
    public static int alpha(int color, float opacity) { return (Math.round(Math.max(0, Math.min(1, opacity)) * 255) << 24) | (color & 0xFFFFFF); }
    public static int mix(int from, int to, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = Math.round((from >>> 24) + ((to >>> 24) - (from >>> 24)) * t);
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }
    public static boolean inside(int mx, int my, int x, int y, int w, int h) { return mx >= x && my >= y && mx < x + w && my < y + h; }
}

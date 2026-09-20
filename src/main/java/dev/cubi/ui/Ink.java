package dev.cubi.ui;

import dev.cubi.bridge.Game189;

/** Shared visual language for the HUD, module browser and editor. */
public final class Ink {
    public static final int BACKGROUND = 0xFA111319, PANEL = 0xFF191C24, CARD = 0xFF20242E;
    public static final int LINE = 0xFF303642, WHITE = 0xFFF3F5FA, MUTED = 0xFF8E98A9;
    public static final int MINT = 0xFFA0ECD3, DARK = 0xFF14251F;
    public static final int[] ACCENTS = {MINT, 0xFF9BC8FF, 0xFFC7B4FF, 0xFFFFC4A3};
    private final Game189 game;
    private final UiAtlas atlas;
    public float opacity = 1;

    public Ink(Game189 game) throws Throwable { this.game = game; atlas = new UiAtlas(game); }
    private int tint(int color) { return alpha(color, ((color >>> 24) / 255f) * opacity); }
    public void rect(int x, int y, int w, int h, int color) throws Throwable {
        if (w > 0 && h > 0) game.rect(x, y, x + w, y + h, tint(color));
    }
    public void round(float x, float y, float w, float h, float radius, int color) throws Throwable { atlas.rounded(x, y, w, h, radius, tint(color)); }
    public void text(String value, float x, float y, int color) throws Throwable { text(value, x, y, 10, false, color); }
    public void text(String value, float x, float y, float size, boolean bold, int color) throws Throwable { atlas.text(value, x, y, size, bold, tint(color)); }
    public float width(String value, float size, boolean bold) { return atlas.width(value, size, bold); }
    public void center(String value, float x, float y, float width, float size, boolean bold, int color) throws Throwable {
        text(value, x + (width - width(value, size, bold)) / 2, y, size, bold, color);
    }
    public void center(String value, int x, int y, int width, int color) throws Throwable { center(value, x, y, width, 10, false, color); }
    public void small(String value, float x, float y, int color) throws Throwable { text(value, x, y, 8, false, color); }
    public void outline(int x, int y, int w, int h, int color) throws Throwable {
        rect(x, y, w, 1, color); rect(x, y + h - 1, w, 1, color);
        rect(x, y, 1, h, color); rect(x + w - 1, y, 1, h, color);
    }
    public void logo(int x, int y, int size) throws Throwable { icon(0, x, y, size, MINT); }
    public void icon(int id, float x, float y, float size, int color) throws Throwable { atlas.icon(id, x, y, size, tint(color)); }
    public void button(String label, int x, int y, int w, int h, boolean hover, boolean primary) throws Throwable {
        round(x, y, w, h, 5, primary ? MINT : hover ? LINE : CARD);
        center(label, x, y + (h - 9) / 2f, w, 9, true, primary ? DARK : WHITE);
    }
    public void toggle(float x, float y, float amount, int accent) throws Throwable {
        round(x, y, 27, 15, 7.5f, mix(LINE, accent, amount));
        round(x + 3 + 12 * amount, y + 3, 9, 9, 4.5f, mix(MUTED, DARK, amount));
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

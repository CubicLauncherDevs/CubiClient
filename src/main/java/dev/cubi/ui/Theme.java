package dev.cubi.ui;

/** CubicLauncher's built-in Oscuro palette, adapted to Minecraft GUI units.
 * Reference: CubicLauncher c7b6ecb408472964158a3757b7fd7cabb4af8e42,
 * static/themes/dark/dark.json and src/styles/shared/components.css.
 */
public final class Theme {
    public static final int REVISION = 1;
    public static final String NAME = "Cubic · Oscuro";
    public static final String FONT = "Cantarell";
    public static final int BACKGROUND = 0xFF0C0C0C, SIDEBAR = 0xFF0F1010;
    public static final int CARD = 0xFF16161A, SELECTED = 0xFF1C1D1D, INPUT = 0xFF1C1C1C;
    public static final int TEXT = 0xFFD8D8D8, SECONDARY = 0xFF909090, MUTED = 0xFF787878;
    public static final int BORDER = 0xFF242424, BORDER_HOVER = 0xFF383838, BORDER_FOCUS = 0xFF777777;
    public static final int ACCENT = 0xFFFFFFFF, ACCENT_HOVER = 0xFFE0E0E0, ON_ACCENT = BACKGROUND;
    public static final int OVERLAY = 0xB3000000, EDITOR_OVERLAY = 0x25000000;
    public static final int HUD_SHADOW = 0x26000000, TEXT_SHADOW = 0x66000000;
    public static final int WINDOW_SHADOW = 0x66000000;
    public static final int SUCCESS = 0xFF9BD4AA, WARNING = 0xFFEAC47C, DANGER = 0xFFE58F8F;
    public static final float WINDOW_RADIUS = 12, CARD_RADIUS = 8, CONTROL_RADIUS = 4, HUD_RADIUS = 3;
    public static final float STROKE = 0.65f;
    private Theme() { }
}

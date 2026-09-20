package dev.cubi.ui;

/** Shared hit regions: rendering, mouse dispatch and regression tests use the same layout. */
public final class DeckLayout {
    public static final int WIDTH = 560, HEIGHT = 362;
    public static final Rect EDIT = new Rect(398, 18, 104, 30), CLOSE = new Rect(510, 18, 30, 30);
    public static final Rect MODULES_TAB = new Rect(20, 72, 114, 28), APPEARANCE_TAB = new Rect(142, 72, 114, 28);
    public static final Rect PERFORMANCE_TAB = new Rect(264, 72, 132, 28);
    public static final Rect[] PROFILES = {new Rect(20, 116, 168, 26), new Rect(196, 116, 168, 26), new Rect(372, 116, 168, 26)};
    public static final Rect[] PERFORMANCE_OPTIONS = new Rect[11];
    public static final Rect RESTORE_PERFORMANCE = new Rect(20, 300, 166, 24), DIAGNOSTICS = new Rect(194, 300, 166, 24);
    public static final Rect CAPTURE_START = new Rect(20, 155, 166, 26), CAPTURE_STOP = new Rect(194, 155, 166, 26), CAPTURE_EXPORT = new Rect(368, 155, 172, 26);
    public static final Rect BACK = new Rect(20, 116, 65, 26), ENABLE = new Rect(430, 116, 110, 26);
    public static final Rect PREVIEW = new Rect(20, 160, 174, 156), OPTIONS = new Rect(210, 160, 330, 156);
    public static final Rect SCALE_MINUS = new Rect(424, 167, 22, 22), SCALE_PLUS = new Rect(502, 167, 22, 22);
    public static final Rect OPACITY = new Rect(371, 194, 153, 22);
    public static final Rect BACKGROUND = new Rect(490, 223, 34, 23), SHADOW = new Rect(490, 252, 34, 23);
    public static final Rect MODULE_BIND = new Rect(400, 283, 124, 24);
    public static final Rect THEME = new Rect(20, 148, 210, 164), GENERAL = new Rect(246, 148, 294, 164);
    public static final Rect WATERMARK = new Rect(492, 158, 32, 24), MENU_BIND = new Rect(416, 211, 108, 25);
    public static final Rect[] CARDS = new Rect[2], TOGGLES = new Rect[2], SETTINGS = new Rect[2], ACCENTS = new Rect[4];
    public static final int EDITOR_WIDTH = 304, EDITOR_HEIGHT = 44;
    public static final Rect EDITOR_MINUS = new Rect(100, 18, 18, 18), EDITOR_PLUS = new Rect(157, 18, 18, 18);
    public static final Rect EDITOR_RESET = new Rect(185, 10, 68, 24), EDITOR_DONE = new Rect(260, 10, 34, 24);
    static {
        for (int i = 0; i < PERFORMANCE_OPTIONS.length; i++) PERFORMANCE_OPTIONS[i] = new Rect(20 + (i % 2) * 266, 150 + (i / 2) * 24, 254, 22);
        for (int i = 0; i < CARDS.length; i++) {
            int x = 20 + i * 266;
            CARDS[i] = new Rect(x, 148, 254, 164);
            TOGGLES[i] = new Rect(x + 12, 278, 186, 23);
            SETTINGS[i] = new Rect(x + 210, 278, 32, 23);
        }
        for (int i = 0; i < 4; i++) ACCENTS[i] = new Rect(34 + 44 * i, 238, 34, 30);
    }
    private DeckLayout() { }
    public static float zoom(int width, int height) { return Math.max(0.1f, Math.min(1, Math.min((width - 16f) / WIDTH, (height - 16f) / HEIGHT))); }
    public static float originX(int width, float zoom) { return (width - WIDTH * zoom) / 2; }
    public static float originY(int height, float zoom) { return (height - HEIGHT * zoom) / 2; }
    public static float opacityAt(float x) { return 0.1f + Math.max(0, Math.min(1, (x - OPACITY.x - 5) / (OPACITY.w - 10))) * 0.75f; }
    public static int editorX(int width) { return (width - EDITOR_WIDTH) / 2; }
    public static int editorY(int height) { return height - EDITOR_HEIGHT - 10; }

    public static final class Rect {
        public final int x, y, w, h;
        public Rect(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        public boolean contains(float mx, float my) { return mx >= x && my >= y && mx < x + w && my < y + h; }
        public boolean intersects(Rect other) { return x < other.x + other.w && x + w > other.x && y < other.y + other.h && y + h > other.y; }
        public int centerX() { return x + w / 2; }
        public int centerY() { return y + h / 2; }
    }
}

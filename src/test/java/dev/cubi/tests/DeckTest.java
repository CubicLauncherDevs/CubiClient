package dev.cubi.tests;

import dev.cubi.core.ClientIdentity;
import dev.cubi.module.ModuleRegistry;
import dev.cubi.ui.DeckLayout;
import dev.cubi.ui.DeckLayout.Rect;
import dev.cubi.ui.DeckState;
import dev.cubi.ui.DeckState.Page;
import dev.cubi.ui.HudPlacement;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Navigation, hit testing and text fit checked without constructing an OpenGL context. */
public final class DeckTest {
    private static int assertions;
    private DeckTest() { }
    public static int run() throws Exception {
        assertions = 0;
        navigation(); geometry(); watermark();
        return assertions;
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void navigation() {
        DeckState nav = new DeckState(ModuleRegistry.COUNT);
        check(nav.page() == Page.MODULES && !nav.capturing(), "Menu opens on an uncluttered module browser");
        nav.settings(1);
        check(nav.page() == Page.SETTINGS && nav.selected() == 1, "Settings target the requested module");
        nav.captureModule();
        check(nav.binding() == 1, "Binding focus belongs to the selected module");
        check(!nav.back() && !nav.capturing() && nav.page() == Page.SETTINGS, "First Escape cancels key capture only");
        check(!nav.back() && nav.page() == Page.MODULES, "Escape from settings returns to modules");
        check(nav.back(), "Escape from a root tab closes the screen");
        nav.tab(Page.APPEARANCE); nav.captureMenu();
        check(nav.binding() == DeckState.MENU_BINDING, "Menu shortcut is a global appearance setting");
        nav.edit(); nav.edit();
        check(nav.page() == Page.EDITOR && !nav.capturing(), "Editor clears keyboard capture");
        check(!nav.back() && nav.page() == Page.APPEARANCE, "Editor preserves its origin even if requested twice");
        nav.settings(0); nav.edit(); nav.nextModule();
        check(nav.selected() == 1, "Tab selects the next widget while editing");
        check(!nav.back() && nav.page() == Page.SETTINGS && nav.selected() == 1, "Editor returns to settings for the selected widget");
        nav.select(ModuleRegistry.COUNT - 1); nav.nextModule();
        check(nav.selected() == 0, "Widget selection wraps without indexing past the registry");
        nav.captureModule(); nav.tab(Page.APPEARANCE);
        check(!nav.capturing() && nav.page() == Page.APPEARANCE, "Changing root tabs clears old binding focus");
        check(nav.back(), "Appearance is a root view, not an extra back stack");
        boolean guarded = false;
        try { nav.captureModule(); } catch (IllegalStateException expected) { guarded = true; }
        check(guarded, "A hidden module-binding control cannot capture keys from appearance");
        guarded = false;
        try { nav.select(ModuleRegistry.COUNT); } catch (IllegalArgumentException expected) { guarded = true; }
        check(guarded && nav.selected() == 0, "Invalid selections cannot corrupt navigation");
        nav.tab(Page.PERFORMANCE); nav.diagnostics(); nav.edit();
        check(!nav.back() && nav.page() == Page.DIAGNOSTICS, "Editor returns to diagnostics");
        check(!nav.back() && nav.page() == Page.PERFORMANCE && nav.back(), "Diagnostics returns to performance before closing");
        nav.tab(Page.MODULES);
        check(nav.modulePages() == 3 && nav.visibleModule(0) == 0 && nav.visibleModule(1) == 1, "Browser starts with FPS and Keystrokes");
        nav.turnModules(-1);
        check(nav.modulePage() == 0, "Previous page clamps at the beginning");
        nav.turnModules(1);
        check(nav.visibleModule(0) == 2 && nav.visibleModule(1) == 3, "Second page targets ping and armor, not the first two widgets");
        nav.settings(nav.visibleModule(1)); nav.captureModule();
        check(nav.binding() == 3, "Binding belongs to armor on the second browser page");
        nav.turnModules(1);
        check(nav.modulePage() == 1, "Hidden pagination cannot change binding focus");
        nav.back(); nav.back();
        check(nav.page() == Page.MODULES && nav.modulePage() == 1, "Back from settings preserves browser page");
        nav.turnModules(1); nav.turnModules(1);
        check(nav.modulePage() == 2 && nav.visibleModule(0) == 4 && nav.visibleModule(1) == 5, "Last page targets coordinates and server and clamps");
        nav.edit(); nav.select(5); nav.nextModule(); nav.back();
        check(nav.modulePage() == 0 && nav.selected() == 0, "Editor wrap reveals the selected widget's page");
        DeckState odd = new DeckState(5); odd.turnModules(2);
        check(odd.visibleModule(0) == 4 && odd.visibleModule(1) == -1 && odd.visibleModule(2) == -1,
                "Partial pages expose no nonexistent module targets");
    }

    private static void geometry() {
        List<Rect> header = Arrays.asList(DeckLayout.EDIT, DeckLayout.CLOSE, DeckLayout.MODULES_TAB, DeckLayout.APPEARANCE_TAB, DeckLayout.PERFORMANCE_TAB);
        List<Rect> modules = new ArrayList<Rect>(header);
        modules.add(DeckLayout.MODULE_PREVIOUS); modules.add(DeckLayout.MODULE_NEXT);
        for (int i = 0; i < DeckLayout.CARDS.length; i++) {
            Rect card = DeckLayout.CARDS[i], toggle = DeckLayout.TOGGLES[i], settings = DeckLayout.SETTINGS[i];
            check(contains(card, toggle) && contains(card, settings) && !toggle.intersects(settings), "Activation and settings are distinct targets in card " + i);
            check(toggle.contains(toggle.centerX(), toggle.centerY()) && !settings.contains(toggle.centerX(), toggle.centerY()),
                    "Module activation cannot hit its settings button " + i);
            modules.add(toggle); modules.add(settings);
        }
        check(disjoint(modules), "Module browser actions do not overlap");
        List<Rect> settings = new ArrayList<Rect>(header);
        settings.addAll(Arrays.asList(DeckLayout.BACK, DeckLayout.ENABLE, DeckLayout.SCALE_MINUS, DeckLayout.SCALE_PLUS,
                DeckLayout.OPACITY, DeckLayout.BACKGROUND, DeckLayout.SHADOW, DeckLayout.MODULE_BIND));
        check(disjoint(settings), "Module settings actions do not overlap");
        for (Rect rect : Arrays.asList(DeckLayout.SCALE_MINUS, DeckLayout.SCALE_PLUS, DeckLayout.OPACITY,
                DeckLayout.BACKGROUND, DeckLayout.SHADOW, DeckLayout.MODULE_BIND)) {
            check(contains(DeckLayout.OPTIONS, rect), "Setting controls fit inside their dedicated column");
        }
        check(!DeckLayout.PREVIEW.intersects(DeckLayout.OPTIONS), "Preview does not collide with settings");
        List<Rect> appearance = new ArrayList<Rect>(header);
        appearance.add(DeckLayout.WATERMARK); appearance.add(DeckLayout.MENU_BIND);
        appearance.addAll(Arrays.asList(DeckLayout.ACCENTS));
        check(disjoint(appearance), "Appearance controls have independent hit regions");
        List<Rect> performance = new ArrayList<Rect>(header);
        performance.addAll(Arrays.asList(DeckLayout.PROFILES));
        performance.addAll(Arrays.asList(DeckLayout.PERFORMANCE_OPTIONS));
        performance.add(DeckLayout.RESTORE_PERFORMANCE); performance.add(DeckLayout.DIAGNOSTICS);
        check(disjoint(performance), "Performance profiles, options and actions do not overlap");
        List<Rect> diagnostics = new ArrayList<Rect>(header);
        diagnostics.addAll(Arrays.asList(DeckLayout.BACK, DeckLayout.CAPTURE_START, DeckLayout.CAPTURE_STOP, DeckLayout.CAPTURE_EXPORT));
        check(disjoint(diagnostics), "Diagnostic actions do not overlap");
        List<Rect> editor = Arrays.asList(DeckLayout.EDITOR_MINUS, DeckLayout.EDITOR_PLUS, DeckLayout.EDITOR_RESET, DeckLayout.EDITOR_DONE);
        check(disjoint(editor), "Editor toolbar actions do not overlap");
        Rect bar = new Rect(0, 0, DeckLayout.EDITOR_WIDTH, DeckLayout.EDITOR_HEIGHT);
        for (Rect rect : editor) check(contains(bar, rect), "Editor controls remain inside the toolbar");
        check(DeckLayout.opacityAt(-100) == 0.1f && DeckLayout.opacityAt(1000) == 0.85f, "Slider clamps drags outside its new bounds");
        check(Math.abs(DeckLayout.opacityAt(DeckLayout.OPACITY.centerX()) - 0.475f) < 0.004f, "Slider midpoint maps correctly");

        List<Rect> all = new ArrayList<Rect>(modules); all.addAll(settings); all.addAll(appearance);
        all.addAll(performance); all.addAll(diagnostics);
        Rect canvas = new Rect(0, 0, DeckLayout.WIDTH, DeckLayout.HEIGHT);
        boolean fits = true;
        for (Rect rect : all) fits &= contains(canvas, rect);
        check(fits, "Every clickable control fits in the menu canvas");
        int[][] sizes = {{320, 240}, {426, 240}, {854, 480}, {1920, 1080}};
        for (int[] size : sizes) {
            float zoom = DeckLayout.zoom(size[0], size[1]);
            float ox = DeckLayout.originX(size[0], zoom), oy = DeckLayout.originY(size[1], zoom);
            boolean hit = true;
            for (Rect rect : all) {
                int sx = Math.round(ox + rect.centerX() * zoom), sy = Math.round(oy + rect.centerY() * zoom);
                int mx = (int) ((sx - ox) / zoom), my = (int) ((sy - oy) / zoom);
                hit &= rect.contains(mx, my);
            }
            check(hit && ox >= 7.9f && oy >= 7.9f, "Scaled controls remain clickable at " + size[0] + "x" + size[1]);
        }
    }

    private static boolean contains(Rect parent, Rect child) {
        return child.x >= parent.x && child.y >= parent.y && child.x + child.w <= parent.x + parent.w && child.y + child.h <= parent.y + parent.h;
    }
    private static boolean disjoint(List<Rect> items) {
        for (int i = 0; i < items.size(); i++) for (int j = i + 1; j < items.size(); j++) if (items.get(i).intersects(items.get(j))) return false;
        return true;
    }

    private static void watermark() throws Exception {
        float[] bold = new float[224];
        try (DataInputStream metrics = new DataInputStream(DeckTest.class.getResourceAsStream("/assets/cubi/ui/atlas.bin"))) {
            metrics.readInt();
            for (int face = 0; face < 2; face++) {
                metrics.readFloat();
                for (int i = 0; i < 224; i++) {
                    float value = metrics.readFloat();
                    if (face == 1) bold[i] = value;
                }
            }
        }
        float right = 29 + width(ClientIdentity.NAME, 10, bold);
        int y = HudPlacement.watermarkY(320, 240, right);
        check(right <= 308 && y + 13 < 240 - 22, "Full CubiClient watermark sits above the hotbar on a narrow screen");
        check(HudPlacement.watermarkY(854, 480, right) == 480 - 23, "Wide-screen watermark stays in the unobstructed bottom-left corner");
        check(HudPlacement.watermarkY(240, 100, right) >= 0, "Watermark stays within a short viewport");
        check(46 + width(ClientIdentity.NAME, 14, bold) <= 154, "Full client name fits the Minecraft main-menu badge");
        check(54 + width(ClientIdentity.NAME, 17, bold) + 20 < DeckLayout.EDIT.x, "Header name and editor button do not collide");
        check(12 + width("Keystrokes", 10, bold) < DeckLayout.EDITOR_MINUS.x, "Longest module name fits the editor selection group");
        check(width("LMB", 7, bold) <= 35 && width("RMB", 7, bold) <= 35 && width("256 CPS", 7, bold) <= 35,
                "Mouse labels and maximum bounded CPS fit inside each Keystrokes mouse cell");
    }
    private static float width(String value, float size, float[] advances) {
        float width = 0;
        for (int i = 0; i < value.length(); i++) width += advances[value.charAt(i) - 32];
        return width * size / 36;
    }
}

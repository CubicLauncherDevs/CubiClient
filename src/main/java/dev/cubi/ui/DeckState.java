package dev.cubi.ui;

/** Navigation and keyboard focus, independent from Minecraft and OpenGL. */
public final class DeckState {
    public enum Page { MODULES, APPEARANCE, SETTINGS, EDITOR }
    public static final int NO_BINDING = -1, MENU_BINDING = -2;
    private final int moduleCount;
    private Page page = Page.MODULES, editorReturn = Page.MODULES;
    private int selected, binding = NO_BINDING;

    public DeckState(int moduleCount) {
        if (moduleCount < 1) throw new IllegalArgumentException("At least one HUD module is required");
        this.moduleCount = moduleCount;
    }
    public Page page() { return page; }
    public int selected() { return selected; }
    public int binding() { return binding; }
    public boolean capturing() { return binding != NO_BINDING; }
    public void select(int index) {
        if (index < 0 || index >= moduleCount) throw new IllegalArgumentException("Invalid module index");
        selected = index;
    }
    public void nextModule() { selected = (selected + 1) % moduleCount; }
    public void tab(Page target) {
        if (target != Page.MODULES && target != Page.APPEARANCE) throw new IllegalArgumentException("Not a root tab");
        page = target;
        cancelCapture();
    }
    public void settings(int index) { select(index); page = Page.SETTINGS; cancelCapture(); }
    public void edit() {
        if (page == Page.EDITOR) return;
        editorReturn = page;
        page = Page.EDITOR;
        cancelCapture();
    }
    public void captureModule() {
        if (page != Page.SETTINGS) throw new IllegalStateException("Module binding is only available in settings");
        binding = selected;
    }
    public void captureMenu() {
        if (page != Page.APPEARANCE) throw new IllegalStateException("Menu binding is only available in appearance");
        binding = MENU_BINDING;
    }
    public void cancelCapture() { binding = NO_BINDING; }

    /** Returns true only when Escape should close the entire screen. */
    public boolean back() {
        if (capturing()) { cancelCapture(); return false; }
        if (page == Page.EDITOR) { page = editorReturn; return false; }
        if (page == Page.SETTINGS) { page = Page.MODULES; return false; }
        return true;
    }
}

package dev.cubi.ui;

/** Navigation and keyboard focus, independent from Minecraft and OpenGL. */
public final class DeckState {
    public enum Page { MODULES, APPEARANCE, PERFORMANCE, DIAGNOSTICS, SETTINGS, EDITOR }
    public static final int NO_BINDING = -1, MENU_BINDING = -2;
    private final int moduleCount;
    private Page page = Page.MODULES, editorReturn = Page.MODULES;
    private int selected, modulePage, binding = NO_BINDING;

    public DeckState(int moduleCount) {
        if (moduleCount < 1) throw new IllegalArgumentException("At least one HUD module is required");
        this.moduleCount = moduleCount;
    }
    public Page page() { return page; }
    public int selected() { return selected; }
    public int modulePage() { return modulePage; }
    public int modulePages() { return (moduleCount + DeckLayout.MODULES_PER_PAGE - 1) / DeckLayout.MODULES_PER_PAGE; }
    public int visibleModule(int slot) {
        if (slot < 0 || slot >= DeckLayout.MODULES_PER_PAGE) return -1;
        int index = modulePage * DeckLayout.MODULES_PER_PAGE + slot;
        return index < moduleCount ? index : -1;
    }
    public void turnModules(int direction) {
        if (page != Page.MODULES || capturing()) return;
        modulePage = Math.max(0, Math.min(modulePages() - 1, modulePage + direction));
    }
    public int binding() { return binding; }
    public boolean capturing() { return binding != NO_BINDING; }
    public void select(int index) {
        if (index < 0 || index >= moduleCount) throw new IllegalArgumentException("Invalid module index");
        selected = index;
        modulePage = selected / DeckLayout.MODULES_PER_PAGE;
    }
    public void nextModule() { select((selected + 1) % moduleCount); }
    public void tab(Page target) {
        if (target != Page.MODULES && target != Page.APPEARANCE && target != Page.PERFORMANCE) throw new IllegalArgumentException("Not a root tab");
        page = target;
        cancelCapture();
    }
    public void settings(int index) { select(index); page = Page.SETTINGS; cancelCapture(); }
    public void diagnostics() { page = Page.DIAGNOSTICS; cancelCapture(); }
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
        if (page == Page.DIAGNOSTICS) { page = Page.PERFORMANCE; return false; }
        return true;
    }
}

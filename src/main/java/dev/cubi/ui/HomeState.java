package dev.cubi.ui;

/** Focus and deferred actions. Screens only change after vanilla finishes dispatching input. */
public final class HomeState {
    private Object owner;
    private int focus = -1, pending = -1;

    public void bind(Object screen) {
        if (owner != screen) { owner = screen; focus = -1; pending = -1; }
    }
    public int focus() { return focus; }
    public void clearFocus() { focus = -1; }
    public void move(int direction, boolean[] enabled) {
        int next = focus;
        if (next < 0) next = direction > 0 ? -1 : 0;
        for (int i = 0; i < enabled.length; i++) {
            next = (next + direction + enabled.length) % enabled.length;
            if (enabled[next]) { focus = next; return; }
        }
        focus = -1;
    }
    public void request(int control, boolean[] enabled) {
        if (control >= 0 && control < enabled.length && enabled[control] && pending < 0) pending = control;
    }
    public int take(Object current) {
        int result = current == owner ? pending : -1;
        pending = -1;
        return result;
    }
}

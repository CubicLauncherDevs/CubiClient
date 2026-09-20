package dev.cubi.core;

/** A GUI change is committed only after vanilla has finished dispatching input. */
public final class MenuActivation {
    private boolean pending;
    private Object origin;

    public void request(Object screen) {
        pending = true;
        origin = screen;
    }

    public boolean take(Object screen) {
        boolean open = pending && screen == origin;
        pending = false;
        origin = null;
        return open;
    }
}

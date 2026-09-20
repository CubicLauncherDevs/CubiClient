package dev.cubi.performance;

/** Includes every input to vanilla ScaledResolution, including effective Unicode mode. */
public final class ResolutionCache {
    private int width = -1, height = -1, scale = -1;
    private boolean unicode;
    public boolean matches(int w, int h, int guiScale, boolean useUnicode) {
        return w == width && h == height && guiScale == scale && unicode == useUnicode;
    }
    public void update(int w, int h, int guiScale, boolean useUnicode) {
        width = w; height = h; scale = guiScale; unicode = useUnicode;
    }
}

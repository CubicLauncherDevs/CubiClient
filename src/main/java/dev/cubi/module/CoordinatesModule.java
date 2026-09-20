package dev.cubi.module;

import dev.cubi.core.CubiClient;
import dev.cubi.config.ClientConfig;
import dev.cubi.ui.Ink;
import dev.cubi.ui.Theme;

public final class CoordinatesModule extends HudModule {
    private int x, y, z;
    private boolean available;
    private final String[] labels = {"X  --", "Y  --", "Z  --"};
    private static final String[] EXAMPLE = {"X  -128", "Y  64", "Z  256"};
    public CoordinatesModule(ClientConfig config) { super(config, "coordinates", "Coordenadas", "Tu posición en el mundo", "05", 104, 56); }
    @Override public void tick(CubiClient client) throws Throwable {
        Object p = client.game.player();
        if (p == null) {
            if (available) { labels[0] = "X  --"; labels[1] = "Y  --"; labels[2] = "Z  --"; }
            available = false; return;
        }
        int nx = client.game.blockX(p), ny = client.game.blockY(p), nz = client.game.blockZ(p);
        if (!available || nx != x) labels[0] = "X  " + nx;
        if (!available || ny != y) labels[1] = "Y  " + ny;
        if (!available || nz != z) labels[2] = "Z  " + nz;
        x = nx; y = ny; z = nz; available = true;
    }
    @Override public void paint(CubiClient client, Ink ink, boolean preview) throws Throwable {
        plate(ink);
        for (int i = 0; i < 3; i++) fittedText(ink, preview ? EXAMPLE[i] : labels[i], 8, 8 + i * 15, width - 16, 10, Theme.TEXT);
    }
    @Override public int icon() { return 9; }
    @Override public void clearData() { available = false; labels[0] = "X  --"; labels[1] = "Y  --"; labels[2] = "Z  --"; }
}

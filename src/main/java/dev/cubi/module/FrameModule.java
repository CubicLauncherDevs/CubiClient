package dev.cubi.module;

import dev.cubi.core.CubiClient;
import dev.cubi.ui.Ink;

public final class FrameModule extends HudModule {
    public FrameModule(CubiClient client) { super(client, "frames", "FPS", "Fluidez de tu juego", "01", 70, 24); }

    @Override
    public void paint(CubiClient client, Ink ink, boolean preview) throws Throwable {
        metric(ink, preview ? "144" : client.fpsLabel, "FPS");
    }
}

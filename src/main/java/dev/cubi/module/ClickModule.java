package dev.cubi.module;

import dev.cubi.core.CubiClient;
import dev.cubi.ui.Ink;

public final class ClickModule extends HudModule {
    public ClickModule(CubiClient client) { super(client, "clicks", "CPS", "Cada clic cuenta", "02", 86, 24); }

    @Override
    public void paint(CubiClient client, Ink ink, boolean preview) throws Throwable {
        metric(ink, preview ? "8 | 6" : client.clicksLabel, "CPS");
    }
}

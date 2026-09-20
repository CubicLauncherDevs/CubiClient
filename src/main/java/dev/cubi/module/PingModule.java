package dev.cubi.module;

import dev.cubi.core.CubiClient;
import dev.cubi.config.ClientConfig;
import dev.cubi.ui.Ink;

public final class PingModule extends HudModule {
    private int previous = Integer.MIN_VALUE;
    private String label = "--";
    public PingModule(ClientConfig config) { super(config, "ping", "Ping", "Latencia de tu conexión", "03", 70, 24); }
    @Override public void tick(CubiClient client) throws Throwable {
        int value = client.game.ping();
        if (value != previous) { previous = value; label = HudValues.ping(value); }
    }
    @Override public void paint(CubiClient client, Ink ink, boolean preview) throws Throwable {
        metric(ink, preview ? "42" : label, "ms");
    }
    @Override public int icon() { return 7; }
    @Override public void clearData() { previous = Integer.MIN_VALUE; label = "--"; }
}

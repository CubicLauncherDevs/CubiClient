package dev.cubi.module;

import dev.cubi.core.CubiClient;
import dev.cubi.config.ClientConfig;
import dev.cubi.ui.Ink;
import dev.cubi.ui.Theme;
import dev.cubi.ui.ServerIcon;

public final class ServerModule extends HudModule {
    private String address = "Sin conexión", label = address;
    private ServerIcon image;
    public ServerModule(ClientConfig config) { super(config, "server", "Servidor", "Icono y dirección del servidor", "06", 136, 24); }
    @Override public void tick(CubiClient client) throws Throwable {
        String next = client.game.server();
        if (!next.equals(address)) { address = next; label = client.ink.ellipsize(next, width - 30, 9, true); }
        if (image == null) image = new ServerIcon(client.game);
        image.update(client.game.serverIcon());
    }
    @Override public void paint(CubiClient client, Ink ink, boolean preview) throws Throwable {
        plate(ink);
        if (image != null && image.texture() != 0) ink.image(image.texture(), 4, 4, 16);
        else ink.icon(10, 4, 4, 16, Theme.SECONDARY);
        fittedText(ink, preview && address.equals("Sin conexión") ? "play.example.net" : label,
                25, 8, width - 30, 9, Theme.TEXT);
    }
    @Override public int icon() { return 10; }
    @Override public void clearData() throws Throwable {
        address = "Sin conexión"; label = address;
        if (image != null) image.clear();
    }
}

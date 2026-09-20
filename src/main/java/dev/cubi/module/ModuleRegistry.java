package dev.cubi.module;

import dev.cubi.core.CubiClient;
import dev.cubi.config.ClientConfig;
import dev.cubi.core.ClientIdentity;
import dev.cubi.ui.HudPlacement;
import dev.cubi.ui.Theme;

/** Typed lifecycle dispatch. Stable array: no stream, reflection or event allocation. */
public final class ModuleRegistry {
    public static final int COUNT = 6;
    public final HudModule[] all;
    public final FrameModule frames;
    public final KeysModule keys;
    private Object previousWorld;

    public ModuleRegistry(ClientConfig config) {
        frames = new FrameModule(config);
        keys = new KeysModule(config);
        all = new HudModule[] {frames, keys, new PingModule(config), new ArmorModule(config),
                new CoordinatesModule(config), new ServerModule(config)};
    }

    public void tick(CubiClient client) throws Throwable {
        Object world = client.game.worldIdentity();
        if (world != previousWorld) {
            for (HudModule module : all) module.clearData();
            previousWorld = world;
        }
        for (HudModule module : all) if (module.state.enabled) module.tick(client);
    }
    public boolean visible(CubiClient client) {
        if (client.config.watermark) return true;
        for (HudModule module : all) if (module.state.enabled) return true;
        return false;
    }
    public void render(CubiClient client) throws Throwable {
        for (HudModule module : all) if (module.state.enabled) module.render(client, false);
        if (client.config.watermark) {
            float right = 29 + client.ink.width(ClientIdentity.NAME, 10, true);
            int y = HudPlacement.watermarkY(client.game.width, client.game.height, right);
            boolean batch = client.config.performance.hudBatching;
            if (batch) client.ink.beginBatch();
            try {
                client.ink.icon(0, 12, y, 13, client.accent());
                client.ink.text(ClientIdentity.NAME, 29, y + 2, 10, true, Theme.TEXT);
            } finally { if (batch) client.ink.endBatch(); }
        }
    }
    public void resetLayout(CubiClient client) {
        resetLayout(client.config, client.game.width, client.game.height);
    }
    public void resetLayout(ClientConfig config, int width, int height) {
        for (int i = 0; i < all.length; i++) { all[i].state.scale = 1; placeDefault(width, height, i); }
        config.layoutInitialized = true;
        config.layoutRevision = 2;
        config.changed();
    }
    public void migrateLayout(ClientConfig config, int width, int height) {
        if (!config.layoutInitialized) { resetLayout(config, width, height); return; }
        for (int i = 0; i < all.length; i++) {
            HudModule module = all[i];
            if (module.newState) { placeDefault(width, height, i); config.changed(); }
            else if (config.layoutRevision < 2 && i < 2) {
                float x = module.state.x * Math.max(0, width - 96 * module.state.scale);
                float y = module.state.y * Math.max(0, height - (module.id.equals("keys") ? 112 : 42) * module.state.scale);
                module.position(x, y, width, height);
            }
        }
        if (config.layoutRevision < 2) { config.layoutRevision = 2; config.changed(); }
    }
    private void placeDefault(int width, int height, int index) {
        HudModule module = all[index];
        module.position(HudPlacement.defaultX(index, width, module.pixelWidth()),
                HudPlacement.defaultY(index), width, height);
        module.newState = false;
    }
}

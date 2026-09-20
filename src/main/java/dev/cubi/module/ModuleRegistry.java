package dev.cubi.module;

import dev.cubi.core.CubiClient;

/** Typed lifecycle dispatch. Stable array: no stream, reflection or event allocation. */
public final class ModuleRegistry {
    public final HudModule[] all;

    public ModuleRegistry(CubiClient client) {
        all = new HudModule[] {new FrameModule(client), new ClickModule(client), new KeysModule(client)};
    }

    public void tick(CubiClient client) throws Throwable {
        for (HudModule module : all) if (module.state.enabled) module.tick(client);
    }
    public void render(CubiClient client) throws Throwable {
        for (HudModule module : all) if (module.state.enabled) module.render(client, false);
        if (client.config.watermark) {
            client.ink.icon(0, 12, client.game.height - 23, 13, client.accent());
            client.ink.text("cubi", 29, client.game.height - 21, 11, true, 0xDDFFFFFF);
        }
    }
    public void resetLayout(CubiClient client) {
        int y = 12;
        for (HudModule module : all) {
            module.state.scale = 1;
            module.position(12, y, client.game.width, client.game.height);
            y += module.height + 8;
        }
        client.config.layoutInitialized = true;
        client.config.layoutRevision = 2;
        client.config.changed();
    }
    public void migrateLayout(CubiClient client) {
        if (!client.config.layoutInitialized) { resetLayout(client); return; }
        if (client.config.layoutRevision >= 2) return;
        for (HudModule module : all) {
            float x = module.state.x * Math.max(0, client.game.width - 96 * module.state.scale);
            float y = module.state.y * Math.max(0, client.game.height - (module.id.equals("keys") ? 112 : 42) * module.state.scale);
            module.position(x, y, client.game.width, client.game.height);
        }
        client.config.layoutRevision = 2;
        client.config.changed();
    }
}

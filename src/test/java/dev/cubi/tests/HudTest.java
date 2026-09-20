package dev.cubi.tests;

import dev.cubi.config.ClientConfig;
import dev.cubi.module.HudModule;
import dev.cubi.module.HudValues;
import dev.cubi.module.ModuleRegistry;
import dev.cubi.ui.DeckLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Data boundaries and real registry/configuration migration without Minecraft or OpenGL. */
public final class HudTest {
    private static int assertions;
    private HudTest() { }
    public static int run() throws Exception {
        assertions = 0;
        values(); defaults(); migration();
        return assertions;
    }
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static void values() {
        check(HudValues.block(-0.01) == -1 && HudValues.block(-128.99) == -129
                && HudValues.block(-128) == -128, "Negative coordinates use block floor, not truncation");
        check(HudValues.block(0) == 0 && HudValues.block(29_999_999.99) == 29_999_999
                && HudValues.block(-29_999_999.99) == -30_000_000, "Coordinates handle zero and both world borders");
        check(HudValues.ping(-1).equals("--") && HudValues.ping(0).equals("0")
                && HudValues.ping(1234).equals("1234"), "Missing ping differs from a valid zero or high latency");
        check(HudValues.server(false, false, "old.example.net").equals("Sin conexión"), "Disconnect clears stale server address");
        check(HudValues.server(true, true, "old.example.net").equals("Un jugador"), "Local worlds override old multiplayer address");
        check(HudValues.server(true, false, "play.example.net:25570").equals("play.example.net:25570")
                && HudValues.server(true, false, "[2001:db8::1]:25565").equals("[2001:db8::1]:25565"), "Hostname, port and IPv6 are preserved");
        check(HudValues.server(true, false, null).equals("Servidor desconocido")
                && HudValues.server(true, false, " ").equals("Servidor desconocido"), "Missing remote metadata is not mislabeled as singleplayer");
        check(HudValues.durability(363, 0, true) == 100 && HudValues.durability(363, 1, true) == 99
                && HudValues.durability(363, 363, true) == 0, "Durability is remaining percentage with conservative rounding");
        check(HudValues.durability(100, -50, true) == 100 && HudValues.durability(100, 150, true) == 0
                && HudValues.durability(Integer.MAX_VALUE, Integer.MIN_VALUE, true) == 100, "Invalid damage clamps without overflow");
        check(HudValues.durability(0, 0, true) == HudValues.UNBREAKABLE
                && HudValues.durability(363, 0, false) == HudValues.UNBREAKABLE, "Non-damageable equipment and Unbreakable NBT have no percentage");
        check(HudValues.durabilityLabel(HudValues.EMPTY).equals("--")
                && HudValues.durabilityLabel(HudValues.UNBREAKABLE).equals("N/A")
                && HudValues.durabilityLabel(15).equals("15%"), "Empty, unbreakable and damaged armor have distinct labels");
    }
    private static void defaults() {
        ClientConfig config = new ClientConfig();
        ModuleRegistry registry = new ModuleRegistry(config);
        check(registry.all.length == ModuleRegistry.COUNT && registry.all.length == 6, "Six HUD modules registered");
        Set<String> ids = new HashSet<String>();
        for (HudModule module : registry.all) ids.add(module.id);
        check(ids.size() == 6 && ids.contains("frames") && ids.contains("keys") && ids.contains("ping")
                && ids.contains("armor") && ids.contains("coordinates") && ids.contains("server"), "Stable, unique module IDs");
        for (int[] size : new int[][] {{320, 240}, {426, 240}, {854, 480}, {1920, 1080}}) {
            registry.resetLayout(config, size[0], size[1]);
            DeckLayout.Rect[] bounds = new DeckLayout.Rect[registry.all.length];
            boolean fits = true, separate = true;
            for (int i = 0; i < bounds.length; i++) {
                HudModule m = registry.all[i];
                bounds[i] = new DeckLayout.Rect(m.x(size[0]), m.y(size[1]), m.pixelWidth(), m.pixelHeight());
                fits &= bounds[i].x >= 0 && bounds[i].y >= 0 && bounds[i].x + bounds[i].w <= size[0]
                        && bounds[i].y + bounds[i].h < DeckLayout.editorY(size[1]);
                for (int j = 0; j < i; j++) separate &= !bounds[i].intersects(bounds[j]);
            }
            check(fits && separate, "Default HUD fits without overlap or editor collision at " + size[0] + "x" + size[1]);
        }
    }
    private static void migration() throws Exception {
        Path directory = Files.createTempDirectory("cubi-hud-test-");
        Path file = directory.resolve("config.json");
        try {
            ClientConfig config = ClientConfig.load(file);
            config.layoutInitialized = true; config.layoutRevision = 2;
            config.state("frames").x = 0.6f; config.state("frames").key = 37;
            config.state("keys").y = 0.43f; config.state("keys").scale = 1.25f;
            config.state("keys").enabled = false; config.state("clicks").key = 36;
            config.state("ping").x = 0.33f; config.state("ping").y = 0.47f;
            config.state("ping").key = 40; config.state("ping").opacity = 0.7f;
            check(config.save(), "Pre-existing HUD configuration saves");
            ClientConfig loaded = ClientConfig.load(file);
            ModuleRegistry registry = new ModuleRegistry(loaded);
            registry.migrateLayout(loaded, 854, 480);
            check(loaded.state("frames").x == 0.6f && loaded.state("frames").key == 37
                    && loaded.state("keys").y == 0.43f && loaded.state("keys").scale == 1.25f
                    && !loaded.state("keys").enabled && loaded.state("clicks").key == 36,
                    "Adding widgets preserves old positions, scale, bindings, visibility and legacy clicks");
            check(loaded.state("ping").x == 0.33f && loaded.state("ping").y == 0.47f
                    && loaded.state("ping").key == 40 && loaded.state("ping").opacity == 0.7f,
                    "Already configured new modules are not reset");
            check(registry.all[3].x(854) == 706 && registry.all[3].y(480) == 58
                    && registry.all[4].y(480) == 108 && registry.all[5].y(480) == 12,
                    "Only missing modules receive default placement");
            registry.all[3].position(200, 150, 854, 480);
            registry.migrateLayout(loaded, 854, 480);
            check(registry.all[3].x(854) == 200 && registry.all[3].y(480) == 150, "Migration is idempotent in the same session");
            loaded.state("server").enabled = false; loaded.state("server").key = 45;
            loaded.changed(); check(loaded.save(), "New modules persist");
            ClientConfig restarted = ClientConfig.load(file);
            ModuleRegistry again = new ModuleRegistry(restarted);
            again.migrateLayout(restarted, 854, 480);
            check(again.all[3].x(854) == 200 && again.all[3].y(480) == 150
                    && !again.all[5].state.enabled && again.all[5].state.key == 45, "New placement and controls survive restart");
            java.nio.file.attribute.FileTime modified = Files.getLastModifiedTime(file);
            check(restarted.save() && modified.equals(Files.getLastModifiedTime(file)), "An unchanged restart does not write configuration");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
    }
}

package dev.cubi.tests;

import dev.cubi.core.CubiClient;
import dev.cubi.core.ClientIdentity;
import dev.cubi.core.Hooks;
import dev.cubi.bridge.Game189;
import dev.cubi.config.ClientConfig;
import dev.cubi.ui.ControlDeck;
import dev.cubi.ui.DeckLayout;
import dev.cubi.ui.DeckState;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Real OpenGL test of either the library or the packaged replacement jar. */
public final class SmokeTest {
    private static CubiClient client;
    private static Object minecraft;
    private static Method schedule;
    private static long keyboardTimestamp = System.nanoTime();
    private static int originalMenuKey;
    private static boolean originalRepeatEvents;
    private SmokeTest() { }

    public static void main(String[] arguments) throws Exception {
        boolean replacement = Boolean.getBoolean("cubi.smoke.replacement");
        if (replacement) {
            File expected = new File(System.getProperty("cubi.smoke.jar")).getCanonicalFile();
            File actual = new File(CubiClient.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalFile();
            require(expected.equals(actual), "Cubi is loaded from the replacement artifact, not build/classes");
            for (String dependency : new String[] {"net.minecraft.launchwrapper.Launch", "org.objectweb.asm.ClassWriter"}) {
                try {
                    Class.forName(dependency, false, SmokeTest.class.getClassLoader());
                    throw new AssertionError("Unexpected runtime dependency: " + dependency);
                } catch (ClassNotFoundException expectedAbsence) { /* Deliberately absent. */ }
            }
            Class<?> entry = Class.forName("net.minecraft.client.main.Main", false, SmokeTest.class.getClassLoader());
            require(expected.equals(new File(entry.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalFile()),
                    "Minecraft entry is loaded from the replacement artifact");
            System.out.println("PASS / DIRECT: replacement artifact with vanilla libraries only.");
        }
        Thread automation = new Thread(new Runnable() {
            @Override public void run() {
                try { exercise(); }
                catch (Throwable error) { error.printStackTrace(); System.exit(2); }
            }
        }, "cubi-smoke-test");
        automation.setDaemon(true);
        automation.start();
        String entry = replacement ? "net.minecraft.client.main.Main" : "net.minecraft.launchwrapper.Launch";
        Class.forName(entry).getDeclaredMethod("main", String[].class).invoke(null, (Object) arguments);
    }

    private static void exercise() throws Exception {
        Field instance = Hooks.class.getDeclaredField("client"); instance.setAccessible(true);
        Field failed = Hooks.class.getDeclaredField("failed"); failed.setAccessible(true);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
        while (client == null && System.nanoTime() < deadline) {
            if (failed.getBoolean(null)) throw new AssertionError("Cubi initialization failed");
            client = (CubiClient) instance.get(null);
            Thread.sleep(100);
        }
        if (client == null) throw new AssertionError("Minecraft did not initialize within 90s");
        Class<?> mc = Game189.type("ave");
        minecraft = mc.getDeclaredMethod("A").invoke(null);
        schedule = mc.getDeclaredMethod("a", Runnable.class);
        onGame(new Action() { @Override public void run() throws Throwable {
            // Isolate this automated process from clicks/keys in the shared desktop.
            // Callbacks below are driven explicitly; the shipping jar has no test code.
            for (Class<?> input : new Class<?>[] {Keyboard.class, org.lwjgl.input.Mouse.class}) {
                Field queue = input.getDeclaredField("readBuffer");
                queue.setAccessible(true);
                queue.set(null, java.nio.ByteBuffer.allocateDirect(0));
            }
            client.modules.resetLayout(client);
            client.open();
        } });
        Thread.sleep(1000);
        onGame(new Action() {
            @Override public void run() throws Throwable {
                require(client.game.screen() == client.controlScreen, "Generated GuiScreen opens");
                require(Display.getTitle().startsWith(ClientIdentity.NAME), "Game window shows the full client name");
                requirePage(DeckState.Page.MODULES, "Screen opens on the module browser");
                require(client.game.movementName(0).equals("W") && client.game.movementName(1).equals("A")
                        && client.game.movementName(2).equals("S") && client.game.movementName(3).equals("D"), "Movement mappings match vanilla controls");
                screenshot("01-control-deck.png");
                boolean original = client.modules.all[0].state.enabled;
                click(DeckLayout.TOGGLES[0]);
                require(client.modules.all[0].state.enabled != original, "Module toggle");
                requirePage(DeckState.Page.MODULES, "Toggling a module does not open its settings");
                click(DeckLayout.TOGGLES[0]);
                click(DeckLayout.SETTINGS[0]);
                requirePage(DeckState.Page.SETTINGS, "Gear opens module settings");
            }
        });
        Thread.sleep(600);
        onGame(new Action() {
            @Override public void run() throws Throwable {
                screenshot("05-module-settings.png");
                float scale = client.modules.all[0].state.scale;
                click(DeckLayout.SCALE_PLUS);
                require(client.modules.all[0].state.scale > scale, "Scale control");
                click(DeckLayout.SCALE_MINUS);
                click(DeckLayout.MODULE_BIND);
                ControlDeck.type('g', Keyboard.KEY_G);
                require(client.modules.all[0].state.key == Keyboard.KEY_G, "Binding capture");
                click(DeckLayout.MODULE_BIND);
                ControlDeck.type('\b', Keyboard.KEY_BACK);
                require(client.modules.all[0].state.key == 0, "Binding clear");
                boolean background = client.modules.all[0].state.background;
                click(DeckLayout.BACKGROUND);
                require(client.modules.all[0].state.background != background, "Background control");
                click(DeckLayout.BACKGROUND);
                boolean shadow = client.modules.all[0].state.shadow;
                click(DeckLayout.SHADOW);
                require(client.modules.all[0].state.shadow != shadow, "Text shadow control");
                click(DeckLayout.SHADOW);
                click(DeckLayout.OPACITY.x + DeckLayout.OPACITY.w - 10, DeckLayout.OPACITY.centerY());
                ControlDeck.release(0, 0, 0);
                require(client.modules.all[0].state.opacity > 0.65f, "Opacity slider");
                click(DeckLayout.APPEARANCE_TAB);
                requirePage(DeckState.Page.APPEARANCE, "Global settings are on their own tab");
            }
        });
        Thread.sleep(600);
        onGame(new Action() {
            @Override public void run() throws Throwable {
                screenshot("06-appearance.png");
                boolean watermark = client.config.watermark;
                click(DeckLayout.WATERMARK);
                require(client.config.watermark != watermark, "Full-name watermark can be disabled");
                click(DeckLayout.WATERMARK);
                click(DeckLayout.ACCENTS[1]);
                require(client.config.accent == 1, "Accent swatch");
                ClientConfig persisted = ClientConfig.load(client.game.directory.toPath().resolve("cubiclient/config.json"));
                require(persisted.accent == 1 && persisted.state("frames").opacity > 0.65f, "Visual settings persist");
                click(DeckLayout.ACCENTS[0]);
                performanceRegression();
                client.modules.all[0].state.opacity = 0.48f;
                client.config.changed();
                click(DeckLayout.MODULES_TAB);
                click(DeckLayout.SETTINGS[0]);
                click(DeckLayout.EDIT);
                requirePage(DeckState.Page.EDITOR, "Editor opens from module settings");
                float x = client.modules.all[0].state.x;
                ControlDeck.type('\0', Keyboard.KEY_RIGHT);
                require(client.modules.all[0].state.x > x, "Editor movement");
            }
        });
        Thread.sleep(1000);
        onGame(new Action() {
            @Override public void run() throws Throwable {
                require(client.controlScreen != null && client.game.screen() == client.controlScreen, "Editor remains open");
                screenshot("02-layout-editor.png");
                ControlDeck.type('\0', Keyboard.KEY_ESCAPE);
                requirePage(DeckState.Page.SETTINGS, "Editor returns to the page it came from");
                client.modules.resetLayout(client);
                ControlDeck.type('\0', Keyboard.KEY_ESCAPE);
                requirePage(DeckState.Page.MODULES, "Escape from settings returns to modules");
                ControlDeck.type('\0', Keyboard.KEY_ESCAPE);
                require(client.game.screen() != client.controlScreen, "Screen closes to previous menu");
                require(!failed.getBoolean(null), "No hook errors during rendering");
                require(org.lwjgl.opengl.GL11.glGetError() == org.lwjgl.opengl.GL11.GL_NO_ERROR, "UI leaves no GL error");
                System.out.println("PASS / OpenGL: atlas, menu, toggles, scale, bindings, opacity, accent, persistence and editor.");
                Object settings = minecraft.getClass().getDeclaredField("t").get(minecraft);
                settings.getClass().getDeclaredField("z").setBoolean(settings, false); // pauseOnLostFocus
                settings.getClass().getDeclaredField("c").setInt(settings, 4); // renderDistanceChunks
                settings.getClass().getDeclaredField("aA").setBoolean(settings, false); // hideGUI
                settings.getClass().getDeclaredField("aC").setBoolean(settings, false); // showDebugInfo
                Class<?> worldSettings = Game189.type("adp");
                Class<?> gameType = Game189.type("adp$a");
                Class<?> worldType = Game189.type("adr");
                Object creative = gameType.getDeclaredMethod("a", int.class).invoke(null, 1);
                Object flat = worldType.getDeclaredField("c").get(null);
                Object options = worldSettings.getConstructor(long.class, gameType, boolean.class, boolean.class, worldType)
                        .newInstance(189L, creative, false, false, flat);
                minecraft.getClass().getDeclaredMethod("a", String.class, String.class, worldSettings)
                        .invoke(minecraft, "cubi-smoke", "Cubi smoke test", options);
            }
        });
        final boolean[] ready = {false};
        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (!ready[0] && System.nanoTime() < deadline) {
            Thread.sleep(500);
            onGame(new Action() { @Override public void run() throws Throwable { ready[0] = client.game.inWorld() && client.game.screen() == null; } });
        }
        require(ready[0], "Singleplayer world loads");
        // Wait for an actual sample rather than assuming a slow chunk upload finishes in 3s.
        final boolean[] measured = {false};
        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!measured[0] && System.nanoTime() < deadline) {
            Thread.sleep(500);
            onGame(new Action() { @Override public void run() { measured[0] = !client.performanceLabel.contains("--"); } });
        }
        onGame(new Action() {
            @Override public void run() throws Throwable {
                require(!failed.getBoolean(null), "No HUD errors in world");
                require(!client.performanceLabel.contains("--"), "HUD was rendered and measured");
                screenshot("03-in-world.png");
                particleVisibilityRegression();
                System.out.println("PASS / WORLD: vanilla singleplayer + HUD. Last elapsed HUD sample: " + client.performanceLabel);
                client.open();
                click(DeckLayout.PERFORMANCE_TAB);
            }
        });
        Thread.sleep(600);
        onGame(new Action() {
            @Override public void run() throws Throwable {
                screenshot("07-performance.png");
                click(DeckLayout.DIAGNOSTICS);
            }
        });
        Thread.sleep(600);
        onGame(new Action() {
            @Override public void run() throws Throwable {
                screenshot("08-diagnostics.png");
                click(DeckLayout.CAPTURE_START);
                require(client.capture.active(), "Diagnostics starts from the performance UI");
                ControlDeck.type('\0', client.config.menuKey);
            }
        });
        Thread.sleep(8000);
        onGame(new Action() {
            @Override public void run() throws Throwable {
                client.open();
                click(DeckLayout.PERFORMANCE_TAB); click(DeckLayout.DIAGNOSTICS);
                click(DeckLayout.CAPTURE_STOP);
                require(client.capture.count() > 0 && client.capture.summary() != null, "Frame hooks record gameplay after warmup");
                click(DeckLayout.CAPTURE_EXPORT);
                require(client.config.status.startsWith("Exportada"), "Capture exports from the diagnostic UI");
                require(org.lwjgl.opengl.GL11.glGetError() == org.lwjgl.opengl.GL11.GL_NO_ERROR, "Performance views leave no GL error");
                System.out.println("PASS / CAPTURE: frame/stage hooks, warmup, summary and JSON export; " + client.capture.count() + " frames (functional test, not a benchmark).");
            }
        });
        Thread.sleep(600);
        onGame(new Action() {
            @Override public void run() throws Throwable {
                screenshot("09-diagnostic-result.png");
                ControlDeck.type('\0', client.config.menuKey);
                originalMenuKey = client.config.menuKey;
                originalRepeatEvents = Keyboard.areRepeatEventsEnabled();
                client.config.menuKey = Keyboard.KEY_RSHIFT;
                Keyboard.enableRepeatEvents(true);
                sendKey(Keyboard.KEY_RSHIFT, true, false);
                requireMenuOpen("RSHIFT opens the menu in-world and keeps it open through the whole tick");
                Object opened = client.controlScreen;
                sendKey(Keyboard.KEY_RSHIFT, true, true);
                require(client.game.screen() == opened, "Holding the opening key does not close the menu");
                sendKey(Keyboard.KEY_RSHIFT, false, false);
                require(client.game.screen() == opened, "Releasing the opening key keeps the menu open");
            }
        });
        Thread.sleep(1000);
        onGame(new Action() {
            @Override public void run() throws Throwable {
                try {
                    requireMenuOpen("Menu remains visible across subsequent world ticks");
                    screenshot("04-in-world-menu.png");
                    keyboardRegression();
                    require(!failed.getBoolean(null), "No hook errors after native keyboard regression");
                    System.out.println("PASS / KEYBOARD: native event queue, in-world opening, repeat, release, closing, rebinding, module shortcuts and chat.");
                } finally {
                    client.config.menuKey = originalMenuKey;
                    Keyboard.enableRepeatEvents(originalRepeatEvents);
                    client.config.changed(); client.config.save();
                }
                minecraft.getClass().getDeclaredMethod("m").invoke(minecraft);
            }
        });
    }

    private static void performanceRegression() throws Throwable {
        dev.cubi.performance.VideoSettings original = client.game.videoSettings();
        dev.cubi.performance.PerformanceSettings saved = client.config.performance;
        client.config.performance = new dev.cubi.performance.PerformanceSettings();
        try {
            click(DeckLayout.PERFORMANCE_TAB);
            requirePage(DeckState.Page.PERFORMANCE, "Performance tab opens");
            click(DeckLayout.PROFILES[1]);
            dev.cubi.performance.VideoSettings competitive = client.game.videoSettings();
            require(!competitive.fancy && competitive.clouds == 0 && !competitive.shadows, "Competitive applies real vanilla fields");
            click(DeckLayout.PERFORMANCE_OPTIONS[9]);
            require(!client.config.performance.particleCulling && client.config.performance.profile.equals("custom"), "Culling can be independently disabled");
            click(DeckLayout.RESTORE_PERFORMANCE);
            require(client.game.videoSettings().same(original) && client.config.performance.particleCulling, "Restore recovers original video and optimization controls");
            click(DeckLayout.DIAGNOSTICS);
            requirePage(DeckState.Page.DIAGNOSTICS, "Diagnostics opens separately");
            ControlDeck.type('\0', Keyboard.KEY_ESCAPE);
            requirePage(DeckState.Page.PERFORMANCE, "Diagnostics Escape returns to performance");
        } finally {
            client.game.applyVideo(original); client.config.performance = saved;
            client.config.changed(); client.config.save();
        }
    }

    private static void particleVisibilityRegression() throws Throwable {
        Class<?> particle = Game189.type("beb");
        Field[] camera = {particle.getDeclaredField("aw"), particle.getDeclaredField("ax"), particle.getDeclaredField("ay")};
        double[] previous = new double[3];
        for (int i = 0; i < 3; i++) { previous[i] = camera[i].getDouble(null); camera[i].setDouble(null, 0); }
        boolean culling = client.config.performance.particleCulling;
        int matrixMode = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MATRIX_MODE);
        org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);
        org.lwjgl.opengl.GL11.glPushMatrix(); org.lwjgl.opengl.GL11.glLoadIdentity();
        org.lwjgl.opengl.GL11.glOrtho(-1, 1, -1, 1, -1, 1);
        org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
        org.lwjgl.opengl.GL11.glPushMatrix(); org.lwjgl.opengl.GL11.glLoadIdentity();
        try {
            java.lang.reflect.Constructor<?> constructor = particle.getConstructor(Game189.type("adm"), double.class,
                    double.class, double.class, double.class, double.class, double.class);
            Object inside = constructor.newInstance(client.game.worldIdentity(), 0d, 0d, 0d, 0d, 0d, 0d);
            Object outside = constructor.newInstance(client.game.worldIdentity(), 10d, 0d, 0d, 0d, 0d, 0d);
            client.config.performance.particleCulling = true;
            Hooks.particlesBegin();
            require(Hooks.particleVisible(inside, 0, 1, 1, 0, 0, 1), "Actual frustum retains visible vanilla particle");
            require(!Hooks.particleVisible(outside, 0, 1, 1, 0, 0, 1), "Actual frustum skips offscreen vanilla particle");
            // A culled base particle must return before touching the null vertex buffer.
            particle.getMethod("a", Game189.type("bfd"), Game189.type("pk"), float.class, float.class, float.class,
                    float.class, float.class, float.class).invoke(outside, null, null, 0f, 1f, 1f, 0f, 0f, 1f);
            Hooks.particlesEnd();
            require(Hooks.particleVisible(outside, 0, 1, 1, 0, 0, 1), "Special/out-of-pass rendering bypasses culling");
            require(Hooks.particleCullingAvailable(), "Culling bindings remain healthy");
            System.out.println("PASS / PERFORMANCE: profiles, restore, actual particle frustum and early render return.");
        } finally {
            Hooks.particlesEnd(); client.config.performance.particleCulling = culling;
            for (int i = 0; i < 3; i++) camera[i].setDouble(null, previous[i]);
            org.lwjgl.opengl.GL11.glPopMatrix();
            org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION); org.lwjgl.opengl.GL11.glPopMatrix();
            org.lwjgl.opengl.GL11.glMatrixMode(matrixMode);
        }
    }

    /** Feed real LWJGL records into vanilla's runTick; never call Cubi's key handler directly. */
    private static void sendKey(int key, boolean down, boolean repeat) throws Exception {
        Field queue = Keyboard.class.getDeclaredField("readBuffer");
        queue.setAccessible(true);
        ByteBuffer event = ByteBuffer.allocateDirect(Keyboard.EVENT_SIZE);
        event.putInt(key).put((byte) (down ? 1 : 0)).putInt(0).putLong(++keyboardTimestamp).put((byte) (repeat ? 1 : 0));
        event.flip();
        queue.set(null, event);
        try {
            minecraft.getClass().getDeclaredMethod("s").invoke(minecraft);
            require(!event.hasRemaining(), "Vanilla consumed the queued keyboard event");
        } finally { queue.set(null, ByteBuffer.allocateDirect(0)); }
    }

    private static void requireMenuOpen(String message) throws Throwable {
        require(client.controlScreen != null && client.game.screen() == client.controlScreen, message);
    }

    private static void keyboardRegression() throws Throwable {
        sendKey(Keyboard.KEY_RSHIFT, true, false);
        require(client.game.screen() == null, "Next RSHIFT press closes instead of reopening");
        sendKey(Keyboard.KEY_RSHIFT, false, false);
        require(client.game.screen() == null, "Release after closing does not reopen");
        sendKey(Keyboard.KEY_RSHIFT, true, false);
        requireMenuOpen("Menu can be reopened in the same world");
        sendKey(Keyboard.KEY_RSHIFT, false, false);

        click(DeckLayout.APPEARANCE_TAB);
        click(DeckLayout.MENU_BIND); // Capture a new menu shortcut through the real screen input path.
        sendKey(Keyboard.KEY_RCONTROL, true, false);
        require(client.config.menuKey == Keyboard.KEY_RCONTROL, "Menu shortcut can be rebound");
        requireMenuOpen("Capturing the menu key does not immediately close it");
        sendKey(Keyboard.KEY_RCONTROL, false, false);
        sendKey(Keyboard.KEY_ESCAPE, true, false);
        require(client.game.screen() == null, "Escape returns to the world");
        sendKey(Keyboard.KEY_ESCAPE, false, false);
        sendKey(Keyboard.KEY_RSHIFT, true, false);
        require(client.game.screen() == null, "Old menu shortcut is inactive after rebinding");
        sendKey(Keyboard.KEY_RSHIFT, false, false);
        sendKey(Keyboard.KEY_RCONTROL, true, false);
        requireMenuOpen("Rebound key opens the menu in-world");
        sendKey(Keyboard.KEY_RCONTROL, false, false);

        int oldBinding = client.modules.all[0].state.key;
        boolean oldEnabled = client.modules.all[0].state.enabled;
        try {
            click(DeckLayout.SETTINGS[0]);
            click(DeckLayout.MODULE_BIND);
            sendKey(Keyboard.KEY_G, true, false);
            require(client.modules.all[0].state.key == Keyboard.KEY_G, "Module shortcut captures a native event");
            require(client.modules.all[0].state.enabled == oldEnabled, "Binding capture does not toggle the module");
            sendKey(Keyboard.KEY_G, false, false);
            sendKey(Keyboard.KEY_RCONTROL, true, false);
            require(client.game.screen() == null, "Rebound menu key closes once");
            sendKey(Keyboard.KEY_RCONTROL, false, false);
            sendKey(Keyboard.KEY_G, true, false);
            require(client.modules.all[0].state.enabled != oldEnabled, "Module shortcut works in-world");
            sendKey(Keyboard.KEY_G, true, true);
            require(client.modules.all[0].state.enabled != oldEnabled, "Repeat does not retoggle a module");
            sendKey(Keyboard.KEY_G, false, false);
            sendKey(Keyboard.KEY_G, true, false);
            require(client.modules.all[0].state.enabled == oldEnabled, "Second press toggles module back");
            sendKey(Keyboard.KEY_G, false, false);
        } finally {
            client.modules.all[0].state.key = oldBinding;
            client.modules.all[0].state.enabled = oldEnabled;
            client.config.changed();
        }
        Object chat = Game189.type("awv").getDeclaredConstructor().newInstance();
        client.game.show(chat);
        sendKey(Keyboard.KEY_RCONTROL, true, false);
        require(client.game.screen() == chat, "Menu shortcut does not replace chat");
        sendKey(Keyboard.KEY_RCONTROL, false, false);
        sendKey(Keyboard.KEY_ESCAPE, true, false);
        require(client.game.screen() == null, "Chat closes normally");
        sendKey(Keyboard.KEY_ESCAPE, false, false);
    }

    private static void click(int x, int y) {
        float zoom = DeckLayout.zoom(client.game.width, client.game.height);
        int px = Math.round(DeckLayout.originX(client.game.width, zoom) + x * zoom);
        int py = Math.round(DeckLayout.originY(client.game.height, zoom) + y * zoom);
        ControlDeck.click(px, py, 0);
    }

    private static void click(DeckLayout.Rect rect) { click(rect.centerX(), rect.centerY()); }

    private static void requirePage(DeckState.Page expected, String message) throws Exception {
        Field field = ControlDeck.class.getDeclaredField("nav");
        field.setAccessible(true);
        require(((DeckState) field.get(null)).page() == expected, message);
    }

    private static void screenshot(String name) throws Exception {
        Class<?> helper = Game189.type("avj");
        Class<?> buffer = Game189.type("bfw");
        Object framebuffer = minecraft.getClass().getDeclaredMethod("b").invoke(minecraft);
        helper.getDeclaredMethod("a", File.class, String.class, int.class, int.class, buffer)
                .invoke(null, client.game.directory, name, Display.getWidth(), Display.getHeight(), framebuffer);
        require(new File(client.game.directory, "screenshots/" + name).isFile(), "Screenshot created");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private interface Action { void run() throws Throwable; }
    private static void onGame(final Action action) throws Exception {
        Future<?> result = (Future<?>) schedule.invoke(minecraft, new Runnable() {
            @Override public void run() {
                try { action.run(); }
                catch (Throwable error) { throw new RuntimeException(error); }
            }
        });
        result.get(60, TimeUnit.SECONDS);
    }
}

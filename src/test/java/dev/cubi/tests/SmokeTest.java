package dev.cubi.tests;

import dev.cubi.core.CubiClient;
import dev.cubi.core.Hooks;
import dev.cubi.bridge.Game189;
import dev.cubi.config.ClientConfig;
import dev.cubi.ui.ControlDeck;
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
                require(client.game.movementName(0).equals("W") && client.game.movementName(1).equals("A")
                        && client.game.movementName(2).equals("S") && client.game.movementName(3).equals("D"), "Movement mappings match vanilla controls");
                screenshot("01-control-deck.png");
                boolean original = client.modules.all[0].state.enabled;
                click(144, 133);
                require(client.modules.all[0].state.enabled != original, "Module toggle");
                click(144, 133);
                float scale = client.modules.all[0].state.scale;
                click(114, 303);
                require(client.modules.all[0].state.scale > scale, "Scale control");
                click(46, 303);
                click(432, 253);
                ControlDeck.type('g', Keyboard.KEY_G);
                require(client.modules.all[0].state.key == Keyboard.KEY_G, "Binding capture");
                click(432, 253);
                ControlDeck.type('\b', Keyboard.KEY_BACK);
                require(client.modules.all[0].state.key == 0, "Binding clear");
                boolean background = client.modules.all[0].state.background;
                click(307, 301);
                require(client.modules.all[0].state.background != background, "Background control");
                click(307, 301);
                boolean shadow = client.modules.all[0].state.shadow;
                click(362, 301);
                require(client.modules.all[0].state.shadow != shadow, "Text shadow control");
                click(362, 301);
                click(246, 303);
                ControlDeck.release(0, 0, 0);
                require(client.modules.all[0].state.opacity > 0.65f, "Opacity slider");
                click(435, 302);
                require(client.config.accent == 1, "Accent swatch");
                ClientConfig persisted = ClientConfig.load(client.game.directory.toPath().resolve("cubiclient/config.json"));
                require(persisted.accent == 1 && persisted.state("frames").opacity > 0.65f, "Visual settings persist");
                click(417, 302);
                client.modules.all[0].state.opacity = 0.48f;
                client.config.changed();
                click(410, 34);
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
                client.modules.resetLayout(client);
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
                System.out.println("PASS / WORLD: vanilla singleplayer + HUD. Last CPU sample: " + client.performanceLabel);
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

        click(72, 340); // Capture a new menu shortcut through the real screen input path.
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
            click(432, 253);
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
        float zoom = Math.min(1, Math.min((client.game.width - 16f) / 520, (client.game.height - 16f) / 352));
        int px = Math.round((client.game.width - 520 * zoom) / 2 + x * zoom);
        int py = Math.round((client.game.height - 352 * zoom) / 2 + y * zoom);
        ControlDeck.click(px, py, 0);
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

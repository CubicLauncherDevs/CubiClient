package dev.cubi.core;

import dev.cubi.bridge.Game189;
import dev.cubi.bridge.ScreenFactory;
import dev.cubi.config.ClientConfig;
import dev.cubi.module.HudModule;
import dev.cubi.module.ModuleRegistry;
import dev.cubi.ui.ControlDeck;
import dev.cubi.ui.Ink;
import org.lwjgl.opengl.Display;

public final class CubiClient {
    public final Game189 game;
    public final ClientConfig config;
    public final ModuleRegistry modules;
    public final Ink ink;
    public final ClickWindow left = new ClickWindow(), right = new ClickWindow();
    public String fpsLabel = "0", leftLabel = "0", rightLabel = "0";
    public String clicksLabel = "0 | 0";
    public String performanceLabel = "HUD / -- us";
    public Object controlScreen;
    private int ticks, previousFps = -1, previousLeft = -1, previousRight = -1;
    private long renderNanos, renderSamples;
    private final MenuActivation menuActivation = new MenuActivation();

    public CubiClient() throws Throwable {
        game = new Game189();
        ink = new Ink(game);
        config = ClientConfig.load(game.directory.toPath().resolve("cubiclient/config.json"));
        modules = new ModuleRegistry(this);
        modules.migrateLayout(this);
        config.save();
        Display.setTitle(ClientIdentity.NAME + " | " + ClientIdentity.MINECRAFT);
        System.out.println("[Cubi] Interfaz renovada lista. Abre el menú con tu tecla configurada (RSHIFT por defecto).");
    }

    public void tick() throws Throwable {
        game.resize();
        if (game.screen() != null || !game.inWorld()) { left.clear(); right.clear(); }
        long now = System.nanoTime();
        int fps = game.fps(), l = left.count(now), r = right.count(now);
        if (fps != previousFps) { fpsLabel = Integer.toString(fps); previousFps = fps; }
        if (l != previousLeft || r != previousRight) {
            leftLabel = Integer.toString(l); previousLeft = l;
            rightLabel = Integer.toString(r); previousRight = r;
            clicksLabel = leftLabel + " | " + rightLabel;
        }
        if (ticks++ % 20 == 0) {
            if (renderSamples != 0) performanceLabel = "HUD / " + (renderNanos / renderSamples / 1000) + " us";
            renderSamples = 0; renderNanos = 0;
        }
        modules.tick(this);
        // runTick forwards its current key to a newly opened GuiScreen. Opening here,
        // after that input loop, keeps the opening press from closing the menu again.
        if (menuActivation.take(game.screen())) open();
    }

    public void key(int key) throws Throwable {
        Object screen = game.screen();
        if (key == config.menuKey && (screen == null || screen.getClass().getName().equals("aya"))) {
            menuActivation.request(screen);
            return;
        }
        if (screen == null && game.inWorld()) {
            for (HudModule module : modules.all) {
                if (module.state.key != 0 && module.state.key == key) {
                    module.state.enabled = !module.state.enabled;
                    config.changed();
                }
            }
            config.save();
        }
    }

    public void open() throws Throwable {
        ControlDeck.open(this, game.screen());
        controlScreen = ScreenFactory.create();
        game.show(controlScreen);
    }

    public void recordRender(long nanos) { renderNanos += nanos; renderSamples++; }
    public int accent() { return Ink.ACCENTS[config.accent]; }
}

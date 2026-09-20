package dev.cubi.core;

import dev.cubi.bridge.Game189;
import dev.cubi.bridge.ScreenFactory;
import dev.cubi.config.ClientConfig;
import dev.cubi.module.HudModule;
import dev.cubi.module.ModuleRegistry;
import dev.cubi.ui.ControlDeck;
import dev.cubi.ui.Ink;
import dev.cubi.performance.FrameCapture;
import dev.cubi.performance.PerformanceSettings;
import dev.cubi.performance.VideoSettings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

public final class CubiClient {
    public final Game189 game;
    public final ClientConfig config;
    public final ModuleRegistry modules;
    public final Ink ink;
    public final ClickWindow left = new ClickWindow(), right = new ClickWindow();
    public final FrameCapture capture = new FrameCapture();
    public Object captureWorld;
    private VideoSettings captureVideo;
    private int captureWidth, captureHeight;
    private boolean captureHudVisible;
    public String fpsLabel = "0", leftLabel = "0 CPS", rightLabel = "0 CPS";
    public String performanceLabel = "HUD / -- us";
    public Object controlScreen;
    private int ticks, previousFps = -1, previousLeft = -1, previousRight = -1;
    private long renderNanos, renderSamples;
    private final MenuActivation menuActivation = new MenuActivation();

    public CubiClient() throws Throwable {
        game = new Game189();
        ink = new Ink(game);
        config = ClientConfig.load(game.directory.toPath().resolve("cubiclient/config.json"));
        modules = new ModuleRegistry(config);
        modules.migrateLayout(config, game.width, game.height);
        reconcilePerformance();
        config.save();
        Display.setTitle(ClientIdentity.NAME + " | " + ClientIdentity.MINECRAFT);
        System.out.println("[Cubi] Interfaz renovada lista. Abre el menú con tu tecla configurada (RSHIFT por defecto).");
    }

    public void tick() throws Throwable {
        game.resize();
        if (game.screen() != null || !game.inWorld()) { left.clear(); right.clear(); }
        int l = 0, r = 0;
        if (modules.frames.state.enabled) {
            int fps = game.fps();
            if (fps != previousFps) { fpsLabel = Integer.toString(fps); previousFps = fps; }
        }
        if (modules.keys.state.enabled) {
            long now = System.nanoTime(); l = left.count(now); r = right.count(now);
        } else { left.clear(); right.clear(); }
        if (l != previousLeft || r != previousRight) {
            leftLabel = l + " CPS"; previousLeft = l;
            rightLabel = r + " CPS"; previousRight = r;
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
                    capture.stop("HUD modificado");
                    module.state.enabled = !module.state.enabled;
                    config.changed();
                }
            }
            config.save();
        }
    }

    public void open() throws Throwable {
        reconcilePerformance();
        ControlDeck.open(this, game.screen());
        controlScreen = ScreenFactory.create();
        game.show(controlScreen);
    }

    public void recordRender(long nanos) { renderNanos += nanos; renderSamples++; }
    public int accent() { return Ink.ACCENTS[config.accent]; }

    public void reconcilePerformance() throws Throwable {
        String old = config.performance.profile;
        config.performance.reconcile(game.videoSettings());
        if (!old.equals(config.performance.profile)) { config.changed(); config.save(); }
    }

    public void performanceProfile(boolean competitive) throws Throwable {
        capture.stop("Ajustes modificados");
        PerformanceSettings p = config.performance;
        VideoSettings current = game.videoSettings();
        p.remember(current);
        config.changed(); config.save();
        VideoSettings next = VideoSettings.preset(current, competitive);
        game.applyVideo(next);
        p.particleCulling = true; p.hudBatching = true;
        p.profile = competitive ? "competitive" : "balanced";
        p.appliedVideo = game.videoSettings();
        config.changed(); config.save();
    }

    public void customizeVideo(VideoSettings next) throws Throwable {
        capture.stop("Ajustes modificados");
        config.performance.remember(game.videoSettings());
        config.changed(); config.save();
        game.applyVideo(next);
        config.performance.profile = "custom";
        config.changed(); config.save();
    }

    public void restorePerformance() throws Throwable {
        PerformanceSettings p = config.performance;
        if (p.previousVideo == null) { config.status = "Sin ajustes anteriores"; return; }
        capture.stop("Ajustes restaurados");
        game.applyVideo(p.previousVideo.copy());
        p.particleCulling = p.previousCulling; p.hudBatching = p.previousBatching;
        p.previousVideo = null; p.appliedVideo = null; p.profile = "custom";
        config.changed(); config.save();
    }

    public void startCapture() throws Throwable {
        if (!game.inWorld()) { config.status = "Entra a un mundo para capturar"; return; }
        JsonObject context = new JsonObject();
        context.addProperty("minecraft", ClientIdentity.MINECRAFT);
        context.addProperty("java", System.getProperty("java.version"));
        context.addProperty("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        context.addProperty("processors", Runtime.getRuntime().availableProcessors());
        context.addProperty("maxHeapBytes", Runtime.getRuntime().maxMemory());
        context.addProperty("gpu", GL11.glGetString(GL11.GL_RENDERER));
        context.addProperty("driver", GL11.glGetString(GL11.GL_VERSION));
        context.addProperty("width", Display.getWidth()); context.addProperty("height", Display.getHeight());
        Gson gson = new Gson();
        captureVideo = game.videoSettings();
        captureWidth = Display.getWidth(); captureHeight = Display.getHeight(); captureHudVisible = game.hudVisible();
        context.addProperty("hudVisible", captureHudVisible);
        context.add("video", gson.toJsonTree(captureVideo));
        context.add("cubi", gson.toJsonTree(config));
        context.addProperty("particleCullingAvailable", Hooks.particleCullingAvailable());
        captureWorld = game.worldIdentity();
        capture.start(context);
        config.status = "Captura lista: cierra el menú";
    }

    public boolean captureEligible() throws Throwable {
        if (game.worldIdentity() != captureWorld) { capture.stop("Cambio de mundo"); return false; }
        if (Display.getWidth() != captureWidth || Display.getHeight() != captureHeight
                || game.hudVisible() != captureHudVisible || !game.videoMatches(captureVideo)) {
            capture.stop("Vídeo/HUD modificado"); return false;
        }
        return game.screen() == null && Display.isActive();
    }
}

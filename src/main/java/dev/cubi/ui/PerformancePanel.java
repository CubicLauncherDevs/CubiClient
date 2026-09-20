package dev.cubi.ui;

import dev.cubi.core.CubiClient;
import dev.cubi.core.Hooks;
import dev.cubi.performance.FrameCapture;
import dev.cubi.performance.PerformanceSettings;
import dev.cubi.performance.VideoSettings;
import java.nio.file.Path;
import java.util.Locale;
import static dev.cubi.ui.DeckLayout.*;

/** Settings are read on entry/action, never reflected or rewritten in the draw loop. */
final class PerformancePanel {
    private final CubiClient client;
    private VideoSettings video;
    private final String[] labels = new String[11];
    private String stats = "Sin resultados", stages = "", memory = "", status = "";
    private long nextStatus;

    PerformancePanel(CubiClient client) throws Throwable { this.client = client; refresh(); }

    private void refresh() throws Throwable {
        video = client.game.videoSettings();
        video.sanitize();
        PerformanceSettings p = client.config.performance;
        labels[0] = "Distancia: " + video.distance + " chunks";
        labels[1] = "Límite FPS: " + (video.fps >= 260 ? "Sin límite" : video.fps);
        labels[2] = "Partículas: " + new String[] {"Todas", "Reducidas", "Mínimas"}[video.particles];
        labels[3] = "Nubes: " + new String[] {"No", "Rápidas", "Detalladas"}[video.clouds];
        labels[4] = "Gráficos: " + (video.fancy ? "Detallados" : "Rápidos");
        labels[5] = "Luz suave: " + new String[] {"No", "Mínima", "Máxima"}[video.ambientOcclusion];
        labels[6] = "Sombras de entidades: " + yes(video.shadows);
        labels[7] = "VSync: " + yes(video.vsync);
        labels[8] = "VBO: " + yes(video.vbo);
        labels[9] = "Culling de partículas: " + (Hooks.particleCullingAvailable() ? yes(p.particleCulling) : "No disponible");
        labels[10] = "Agrupar dibujo HUD: " + yes(p.hudBatching);
        FrameCapture.Summary result = client.capture.summary();
        if (result != null) {
            stats = String.format(Locale.ROOT, "%.0f FPS medios / p95 %.2f ms / p99 %.2f ms", result.fps, result.p95, result.p99);
            stages = client.capture.stageSummary(); memory = client.capture.memorySummary();
        } else { stats = "Sin resultados: termina una captura en partida"; stages = ""; memory = ""; }
        nextStatus = 0;
    }

    void draw(boolean diagnostics, int mx, int my) throws Throwable {
        Ink ink = client.ink;
        if (diagnostics) {
            button("Volver", BACK, mx, my, false);
            ink.text("Diagnóstico de rendimiento", 102, 121, 13, true, Theme.TEXT);
            button("Iniciar captura", CAPTURE_START, mx, my, false);
            button("Detener", CAPTURE_STOP, mx, my, false);
            button("Exportar JSON", CAPTURE_EXPORT, mx, my, false);
            if (System.nanoTime() >= nextStatus) { status = client.capture.status(); nextStatus = System.nanoTime() + 1_000_000_000L; }
            ink.text(status, 20, 194, 11, true, Theme.TEXT);
            ink.text(stats, 20, 218, 10, false, Theme.TEXT);
            ink.small(stages, 20, 241, Theme.SECONDARY);
            ink.small(memory, 20, 257, Theme.SECONDARY);
            ink.small("Cierra el menú: 5 s de calentamiento y 60 s de partida enfocada.", 20, 281, Theme.SECONDARY);
            ink.small("Vuelve aquí para exportar. Tiempos transcurridos; no son tiempo GPU puro.", 20, 297, Theme.SECONDARY);
            ink.small("Los menús y la ventana sin foco no aportan muestras de fotograma.", 20, 313, Theme.MUTED);
            return;
        }
        String profile = client.config.performance.profile;
        button("Equilibrado", PROFILES[0], mx, my, profile.equals("balanced"));
        button("Competitivo", PROFILES[1], mx, my, profile.equals("competitive"));
        button("Personalizado", PROFILES[2], mx, my, profile.equals("custom"));
        for (int i = 0; i < labels.length; i++) button(labels[i], PERFORMANCE_OPTIONS[i], mx, my, false);
        ink.small("Optimización local, sin cambiar la simulación", 290, 277, Theme.MUTED);
        button("Restaurar anteriores", RESTORE_PERFORMANCE, mx, my, false);
        button("Diagnóstico", DIAGNOSTICS, mx, my, false);
        ink.small("Perfiles: aplicar al pulsar", 376, 308, Theme.MUTED);
    }

    void click(DeckState nav, int mx, int my) {
        try {
            if (nav.page() == DeckState.Page.DIAGNOSTICS) {
                if (BACK.contains(mx, my)) nav.back();
                else if (CAPTURE_START.contains(mx, my)) client.startCapture();
                else if (CAPTURE_STOP.contains(mx, my)) client.capture.stop("Detenida por el usuario");
                else if (CAPTURE_EXPORT.contains(mx, my)) {
                    Path output = client.capture.export(client.game.directory.toPath().resolve("cubiclient/benchmarks"));
                    client.config.status = "Exportada en cubiclient/benchmarks";
                    System.out.println("[Cubi] Captura exportada: " + output);
                }
            } else if (PROFILES[0].contains(mx, my)) client.performanceProfile(false);
            else if (PROFILES[1].contains(mx, my)) client.performanceProfile(true);
            else if (PROFILES[2].contains(mx, my)) { client.capture.stop("Perfil modificado"); custom(); }
            else if (RESTORE_PERFORMANCE.contains(mx, my)) client.restorePerformance();
            else if (DIAGNOSTICS.contains(mx, my)) nav.diagnostics();
            else for (int i = 0; i < PERFORMANCE_OPTIONS.length; i++) {
                if (PERFORMANCE_OPTIONS[i].contains(mx, my)) { change(i); break; }
            }
            refresh();
        } catch (Throwable error) {
            client.config.status = "Error: consulta el registro";
            System.err.println("[Cubi] Panel de rendimiento: " + error);
        }
    }

    private void change(int index) throws Throwable {
        VideoSettings next = client.game.videoSettings();
        PerformanceSettings p = client.config.performance;
        if (index >= 9) {
            client.capture.stop("Optimización modificada");
            p.remember(next);
            if (index == 9) p.particleCulling = !p.particleCulling;
            else p.hudBatching = !p.hudBatching;
            custom(); return;
        }
        switch (index) {
            case 0: next.distance = next.distance >= 32 ? 2 : next.distance + 2; break;
            case 1: next.fps = next.fps >= 260 ? 30 : next.fps < 60 ? 60 : next.fps < 120 ? 120 : next.fps < 144 ? 144 : next.fps < 240 ? 240 : 260; break;
            case 2: next.particles = (next.particles + 1) % 3; break;
            case 3: next.clouds = (next.clouds + 1) % 3; break;
            case 4: next.fancy = !next.fancy; break;
            case 5: next.ambientOcclusion = (next.ambientOcclusion + 1) % 3; break;
            case 6: next.shadows = !next.shadows; break;
            case 7: next.vsync = !next.vsync; break;
            case 8: next.vbo = !next.vbo; break;
            default: return;
        }
        client.customizeVideo(next);
    }
    private void custom() { client.config.performance.profile = "custom"; client.config.changed(); client.config.save(); }
    private static String yes(boolean value) { return value ? "Sí" : "No"; }
    private void button(String text, Rect r, int mx, int my, boolean selected) throws Throwable {
        client.ink.button(text, r.x, r.y, r.w, r.h, r.contains(mx, my), selected, client.accent());
    }
}

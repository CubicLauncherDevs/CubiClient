package dev.cubi.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cubi.config.ClientConfig;
import dev.cubi.performance.FrameCapture;
import dev.cubi.performance.ParticleVisibility;
import dev.cubi.performance.PerformanceSettings;
import dev.cubi.performance.ResolutionCache;
import dev.cubi.performance.VideoSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class PerformanceTest {
    private static int assertions;
    private PerformanceTest() { }
    static int run() throws Exception {
        assertions = 0;
        resolution(); particles(); captures(); profiles();
        return assertions;
    }
    private static void check(boolean value, String message) {
        assertions++; if (!value) throw new AssertionError(message);
    }

    private static void resolution() {
        ResolutionCache cache = new ResolutionCache();
        check(!cache.matches(854, 480, 0, false), "First resolution must be computed");
        cache.update(854, 480, 0, false);
        check(cache.matches(854, 480, 0, false), "Unchanged resolution can be reused");
        check(!cache.matches(854, 480, 2, false) && !cache.matches(854, 480, 0, true), "Scale and Unicode invalidate resolution");
        check(!cache.matches(1920, 480, 0, false) && !cache.matches(854, 1080, 0, false), "Both display dimensions invalidate resolution");
    }

    private static void particles() {
        float[][] planes = {{1,0,0,1}, {-1,0,0,1}, {0,1,0,1}, {0,-1,0,1}, {0,0,1,1}, {0,0,-1,1}};
        check(visible(planes, 0, 0, 0, 1), "Central billboard is visible");
        check(!visible(planes, 3, 0, 0, 1) && !visible(planes, -3, 0, 0, 1), "Entirely offscreen particles are skipped on both sides");
        check(!visible(planes, 0, 3, 0, 1) && !visible(planes, 0, 0, -3, 1), "Vertical and depth planes are respected");
        check(visible(planes, 1.05f, 0, 0, 1), "Particle touching edge is not culled by its center");
        check(visible(planes, 3, 0, 0, 30), "Large particles use rendered scale rather than collision bounds");
        check(visible(planes, 3, 0, 0, -30), "Negative render scale remains conservative");
        check(visible(planes, Float.NaN, 0, 0, 1) && visible(planes, 3, 0, 0, Float.POSITIVE_INFINITY), "Uncertain bounds retain vanilla rendering");
        check(ParticleVisibility.center(30_000_000, 30_000_002, 0.5f, 30_000_000) == 1, "Interpolation subtracts camera before float conversion");
        check(ParticleVisibility.center(-5, 5, 0.25f, 1) == -3.5f, "Moving particle interpolation matches vanilla");
        java.util.Random random = new java.util.Random(189);
        boolean conservative = true;
        // Reference test: every actual quad corner inside the frustum must keep the billboard.
        for (int i = 0; i < 10000; i++) {
            float x = random.nextFloat() * 4 - 2, y = random.nextFloat() * 4 - 2, z = random.nextFloat() * 4 - 2;
            float size = random.nextFloat() * 20, rx = random.nextFloat() * 2 - 1, rxz = random.nextFloat() * 2 - 1;
            float rz = random.nextFloat() * 2 - 1, ryz = random.nextFloat() * 2 - 1, rxy = random.nextFloat() * 2 - 1;
            float half = size * 0.1f;
            boolean cornerInside = false;
            for (int a = -1; a <= 1; a += 2) for (int b = -1; b <= 1; b += 2) {
                float vx = x + a * rx * half + b * ryz * half;
                float vy = y + b * rxz * half;
                float vz = z + a * rz * half + b * rxy * half;
                cornerInside |= Math.abs(vx) <= 1 && Math.abs(vy) <= 1 && Math.abs(vz) <= 1;
            }
            if (cornerInside && !ParticleVisibility.visible(planes, x, y, z, size, rx, rxz, rz, ryz, rxy)) conservative = false;
        }
        check(conservative, "Rotated billboards with visible corners are never rejected (10000 cases)");
    }
    private static boolean visible(float[][] planes, float x, float y, float z, float size) {
        return ParticleVisibility.visible(planes, x, y, z, size, 1, 1, 0, 0, 1);
    }

    private static void captures() throws Exception {
        FrameCapture capture = new FrameCapture();
        capture.start(new JsonObject());
        capture.begin(-6_000_000_000L, true); capture.end(-1_000_000_000L, true);
        check(capture.count() == 0, "Warmup is excluded even with negative nanoTime");
        capture.begin(0, false); capture.end(10_000_000, true);
        capture.begin(10_000_000, true); capture.end(20_000_000, false);
        check(capture.count() == 0, "Menus/focus transitions cannot contribute partial frames");
        capture.begin(30_000_000, true);
        capture.stageBegin(FrameCapture.TICK, 31_000_000); capture.stageEnd(FrameCapture.TICK, 33_000_000);
        capture.stageBegin(FrameCapture.TICK, 34_000_000); capture.stageEnd(FrameCapture.TICK, 36_000_000);
        capture.end(50_000_000, true);
        capture.stop("Test");
        FrameCapture.Summary result = capture.summary();
        check(result.fps == 50 && result.p95 == 20 && result.p99 == 20, "FPS uses elapsed duration, not the average of instantaneous FPS");
        long[] varied = new long[100];
        for (int i = 0; i < varied.length; i++) varied[i] = (i + 1) * 1_000_000L;
        result = FrameCapture.summarize(varied, varied.length);
        check(result.p95 == 95 && result.p99 == 99 && varied[0] == 1_000_000, "Nearest-rank percentiles preserve the original sample order");
        Path directory = Files.createTempDirectory("cubi-capture-");
        try {
            Path file = capture.export(directory);
            JsonObject json = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
            check(json.getAsJsonArray("stageTotalNanos").get(0).getAsLong() == 4_000_000, "Multiple ticks accumulate in one frame");
            check(json.getAsJsonArray("frameNanos").size() == 1 && json.get("samples").getAsInt() == 1, "Export includes only recorded samples");
            Files.delete(file);
        } finally { Files.delete(directory); }
        capture.start(new JsonObject());
        capture.begin(0, true); capture.end(5_000_000_000L, true);
        capture.begin(5_000_000_000L, true); capture.end(65_000_000_000L, true);
        check(!capture.active() && capture.count() == 1 && capture.summary().p99 == 60000, "Long stalls are retained and duration stops capture");
        capture.start(new JsonObject());
        capture.begin(0, true); capture.end(5_000_000_000L, true);
        for (int i = 0; i < FrameCapture.CAPACITY + 2; i++) { capture.begin(i * 10L, true); capture.end(i * 10L + 10, true); }
        check(!capture.active() && capture.count() == FrameCapture.CAPACITY, "Extremely high FPS stops at capacity without overwriting samples");
    }

    private static void profiles() throws Exception {
        VideoSettings original = new VideoSettings();
        original.distance = 4; original.fps = 144; original.vsync = true; original.vbo = false;
        VideoSettings fast = VideoSettings.preset(original, true);
        check(fast.distance == 4 && fast.fps == 144 && fast.vsync && !fast.vbo, "Competitive preserves a shorter distance and hardware-dependent choices");
        check(!fast.fancy && fast.clouds == 0 && !fast.shadows && fast.particles == 1 && fast.ambientOcclusion == 0, "Competitive reduces documented effects");
        check(original.fancy && original.clouds == 2, "Preset construction does not mutate the prior settings");
        PerformanceSettings p = new PerformanceSettings();
        p.remember(original); p.remember(fast);
        check(p.previousVideo.same(original), "Repeated profile changes retain the first restore point");
        p.appliedVideo = fast; p.profile = "competitive"; p.reconcile(fast.copy());
        check(p.profile.equals("competitive"), "Unchanged vanilla options keep their profile");
        p.reconcile(original);
        check(p.profile.equals("custom"), "External vanilla edits become custom instead of being overwritten");
        Path directory = Files.createTempDirectory("cubi-performance-");
        Path file = directory.resolve("config.json");
        try {
            ClientConfig c = ClientConfig.load(file);
            c.performance = p; c.performance.particleCulling = false; c.changed(); c.save();
            ClientConfig reloaded = ClientConfig.load(file);
            check(!reloaded.performance.particleCulling && reloaded.performance.previousVideo.same(original), "Optimization controls and restore point survive restart");
            Files.write(file, "{\"schema\":1,\"performance\":null}".getBytes(StandardCharsets.UTF_8));
            check(ClientConfig.load(file).performance.hudBatching, "Null performance settings recover without discarding existing configuration");
            Files.write(file, "{\"schema\":1}".getBytes(StandardCharsets.UTF_8));
            check(ClientConfig.load(file).performance.previousVideo == null, "Legacy configuration does not auto-apply a video preset");
        } finally { Files.deleteIfExists(file); Files.delete(directory); }
    }
}

package dev.cubi.tests;

import dev.cubi.camera.ZoomController;
import dev.cubi.camera.ZoomSettings;
import dev.cubi.config.ClientConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Camera math, interruption/rearming and legacy configuration without LWJGL. */
public final class ZoomTest {
    private static int assertions;
    private ZoomTest() { }
    public static int run() throws Exception {
        assertions = 0;
        optics(); interruption(); smoothing(); configuration();
        return assertions;
    }
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.0001, message + ": " + actual + " != " + expected);
    }
    private static void optics() {
        ZoomSettings settings = new ZoomSettings(); settings.smooth = false;
        ZoomController zoom = new ZoomController(); Object world = new Object();
        zoom.update(settings, world, true, false, 0);
        for (float amount : new float[] {2, 4, 8}) {
            settings.magnification = amount;
            zoom.update(settings, world, true, true, 10_000_000);
            for (float fov : new float[] {30, 70, 90, 110}) {
                float result = zoom.fov(fov, true);
                near(Math.tan(Math.toRadians(fov) / 2) / Math.tan(Math.toRadians(result) / 2), amount,
                        "Optical magnification is stable across vanilla FOV values");
                near(zoom.fov(fov, false), fov, "Hand projection remains vanilla");
                near(zoom.fov(fov, true), result, "Repeated FOV queries cannot advance the transition");
            }
            near(zoom.mouse(12), 12 / amount, "Horizontal mouse motion follows optical scale");
            near(zoom.mouse(-8), -8 / amount, "Vertical/inverted mouse direction is preserved");
        }
        settings.adaptSensitivity = false;
        zoom.update(settings, world, true, true, 20_000_000);
        near(zoom.mouse(-12), -12, "Disabling adaptation retains vanilla sensitivity while zoomed");
        zoom.update(settings, world, true, false, 30_000_000);
        near(zoom.fov(90, true), 90, "Release restores normal FOV immediately without smoothing");
        near(zoom.mouse(12), 12, "Release restores full mouse motion");
        check(settings.magnification == 8 && settings.key == 46 && !settings.smooth && !settings.adaptSensitivity,
                "Rendering never mutates persisted settings");
    }
    private static void interruption() {
        ZoomSettings settings = new ZoomSettings(); settings.smooth = false;
        ZoomController zoom = new ZoomController(); Object world = new Object();
        zoom.update(settings, world, true, false, 0);
        zoom.update(settings, world, true, true, 1);
        check(zoom.fov(70, true) < 70, "Holding the key activates zoom");
        zoom.update(settings, world, false, true, 2);
        near(zoom.fov(70, true), 70, "Opening a screen or losing focus cancels immediately");
        near(zoom.mouse(10), 10, "Cancellation also restores sensitivity");
        zoom.update(settings, world, true, true, 3);
        near(zoom.fov(70, true), 70, "Held key cannot reactivate when focus returns");
        zoom.update(settings, world, true, false, 4);
        zoom.update(settings, world, true, true, 5);
        check(zoom.fov(70, true) < 70, "Fresh press rearms zoom");
        world = new Object();
        zoom.update(settings, world, true, true, 6);
        near(zoom.fov(70, true), 70, "World/dimension changes discard held zoom");
        zoom.update(settings, world, true, false, 7);
        zoom.update(settings, world, true, true, 8);
        zoom.cancel();
        near(zoom.mouse(10), 10, "Changing zoom options clears the runtime snapshot");
        zoom.update(settings, world, true, true, 9);
        near(zoom.fov(70, true), 70, "Explicit cancel requires releasing the key");
        zoom.update(settings, world, true, false, 10);
        zoom.update(settings, world, true, true, 11);
        settings.key = 47;
        zoom.update(settings, world, true, true, 12);
        near(zoom.fov(70, true), 70, "Rebinding cannot activate from an already-held new key");
        zoom.update(settings, world, true, false, 13);
        zoom.update(settings, world, true, true, 14);
        settings.enabled = false;
        zoom.update(settings, world, true, true, 15);
        near(zoom.fov(70, true), 70, "Disabled zoom is a no-op");
        settings.enabled = true; settings.key = 0;
        zoom.update(settings, world, true, true, 16);
        near(zoom.fov(70, true), 70, "Unassigned binding cannot zoom");
        settings.key = 46;
        zoom.update(settings, null, true, false, 17);
        zoom.update(settings, null, true, true, 18);
        near(zoom.fov(70, true), 70, "No world means no camera effect");
    }
    private static void smoothing() {
        ZoomSettings settings = new ZoomSettings(); Object world = new Object();
        ZoomController slow = new ZoomController(), fast = new ZoomController();
        slow.update(settings, world, true, false, 0); fast.update(settings, world, true, false, 0);
        for (int i = 1; i <= 6; i++) slow.update(settings, world, true, true, i * 1_000_000_000L / 30);
        for (int i = 1; i <= 48; i++) fast.update(settings, world, true, true, i * 1_000_000_000L / 240);
        near(slow.fov(70, true), fast.fov(70, true), "Equal elapsed time gives the same zoom at 30 and 240 FPS");
        check(slow.fov(70, true) > 19.8f && slow.fov(70, true) < 70, "Smooth entry progresses toward 4x without overshooting");
        float before = slow.fov(70, true);
        slow.update(settings, world, true, false, 210_000_000);
        check(slow.fov(70, true) > before && slow.fov(70, true) < 70, "Release animates back instead of jumping");
        for (int i = 1; i <= 60; i++) slow.update(settings, world, true, false, 210_000_000L + i * 1_000_000_000L / 60);
        near(slow.fov(70, true), 70, "Transition settles to exact vanilla FOV");
        near(slow.mouse(1), 1, "Transition settles to exact vanilla sensitivity");
        slow.update(settings, world, true, true, 1_220_000_000L);
        slow.update(settings, world, false, true, 1_230_000_000L);
        near(slow.fov(70, true), 70, "Interruptions bypass smoothing to restore the camera immediately");
    }
    private static void configuration() throws Exception {
        ZoomSettings settings = new ZoomSettings();
        settings.key = -1; settings.magnification = Float.NaN; settings.sanitize();
        check(settings.key == 46 && settings.magnification == 4, "Invalid zoom settings recover defaults");
        settings.magnification = 100; settings.sanitize(); check(settings.magnification == 8, "Zoom has an upper bound");
        settings.key = 1; settings.magnification = 0; settings.sanitize();
        check(settings.key == 46 && settings.magnification == 2, "Escape cannot be the zoom binding and magnification has a lower bound");
        Path directory = Files.createTempDirectory("cubi-zoom-test-"); Path file = directory.resolve("config.json");
        try {
            String legacy = "{\"schema\":1,\"themeRevision\":1,\"menuKey\":54,\"modules\":{\"frames\":{\"x\":0.7,\"key\":46}}}";
            Files.write(file, legacy.getBytes(StandardCharsets.UTF_8));
            ClientConfig config = ClientConfig.load(file);
            check(config.zoom.enabled && config.zoom.key == 46 && config.zoom.magnification == 4 && config.zoom.smooth
                    && config.zoom.adaptSensitivity, "Old configurations receive complete zoom defaults");
            check(config.state("frames").x == 0.7f && config.state("frames").key == 46 && !config.zoomKeyAvailable(46),
                    "A legacy HUD binding takes precedence without moving or rebinding the widget");
            check(!config.zoomKeyAvailable(54) && !config.zoomKeyAvailable(0) && config.zoomKeyAvailable(47),
                    "Zoom binding rejects reserved/conflicting keys but accepts a free key");
            config.zoom.key = 47; config.zoom.magnification = 5.5f; config.zoom.enabled = false;
            config.zoom.smooth = false; config.zoom.adaptSensitivity = false;
            config.changed(); check(config.save(), "Zoom preferences save");
            ClientConfig saved = ClientConfig.load(file);
            check(saved.zoom.key == 47 && saved.zoom.magnification == 5.5f && !saved.zoom.enabled && !saved.zoom.smooth
                    && !saved.zoom.adaptSensitivity && saved.state("frames").key == 46, "Zoom preferences and existing binds survive restart");
            java.nio.file.attribute.FileTime modified = Files.getLastModifiedTime(file);
            check(saved.save() && modified.equals(Files.getLastModifiedTime(file)), "Unchanged zoom settings do not rewrite config");
            Files.write(file, "{\"schema\":1,\"modules\":{},\"zoom\":null}".getBytes(StandardCharsets.UTF_8));
            check(ClientConfig.load(file).zoom.key == 46, "Explicit null zoom settings recover safely");
        } finally { Files.deleteIfExists(file); Files.delete(directory); }
    }
}

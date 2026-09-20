package dev.cubi.performance;

/** Snapshot of the vanilla options managed by the performance panel. */
public final class VideoSettings {
    public int distance = 8, fps = 120, clouds = 2, particles, ambientOcclusion = 2;
    public boolean fancy = true, vsync, vbo = true, shadows = true;

    public void sanitize() {
        distance = Math.max(2, Math.min(32, distance));
        fps = Math.max(10, Math.min(260, fps));
        clouds = Math.max(0, Math.min(2, clouds));
        particles = Math.max(0, Math.min(2, particles));
        ambientOcclusion = Math.max(0, Math.min(2, ambientOcclusion));
    }

    public VideoSettings copy() {
        VideoSettings v = new VideoSettings();
        v.distance = distance; v.fps = fps; v.clouds = clouds; v.particles = particles;
        v.ambientOcclusion = ambientOcclusion; v.fancy = fancy; v.vsync = vsync;
        v.vbo = vbo; v.shadows = shadows;
        return v;
    }

    public boolean same(VideoSettings v) {
        return v != null && distance == v.distance && fps == v.fps && clouds == v.clouds
                && particles == v.particles && ambientOcclusion == v.ambientOcclusion
                && fancy == v.fancy && vsync == v.vsync && vbo == v.vbo && shadows == v.shadows;
    }

    /** Conservative on unknown hardware: retain the user's distance, cap and VBO choice. */
    public static VideoSettings preset(VideoSettings current, boolean competitive) {
        VideoSettings v = current.copy();
        v.clouds = competitive ? 0 : Math.min(v.clouds, 1);
        v.particles = competitive ? 1 : 0;
        v.shadows = !competitive;
        v.fancy = !competitive;
        v.ambientOcclusion = competitive ? 0 : 2;
        if (competitive) v.distance = Math.min(v.distance, 8);
        return v;
    }
}

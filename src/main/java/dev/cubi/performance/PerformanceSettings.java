package dev.cubi.performance;

/** Cubi options persist here; vanilla remains the authority for its video options. */
public final class PerformanceSettings {
    public String profile = "balanced";
    public boolean particleCulling = true, hudBatching = true;
    public VideoSettings previousVideo, appliedVideo;
    public boolean previousCulling = true, previousBatching = true;

    public void sanitize() {
        if (!"balanced".equals(profile) && !"competitive".equals(profile) && !"custom".equals(profile)) profile = "custom";
        if (previousVideo != null) previousVideo.sanitize();
        if (appliedVideo != null) appliedVideo.sanitize();
    }

    public void remember(VideoSettings current) {
        if (previousVideo != null) return;
        previousVideo = current.copy();
        previousCulling = particleCulling; previousBatching = hudBatching;
    }

    public void reconcile(VideoSettings current) {
        if (appliedVideo != null && !appliedVideo.same(current)) profile = "custom";
    }
}

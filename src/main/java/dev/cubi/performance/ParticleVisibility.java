package dev.cubi.performance;

/** Camera-relative billboard bounds; no world simulation, allocations or GL calls. */
public final class ParticleVisibility {
    private ParticleVisibility() { }

    public static float center(double previous, double current, float partial, double camera) {
        // Match vanilla's final float conversion, even far from the world origin.
        return (float) (previous + (current - previous) * partial - camera);
    }

    public static boolean visible(float[][] planes, float x, float y, float z, float size,
                                  float rx, float rxz, float rz, float ryz, float rxy) {
        double half = Math.abs(0.1f * size);
        double ex = ((double) Math.abs(rx) + Math.abs(ryz)) * half + 0.002 + 4 * Math.ulp(x);
        double ey = Math.abs(rxz) * half + 0.002 + 4 * Math.ulp(y);
        double ez = ((double) Math.abs(rz) + Math.abs(rxy)) * half + 0.002 + 4 * Math.ulp(z);
        ex += 4 * Math.ulp((float) ex); ey += 4 * Math.ulp((float) ey); ez += 4 * Math.ulp((float) ez);
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
                || !Double.isFinite(ex + ey + ez) || planes == null) return true;
        for (float[] p : planes) {
            double distance = p[0] * (double) x + p[1] * (double) y + p[2] * (double) z + p[3];
            double radius = Math.abs(p[0]) * ex + Math.abs(p[1]) * ey + Math.abs(p[2]) * ez;
            if (distance + radius < -0.002) return false;
        }
        return true;
    }
}

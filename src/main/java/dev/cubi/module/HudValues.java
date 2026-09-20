package dev.cubi.module;

/** Formatting and edge cases shared by live data and headless checks. */
public final class HudValues {
    public static final int EMPTY = -2, UNBREAKABLE = -1;
    private HudValues() { }
    public static int block(double position) { return (int) Math.floor(position); }
    public static String ping(int milliseconds) { return milliseconds < 0 ? "--" : Integer.toString(milliseconds); }
    public static String server(boolean inWorld, boolean local, String address) {
        if (!inWorld) return "Sin conexión";
        if (local) return "Un jugador";
        return address == null || address.trim().isEmpty() ? "Servidor desconocido" : address;
    }
    public static int durability(int maximum, int damage, boolean damageable) {
        if (!damageable || maximum <= 0) return UNBREAKABLE;
        long remaining = Math.max(0L, Math.min((long) maximum, (long) maximum - damage));
        return (int) (remaining * 100 / maximum);
    }
    public static String durabilityLabel(int percent) {
        return percent == EMPTY ? "--" : percent == UNBREAKABLE ? "N/A" : percent + "%";
    }
}

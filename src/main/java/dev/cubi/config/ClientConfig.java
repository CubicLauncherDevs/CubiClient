package dev.cubi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cubi.ui.Theme;
import dev.cubi.performance.PerformanceSettings;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientConfig {
    public static final int SCHEMA = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public int schema = SCHEMA;
    public int menuKey = 54; // LWJGL RSHIFT
    public boolean layoutInitialized;
    public int layoutRevision;
    public int themeRevision;
    public int accent;
    public boolean watermark = true;
    public PerformanceSettings performance = new PerformanceSettings();
    public Map<String, ModuleState> modules = new LinkedHashMap<String, ModuleState>();
    private transient Path file;
    private transient boolean dirty;
    private transient boolean writable = true;
    public transient String status = "Listo";

    public static final class ModuleState {
        public boolean enabled = true;
        public int key;
        public float x = 0.02f, y = 0.03f, scale = 1.0f;
        public boolean background = true, shadow = true;
        public float opacity = 0.48f;
        public int styleRevision;

        public void sanitize() {
            x = finiteClamp(x, 0, 1, 0.02f);
            y = finiteClamp(y, 0, 1, 0.03f);
            scale = finiteClamp(scale, 0.75f, 2.0f, 1);
            opacity = finiteClamp(opacity, 0, 0.85f, 0.48f);
            if (key < 0 || key > 255 || key == 1) key = 0;
        }
    }

    public static float finiteClamp(float number, float minimum, float maximum, float fallback) {
        if (Float.isNaN(number) || Float.isInfinite(number)) return fallback;
        return Math.max(minimum, Math.min(maximum, number));
    }

    public static ClientConfig load(Path file) {
        ClientConfig config = new ClientConfig();
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                ClientConfig parsed = GSON.fromJson(reader, ClientConfig.class);
                if (parsed == null || parsed.modules == null) throw new IOException("Configuración vacía");
                if (parsed.schema != SCHEMA) throw new IOException("Versión de configuración no compatible");
                config = parsed;
                // Gson invokes the constructor, but transient state must remain explicit.
                config.writable = true;
            } catch (IOException | RuntimeException error) {
                try {
                    Path backup = file.resolveSibling(file.getFileName() + ".invalid-" + System.currentTimeMillis());
                    Files.copy(file, backup);
                    config.status = "Configuración recuperada";
                } catch (IOException backupError) {
                    config.writable = false;
                    config.status = "Solo lectura";
                }
                System.err.println("[Cubi] Configuración recuperada: " + error);
            }
        }
        if (config.menuKey <= 1 || config.menuKey > 255) config.menuKey = 54;
        // Apply the requested official preset once without moving widgets or resetting binds.
        // Subsequent accent customizations remain user choices and survive restarts.
        if (config.themeRevision < Theme.REVISION) {
            config.accent = 0;
            config.themeRevision = Theme.REVISION;
            config.dirty = true;
        }
        if (config.accent < 0 || config.accent > 3) config.accent = 0;
        if (config.performance == null) config.performance = new PerformanceSettings();
        config.performance.sanitize();
        config.file = file;
        return config;
    }

    public ModuleState state(String id) {
        ModuleState state = modules.get(id);
        if (state == null) { state = new ModuleState(); modules.put(id, state); dirty = true; }
        state.sanitize();
        return state;
    }

    public void changed() { dirty = true; }

    public boolean save() {
        if (!dirty) return true;
        if (!writable || file == null) return false;
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.write(temporary, GSON.toJson(this).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            status = "Guardado";
            return true;
        } catch (IOException | RuntimeException error) {
            status = "Error al guardar";
            System.err.println("[Cubi] No se pudo guardar " + file + ": " + error);
            return false;
        }
    }
}

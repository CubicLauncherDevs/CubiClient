package dev.cubi.performance;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Main-thread, opt-in capture. Fixed storage allocated on Start, never per frame. */
public final class FrameCapture {
    public static final int TICK = 0, RENDER = 1, PRESENT = 2, LIMIT = 3;
    public static final int CAPACITY = 131072;
    private static final long WARMUP = 5_000_000_000L, DURATION = 60_000_000_000L;
    private final long[] stageStart = new long[4], stageNanos = new long[4], totals = new long[4];
    private long[] frames;
    private int count;
    private long start, elapsed, warmup, gcStartCount, gcStartMillis, gcCount, gcMillis, memoryStart, memoryEnd;
    private boolean active, recording;
    private String reason = "Sin captura";
    private JsonObject context;
    private List<GarbageCollectorMXBean> collectors;
    private Summary summary;

    public void start(JsonObject metadata) {
        if (frames == null) frames = new long[CAPACITY];
        collectors = ManagementFactory.getGarbageCollectorMXBeans();
        context = metadata;
        count = 0; elapsed = 0; warmup = 0; recording = false; summary = null;
        Arrays.fill(totals, 0);
        gcStartCount = gc(false); gcStartMillis = gc(true);
        memoryStart = usedMemory(); memoryEnd = memoryStart;
        active = true; reason = "Calentamiento";
    }

    public boolean active() { return active; }
    public int count() { return count; }
    public String status() {
        return active ? (warmup < WARMUP ? "Calentamiento: " + warmup / 1_000_000_000L + " / 5 s"
                : "Capturando: " + elapsed / 1_000_000_000L + " / 60 s") : reason;
    }

    public void begin(long now, boolean eligible) {
        recording = active && eligible;
        if (!recording) return;
        start = now;
        Arrays.fill(stageNanos, 0);
    }

    public void stageBegin(int stage, long now) { if (recording) stageStart[stage] = now; }
    public void stageEnd(int stage, long now) { if (recording) stageNanos[stage] += Math.max(0, now - stageStart[stage]); }

    public void end(long now, boolean eligible) {
        if (!recording) return;
        recording = false;
        if (!active || !eligible || now - start <= 0) return;
        long nanos = now - start;
        if (warmup < WARMUP) {
            warmup += nanos;
            if (warmup >= WARMUP) {
                // Exclude initialization and warmup GC from the reported capture.
                gcStartCount = gc(false); gcStartMillis = gc(true); memoryStart = usedMemory();
            }
            return;
        }
        frames[count++] = nanos; elapsed += nanos;
        for (int i = 0; i < 4; i++) totals[i] += stageNanos[i];
        if (count == CAPACITY) stop("Buffer completo");
        else if (elapsed >= DURATION) stop("Captura completada");
    }

    public void stop(String why) {
        if (!active) return;
        active = false; recording = false; reason = why;
        gcCount = Math.max(0, gc(false) - gcStartCount);
        gcMillis = Math.max(0, gc(true) - gcStartMillis);
        memoryEnd = usedMemory();
    }

    private long gc(boolean millis) {
        long value = 0;
        for (GarbageCollectorMXBean bean : collectors) value += Math.max(0, millis ? bean.getCollectionTime() : bean.getCollectionCount());
        return value;
    }
    private static long usedMemory() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    /** Called by UI actions, never by the frame hook. */
    public Summary summary() {
        if (active || count == 0) return null;
        if (summary == null) summary = summarize(frames, count);
        return summary;
    }

    public static Summary summarize(long[] values, int length) {
        if (length <= 0 || length > values.length) throw new IllegalArgumentException("Empty or invalid capture");
        long[] sorted = Arrays.copyOf(values, length);
        Arrays.sort(sorted);
        double sum = 0;
        for (long value : sorted) { if (value <= 0) throw new IllegalArgumentException("Invalid frametime"); sum += value; }
        return new Summary(length * 1_000_000_000.0 / sum, sorted[(int) Math.ceil(length * 0.95) - 1] / 1_000_000.0,
                sorted[(int) Math.ceil(length * 0.99) - 1] / 1_000_000.0, sum / 1_000_000_000.0);
    }

    public Path export(Path directory) throws IOException {
        Summary result = summary();
        if (result == null) throw new IOException("Detén una captura con muestras antes de exportar");
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "capture-", ".tmp");
        Path output = temporary.resolveSibling(temporary.getFileName().toString().replace(".tmp", ".json"));
        try {
            try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))) {
                writer.beginObject();
                writer.name("schema").value(1); writer.name("version").value("0.0.1");
                writer.name("reason").value(reason); writer.name("context");
                new com.google.gson.Gson().toJson(context, writer);
                writer.name("samples").value(count); writer.name("seconds").value(result.seconds);
                writer.name("averageFps").value(result.fps); writer.name("p95Millis").value(result.p95);
                writer.name("p99Millis").value(result.p99);
                writer.name("gcCollections").value(gcCount); writer.name("gcMillis").value(gcMillis);
                writer.name("heapStartBytes").value(memoryStart); writer.name("heapEndBytes").value(memoryEnd);
                writer.name("stageTotalNanos").beginArray();
                for (long total : totals) writer.value(total);
                writer.endArray(); writer.name("frameNanos").beginArray();
                for (int i = 0; i < count; i++) writer.value(frames[i]);
                writer.endArray(); writer.endObject();
            }
            Files.move(temporary, output);
            return output;
        } finally { Files.deleteIfExists(temporary); }
    }

    public String stageSummary() {
        if (count == 0) return "Sin muestras de partida";
        return "Tick " + millis(totals[0]) + " / Render " + millis(totals[1])
                + " / Presentación " + millis(totals[2]) + " / Límite " + millis(totals[3]) + " ms";
    }
    private long millis(long total) { return Math.round(total / (double) count / 1_000_000.0); }
    public String memorySummary() {
        return "Heap " + memoryStart / 1048576 + " -> " + memoryEnd / 1048576
                + " MiB / GC " + gcCount + " veces, " + gcMillis + " ms";
    }
    public static final class Summary {
        public final double fps, p95, p99, seconds;
        Summary(double fps, double p95, double p99, double seconds) {
            this.fps = fps; this.p95 = p95; this.p99 = p99; this.seconds = seconds;
        }
    }
}

package dev.cubi.tests;

import dev.cubi.build.ScreenGenerator;
import dev.cubi.config.ClientConfig;
import dev.cubi.core.ClickWindow;
import dev.cubi.core.MenuActivation;
import dev.cubi.launch.CubiTransformer;
import dev.cubi.launch.CubiTweaker;
import dev.cubi.ui.HudPlacement;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

public final class SelfTest {
    private static int assertions;
    private SelfTest() { }

    public static void main(String[] args) throws Exception {
        clicks();
        configuration();
        placement();
        menuActivation();
        menuDispatchContract();
        arguments();
        try (ZipFile vanilla = new ZipFile(args[0])) {
            integration(vanilla);
        }
        System.out.println("PASS / " + assertions + " comprobaciones: CPS, configuración, argumentos, mappings y bytecode 1.8.9.");
    }

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static void clicks() {
        ClickWindow counter = new ClickWindow();
        check(counter.count(0) == 0, "Empty counter");
        counter.press(0); counter.press(10_000_000); counter.press(900_000_000);
        check(counter.count(999_999_999) == 3, "Multiple clicks within one tick are counted");
        check(counter.count(1_000_000_000) == 2, "Exactly one-second-old clicks expire");
        check(counter.count(1_010_000_000) == 1, "Rolling rather than tumbling window");
        check(counter.count(1_900_000_000) == 0, "Idle window empties");
        for (int i = 0; i < 1000; i++) counter.press(2_000_000_000L + i);
        check(counter.count(2_000_001_000L) == 256, "Bounded ring wraps safely");
        check(counter.count(3_000_001_000L) == 0, "Wrapped ring expires");
        counter.press(-200); counter.press(-100);
        check(counter.count(0) == 2, "nanoTime need not be positive");
        counter.clear();
        check(counter.count(0) == 0, "Entering a GUI clears old input");
    }

    private static void configuration() throws Exception {
        Path directory = Files.createTempDirectory("cubi-config-test-");
        try {
            Path file = directory.resolve("config.json");
            ClientConfig config = ClientConfig.load(file);
            ClientConfig.ModuleState state = config.state("frames");
            state.enabled = false; state.x = 0.6f; state.y = 0.4f; state.scale = 1.5f; state.key = 37;
            config.menuKey = 50; config.accent = 2; config.watermark = false;
            state.background = false; state.shadow = false; state.opacity = 0.65f;
            config.changed();
            check(config.save(), "Configuration saves");
            ClientConfig loaded = ClientConfig.load(file);
            check(!loaded.state("frames").enabled && loaded.state("frames").key == 37, "Enabled and binding round-trip");
            check(loaded.menuKey == 50 && loaded.state("frames").x == 0.6f && loaded.state("frames").scale == 1.5f, "Layout and menu binding round-trip");
            check(loaded.accent == 2 && !loaded.watermark && !loaded.state("frames").background
                    && !loaded.state("frames").shadow && loaded.state("frames").opacity == 0.65f, "Visual settings round-trip");
            java.nio.file.attribute.FileTime modified = Files.getLastModifiedTime(file);
            check(loaded.save() && modified.equals(Files.getLastModifiedTime(file)), "Clean configuration causes no disk write");
            state.x = Float.NaN; state.y = -10; state.scale = Float.POSITIVE_INFINITY; state.key = 300; state.opacity = Float.NaN;
            state.sanitize();
            check(state.x == 0.02f && state.y == 0 && state.scale == 1 && state.key == 0, "Untrusted config numbers are clamped");
            check(state.opacity == 0.48f, "Invalid opacity recovers");
            Files.write(file, "{broken json".getBytes(StandardCharsets.UTF_8));
            ClientConfig recovered = ClientConfig.load(file);
            check(recovered.state("keys").enabled, "Corrupt config falls back to defaults");
            try (java.util.stream.Stream<Path> files = Files.list(directory)) {
                check(files.anyMatch(p -> p.getFileName().toString().contains(".invalid-")), "Corrupt config is backed up");
            }
            check(new String(Files.readAllBytes(file), StandardCharsets.UTF_8).equals("{broken json"), "Loading does not overwrite corrupt data");
            Files.write(file, "{\"schema\":1,\"menuKey\":1,\"modules\":{\"frames\":null}}".getBytes(StandardCharsets.UTF_8));
            ClientConfig nullable = ClientConfig.load(file);
            check(nullable.menuKey == 54 && nullable.state("frames") != null, "Null module and reserved menu key recover");
            Files.write(file, "{\"schema\":1,\"accent\":99,\"layoutInitialized\":true,\"modules\":{\"frames\":{\"x\":0.6,\"y\":0.4,\"scale\":1.5,\"key\":37}}}".getBytes(StandardCharsets.UTF_8));
            ClientConfig legacy = ClientConfig.load(file);
            check(legacy.accent == 0 && legacy.state("frames").background && legacy.state("frames").shadow
                    && legacy.state("frames").opacity == 0.48f, "Schema-1 configuration gets new visual defaults");
            check(legacy.state("frames").x == 0.6f && legacy.state("frames").key == 37 && legacy.layoutInitialized,
                    "Legacy custom placement and binding survive loading");
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                Path[] ordered = paths.sorted(java.util.Comparator.reverseOrder()).toArray(Path[]::new);
                for (Path path : ordered) Files.delete(path);
            }
        }
    }

    private static void placement() {
        check(HudPlacement.snap(13, 70, 320, 4) == 12, "Snap to screen margin");
        check(HudPlacement.snap(124, 70, 320, 4) == 125, "Snap to screen center");
        check(HudPlacement.snap(239, 70, 320, 4) == 238, "Snap to right margin");
        check(HudPlacement.snap(124, 70, 320, 0) == 124, "Fine adjustment bypasses magnetism");
        check(HudPlacement.snap(-20, 70, 320, 0) == 0 && HudPlacement.snap(300, 70, 320, 0) == 250, "Placement remains on-screen");
        check(HudPlacement.snap(12, 400, 320, 4) == 0, "Oversized widget remains anchored");
    }

    private static void menuActivation() {
        MenuActivation activation = new MenuActivation();
        check(!activation.take(null), "No menu opens without a key request");
        Object mainMenu = new Object(), chat = new Object();
        activation.request(null);
        check(activation.take(null), "An in-world request opens at the end of input dispatch");
        check(!activation.take(null), "The same request cannot reopen a closed menu");
        activation.request(mainMenu);
        check(activation.take(mainMenu), "Main menu opening uses the same deferred path");
        activation.request(null);
        check(!activation.take(chat), "A pending request must not overwrite a newly opened chat or inventory");
        check(!activation.take(null), "A cancelled request cannot reopen on a later tick");
        activation.request(null);
        activation.request(null);
        check(activation.take(null) && !activation.take(null), "Several key events in one tick yield one opening");
    }

    private static void menuDispatchContract() throws Exception {
        // Inspect the shipping class without initializing Minecraft/LWJGL or a display.
        ClassNode client = new ClassNode();
        new ClassReader("dev.cubi.core.CubiClient").accept(client, 0);
        boolean requestsFromKey = false, opensInKey = false, opensAfterTake = false;
        for (Object entry : client.methods) {
            MethodNode method = (MethodNode) entry;
            boolean tookRequest = false;
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (method.name.equals("key")) {
                    requestsFromKey |= call.owner.equals("dev/cubi/core/MenuActivation") && call.name.equals("request");
                    opensInKey |= call.owner.equals("dev/cubi/core/CubiClient") && call.name.equals("open");
                }
                if (method.name.equals("tick")) {
                    tookRequest |= call.owner.equals("dev/cubi/core/MenuActivation") && call.name.equals("take");
                    opensAfterTake |= tookRequest && call.owner.equals("dev/cubi/core/CubiClient") && call.name.equals("open");
                }
            }
        }
        check(requestsFromKey, "Keyboard handler queues menu activation");
        check(!opensInKey, "Keyboard dispatch must not open a GUI before vanilla forwards the same event");
        check(opensAfterTake, "Tick commits the queued GUI change after input dispatch");
    }

    private static void arguments() {
        CubiTweaker tweaker = new CubiTweaker();
        tweaker.acceptOptions(Arrays.asList("--username", "Player", "--accessToken", "test-token"),
                new java.io.File("game space"), new java.io.File("assets"), "CubiClient-1.8.9");
        List<String> result = Arrays.asList(tweaker.getLaunchArguments());
        check(result.contains("test-token") && result.contains("--gameDir") && result.contains("CubiClient-1.8.9"), "Launcher arguments are preserved");
        check(tweaker.getLaunchTarget().equals("net.minecraft.client.main.Main"), "Vanilla launch entry");
    }

    private static byte[] bytes(ZipFile jar, String name) throws Exception {
        try (InputStream stream = jar.getInputStream(jar.getEntry(name + ".class")); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] block = new byte[8192]; int size;
            while ((size = stream.read(block)) != -1) out.write(block, 0, size);
            return out.toByteArray();
        }
    }

    private static ClassNode node(ZipFile jar, String name) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(bytes(jar, name)).accept(node, 0);
        return node;
    }

    private static void method(ClassNode owner, String name, String descriptor) {
        for (Object entry : owner.methods) {
            MethodNode method = (MethodNode) entry;
            if (method.name.equals(name) && method.desc.equals(descriptor)) { check(true, "Method found"); return; }
        }
        check(false, "Missing method " + owner.name + "." + name + descriptor);
    }

    private static void field(ClassNode owner, String name, String descriptor) {
        for (Object entry : owner.fields) {
            FieldNode field = (FieldNode) entry;
            if (field.name.equals(name) && field.desc.equals(descriptor)) { check(true, "Field found"); return; }
        }
        check(false, "Missing field " + owner.name + "." + name + ":" + descriptor);
    }

    private static void integration(ZipFile vanilla) throws Exception {
        CubiTransformer transformer = new CubiTransformer();
        for (String name : new String[] {"ave", "avo", "aya"}) {
            byte[] original = bytes(vanilla, name);
            byte[] transformed = transformer.transform(name, name, original);
            new ClassReader(transformed).accept(new CheckClassAdapter(new ClassWriter(0), true), 0);
            ClassNode output = new ClassNode();
            new ClassReader(transformed).accept(output, 0);
            int hooks = 0;
            for (Object item : output.methods) {
                for (AbstractInsnNode instruction = ((MethodNode) item).instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                    if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("dev/cubi/core/Hooks")) hooks++;
                }
            }
            check(hooks >= (name.equals("ave") ? 3 : 1), "Hooks are present in " + name);
            check(!Arrays.equals(original, transformed), "Transformation changes target " + name);
        }
        byte[] guiBytes = bytes(vanilla, "axu");
        check(transformer.transform("axu", "axu", guiBytes) == guiBytes, "Unrelated classes remain untouched");
        check(transformer.transform("missing", "missing", null) == null, "Missing classes remain missing");
        ClassNode mc = node(vanilla, "ave");
        method(mc, "A", "()Lave;"); method(mc, "a", "(Laxu;)V"); method(mc, "ai", "()I");
        field(mc, "k", "Lavn;"); field(mc, "t", "Lavh;"); field(mc, "v", "Ljava/io/File;");
        field(mc, "m", "Laxu;"); field(mc, "f", "Lbdb;");
        ClassNode settings = node(vanilla, "avh");
        field(settings, "aA", "Z"); field(settings, "aC", "Z");
        for (String name : new String[] {"Y", "aa", "Z", "ab", "ac"}) field(settings, name, "Lavb;");
        method(node(vanilla, "avb"), "i", "()I");
        method(node(vanilla, "avn"), "a", "(Ljava/lang/String;FFI)I");
        method(node(vanilla, "avn"), "a", "(Ljava/lang/String;)I");
        method(node(vanilla, "avp"), "a", "(IIIII)V");
        method(node(vanilla, "bfl"), "c", "(FFFF)V");
        method(node(vanilla, "bfl"), "i", "(I)V");
        method(node(vanilla, "bfl"), "a", "(IIII)V");
        for (String name : new String[] {"w", "l", "k", "d", "c"}) method(node(vanilla, "bfl"), name, "()V");
        ClassNode resolution = node(vanilla, "avr");
        method(resolution, "<init>", "(Lave;)V"); method(resolution, "a", "()I"); method(resolution, "b", "()I");
        byte[] screen = ScreenGenerator.bytecode();
        new ClassReader(screen).accept(new CheckClassAdapter(new ClassWriter(0), true), 0);
        ClassNode generated = new ClassNode();
        new ClassReader(screen).accept(generated, 0);
        ClassNode parent = node(vanilla, "axu");
        for (Object item : generated.methods) {
            MethodNode override = (MethodNode) item;
            method(parent, override.name, override.desc);
            check((override.access & Opcodes.ACC_PUBLIC) != 0, "Generated overrides accessible");
        }
        System.out.println("PASS / Transformadores y GuiScreen analizados sobre el cliente original de Mojang.");
    }
}
